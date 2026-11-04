import {onObjectFinalized} from "firebase-functions/v2/storage";
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

// Limits applied to AI output before it is written to Firestore
const MAX_PLANT_TYPE_LEN = 100;
const MAX_TEXT_LEN = 500;
const MAX_DESCRIPTION_LEN = 2000;
const MAX_LIST_ITEMS = 10;
const SEVERITIES = ["LOW", "MODERATE", "SEVERE", "CRITICAL"] as const;
const DEFAULT_SEVERITY = "MODERATE";

type AnalysisRef = FirebaseFirestore.DocumentReference;

/**
 * Cloud Function triggered when image is uploaded to Storage
 * Analyzes plant image using Vertex AI Gemini
 */
export const analyzeImage = onObjectFinalized(
  {
    timeoutSeconds: 120,
    memory: "1GiB",
    maxInstances: 10,
    region: LOCATION,
  },
  async (event) => {
    const filePath = event.data.name;
    const bucket = event.data.bucket;

    // Only process images in the 'images/' folder
    if (!filePath || !filePath.startsWith("images/")) {
      console.log("Skipping file outside images/");
      return;
    }

    // Extract userId from path: images/{userId}/{filename}
    const pathParts = filePath.split("/");
    if (pathParts.length < 3) {
      console.error("Invalid file path structure");
      return;
    }
    const userId = pathParts[1];

    // The specific analysis doc this invocation owns. Only this doc is
    // ever marked FAILED by the error handler.
    let analysisRef: AnalysisRef | null = null;

    try {
      analysisRef = await findAnalysisDoc(bucket, filePath, userId);
      if (!analysisRef) {
        console.log("No recent pending analysis found; skipping");
        return;
      }
      console.log("Processing analysis:", analysisRef.id);

      // Validate type and size before downloading anything
      const contentType = event.data.contentType || "";
      const size = Number(event.data.size) || 0;
      if (!ALLOWED_CONTENT_TYPES.includes(contentType)) {
        await rejectUpload(
          analysisRef, bucket, filePath, "Unsupported image type"
        );
        return;
      }
      if (size > MAX_IMAGE_BYTES) {
        await rejectUpload(
          analysisRef, bucket, filePath, "Image too large (max 5 MB)"
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

      await processAnalysis(analysisRef, bucket, filePath, contentType);
      console.log("Analysis completed successfully:", analysisRef.id);
    } catch (error) {
      console.error("Error analyzing image:", error);
      if (analysisRef) {
        await markFailed(
          analysisRef,
          error instanceof Error ? error.message : "Unknown error"
        ).catch((e) => console.error("Failed to mark analysis FAILED:", e));
      }
    }
  }
);

/**
 * Locate the PENDING analysis doc for this upload. Retries briefly because
 * the client creates the doc after upload; falls back to the user's most
 * recent PENDING doc. Also expires the user's stale PENDING docs.
 * @param {string} bucket - Storage bucket name
 * @param {string} filePath - Object path
 * @param {string} userId - Owner uid from the path
 * @return {Promise<AnalysisRef | null>} matching doc ref, or null
 */
async function findAnalysisDoc(
  bucket: string,
  filePath: string,
  userId: string
): Promise<AnalysisRef | null> {
  const baseImageUrl = "https://firebasestorage.googleapis.com/v0/b/" +
    `${bucket}/o/${encodeURIComponent(filePath)}?alt=media`;

  const maxRetries = 3;
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    if (attempt > 0) {
      // Wait before retrying (500ms, 1000ms)
      await new Promise((resolve) => setTimeout(resolve, 500 * attempt));
    }

    const twoMinutesAgo = Date.now() - (2 * 60 * 1000);
    const pending = await firestore
      .collection("analyses")
      .where("userId", "==", userId)
      .where("status", "==", "PENDING")
      .where("timestamp", ">", twoMinutesAgo)
      .orderBy("timestamp", "desc")
      .get();

    const match = pending.docs.find((doc) => {
      const url = doc.data().imageUrl;
      return typeof url === "string" && url.startsWith(baseImageUrl);
    });

    if (match) {
      // Clean up this user's OTHER old pending analyses
      const fiveMinutesAgo = Date.now() - (5 * 60 * 1000);
      const oldDocs = pending.docs.filter((d) => {
        return d.id !== match.id && d.data().timestamp < fiveMinutesAgo;
      });
      if (oldDocs.length > 0) {
        console.log(`Cleaning up ${oldDocs.length} old analyses`);
        const batch = firestore.batch();
        oldDocs.forEach((oldDoc) => {
          batch.update(oldDoc.ref, {
            status: "FAILED",
            error: "Timeout - no processing after 5 minutes",
          });
        });
        await batch.commit();
      }
      return match.ref;
    }
  }

  // Fallback: most recent PENDING doc for this user in the last 5 minutes
  const fiveMinutesAgo = Date.now() - (5 * 60 * 1000);
  const fallback = await firestore
    .collection("analyses")
    .where("userId", "==", userId)
    .where("status", "==", "PENDING")
    .where("timestamp", ">", fiveMinutesAgo)
    .orderBy("timestamp", "desc")
    .limit(1)
    .get();

  if (fallback.empty) {
    return null;
  }
  console.log("Using fallback analysis:", fallback.docs[0].id);
  return fallback.docs[0].ref;
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
  await ref.update({status: "PROCESSING"});

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
