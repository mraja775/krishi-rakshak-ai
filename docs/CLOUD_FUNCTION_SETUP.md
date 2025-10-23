# Cloud Function Setup for AI Analysis

This guide explains how to set up the Firebase Cloud Function that processes uploaded plant images using Google's Vertex AI Gemini 2.5 Pro model.

---

## Architecture Overview

```
[Android App]
    → Upload image to Firebase Storage
    → Create placeholder analysis in Firestore (status: PENDING)

[Cloud Function Trigger]
    → Triggered by new image upload
    → Call Vertex AI Gemini 2.5 Pro API with image
    → Parse AI response
    → Update Firestore analysis record (status: COMPLETED)

[Android App]
    → Observes Firestore for updates
    → Displays AI results to user
```

---

## Prerequisites

1. **Firebase Project** with:
   - Firestore Database
   - Firebase Storage
   - Blaze Plan (pay-as-you-go) - required for external API calls

2. **Google Cloud Project** (same project as Firebase):
   - Vertex AI API enabled
   - Service account with Vertex AI permissions

3. **Development Tools**:
   - Node.js 18 or higher
   - Firebase CLI: `npm install -g firebase-tools`

---

## Step 1: Initialize Firebase Functions

### 1.1 Login to Firebase
```bash
firebase login
```

### 1.2 Initialize Functions in Project
```bash
cd /path/to/krishirakshamobileapp
firebase init functions
```

Select:
- **Language**: TypeScript
- **ESLint**: Yes
- **Install dependencies**: Yes

This creates a `functions/` directory with:
```
functions/
├── src/
│   └── index.ts
├── package.json
├── tsconfig.json
└── .eslintrc.js
```

---

## Step 2: Enable Vertex AI API

### 2.1 Via Google Cloud Console
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Select your Firebase project
3. Navigate to **APIs & Services** > **Library**
4. Search for "Vertex AI API"
5. Click **Enable**

### 2.2 Via CLI
```bash
gcloud services enable aiplatform.googleapis.com --project=YOUR_PROJECT_ID
```

Replace `YOUR_PROJECT_ID` with your Firebase project ID.

---

## Step 3: Install Dependencies

```bash
cd functions
npm install @google-cloud/aiplatform
npm install @google-cloud/storage
npm install firebase-admin
npm install firebase-functions
```

---

## Step 4: Implement Cloud Function

Create or replace `functions/src/index.ts`:

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { Storage } from '@google-cloud/storage';
import { PredictionServiceClient } from '@google-cloud/aiplatform';
import { google } from '@google-cloud/aiplatform/build/protos/protos';

admin.initializeApp();

const storage = new Storage();
const firestore = admin.firestore();

// Vertex AI configuration
const PROJECT_ID = process.env.GCLOUD_PROJECT || '';
const LOCATION = 'us-central1'; // or your preferred region
const MODEL = 'gemini-2.5-pro';

/**
 * Cloud Function triggered when image is uploaded to Storage
 * Analyzes plant image using Vertex AI Gemini 2.5 Pro
 */
export const analyzeImage = functions
  .runWith({
    timeoutSeconds: 540, // 9 minutes (AI can take time)
    memory: '2GB'
  })
  .storage
  .object()
  .onFinalize(async (object) => {
    const filePath = object.name;
    const bucket = object.bucket;

    // Only process images in the 'images/' folder
    if (!filePath || !filePath.startsWith('images/')) {
      console.log('Skipping non-image file:', filePath);
      return;
    }

    console.log('Processing image:', filePath);

    try {
      // Extract userId from path: images/{userId}/{filename}
      const pathParts = filePath.split('/');
      if (pathParts.length < 3) {
        console.error('Invalid file path structure:', filePath);
        return;
      }
      const userId = pathParts[1];

      // Find the corresponding analysis record
      const analysesSnapshot = await firestore
        .collection('analyses')
        .where('userId', '==', userId)
        .where('status', '==', 'PENDING')
        .orderBy('timestamp', 'desc')
        .limit(1)
        .get();

      if (analysesSnapshot.empty) {
        console.log('No pending analysis found for user:', userId);
        return;
      }

      const analysisDoc = analysesSnapshot.docs[0];
      const analysisId = analysisDoc.id;

      // Update status to PROCESSING
      await analysisDoc.ref.update({ status: 'PROCESSING' });

      // Download image from Storage
      const file = storage.bucket(bucket).file(filePath);
      const [imageBuffer] = await file.download();
      const base64Image = imageBuffer.toString('base64');

      // Call Vertex AI Gemini 2.5 Pro
      const aiResult = await analyzeWithGemini(base64Image);

      // Update Firestore with results
      await analysisDoc.ref.update({
        plantType: aiResult.plantType,
        diseaseInfo: aiResult.diseaseInfo,
        confidence: aiResult.confidence,
        recommendations: aiResult.recommendations,
        status: 'COMPLETED',
        completedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      console.log('Analysis completed successfully:', analysisId);

    } catch (error) {
      console.error('Error analyzing image:', error);

      // Mark analysis as failed
      const analysesSnapshot = await firestore
        .collection('analyses')
        .where('status', '==', 'PROCESSING')
        .orderBy('timestamp', 'desc')
        .limit(1)
        .get();

      if (!analysesSnapshot.empty) {
        await analysesSnapshot.docs[0].ref.update({
          status: 'FAILED',
          error: error instanceof Error ? error.message : 'Unknown error'
        });
      }
    }
  });

/**
 * Analyze plant image using Vertex AI Gemini 2.5 Pro
 */
async function analyzeWithGemini(base64Image: string): Promise<any> {
  const client = new PredictionServiceClient({
    apiEndpoint: `${LOCATION}-aiplatform.googleapis.com`
  });

  const prompt = `
You are an expert agricultural AI assistant specializing in plant disease detection and diagnosis.

Analyze the provided plant image and provide a comprehensive assessment in JSON format.

**Your response must be valid JSON matching this exact structure:**

{
  "plantType": "Common name of the plant (e.g., Tomato, Rice, Wheat)",
  "diseaseInfo": {
    "name": "Disease name (e.g., Late Blight, Leaf Rust) or null if healthy",
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
- Severity levels: LOW (minor issue), MODERATE (needs attention), SEVERE (urgent), CRITICAL (emergency)
- Confidence: 0.0 to 1.0 (e.g., 0.85 = 85% confident)
- Recommendations: practical, actionable advice for farmers
- Be specific and accurate based on visible symptoms in the image

Return ONLY the JSON object, no additional text.
`;

  const endpoint = client.endpointPath(PROJECT_ID, LOCATION, MODEL, MODEL);

  const request: google.cloud.aiplatform.v1.IPredictRequest = {
    endpoint,
    instances: [
      {
        structValue: {
          fields: {
            prompt: { stringValue: prompt },
            image: {
              structValue: {
                fields: {
                  bytesBase64Encoded: { stringValue: base64Image }
                }
              }
            }
          }
        }
      }
    ],
    parameters: {
      structValue: {
        fields: {
          temperature: { numberValue: 0.2 }, // Low temp for consistent results
          maxOutputTokens: { numberValue: 2048 },
          topP: { numberValue: 0.95 },
          topK: { numberValue: 40 }
        }
      }
    }
  };

  const [response] = await client.predict(request);

  // Parse AI response
  const predictions = response.predictions;
  if (!predictions || predictions.length === 0) {
    throw new Error('No predictions returned from Gemini');
  }

  const content = predictions[0].structValue?.fields?.content?.stringValue || '';

  // Extract JSON from response (AI might wrap it in markdown)
  const jsonMatch = content.match(/\{[\s\S]*\}/);
  if (!jsonMatch) {
    throw new Error('Failed to extract JSON from Gemini response');
  }

  const result = JSON.parse(jsonMatch[0]);

  // Validate structure
  if (!result.plantType || result.confidence === undefined) {
    throw new Error('Invalid response structure from Gemini');
  }

  return result;
}
```

---

## Step 5: Set Environment Variables

Create `functions/.env` (for local testing):
```bash
GCLOUD_PROJECT=your-project-id
```

For production, Firebase automatically sets `GCLOUD_PROJECT`.

---

## Step 6: Configure Firestore Rules

Update Firestore security rules to allow Cloud Function writes:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Analyses collection
    match /analyses/{analysisId} {
      // Users can read their own analyses
      allow read: if request.auth != null &&
                     resource.data.userId == request.auth.uid;

      // Users can create their own analyses
      allow create: if request.auth != null &&
                       request.resource.data.userId == request.auth.uid;

      // Cloud Functions can update any analysis
      allow update: if request.auth == null; // Cloud Function has no auth

      // Users can delete their own analyses
      allow delete: if request.auth != null &&
                       resource.data.userId == request.auth.uid;
    }
  }
}
```

Deploy rules:
```bash
firebase deploy --only firestore:rules
```

---

## Step 7: Configure Storage Rules

Update Storage security rules:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {

    // Images folder
    match /images/{userId}/{filename} {
      // Users can upload images to their own folder
      allow write: if request.auth != null &&
                      request.auth.uid == userId;

      // Users can read their own images
      allow read: if request.auth != null &&
                     request.auth.uid == userId;
    }
  }
}
```

Deploy rules:
```bash
firebase deploy --only storage
```

---

## Step 8: Deploy Cloud Function

### 8.1 Build TypeScript
```bash
cd functions
npm run build
```

### 8.2 Deploy to Firebase
```bash
firebase deploy --only functions
```

This will:
- Upload function code to Firebase
- Provision Cloud Function
- Set up Storage trigger

### 8.3 Verify Deployment
Check the Firebase Console:
- Go to **Functions** tab
- Verify `analyzeImage` function is deployed
- Check logs for any errors

---

## Step 9: Test the Complete Flow

### 9.1 Run Android App
1. Build and install app on device/emulator
2. Log in with phone authentication
3. Tap "Analyze Plant"
4. Capture or select an image
5. Confirm image

### 9.2 Monitor Flow
Watch Firebase Console:

**Storage**:
- Image appears in `images/{userId}/` folder

**Firestore** (`analyses` collection):
- Document created with status: `PENDING`
- Status updates to: `PROCESSING`
- Status updates to: `COMPLETED` with AI results

**Functions Logs**:
```bash
firebase functions:log
```

Look for:
- "Processing image: images/..."
- "Analysis completed successfully: {analysisId}"

### 9.3 Android App
- Loading screen shows upload progress
- Loading screen shows "AI is analyzing..."
- Results screen displays plant type, disease info, recommendations

---

## Step 10: Monitor Costs

### Vertex AI Pricing (as of 2025)
- Gemini 2.5 Pro: ~$0.00125 per image analysis
- Free tier: First 1000 requests/month

### Firebase Storage
- First 5 GB: Free
- $0.026 per GB after

### Cloud Functions
- First 2M invocations: Free
- $0.40 per million after

**Estimated cost for 1000 analyses/month**: ~$1-2

Monitor usage:
```bash
gcloud billing budgets list
```

---

## Troubleshooting

### Issue: "Permission Denied" in Cloud Function
**Solution**:
1. Go to [IAM & Admin](https://console.cloud.google.com/iam-admin)
2. Find "App Engine default service account"
3. Add role: "Vertex AI User"

### Issue: Cloud Function timeout
**Solution**: Increase timeout in function config:
```typescript
.runWith({
  timeoutSeconds: 540, // 9 minutes max
  memory: '2GB'
})
```

### Issue: Gemini returns invalid JSON
**Solution**:
1. Check prompt formatting
2. Add JSON validation and retry logic
3. Use regex to extract JSON from markdown

### Issue: Analysis stuck in PROCESSING
**Solution**:
1. Check Cloud Function logs: `firebase functions:log`
2. Verify Vertex AI API is enabled
3. Check service account permissions

---

## Advanced Configuration

### Enable CORS for Storage
If you need web access:
```bash
gsutil cors set cors.json gs://your-bucket-name
```

`cors.json`:
```json
[
  {
    "origin": ["*"],
    "method": ["GET"],
    "maxAgeSeconds": 3600
  }
]
```

### Add Retry Logic
For production, add retry logic in Cloud Function:
```typescript
import { Retry } from '@google-cloud/common';

const retryOptions: Retry = {
  retries: 3,
  factor: 2,
  minTimeout: 1000
};
```

### Add Monitoring Alerts
Set up Cloud Monitoring alerts for:
- Function errors
- High latency
- Cost thresholds

---

## Security Best Practices

1. **Never commit credentials**: Use Firebase environment config
2. **Validate image size**: Reject images > 10 MB in Storage rules
3. **Rate limiting**: Use Firestore rules to limit user uploads
4. **Sanitize input**: Validate analysis data before writing to Firestore
5. **Use service account**: Don't use personal credentials in production

---

## Next Steps

After Cloud Function is deployed:
1. Test with various plant images
2. Monitor AI accuracy and adjust prompt if needed
3. Add error handling and retry logic
4. Implement analytics to track success rates
5. Optimize costs by caching common results

---

## Support

For issues:
- Check [Firebase Functions docs](https://firebase.google.com/docs/functions)
- Check [Vertex AI docs](https://cloud.google.com/vertex-ai/docs)
- Review Cloud Function logs in Firebase Console

---

**Status**: Ready for deployment and testing
