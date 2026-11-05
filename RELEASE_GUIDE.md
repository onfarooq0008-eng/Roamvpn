# Roam VPN -- build and release guide

## 1. Open the project
1. Install Android Studio (current stable). It brings its own JDK.
2. `File > Open...` and pick the `roamvpn` folder. Let Gradle sync finish (first sync downloads a lot).
3. If Android Studio offers to update the Gradle/AGP version, say no for now; build first, upgrade later.
4. Press the green Run button with a phone or emulator selected.

## 2. Create your upload key (once)
Run from the project folder (keep the file safe, back it up, never commit it):

    keytool -genkey -v -keystore roamvpn-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias roamvpn

Copy `keystore.properties.example` to `keystore.properties` and fill in the passwords you chose.
Both files are git-ignored. You may reuse the FastVPN key or make a new one; Google Play lets each app
use its own upload key. If you lose the key you can request an upload-key reset from Play Console.

## 3. Build the signed Android App Bundle (AAB)
Android Studio: `Build > Generate Signed App Bundle / APK... > Android App Bundle`, choose
`roamvpn-release.jks`, alias `roamvpn`, build type `release`.
Or from a terminal: `./gradlew bundleRelease`
Output: `app/build/outputs/bundle/release/app-release.aab`

## 4. Before the first upload -- things only you can do
- [ ] **Yandex Ads:** create two ad units (native, interstitial) for `com.roamvpn` in your Yandex partner account and paste the IDs into `app/src/main/java/com/roamvpn/ads/AdConfig.kt` (`nativeAdUnitId` / `interstitialAdUnitId`). The IDs currently there come from your India VPN project and must be replaced. Add Yandex's `app-ads.txt` line to `backend/api/public/app-ads.txt`.
- [ ] **Backend URL / HTTPS:** `DEFAULT_BACKEND_API_URL` in `app/build.gradle` is `https://api.fastvpnn.pp.ua`. Confirm it is the backend you want for the new app.
- [ ] **Website:** redeploy `backend/api/public/` (new Roam VPN pages and logos). Check that `/privacy.html` and `/terms.html` open.
- [ ] **Contact email** (`contact_email` in `res/values/strings.xml`) is still `admin@fastvpnn.pp.ua`; change it if you want a Roam VPN address.
- [ ] **Play listing:** the Rate / Share links point to `https://play.google.com/store/apps/details?id=com.roamvpn`; they only work once the listing is live.
- [ ] Run the checklist in `MANUAL_TESTING.md` on a real device.

## 5. Play Console checklist (separate new app)
- [ ] Create a new app with package `com.roamvpn`; enrol in Play App Signing.
- [ ] App category and VPN declaration: complete the **VPN service** permission declaration (core functionality is a VPN; tunnel encrypted; describe that it does not collect personal data beyond what the privacy policy lists).
- [ ] **Data safety form:** must match the real behaviour. Declare at least: device or other IDs (advertising ID, shared with Yandex Mobile Ads), and the device's public key / assigned tunnel address sent to your backend. Check Yandex's own SDK data disclosure and answer from the SDK's current documentation.
- [ ] **Ads declaration:** "Yes, contains ads".
- [ ] **Privacy policy URL:** `https://api.fastvpnn.pp.ua/privacy.html` (or your own domain).
- [ ] Content rating questionnaire, target audience (not for children), news/government declarations.
- [ ] Store listing: use the app name "Roam VPN", the 512x512 icon (generated from your logo), screenshots from the real app, and no claims you cannot prove ("no logs", "anonymous", "unlimited", "fastest"). Describe what the app really does.
- [ ] Check the current **target API level** requirement in Play Console help before each release; the project targets API 36.
- [ ] Upload the AAB to Internal testing first, install from the Play link, test again, then promote.
- [ ] `app/build.gradle` `versionCode` must increase on every upload.

Passing these checks does not guarantee approval; Google decides during review.

## Signed builds in GitHub Actions

Add these repository secrets (Settings > Secrets and variables > Actions): `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. Pushes to main build a debug APK plus a signed APK and AAB; a `v*` tag fails if secrets are missing and creates a GitHub Release. Raise `appVersionCode` in `app/build.gradle` (or set the `VERSION_CODE_BASE` variable) for every Play upload.
