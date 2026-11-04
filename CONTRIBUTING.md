# Contributing

Thanks for helping farmers get faster, better crop advice! 🌾

## Ground rules

- Be kind. See the [Code of Conduct](CODE_OF_CONDUCT.md).
- Report security issues privately. See [SECURITY.md](SECURITY.md).
- **Never commit secrets or personal data:** `google-services.json`, keystores, service-account keys, `.env` files, real phone numbers, or screenshots and logs that contain them.

## Development setup

Follow [docs/SETUP.md](docs/SETUP.md) to create your **own** Firebase project. Never test against someone else's backend.

## Workflow

1. Fork the repo and create a branch: `git checkout -b feat/short-description`.
2. Make focused changes. Match the existing style (Kotlin official style; the functions use the Google ESLint config).
3. Run the relevant checks (see [docs/TESTING.md](docs/TESTING.md)):
   ```bash
   ./gradlew assembleDebug
   (cd functions && npm run lint && npm run build)
   (cd tests/rules && npm test)
   ```
4. Open a pull request and fill in the template.

## Changes that need extra care

- **Security rules:** keep deny-by-default, never add `request.auth == null` grants, and add tests in `tests/rules/` for every rule you change.
- **Anything that calls Gemini or another paid API:** it must go through the existing per-user rate limit, global daily cap and kill switch. See [docs/COST_CONTROLS.md](docs/COST_CONTROLS.md).
- **Model output:** treat it as untrusted input and validate it the way `sanitizeResult` does.
- **New permissions or collected data:** explain why in the PR and update `docs/SECURITY_MODEL.md`.

## Translations

Strings are in `app/src/main/res/values*/strings.xml`. The app has English, Hindi (`hi`), Tamil (`ta`) and Kannada (`kn`). New languages and corrections from native speakers are very welcome.

## Optional: secret scanning before you push

There's no pre-commit hook. If you'd like a local check, [gitleaks](https://github.com/gitleaks/gitleaks) works well:

```bash
gitleaks detect --source . --no-banner
```

Turning on GitHub secret scanning with push protection is also recommended, including on your fork.

## License

By contributing, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE).
