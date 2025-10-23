# Cleanup Old Pending Analyses

## Quick Firebase Console Method

1. Go to: https://console.firebase.google.com/project/krishi-rakshak-ai/firestore/databases/-default-/data/~2Fanalyses

2. For each document with `status: "PENDING"`:
   - Click document
   - Click trash icon
   - Confirm delete

## Alternative: Mark as FAILED (keeps history)

Run this in Firebase Console > Firestore > Query:

1. Open a document with `status: PENDING"`
2. Edit the `status` field to `"FAILED"`
3. Add an `error` field with value: `"Cleaned up - old pending record"`
4. Save

## Best Practice Going Forward

The Cloud Function now includes:
- 5-minute timeout for pending analyses
- Retry logic with delays
- URL matching by prefix (ignoring tokens)

This should prevent future buildup of stale PENDING records.

## Test After Cleanup

1. Delete/update all old PENDING analyses
2. Capture a new plant image
3. Watch the logs - should now find and process correctly!
