# Clean Up Old Pending Analyses

## Problem
Old PENDING analyses in Firestore are causing the Cloud Function to process the wrong records.

## Quick Fix

### Option 1: Via Firebase Console (Easiest)

1. Go to [Firestore Database](https://console.firebase.google.com/project/krishi-rakshak-ai/firestore)
2. Navigate to `analyses` collection
3. Find all documents with `status: PENDING`
4. Delete them manually (click each doc → Delete)

### Option 2: Via Cloud Function

Create a one-time cleanup function or update existing analyses:

```javascript
// In Firebase Console > Firestore > Run a query
// Find all PENDING analyses older than 5 minutes

const fiveMinutesAgo = Date.now() - (5 * 60 * 1000);

db.collection('analyses')
  .where('status', '==', 'PENDING')
  .where('timestamp', '<', fiveMinutesAgo)
  .get()
  .then(snapshot => {
    snapshot.forEach(doc => {
      doc.ref.update({ status: 'FAILED', error: 'Timeout - cleaned up' });
    });
  });
```

### Option 3: Delete all analyses and start fresh

**For testing only:**
1. Go to Firestore Console
2. Select `analyses` collection
3. Click the three dots → Delete collection
4. Confirm deletion

Then test the app again with a clean slate.

## Permanent Fix

The Cloud Function should have timeout logic to ignore old PENDING analyses. I'll update the function now.
