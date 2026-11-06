# Self-hosting setup

This guide sets up **your own** backend. Nothing in this repository connects to anyone else's Firebase project.

> ⚠️ **Read [COST_CONTROLS.md](COST_CONTROLS.md) before step 6.** The backend calls a paid AI model, so set a budget before you deploy.

## Prerequisites

- Android Studio (recent stable). It includes the Android SDK, an emulator and a JDK (the "JBR").
- Node.js 22+ and npm
- Firebase CLI: `npm install -g firebase-tools`
- A Google account with billing enabled. Cloud Functions and Vertex AI need the **Blaze** (pay-as-you-go) plan.

## 1. Create the Firebase project

1. In the [Firebase console](https://console.firebase.google.com/), click **Add project** and choose a project ID, for example `your-project-id`.
2. Upgrade to the **Blaze** plan (*Usage and billing*).
3. **Authentication → Sign-in method → Phone:** enable it.
   - **Authentication → Settings → SMS region policy:** choose **Allow** and add only the countries you serve, for example India. This blocks SMS-pumping fraud. See [COST_CONTROLS.md](COST_CONTROLS.md).
   - Optional: add test phone numbers with fixed codes for development (*Phone numbers for testing*). These don't send real SMS.
4. **Firestore Database:** create it in production mode and pick a region close to your users.
5. **Storage:** create the default bucket.
6. In the [Google Cloud console](https://console.cloud.google.com/) for the same project, enable the **Vertex AI API** (`aiplatform.googleapis.com`).

## 2. Register the Android app

1. Firebase console → **Project settings → Your apps → Add app → Android**.
2. Package name: `com.krishirakshak.app`. If you change it, also change `applicationId` in `app/build.gradle.kts`.
3. Add your signing certificate fingerprints. Phone Auth and App Check need both **SHA-1 and SHA-256**:
   ```bash
   ./gradlew signingReport        # look for Variant: debug → SHA1 / SHA-256
   ```
   Add the fingerprints of your release or Play App Signing key later, before you publish.
4. Download **`google-services.json`** into `app/`. The file is gitignored, so never commit it. `app/google-services.json.template` shows its shape with placeholder values only.

## 3. Point the Firebase CLI at your project

```bash
firebase login
cp .firebaserc.example .firebaserc
firebase use --add            # pick your-project-id, alias "default"
```

`.firebaserc` is gitignored, so your project ID stays out of the repo.

> **Pitfall:** if your shell exports `GOOGLE_APPLICATION_CREDENTIALS` for another project, the Firebase CLI may use that service account instead of your login, and fail with 401/403 errors. Run Firebase commands with it unset:
> `env -u GOOGLE_APPLICATION_CREDENTIALS firebase deploy …`

## 4. Install function dependencies

```bash
cd functions && npm ci && npm run build && cd ..
```

## 5. Optional: configure the functions

The defaults are safe. To change them, create `functions/.env.<your-project-id>`. Files named `.env*` are gitignored. For example:

```bash
GEMINI_MODEL=gemini-3.8-flash          # any Vertex AI Gemini model your project can use
VERTEX_LOCATION=global                 # newer Gemini models are served from "global"
GLOBAL_DAILY_MAX_ANALYSES=500          # project-wide Gemini calls per UTC day
BUDGET_TOPIC=billing-budget-alerts     # Pub/Sub topic your budget publishes to
BUDGET_HARD_KILL=false                 # "true" = detach billing at 100% of budget
```

Check that the model is current: Google retires Gemini versions regularly. See the [model versions and lifecycle](https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/model-versions) page.

## 6. Deploy the backend

First complete the **required** steps in [COST_CONTROLS.md](COST_CONTROLS.md): the budget, the Pub/Sub topic and the quotas. Then:

```bash
firebase deploy --only firestore:rules,firestore:indexes,storage,functions
```

Composite indexes can take several minutes to build. Until they're ready, analyses fail with "The query requires an index… currently building".

## 7. Build and run the app

```bash
# macOS: use Android Studio's bundled JDK if you have no system Java
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew assembleDebug
```

**On an emulator:** create a device in Android Studio's Device Manager. Use a Google Play system image, because Phone Auth needs Play services. Start it, then:

```bash
./gradlew installDebug
```

To test analysis without a camera, push a photo to the emulator's gallery and choose **Choose from Gallery**:

```bash
adb push leaf.jpg /sdcard/Pictures/
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/leaf.jpg
```

**On a phone:** enable *Developer options → USB debugging*, connect it, accept the prompt, then run `./gradlew installDebug`.

**App Check debug token:** debug builds use the App Check debug provider. On first launch it logs a line such as `Enter this debug secret into the allow list in the Firebase Console…` to Logcat (tag `DebugAppCheckProvider`). Add that token under **App Check → Apps → ⋮ → Manage debug tokens**. Debug builds stop working once App Check is enforced if you skip this step.

## 8. Release builds

- Create an upload keystore outside the repo (never commit it: `*.jks` and `*.keystore` are gitignored), then add its details to your **user-level** `~/.gradle/gradle.properties` (not the project's):
  ```properties
  KRISHI_RELEASE_STORE_FILE=/absolute/path/to/release.jks
  KRISHI_RELEASE_STORE_PASSWORD=...
  KRISHI_RELEASE_KEY_ALIAS=...
  KRISHI_RELEASE_KEY_PASSWORD=...
  ```
  `./gradlew assembleRelease` then signs the APK. Without these properties the release build is produced unsigned.
- Back up the keystore and its password somewhere safe (e.g. a password manager). If you lose it you can't ship updates to an installed app.
- Register the release SHA-1 and SHA-256 (or the Play App Signing ones) in Firebase.
- Release builds attest with **Play Integrity**. Link your Play Console app to the Firebase project under App Check.

## Troubleshooting

| Symptom | Cause / fix |
|---------|-------------|
| Analysis goes straight to FAILED, logs say "requires an index" | Run `firebase deploy --only firestore:indexes` and wait for the build to finish |
| Vertex AI `404 Publisher model … was not found` | The model is retired or not offered in that location. Update `GEMINI_MODEL`, or use `VERTEX_LOCATION=global` |
| `Runtime Node.js NN was decommissioned` | Raise `engines.node` in `functions/package.json` |
| Deploy fails with 401 although `firebase login:list` shows you | Run `firebase logout && firebase login`, and unset `GOOGLE_APPLICATION_CREDENTIALS` |
| "Analysis temporarily disabled" | The kill switch is on. Check `_system/config` (see COST_CONTROLS.md) |
| "Service is at capacity today" | The global daily cap was reached |
