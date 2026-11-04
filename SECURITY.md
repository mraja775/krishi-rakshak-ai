# Security Policy

## Reporting a vulnerability

**Please do not open a public issue for security problems.**

Report them privately through GitHub's private vulnerability reporting:
**[Report a vulnerability](https://github.com/mraja775/krishi-rakshak-ai/security/advisories/new)** (the repository's *Security* tab, then *Report a vulnerability*).

Please include:
- a description of the issue and its impact,
- steps to reproduce, or a proof of concept,
- affected files, rules or versions.

We aim to acknowledge reports within **5 working days** and to agree a fix and disclosure timeline with you. Please give us reasonable time to fix the issue before you disclose it publicly.

## Scope

In scope:
- the Android app in `app/`,
- Cloud Functions in `functions/`,
- `firestore.rules` and `storage.rules`,
- the CI configuration.

Out of scope:
- vulnerabilities in third-party services (Firebase, Google Cloud), which you should report to the vendor;
- self-hosted deployments that changed the rules or guards;
- reports that only show the Firebase config values in the APK. These are public identifiers by design; see [docs/SECURITY_MODEL.md](docs/SECURITY_MODEL.md).

## Please don't

- test against other people's deployments or real users' data;
- run denial-of-service or high-volume tests that cause costs;
- include real phone numbers, tokens or keys in a report.

## Supported versions

Only the latest `main` gets security fixes.
