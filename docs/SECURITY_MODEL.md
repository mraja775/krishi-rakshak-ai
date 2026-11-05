# Security model

## What is public, and what is secret

| Item | Public? | Notes |
|------|---------|-------|
| Source code, rules, function code | ✅ public | Everything in this repo |
| `google-services.json` (API key, app ID, project ID) | Not committed, but **not a secret** | It ships inside every APK and anyone can extract it. It identifies your project; it doesn't grant access. Security comes from the rules, App Check and key restrictions. |
| `.firebaserc` (your project ID) | Not committed | Kept local so forks don't deploy to your project |
| Service-account keys (`*.json` with `private_key`) | 🔴 **secret** | Never create them for this project if you can avoid it, and never commit them. Gitignored: `serviceAccount*.json`, `*-key.json`, `*.p12`, `*.pem` |
| Release keystore (`*.jks`, `*.keystore`) | 🔴 **secret** | Gitignored. Keep it in a password manager or CI secrets. |
| `functions/.env*` | Keep private | Gitignored. Holds configuration, not credentials. |

The functions authenticate to Google APIs with the runtime service account (Application Default Credentials), so the backend needs **no API keys or secrets** at all.

## Firestore and Storage rules

Rules files: [`firestore.rules`](../firestore.rules) and [`storage.rules`](../storage.rules). Tests: [`tests/rules`](../tests/rules).

Principles:

1. **Deny by default.** Every path that isn't listed is denied, including `_system`.
2. **Owner-only.** Users can read and change only documents and objects under their own `uid`.
3. **Clients never write results.** `analyses` can only be *created*, as an empty `PENDING` record (`hasOnly` keys, fixed values, `id` equal to the document ID, image URL inside the caller's own Storage folder, timestamp within ±10 minutes). Updates from clients are denied. Only the Cloud Function writes diagnoses.
4. **Verified identity.** `users/{uid}.phoneNumber` must equal `request.auth.token.phone_number`, so users can't claim another number.
5. **Reference data is read-only.** `crops`, `diseases` and `treatments` are readable by signed-in users and writable only by admins (Admin SDK).
6. **Upload constraints.** Only images (`image/jpeg|png|webp`) under 5 MB, safe filenames, no overwrites.

### Why there is no `request.auth == null` rule

An earlier version contained rules like this:

```
allow write: if request.auth == null; // "for Cloud Functions"
```

The comment reflects a common misunderstanding. **Cloud Functions use the Admin SDK, which bypasses security rules completely.** So `request.auth == null` grants nothing to the backend. It grants access to **every unauthenticated client on the internet**. That rule exposed every user's phone number and let anyone rewrite diagnoses. Never add such a clause. If backend-only data is needed, deny it to clients and write it with the Admin SDK.

## Backend hardening

- `analyzePlantImage` only ever changes the analysis doc it's processing (the error path included).
- The upload type and size are checked again server-side before the image is downloaded.
- Model output is untrusted. `sanitizeResult` enforces types, enums and length limits before anything is stored.
- Logs avoid user IDs, storage paths and full model responses.
- Spend limits: see [COST_CONTROLS.md](COST_CONTROLS.md).

## Android app

- No secrets are compiled in. Debug-only behaviour (verbose logging, disabled phone-auth app verification, the App Check debug provider) is gated by build type.
- Cleartext HTTP is disabled (`usesCleartextTraffic="false"`). The FileProvider isn't exported.
- Release builds use R8 minification and resource shrinking.

## Reporting vulnerabilities

See [SECURITY.md](../SECURITY.md). Please report privately, not in public issues.
