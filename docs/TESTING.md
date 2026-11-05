# Testing

CI (`.github/workflows/ci.yml`) runs all three checks below on every push and pull request to `main`. It needs no secrets.

## Security rules (Firebase emulator)

Requires Java 11+ and the Firebase CLI. The tests use a `demo-*` project ID, so they never touch real resources.

```bash
cd tests/rules
npm install
npm test
```

The tests cover:
- the app's own read and write flows (these must succeed), and
- attacks (these must fail): unauthenticated access, cross-user access, forged results, a spoofed phone number, oversized or non-image uploads, writes to reference data, and access to `_system`.

**Any change to `firestore.rules` or `storage.rules` needs a matching test.**

> On macOS without a system JDK:
> `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" PATH="$JAVA_HOME/bin:$PATH"`

## Cloud Functions

```bash
cd functions
npm ci
npm run lint
npm run build
```

Firebase deploys also run lint and build first (`predeploy` in `firebase.json`).

## Android

```bash
./gradlew assembleDebug          # needs app/google-services.json (or the template copied in)
./gradlew assembleRelease        # unsigned unless you configure signing
./gradlew test                   # JVM unit tests
```

## Manual end-to-end check

1. Deploy to **your own** test project. Don't use production.
2. Run the debug build on an emulator and register its App Check debug token.
3. Sign in with a Firebase test phone number, pick a leaf photo, and confirm the result screen appears.
4. In the Functions logs, check for `Analysis completed successfully`.

Public plant-disease datasets work well for test images. Check each dataset's license before you redistribute images.
