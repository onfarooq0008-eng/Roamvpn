# Roam VPN -- audit, changes and verification report

Source project: `fastvpn_ci_upgraded.zip` (FastVPN v1.0.5). Decisions given: publish as a **separate app**, keep **WireGuard**,
redesign **all screens** plus matching extra screens, new package **`com.roamvpn`**.
The original is untouched; all work is in this copy.

## 1. What was found (Phase 1 audit, before changes)

**Stack:** Kotlin + XML/ViewBinding, AGP 9.0.1, Gradle 9.1, compile/target SDK 36, min SDK 24. WireGuard via the official
`com.wireguard.android:tunnel` GoBackend. Node.js backend (`/backend`) registers each device and hands out a tunnel
address. Yandex Mobile Ads. OkHttp, coroutines, EncryptedSharedPreferences.

**Works as designed (kept as is):** WireGuard tunnel setup, per-device key kept on-device, server-allocated tunnel IP, backend
register/unregister leases, real traffic verification before showing Connected, failover to up to 2 other servers,
bounded timeouts (API 15 s, verification 10 s), single shared tunnel manager, notification Disconnect button, split tunneling,
DNS choice, IPv6 fail-closed routing, no hard-coded private keys or credentials in the app.

**Problems found**

| # | Finding | Action |
|---|---|---|
| 1 | Rotating the phone (or any activity recreation) cancelled a connection in progress and tore the tunnel down | Fixed: `MainActivity` handles orientation/size changes itself (manifest `configChanges`) |
| 2 | No way to cancel a connection attempt; only toasts for errors | Fixed: tap the ring while connecting to cancel; progress text and a persistent red error with "Tap to retry" |
| 3 | `syncStateFromBackend()` could report DOWN while a connect was still in flight, making the state flicker | Fixed: returns CONNECTING while a connect holds the operation lock |
| 4 | On resume during connect verification the UI could start the timer / mark an old server connected | Fixed: guarded by `connectionFlowActive`; timer stopped on failure/cancel |
| 5 | If anything threw after the tunnel came up, the tunnel could be left running while failing over | Fixed: tunnel is brought down before moving on |
| 6 | `roundIcon` pointed at the square icon | Fixed |
| 7 | Server-list failure on first load showed a blank/misleading "No locations found" | Fixed: Loading, Can't load (with Try again) and Empty states |
| 8 | Dialogs used the platform AlertDialog (could render light on the dark theme) | Ads dialog later removed (see section 6); other dialogs use AppCompat |
| 9 | Documentation was stale (mentioned Meta ads, `VpnStateUtil`, a cleartext exception that no longer exists) | Old docs moved to root `OLD_FastVPN_*.md`; new README written |
| 10 | Light/dark toggle: new design is dark-only | Light theme removed (see Decisions) |

**Not verifiable here / left unchanged (reported, not hidden)**
- Latency shown in Locations is a TCP connect time to port 22 of each server (WireGuard is UDP and cannot be probed that way). A server with port 22 firewalled shows as Offline and is never auto-selected.
- "Optimal Location / Auto select" keeps the original behaviour of choosing a **random reachable** server (to spread load), not the lowest-latency one. The **Fastest** chip sorts by measured latency.
- Behaviour on Wi-Fi <-> mobile switches and OEM battery managers depends on the WireGuard library and device; needs real-device testing.
- There is no separate ads dialog any more (requested): ads are allowed automatically when the user taps "Agree & Continue" on the first-run consent screen, which now says the app shows Yandex ads and that Yandex may use the advertising ID. Risk to know about: for users in the EEA/UK/Switzerland a bundled terms tap is generally not valid consent for personalised ads (GDPR/ePrivacy), and Google Play's user-data policy expects the disclosure to be prominent. If you target those regions, use a consent management platform or make ads non-personalised there.
- `EncryptedSharedPreferences` is deprecated upstream; still works.

## 2. Branding and package (Phases 2 and 6)
- App name **Roam VPN** everywhere in the app, notification ("Roam VPN - Connected"), settings, About, share text, website pages, docs.
- Application ID and namespace `com.roamvpn`; Kotlin packages moved to `com/roamvpn/`; manifest, receiver action (`com.roamvpn.ACTION_DISCONNECT`), tunnel name (`roamvpn`), preference files, notification channel, log tags renamed. Version reset to **1.0.0 (code 1)** for the new listing.
- New launcher icons (adaptive + legacy + round), in-app logo, notification icon kept as shield, 512x512 Play icon (`play_icon_512.png`), all generated from the logo you supplied. Website logos/favicons/og-image regenerated.
- Backend server-side names (systemd services, `/opt/fastvpn-*`, certificates, domain `fastvpnn.pp.ua`) were **deliberately not renamed**: renaming would break servers that are already deployed.
- A new package means a new Play listing, new Yandex ad units needed, and users do not carry over from FastVPN.

## 2b. Synced with FastVPN v6 (`fastvpn_ci_upgraded_v6.zip`)

The v6 backend (`backend/`, `server-setup/`) and `app/build.gradle` are identical to the earlier FastVPN project, so Roam VPN keeps using the same API contract (`/api/servers`, `/api/register`, `/api/unregister`) unchanged. The only backend differences from v6 are the rebranded website files in `backend/api/public/`.

App-side v6 improvements ported into Roam VPN (package `com.roamvpn`):
- `ServerCache` / `ServerSource`: server list saved on disk for 24 h, shown instantly, pull-to-refresh forces a fresh fetch; falls back to the saved list if the network fails. New unit test `ServerCacheTest`.
- `VpnTunnelManager`: `stateFlow`; the tunnel reports when it goes down externally and clears the notification; `CONNECTING` can never get stuck.
- `VpnActionReceiver`: notification is removed as soon as the tunnel is down.
- `SecureKeyStore`: `activeRegistration()` is synchronized.
- `ConnectivityCheckUtil`: connectivity probe tries 1.1.1.1 and 8.8.8.8.
- `MainActivity`: cached list on startup, instant UI reset when disconnected from the notification, ping-before-connect when the list came from cache, lease release off the failover path, background pinging only on the Locations tab.

Not ported: v6's "N servers updated X ago" label and connected-card tint (they belong to v6's own layout; the Roam design replaces it). Nothing was compiled: the Android SDK and Maven are unreachable here, so this merge has only passed the static checker and the 23 backend tests.

## 2c. Ads: Unity removed, Yandex Mobile Ads SDK 8.6.0 added

Unity Ads is gone from the code, dependencies, consent text, privacy page, terms and app-ads.txt. `com.yandex.android:mobileads:8.6.0` is now the only ad network:
- A native ad card in the Home server list and one in Locations (`NativeAdSlot`, `view_native_ad.xml`), plus an interstitial after every 3rd manual disconnect (`AdConfig.INTERSTITIAL_EVERY_N_DISCONNECTS`). Never shown while connecting or when switching servers. No banner.
- The SDK's automatic start-up is disabled in the manifest; it starts only after the user taps "Agree & Continue" on first run (`AdsConsent`).
- Failed loads retry with back-off (`RETRY_BASE_MS`, `RETRY_MAX_TRIES`). ProGuard keeps `com.yandex.mobile.ads.**`.
- Ad unit IDs in `AdConfig.kt` are **pre-filled with the IDs from your India VPN project** (`R-M-20337113-1/-2`). They belong to that app: create units for `com.roamvpn` in the Yandex partner account and replace them before publishing. Debug/test builds use Yandex demo units.
- Not compiled here. Add Yandex's app-ads.txt line, update the Play Data safety form, and confirm the Yandex privacy link in `privacy.html`.

## 2c-2. Upgrades ported from the India VPN project

- Engine: parallel multi-port server ping, faster connectivity probe through the VPN network, shorter backend timeouts, non-blocking lease release when switching servers, connection timer that survives rotation. Server rows show Online / Offline / Checking instead of ms.
- `/api/myip` backend route + `IpLookup`; Home shows the real exit IP.
- Speed Test screen (`SpeedTestActivity`, `SpeedTest`, `SpeedGaugeView`) with ping, download and upload against speed.cloudflare.com; bottom-nav "Speed" item.
- Settings: Clear Logs (clears the cached server list).
- Build: `appVersionCode/appVersionName` Gradle properties, lifecycle-runtime-compose exclusion, safer ProGuard rules, `tools:replace` for the network security config, signed APK/AAB GitHub Actions workflow (needs the four keystore secrets, see RELEASE_GUIDE.md).
- Deliberately not ported: side drawer, Recommended tab, light/blue theme and Dark Mode switch (Roam is dark-only), India server pinning, the Language row (English only) and India branding.

## 2d. Website pages (Umar Farooq, admin@fastvpnn.pp.ua)

Written in the Roam dark style: `privacy.html`, `terms.html`, `ads.html` (Ads & Third Parties), `delete-data.html`, `disclaimer.html`, `about.html`, `contact.html`, `404.html`, plus `robots.txt` and `sitemap.xml` (all under `backend/api/public/`). `server.js` now also serves clean URLs (`/privacy`) and returns the 404 page / a JSON 404 for unknown API paths. The home-page footer links to every page.
Corrections made: the old privacy text said the app asks before showing ads (no longer true since the ads dialog was removed); it now says ads start when the user taps Agree. The policy also now states honestly that the VPN server and API see your public IP and that standard server logs may exist.
Not verified: the server could not be started here (the npm registry is unreachable), so routes were checked by reading the code and by static HTML/link validation only. Please open each page after deploying.
Things for you to confirm: the policy says registrations unused for 30 days (and unfinished ones within about an hour) are removed (taken from `server.js`); the Terms say Pakistan law governs (carried over from the earlier terms; change if wrong); log retention is described generally because the nginx/journal settings are not set by `setup.sh`. Use `https://api.fastvpnn.pp.ua/privacy` as the Play Console privacy-policy URL.
Backend note: at start-up `server.js` prints the admin key to the system journal. Anyone who can read the journal can read it; consider removing that line after first setup.

## 3. New UI (Phases 3 and 4)
Dark navy/blue design from your reference, built with native views (no screenshot used as a background).

| Screen | Status |
|---|---|
| Splash (gradient, logo, mountains, progress bar) | New |
| Welcome / Get Started | New (landscape drawn as vector art) |
| Your Privacy Matters (consent, links, accept/decline) | Redesigned; replaces old consent screen |
| Home: ring, status, timer, location card, data / duration / server IP | Redesigned |
| Locations: search, All / Fastest / Favorites (+Streaming), country rows with ms, stars, signal bars, Optimal Location row | Redesigned |
| Settings: header card, general + other groups | Redesigned |
| Split tunneling, notification | Restyled |
| Premium | Built but **hidden** (see below) |

**Elements of the reference that were intentionally NOT made functional, because the feature does not exist:**
- **Account / "Already have an account? Sign In":** the app and backend have no accounts (and the privacy policy says no account is required). Omitted; the welcome screen says "No account required".
- **Premium:** there is no billing or paid tier. `PremiumActivity` exists with the designed layout, but the crown that opens it is hidden while `SHOW_PREMIUM` is `false`. Its three feature lines are placeholders. Do not enable it until Play Billing and backend entitlement exist.
- Reference text such as "100+ countries", "priority customer support", "unlimited data", "No logs policy", "Protects your identity" were **not** copied, because they cannot be verified from the code. The privacy screen uses statements the app really satisfies.
- "Connection Protocol: Auto (Recommended)" became "WireGuard" (the only protocol).

## 4. Permissions, privacy, Play compliance (Phase 7)
Permissions: INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS, AD_ID (for Yandex), BIND_VPN_SERVICE on the WireGuard service. Location and storage permissions are explicitly removed. No `QUERY_ALL_PACKAGES` (uses a narrow `<queries>`). All are in use; none were removed.
Release build: cleartext traffic blocked, debug network config only in debug, R8 + resource shrinking on, logs at debug/verbose stripped.
Marketing text on the website was edited to remove claims the product cannot prove ("Unlimited data", "speed boost", "identity private").
**You must still:** complete the Play VPN declaration and Data safety form accurately (see `RELEASE_GUIDE.md`), fill in the Yandex ad unit IDs, and check that your servers really keep no browsing logs before keeping "No browsing logs" wording. Nothing here is a guarantee of approval.

## 5. Verification actually performed
| Check | Result |
|---|---|
| Android debug / release build | **NOT RUN.** This environment cannot reach Google/Maven/Gradle servers and has no Android SDK. |
| Unit tests (`CountryGroupTest`) / instrumentation tests | **NOT RUN** (need the build). Test file moved to the new package. |
| App on a device/emulator, VPN connect/disconnect/cancel/timeout/switch | **NOT RUN.** No device, no WireGuard servers. Use `MANUAL_TESTING.md`. |
| Screenshots of the new layouts | **NOT PRODUCED.** Layouts can't be rendered without the SDK. Compare against your reference on first run. |
| All XML files well-formed (71 files) | Passed |
| Every `@drawable/@color/@string/@style/@id/...` reference resolves; every `R.x.y` in Kotlin resolves | Passed (custom checker; also proven to catch deliberate errors) |
| Every ViewBinding field used in Kotlin exists in its layout, including included layouts | Passed (same checker) |
| Brace balance, project imports resolve | Passed |
| Independent line-by-line code review acting as the compiler | No compile errors found; 3 confirmed bugs and several suspected ones found and fixed (cancel flag, API-26-only attributes on minSdk 24, stale timer, tunnel left up on exception, double bottom inset) |
| Backend unit tests (`npm test`) | **23 / 23 passed** |
| Release configuration scan | No secrets in the project; signing comes from git-ignored `keystore.properties`; no debug endpoints in main; `USE_TEST_ADS` false for release |

Because the code was never compiled, expect that the first Gradle build may surface a small error the static checks could not see. Send me the message if it does.

## 6. Decisions you may want to revisit
1. Dark-only theme (the reference has no light variant).
2. Premium and Account omitted/hidden (no backend support).
3. Ad-required gate kept as in the original.
4. Auto-select stays random-among-reachable.
5. Welcome/privacy are two screens; accepting still gates all network calls.
