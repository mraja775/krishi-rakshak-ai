# Cost controls: preventing runaway spend

The backend spends money in four places: **Gemini calls** (the main variable cost), **Cloud Functions** compute, **Phone Auth SMS**, and Storage and Firestore. Protection comes in layers. Some layers are in code and deploy automatically. Others are **console settings you must make yourself**, because no code can set them.

> 🔴 Important: **Google Cloud budgets only send alerts. They do not stop spending by themselves.** That's why `budgetGuard` exists. Wire it up as described below.

## Built-in guards (in code)

| Guard | Where | Default | Change via |
|-------|-------|---------|------------|
| Upload type/size limit | `storage.rules` + `analyzePlantImage` | JPEG/PNG/WebP, < 5 MB | edit rules / `MAX_IMAGE_BYTES` |
| Per-user rate limit | `analyzePlantImage` | 20 analyses / user / hour | `RATE_LIMIT_MAX_ANALYSES` |
| **Global daily cap** | `analyzePlantImage` → `_system/usage_<YYYYMMDD>` | 500 Gemini calls / UTC day | env `GLOBAL_DAILY_MAX_ANALYSES` |
| **Kill switch** | `analyzePlantImage` reads `_system/config.aiEnabled` (cached for 60 s) | enabled | set `aiEnabled: false` |
| Function scaling cap | `analyzePlantImage` options | max 10 instances, 1 GiB, 120 s timeout | `maxInstances` etc. |
| **Budget guard** | `budgetGuard` (Pub/Sub) | soft kill at 80 %; hard kill off | env `BUDGET_HARD_KILL` |
| Output size limits | `sanitizeResult` | capped strings and lists | constants in `index.ts` |

Worst case with the defaults: 500 Gemini calls a day, about 15,000 a month. Pick `GLOBAL_DAILY_MAX_ANALYSES` from your per-call price and your budget.

### Kill switch: manual use

Use this to stop all AI spend immediately. Analyses fail with "Analysis temporarily disabled" until you switch it back.

- Console: **Firestore → `_system` → `config`**. Set the field `aiEnabled` (boolean) to `false`.
- Re-enable: set it back to `true`, or delete the field. **`budgetGuard` never re-enables it**, so you always decide.

Clients can't read or write `_system`. The security rules deny it, and `tests/rules` checks this.

## Required console steps

Do these **before** you open the app to users.

### 1. Budget and alerts

1. Google Cloud console → **Billing → Budgets & alerts → Create budget**.
2. Scope it to your project. Set a monthly amount you can afford.
3. Add alert thresholds of 50 %, 80 % and 100 %, and email billing admins.
4. Under **Manage notifications**, tick **Connect a Pub/Sub topic to this budget**. Create the topic **`billing-budget-alerts`**, or your own `BUDGET_TOPIC`, in this project.

The budget then publishes its status to the topic several times a day. `budgetGuard`:
- at **≥ 80 %** sets `_system/config.aiEnabled = false`, which stops Gemini calls;
- at **≥ 100 %**, and only if `BUDGET_HARD_KILL=true`, **detaches the billing account** from the project. That stops *all* paid services. Storage and Firestore stop serving, though the data is kept, until you re-link billing by hand.

Budget data runs a few hours behind actual usage. Treat these as backstops and size your caps so you don't depend on them.

### 2. IAM for the hard kill (only if `BUDGET_HARD_KILL=true`)

`budgetGuard` runs as the project's default compute service account (`PROJECT_NUMBER-compute@developer.gserviceaccount.com`), unless you configure another one. To unlink billing, it needs:

- **Project Billing Manager** (`roles/billing.projectManager`) on the **project**, and
- **Billing Account User** (`roles/billing.user`) on the **billing account**, or a custom role with `billing.resourceAssociations.delete`.

Grant these under **IAM & Admin → IAM** (project) and **Billing → Account management → Permissions** (billing account). Without them, the hard kill logs a permission error. The soft kill still works, because it only writes to Firestore.

### 3. Vertex AI quota caps

**IAM & Admin → Quotas & system limits:** filter for *Vertex AI API* and your Gemini model, then lower the **requests per minute** quota to just above normal peak traffic. This puts a hard ceiling on Gemini calls even if every software guard failed.

### 4. App Check enforcement

App Check makes Firebase reject requests that don't come from your genuine app. Without it, anyone who copies the public config out of the APK can script uploads.

1. **App Check → Apps → your Android app:** register the **Play Integrity** provider. Link your Play Console app.
2. Add **debug tokens** for each developer device or emulator (see SETUP.md §7).
3. Check the **Metrics** tab until most traffic is verified. Then click **Enforce** for **Cloud Storage**, **Cloud Firestore** and **Authentication** (Identity Platform).

### 5. Restrict the Firebase API key

Google Cloud console → **APIs & Services → Credentials → the "Android key (auto created by Firebase)"**:
- **Application restrictions → Android apps:** add package `com.krishirakshak.app` with your debug and release SHA-1s.
- **API restrictions:** limit the key to the Firebase APIs the app uses: Identity Toolkit, Token Service, Firestore, Cloud Storage for Firebase, Firebase Installations, Firebase App Check, and Firebase Analytics/Remote Config if you use them.

### 6. Phone Auth (SMS) abuse

SMS is charged per message and is a common fraud target (SMS pumping).
- **Authentication → Settings → SMS region policy:** allow only the countries you serve, for example India.
- Keep App Check enforcement on for Authentication.
- `setAppVerificationDisabledForTesting(true)` is only used in **debug** builds (`FirebaseModule`). Never ship it in release.

### 7. Storage lifecycle (optional)

Uploaded photos build up over time. You can add a lifecycle rule to the bucket in the Cloud console (**Cloud Storage → bucket → Lifecycle**), for example deleting `images/` objects older than 180 days.

## Checklist

- [ ] Budget created with alerts at 50/80/100 %
- [ ] Budget connected to Pub/Sub topic `billing-budget-alerts`, and `budgetGuard` deployed
- [ ] (Optional) `BUDGET_HARD_KILL=true` plus the IAM roles granted
- [ ] `GLOBAL_DAILY_MAX_ANALYSES` set for your budget
- [ ] Vertex AI requests-per-minute quota lowered
- [ ] App Check registered, debug tokens added, enforced for Storage, Firestore and Auth
- [ ] API key restricted to the Android app and the Firebase APIs
- [ ] SMS region policy set to an allowlist
