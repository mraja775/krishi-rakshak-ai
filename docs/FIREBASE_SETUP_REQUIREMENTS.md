# Firebase Setup Requirements

---

## Current Status ✅

- ✅ Firebase project created
- ✅ Firebase Authentication enabled
- ✅ `google-services.json` file added
- ⚠️ **Billing not enabled** (required for Phone Authentication)

---

## Issue: BILLING_NOT_ENABLED

### Error
```
com.google.firebase.FirebaseException: An internal error has occurred. [ BILLING_NOT_ENABLED ]
```

### Root Cause

Firebase Phone Authentication (SMS OTP) requires the **Blaze Plan** (pay-as-you-go billing).

**Why?** Phone authentication uses SMS messages which cost money to send. Firebase needs billing enabled to charge for SMS messages sent.

### Cost Information

- **Free Tier**: 10,000 verifications/month FREE
- **After that**: ~$0.01 per verification in India
- **No upfront cost**: You only pay if you exceed the free tier

---

## How to Enable Billing

### Step 1: Go to Firebase Console
1. Open [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **krishi-rakshak-ai**

### Step 2: Upgrade to Blaze Plan
1. Click the **⚙️ gear icon** → **Usage and billing**
2. Click **Details & settings** tab
3. Click **Modify plan**
4. Select **Blaze (Pay as you go)** plan
5. Add a payment method (credit/debit card)
6. Set a budget alert (optional but recommended):
   - E.g., Alert when spending > ₹100/month

### Step 3: Verify Phone Auth is Working
1. In Firebase Console → **Authentication** → **Sign-in method**
2. Ensure **Phone** is enabled
3. Add test phone numbers if needed for development:
   - Go to Phone → Add test phone number
   - Example: +91 9999999999 with code 123456

---

## Workaround for Testing Without Billing

If you don't want to enable billing right now, you can:

### Option 1: Use Email/Password Authentication (Temporary)
- Switch to email auth for testing navigation
- No billing required
- Can switch back to phone auth later

### Option 2: Skip Authentication for Development
- Add a debug bypass in the splash screen
- Go directly to home screen
- Test UI/UX without authentication

### Option 3: Use Test Phone Numbers
- Firebase allows test phone numbers without SMS
- No billing required
- But you need to configure them in console first

---

## Recommended Approach

For this project (Krishi Rakshak AI):

1. **Enable Blaze Plan** - It's free for first 10k verifications
2. **Set budget alert** at ₹100 or ₹500 per month
3. **Use test phone numbers** during development
4. **Production**: Real SMS will be sent only in production

---

## Current App Behavior

✅ **App is now working!**
- ✅ App launches without crash
- ✅ Splash screen shows and navigates
- ✅ Phone input screen displays
- ⚠️ Phone OTP sending fails due to billing

---

## Next Steps

**Choose one:**

1. **Enable Firebase Billing** (recommended for full testing)
   - Follow steps above
   - Test complete phone auth flow

2. **Use Development Bypass** (quickest for UI testing)
   - I can add a debug mode to skip authentication
   - Test all screens without Firebase

3. **Add Test Phone Numbers** (middle ground)
   - Enable billing
   - Configure test numbers in Firebase console
   - Test without sending real SMS

---

**Which option would you like to proceed with?**
