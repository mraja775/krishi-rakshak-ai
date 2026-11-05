import {onDocumentCreated} from "firebase-functions/v2/firestore";
import {onMessagePublished} from "firebase-functions/v2/pubsub";
import {GoogleAuth} from "google-auth-library";
import * as admin from "firebase-admin";
import {Storage} from "@google-cloud/storage";
import {VertexAI} from "@google-cloud/vertexai";

admin.initializeApp();

const storage = new Storage();
const firestore = admin.firestore();

// Vertex AI configuration
const PROJECT_ID = process.env.GCLOUD_PROJECT || "";
const LOCATION = "us-central1"; // or your preferred region
// See https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/model-versions
// gemini-3.8-flash is supported until at least 2027-09-02.
const GEMINI_MODEL = process.env.GEMINI_MODEL || "gemini-3.8-flash";
// Newer Gemini models are served from the global endpoint only.
const VERTEX_LOCATION = process.env.VERTEX_LOCATION || "global";

// Abuse / cost controls
const ALLOWED_CONTENT_TYPES = ["image/jpeg", "image/png", "image/webp"];
const MAX_IMAGE_BYTES = 5 * 1024 * 1024; // 5 MB
const RATE_LIMIT_WINDOW_MS = 60 * 60 * 1000; // 1 hour
const RATE_LIMIT_MAX_ANALYSES = 20; // per user per window
// Project-wide ceiling on Gemini calls per UTC day (hard cost backstop).
const GLOBAL_DAILY_MAX_ANALYSES =
  Number(process.env.GLOBAL_DAILY_MAX_ANALYSES) || 500;

// Kill switch / budget guard (see docs/COST_CONTROLS.md)
const SYSTEM_COLLECTION = "_system";
const CONFIG_DOC = "config";
const CONFIG_CACHE_MS = 60 * 1000;
const BUDGET_TOPIC = process.env.BUDGET_TOPIC || "billing-budget-alerts";
const BUDGET_SOFT_KILL_RATIO = 0.8; // disable AI at 80% of budget
const BUDGET_HARD_KILL = process.env.BUDGET_HARD_KILL === "true";

// Limits applied to AI output before it is written to Firestore
const MAX_PLANT_TYPE_LEN = 100;
const MAX_TEXT_LEN = 500;
const MAX_DESCRIPTION_LEN = 2000;
const MAX_LIST_ITEMS = 10;
const SEVERITIES = ["LOW", "MODERATE", "SEVERE", "CRITICAL"] as const;
const DEFAULT_SEVERITY = "MODERATE";

type AnalysisRef = FirebaseFirestore.DocumentReference;

// Stale PENDING docs older than this are marked FAILED when the same user
// starts a new analysis (e.g. the app was killed mid-upload).
const STALE_PENDING_MS = 5 * 60 * 1000;
// Upload filenames the Storage rules allow.
const SAFE_FILENAME = /^[A-Za-z0-9_.-]{1,200}$/;

/**
 * Runs when the app creates a PENDING analysis doc. The app only creates
 * the doc after the image upload has finished, so the image is always
 * there. (The old Storage trigger raced the doc creation and sometimes
 * gave up before the doc existed, leaving the app stuck on "analyzing".)
 */
export const analyzePlantImage = onDocumentCreated(
  {
    document: "analyses/{analysisId}",
    timeoutSeconds: 120,
    memory: "1GiB",
    maxInstances: 10,
    region: LOCATION,
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const analysisRef: AnalysisRef = snap.ref;
    const data = snap.data();

    try {
      // Triggers are at-least-once: claim the doc so a redelivered event
      // can never pay for a second Gemini call.
      if (!(await claimPending(analysisRef))) {
        console.log("Analysis not PENDING (duplicate event?):", snap.id);
        return;
      }
      console.log("Processing analysis:", snap.id);

      const userId = data.userId;
      if (typeof userId !== "string" || userId.length === 0) {
        await markFailed(analysisRef, "Invalid analysis");
        return;
      }

      // The rules already check this, but never trust a client-written URL.
      const image = parseOwnImageUrl(data.imageUrl, userId);
      if (!image) {
        await markFailed(analysisRef, "Invalid image reference");
        return;
      }

      await expireStalePending(userId, analysisRef.id);

      // Validate type and size before downloading anything
      const file = storage.bucket(image.bucket).file(image.path);
      const [exists] = await file.exists();
      if (!exists) {
        await markFailed(analysisRef, "Image not found");
        return;
      }
      const [metadata] = await file.getMetadata();
      const contentType = metadata.contentType || "";
      const size = Number(metadata.size) || 0;
      if (!ALLOWED_CONTENT_TYPES.includes(contentType)) {
        await rejectUpload(
          analysisRef, image.bucket, image.path, "Unsupported image type"
        );
        return;
      }
      if (size > MAX_IMAGE_BYTES) {
        await rejectUpload(
          analysisRef, image.bucket, image.path, "Image too large (max 5 MB)"
        );
        return;
      }

      // Per-user rate limit (counts the current analysis too)
      const windowStart = Date.now() - RATE_LIMIT_WINDOW_MS;
      const countSnap = await firestore
        .collection("analyses")
        .where("userId", "==", userId)
        .where("timestamp", ">", windowStart)
        .count()
        .get();
      if (countSnap.data().count > RATE_LIMIT_MAX_ANALYSES) {
        console.warn("Rate limit exceeded for analysis:", analysisRef.id);
        await markFailed(analysisRef, "Rate limit exceeded");
        return;
      }

      // Kill switch (set manually or by budgetGuard)
      if (!(await isAiEnabled())) {
        console.warn("AI disabled by kill switch:", analysisRef.id);
        await markFailed(analysisRef, "Analysis temporarily disabled");
        return;
      }

      // Project-wide daily cap
      if (!(await reserveDailySlot())) {
        console.warn("Global daily cap reached:", analysisRef.id);
        await markFailed(
          analysisRef,
          "Service is at capacity today, please try again tomorrow"
        );
        return;
      }

      await processAnalysis(analysisRef, image.bucket, image.path, contentType);
      console.log("Analysis completed successfully:", analysisRef.id);
    } catch (error) {
      console.error("Error analyzing image:", error);
      await markFailed(
        analysisRef,
        error instanceof Error ? error.message : "Unknown error"
      ).catch((e) => console.error("Failed to mark analysis FAILED:", e));
    }
  }
);

/**
 * Atomically move the doc from PENDING to PROCESSING.
 * @param {AnalysisRef} ref - Analysis doc
 * @return {Promise<boolean>} true if this invocation claimed it
 */
async function claimPending(ref: AnalysisRef): Promise<boolean> {
  return firestore.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists || snap.get("status") !== "PENDING") return false;
    tx.update(ref, {status: "PROCESSING"});
    return true;
  });
}

/**
 * Parse a Firebase Storage download URL and accept it only if it points
 * at images/{userId}/{file} in this project's default bucket.
 * @param {unknown} imageUrl - URL written by the client
 * @param {string} userId - Owner of the analysis
 * @return {{bucket: string, path: string} | null} object location or null
 */
export function parseOwnImageUrl(
  imageUrl: unknown,
  userId: string
): {bucket: string, path: string} | null {
  if (typeof imageUrl !== "string" || imageUrl.length > 2048) return null;
  let url: URL;
  try {
    url = new URL(imageUrl);
  } catch {
    return null;
  }
  if (url.protocol !== "https:" ||
      url.hostname !== "firebasestorage.googleapis.com") {
    return null;
  }
  const m = url.pathname.match(/^\/v0\/b\/([^/]+)\/o\/([^/]+)$/);
  if (!m) return null;
  const bucket = m[1];
  let path: string;
  try {
    path = decodeURIComponent(m[2]);
  } catch {
    return null;
  }
  if (bucket !== defaultBucket()) return null;
  const parts = path.split("/");
  if (parts.length !== 3 || parts[0] !== "images" || parts[1] !== userId ||
      !SAFE_FILENAME.test(parts[2]) || parts[2].includes("..")) {
    return null;
  }
  return {bucket, path};
}

/**
 * This project's default Storage bucket name.
 * @return {string} bucket name
 */
function defaultBucket(): string {
  return process.env.STORAGE_BUCKET_OVERRIDE || admin.storage().bucket().name;
}

/**
 * Best-effort: mark this user's other PENDING docs older than
 * STALE_PENDING_MS as FAILED so the app stops waiting on them.
 * @param {string} userId - Owner uid
 * @param {string} currentId - The analysis being processed now
 * @return {Promise<void>}
 */
async function expireStalePending(
  userId: string,
  currentId: string
): Promise<void> {
  try {
    const stale = await firestore
      .collection("analyses")
      .where("userId", "==", userId)
      .where("status", "==", "PENDING")
      .where("timestamp", "<", Date.now() - STALE_PENDING_MS)
      .orderBy("timestamp", "desc")
      .limit(20)
      .get();
    const docs = stale.docs.filter((d) => d.id !== currentId);
    if (docs.length === 0) return;
    console.log(`Expiring ${docs.length} stale pending analyses`);
    const batch = firestore.batch();
    docs.forEach((d) => batch.update(d.ref, {
      status: "FAILED",
      error: "Timed out - please try again",
    }));
    await batch.commit();
  } catch (e) {
    console.warn("Could not expire stale pending analyses:", e);
  }
}

/**
 * Download the image, run Gemini, and write sanitized results.
 * @param {AnalysisRef} ref - Analysis doc being processed
 * @param {string} bucket - Storage bucket name
 * @param {string} filePath - Object path
 * @param {string} contentType - Validated image MIME type
 * @return {Promise<void>}
 */
async function processAnalysis(
  ref: AnalysisRef,
  bucket: string,
  filePath: string,
  contentType: string
): Promise<void> {
  const [imageBuffer] = await storage.bucket(bucket).file(filePath).download();
  const aiResult = await analyzeWithGemini(
    imageBuffer.toString("base64"),
    contentType
  );

  await ref.update({
    plantType: aiResult.plantType,
    diseaseInfo: aiResult.diseaseInfo,
    confidence: aiResult.confidence,
    recommendations: aiResult.recommendations,
    status: "COMPLETED",
    completedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
}

/**
 * Mark a single analysis doc as FAILED.
 * @param {AnalysisRef} ref - Analysis doc
 * @param {string} message - Error message shown to the user
 * @return {Promise<void>}
 */
async function markFailed(ref: AnalysisRef, message: string): Promise<void> {
  await ref.update({status: "FAILED", error: message});
}

/**
 * Mark the analysis FAILED and delete the rejected upload.
 * @param {AnalysisRef} ref - Analysis doc
 * @param {string} bucket - Storage bucket name
 * @param {string} filePath - Object path
 * @param {string} message - Error message
 * @return {Promise<void>}
 */
async function rejectUpload(
  ref: AnalysisRef,
  bucket: string,
  filePath: string,
  message: string
): Promise<void> {
  console.warn(`Rejecting upload for analysis ${ref.id}: ${message}`);
  await markFailed(ref, message);
  await storage.bucket(bucket).file(filePath).delete({ignoreNotFound: true});
}

/**
 * Disease information structure
 */
interface DiseaseInfo {
  name: string;
  scientificName: string | null;
  severity: typeof SEVERITIES[number];
  description: string;
  symptoms: string[];
  causes: string[];
  preventiveMeasures: string[];
}

/**
 * Analysis result structure
 */
export interface AnalysisResult {
  plantType: string;
  diseaseInfo: DiseaseInfo | null;
  confidence: number;
  recommendations: string[];
}

/**
 * Trim a value to a capped string, or return null if not a usable string.
 * @param {unknown} value - Raw value
 * @param {number} maxLen - Maximum length
 * @return {string | null} sanitized string or null
 */
function cleanString(value: unknown, maxLen: number): string | null {
  if (typeof value !== "string") return null;
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed.slice(0, maxLen) : null;
}

/**
 * Keep only non-empty strings, capped in count and length.
 * @param {unknown} value - Raw value
 * @return {string[]} sanitized list
 */
function cleanStringList(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return value
    .map((v) => cleanString(v, MAX_TEXT_LEN))
    .filter((v): v is string => v !== null)
    .slice(0, MAX_LIST_ITEMS);
}

/**
 * Validate and coerce raw Gemini output into a safe AnalysisResult.
 * Throws if the output lacks a usable plantType or confidence.
 * @param {unknown} raw - Parsed JSON from the model
 * @return {AnalysisResult} sanitized result
 */
export function sanitizeResult(raw: unknown): AnalysisResult {
  if (typeof raw !== "object" || raw === null || Array.isArray(raw)) {
    throw new Error("Invalid response structure from Gemini");
  }
  const r = raw as Record<string, unknown>;

  const plantType = cleanString(r.plantType, MAX_PLANT_TYPE_LEN);
  if (!plantType) {
    throw new Error("Invalid response structure from Gemini: plantType");
  }

  const conf = typeof r.confidence === "string" ?
    Number(r.confidence) : r.confidence;
  if (typeof conf !== "number" || !Number.isFinite(conf)) {
    throw new Error("Invalid response structure from Gemini: confidence");
  }
  const confidence = Math.min(1, Math.max(0, conf));

  let diseaseInfo: DiseaseInfo | null = null;
  const d = r.diseaseInfo;
  if (typeof d === "object" && d !== null && !Array.isArray(d)) {
    const di = d as Record<string, unknown>;
    const name = cleanString(di.name, MAX_TEXT_LEN);
    // A diseaseInfo with no name means "healthy" for the client
    if (name) {
      const sev = typeof di.severity === "string" ?
        di.severity.trim().toUpperCase() : "";
      const severity = (SEVERITIES as readonly string[]).includes(sev) ?
        sev as DiseaseInfo["severity"] : DEFAULT_SEVERITY;
      diseaseInfo = {
        name,
        scientificName: cleanString(di.scientificName, MAX_TEXT_LEN),
        severity,
        description: cleanString(di.description, MAX_DESCRIPTION_LEN) || "",
        symptoms: cleanStringList(di.symptoms),
        causes: cleanStringList(di.causes),
        preventiveMeasures: cleanStringList(di.preventiveMeasures),
      };
    }
  }

  return {
    plantType,
    diseaseInfo,
    confidence,
    recommendations: cleanStringList(r.recommendations),
  };
}

/**
 * Analyze plant image using Vertex AI Gemini
 * @param {string} base64Image - Base64 encoded image data
 * @param {string} mimeType - Image MIME type
 * @return {Promise<AnalysisResult>} AI analysis result
 */
async function analyzeWithGemini(
  base64Image: string,
  mimeType: string
): Promise<AnalysisResult> {
  const vertexAI = new VertexAI({
    project: PROJECT_ID,
    location: VERTEX_LOCATION,
    // The SDK builds "<location>-aiplatform..." which is wrong for global.
    ...(VERTEX_LOCATION === "global" ?
      {apiEndpoint: "aiplatform.googleapis.com"} : {}),
  });

  const model = vertexAI.getGenerativeModel({
    model: GEMINI_MODEL,
  });

  const prompt = `
You are an expert agricultural AI assistant specializing in plant
disease detection and diagnosis.

Analyze the provided plant image and provide a comprehensive
assessment in JSON format.

**Your response must be valid JSON matching this exact structure:**

{
  "plantType": "Common name (e.g., Tomato, Rice, Wheat)",
  "diseaseInfo": {
    "name": "Disease name (e.g., Late Blight) or null if healthy",
    "scientificName": "Scientific name of disease or null",
    "severity": "LOW | MODERATE | SEVERE | CRITICAL",
    "description": "Brief description of the disease",
    "symptoms": ["Symptom 1", "Symptom 2", "Symptom 3"],
    "causes": ["Cause 1", "Cause 2"],
    "preventiveMeasures": ["Prevention 1", "Prevention 2", "Prevention 3"]
  },
  "confidence": 0.95,
  "recommendations": [
    "Actionable recommendation 1",
    "Actionable recommendation 2",
    "Actionable recommendation 3"
  ]
}

**If the plant is healthy (no disease detected):**
- Set diseaseInfo to null
- Provide general care recommendations
- Set confidence based on your certainty that the plant is healthy

**Important:**
- Severity levels: LOW (minor), MODERATE (needs attention),
  SEVERE (urgent), CRITICAL (emergency)
- Confidence: 0.0 to 1.0 (e.g., 0.85 = 85% confident)
- Recommendations: practical, actionable advice for farmers
- Be specific and accurate based on visible symptoms in the image

Return ONLY the JSON object, no additional text.
`;

  const request = {
    contents: [{
      role: "user",
      parts: [
        {text: prompt},
        {inlineData: {data: base64Image, mimeType}},
      ],
    }],
    generationConfig: {
      temperature: 0.2, // Low temp for consistent results
      maxOutputTokens: 2048,
      topP: 0.95,
      topK: 40,
      responseMimeType: "application/json",
    },
  };

  const response = await model.generateContent(request);
  const result = response.response;

  if (!result.candidates || result.candidates.length === 0) {
    throw new Error("No response from Gemini");
  }

  const parts = result.candidates[0].content?.parts || [];
  const content = parts.map((p) => p.text || "").join("");

  // Extract JSON (defensive: the model might still wrap it in markdown)
  const jsonMatch = content.match(/\{[\s\S]*\}/);
  let parsed: unknown;
  try {
    if (!jsonMatch) throw new Error("no JSON object found");
    parsed = JSON.parse(jsonMatch[0]);
  } catch (e) {
    console.error(
      `Unparseable Gemini response (length ${content.length}):`,
      content.slice(0, 200)
    );
    throw new Error("Failed to extract JSON from Gemini response");
  }

  return sanitizeResult(parsed);
}

// ---------------------------------------------------------------------------
// Spend guards
// ---------------------------------------------------------------------------

let configCache: {aiEnabled: boolean, fetchedAt: number} | null = null;

/**
 * Whether AI analysis is enabled (`_system/config.aiEnabled !== false`).
 * Cached in memory for CONFIG_CACHE_MS to avoid a read per upload.
 * @return {Promise<boolean>} true unless explicitly disabled
 */
async function isAiEnabled(): Promise<boolean> {
  const now = Date.now();
  if (configCache && now - configCache.fetchedAt < CONFIG_CACHE_MS) {
    return configCache.aiEnabled;
  }
  const snap = await firestore
    .collection(SYSTEM_COLLECTION)
    .doc(CONFIG_DOC)
    .get();
  const aiEnabled = snap.get("aiEnabled") !== false;
  configCache = {aiEnabled, fetchedAt: now};
  return aiEnabled;
}

/**
 * Atomically reserve one Gemini call against today's (UTC) global cap.
 * @return {Promise<boolean>} false if the cap has been reached
 */
async function reserveDailySlot(): Promise<boolean> {
  const day = new Date().toISOString().slice(0, 10).replace(/-/g, "");
  const ref = firestore.collection(SYSTEM_COLLECTION).doc(`usage_${day}`);
  return firestore.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const count = Number(snap.get("count")) || 0;
    if (count >= GLOBAL_DAILY_MAX_ANALYSES) {
      return false;
    }
    tx.set(ref, {
      count: count + 1,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});
    return true;
  });
}

/**
 * Cloud Billing budget notification payload (subset).
 * https://cloud.google.com/billing/docs/how-to/budgets-programmatic-notifications
 */
interface BudgetNotification {
  costAmount?: number;
  budgetAmount?: number;
  budgetDisplayName?: string;
  currencyCode?: string;
}

/**
 * Budget guard. Wire a Cloud Billing budget to the Pub/Sub topic
 * BUDGET_TOPIC. At >= 80% of budget it turns AI analysis off via the
 * kill switch. At >= 100% it disables billing for the project, but only
 * if BUDGET_HARD_KILL=true. It never re-enables anything automatically.
 */
export const budgetGuard = onMessagePublished(
  {
    topic: BUDGET_TOPIC,
    region: LOCATION,
    maxInstances: 1,
    timeoutSeconds: 60,
    memory: "256MiB",
  },
  async (event) => {
    const data = event.data.message.json as BudgetNotification | undefined;
    const cost = Number(data?.costAmount);
    const budget = Number(data?.budgetAmount);
    if (!Number.isFinite(cost) || !Number.isFinite(budget) || budget <= 0) {
      console.warn("budgetGuard: ignoring malformed notification");
      return;
    }
    const ratio = cost / budget;
    console.log(
      `budgetGuard: ${data?.budgetDisplayName ?? "budget"} at ` +
      `${(ratio * 100).toFixed(1)}% (${cost} / ${budget} ` +
      `${data?.currencyCode ?? ""})`
    );

    if (ratio >= BUDGET_SOFT_KILL_RATIO) {
      await firestore.collection(SYSTEM_COLLECTION).doc(CONFIG_DOC).set({
        aiEnabled: false,
        disabledReason: `Budget at ${(ratio * 100).toFixed(1)}%`,
        disabledAt: admin.firestore.FieldValue.serverTimestamp(),
      }, {merge: true});
      configCache = null;
      console.warn("budgetGuard: AI analysis disabled (kill switch set)");
    }

    if (ratio >= 1) {
      if (BUDGET_HARD_KILL) {
        await disableProjectBilling();
      } else {
        console.warn(
          "budgetGuard: budget exceeded; BUDGET_HARD_KILL is not 'true', " +
          "so billing was NOT disabled"
        );
      }
    }
  }
);

/**
 * Detach the billing account from this project. This stops ALL paid
 * services, so it is opt-in via BUDGET_HARD_KILL. Requires the function's
 * service account to have Billing Project Manager on the project and
 * permission to unlink billing (see docs/COST_CONTROLS.md).
 * @return {Promise<void>}
 */
async function disableProjectBilling(): Promise<void> {
  const auth = new GoogleAuth({
    scopes: ["https://www.googleapis.com/auth/cloud-billing"],
  });
  const client = await auth.getClient();
  const name = `projects/${PROJECT_ID}`;
  const info = await client.request<{billingEnabled?: boolean}>({
    url: `https://cloudbilling.googleapis.com/v1/${name}/billingInfo`,
  });
  if (!info.data.billingEnabled) {
    console.log("budgetGuard: billing already disabled");
    return;
  }
  await client.request({
    url: `https://cloudbilling.googleapis.com/v1/${name}/billingInfo`,
    method: "PUT",
    data: {billingAccountName: ""},
  });
  console.error("budgetGuard: BILLING DISABLED for", name);
}
