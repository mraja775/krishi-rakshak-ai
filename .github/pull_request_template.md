## What & why

## How it was tested
- [ ] `./gradlew assembleDebug`
- [ ] `cd functions && npm run lint && npm run build` (if functions changed)
- [ ] `cd tests/rules && npm test` (if rules changed — add a test for new rules)
- [ ] Ran the app on a device/emulator (if UI changed)

## Checklist
- [ ] No secrets, `google-services.json`, keystores, service-account keys or personal data committed
- [ ] Security rules still deny by default; no `request.auth == null` grants
- [ ] Any new AI/paid call path respects the rate limit, daily cap and kill switch
- [ ] Docs updated if behaviour or setup changed
