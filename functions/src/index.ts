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

/**
 * Cloud Function triggered when image is uploaded to Storage
 * Analyzes plant image using Vertex AI Gemini 2.0 Flash
 */
export const analyzeImage = onObjectFinalized(
  {
    timeoutSeconds: 540, // 9 minutes (AI can take time)
    memory: "2GiB",
    region: LOCATION,
  },
  async (event) => {
    const filePath = event.data.name;
    const bucket = event.data.bucket;

    // Only process images in the 'images/' folder
    if (!filePath || !filePath.startsWith("images/")) {
      console.log("Skipping non-image file:", filePath);
      return;
    }

    console.log("Processing image:", filePath);

    try {
      // Extract userId from path: images/{userId}/{filename}
      const pathParts = filePath.split("/");
      if (pathParts.length < 3) {
        console.error("Invalid file path structure:", filePath);
        return;
      }
      const userId = pathParts[1];

      // Construct the base image URL (without token)
      const baseImageUrl = `https://firebasestorage.googleapis.com/v0/b/${bucket}/o/${encodeURIComponent(filePath)}?alt=media`;

      console.log("Looking for analysis with base URL:", baseImageUrl);

      // Retry logic: Cloud Function may start before app creates Firestore doc
      let analysesSnapshot = {
        empty: true,
        docs: [] as FirebaseFirestore.QueryDocumentSnapshot[],
      };

      const maxRetries = 3;
      for (let attempt = 0; attempt < maxRetries; attempt++) {
        if (attempt > 0) {
          // Wait before retrying (500ms, 1000ms, 1500ms)
          await new Promise((resolve) => setTimeout(resolve, 500 * attempt));
          console.log(`Retry ${attempt}/${maxRetries} - looking for analysis`);
        }

        // Find analysis by checking if imageUrl starts with baseImageUrl
        // Only get recent ones (within last 2 minutes)
        const twoMinutesAgo = Date.now() - (2 * 60 * 1000);
        const allPendingSnapshot = await firestore
          .collection("analyses")
          .where("userId", "==", userId)
          .where("status", "==", "PENDING")
          .where("timestamp", ">", twoMinutesAgo)
          .orderBy("timestamp", "desc")
          .get();

        console.log(`Found ${allPendingSnapshot.size} recent PENDING analyses`);

        // Find the analysis with matching imageUrl (ignoring token)
        for (const doc of allPendingSnapshot.docs) {
          const docImageUrl = doc.data().imageUrl as string;
          if (docImageUrl && docImageUrl.startsWith(baseImageUrl)) {
            analysesSnapshot = {
              empty: false,
              docs: [doc],
            };
            console.log("Found analysis by URL prefix:", doc.id);

            // Clean up OTHER old pending analyses (not this one)
            const fiveMinutesAgo = Date.now() - (5 * 60 * 1000);
            const oldDocs = allPendingSnapshot.docs.filter((d) => {
              return d.id !== doc.id && d.data().timestamp < fiveMinutesAgo;
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

            break;
          }
        }

        if (!analysesSnapshot.empty) {
          break; // Found it!
        }
      }

      if (analysesSnapshot.empty) {
        console.log("No analysis found after retries:", baseImageUrl);

        // Fallback: find by userId and PENDING (most recent)
        // Only analyses created in last 5 minutes
        const fiveMinutesAgo = Date.now() - (5 * 60 * 1000);

        const fallbackSnapshot = await firestore
          .collection("analyses")
          .where("userId", "==", userId)
          .where("status", "==", "PENDING")
          .where("timestamp", ">", fiveMinutesAgo)
          .orderBy("timestamp", "desc")
          .limit(1)
          .get();

        if (fallbackSnapshot.empty) {
          console.log("No recent pending analysis found for user:", userId);
          console.log("Skipping - analysis may have timed out");
          return;
        }

        const analysisDoc = fallbackSnapshot.docs[0];
        const analysisId = analysisDoc.id;
        const analysisTimestamp = analysisDoc.data().timestamp;

        const ageMs = Date.now() - analysisTimestamp;
        console.log(`Using fallback: ${analysisId} (${ageMs}ms ago)`);

        // Update status to PROCESSING
        await analysisDoc.ref.update({
          status: "PROCESSING",
        });

        console.log("Updated analysis with correct imageUrl:", analysisId);

        // Continue processing with this doc
        const file = storage.bucket(bucket).file(filePath);
        const [imageBuffer] = await file.download();
        const base64Image = imageBuffer.toString("base64");

        const aiResult = await analyzeWithGemini(base64Image);

        await analysisDoc.ref.update({
          plantType: aiResult.plantType,
          diseaseInfo: aiResult.diseaseInfo,
          confidence: aiResult.confidence,
          recommendations: aiResult.recommendations,
          status: "COMPLETED",
          completedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        console.log("Analysis completed successfully:", analysisId);
        return;
      }

      const analysisDoc = analysesSnapshot.docs[0];
      const analysisId = analysisDoc.id;

      // Update status to PROCESSING
      await analysisDoc.ref.update({status: "PROCESSING"});

      // Download image from Storage
      const file = storage.bucket(bucket).file(filePath);
      const [imageBuffer] = await file.download();
      const base64Image = imageBuffer.toString("base64");

      // Call Vertex AI Gemini
      const aiResult = await analyzeWithGemini(base64Image);

      // Update Firestore with results
      await analysisDoc.ref.update({
        plantType: aiResult.plantType,
        diseaseInfo: aiResult.diseaseInfo,
        confidence: aiResult.confidence,
        recommendations: aiResult.recommendations,
        status: "COMPLETED",
        completedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      console.log("Analysis completed successfully:", analysisId);
    } catch (error) {
      console.error("Error analyzing image:", error);

      // Mark analysis as failed
      const analysesSnapshot = await firestore
        .collection("analyses")
        .where("status", "==", "PROCESSING")
        .orderBy("timestamp", "desc")
        .limit(1)
        .get();

      if (!analysesSnapshot.empty) {
        await analysesSnapshot.docs[0].ref.update({
          status: "FAILED",
          error: error instanceof Error ? error.message : "Unknown error",
        });
      }
    }
  }
);

/**
 * Disease information structure
 */
interface DiseaseInfo {
  name: string;
  scientificName: string | null;
  severity: string;
  description: string;
  symptoms: string[];
  causes: string[];
  preventiveMeasures: string[];
}

/**
 * Analysis result structure
 */
interface AnalysisResult {
  plantType: string;
  diseaseInfo: DiseaseInfo | null;
  confidence: number;
  recommendations: string[];
}

/**
 * Analyze plant image using Vertex AI Gemini 2.0 Flash
 * @param {string} base64Image - Base64 encoded image data
 * @return {Promise<AnalysisResult>} AI analysis result
 */
async function analyzeWithGemini(
  base64Image: string
): Promise<AnalysisResult> {
  const vertexAI = new VertexAI({
    project: PROJECT_ID,
    location: LOCATION,
  });

  // Use Gemini 2.0 Flash (fast and cost-effective)
  const model = vertexAI.getGenerativeModel({
    model: "gemini-2.0-flash-exp",
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

  // Create the request with image
  const imagePart = {
    inlineData: {
      data: base64Image,
      mimeType: "image/jpeg",
    },
  };

  const textPart = {
    text: prompt,
  };

  const request = {
    contents: [{role: "user", parts: [textPart, imagePart]}],
    generationConfig: {
      temperature: 0.2, // Low temp for consistent results
      maxOutputTokens: 2048,
      topP: 0.95,
      topK: 40,
    },
  };

  const response = await model.generateContent(request);
  const result = response.response;

  if (!result.candidates || result.candidates.length === 0) {
    throw new Error("No response from Gemini");
  }

  const content = result.candidates[0].content.parts[0].text || "";

  // Extract JSON from response (AI might wrap it in markdown)
  const jsonMatch = content.match(/\{[\s\S]*\}/);
  if (!jsonMatch) {
    console.error("Gemini response:", content);
    throw new Error("Failed to extract JSON from Gemini response");
  }

  const parsedResult = JSON.parse(jsonMatch[0]);

  // Validate structure
  if (!parsedResult.plantType || parsedResult.confidence === undefined) {
    throw new Error("Invalid response structure from Gemini");
  }

  return parsedResult;
}
