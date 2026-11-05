# Roam VPN -- Android VPN app (WireGuard)

Roam VPN (package `com.roamvpn`) is a WireGuard VPN client for Android. It connects to WireGuard servers you run
yourself, and gets its server list and a per-device tunnel address from the control API in `/backend`.

This project is the rebranded and redesigned successor of the old FastVPN app. It is meant to be published as a
**separate** Play Store app. See `AUDIT_REPORT.md` for what was inspected, changed and fixed, and
`RELEASE_GUIDE.md` for building and publishing.

## What the app does

- Splash, welcome and "Your Privacy Matters" first-run screens (the privacy screen must be accepted once)
- Home: big connect ring (Not Connected / Connecting / Connected), live timer, data used, server IP, current location
- Cancel a connection attempt by tapping the ring again; clear progress text and error messages with retry
- Locations: servers grouped by country, search, All / Fastest / Favorites (+ Streaming if the backend flags servers), "Optimal Location" auto-select
- Settings: Auto Connect, Notifications, Kill Switch shortcut, Split Tunneling, DNS choice, legal links, About
- "Connected" is only shown after the app has verified that traffic really flows through the tunnel
- Persistent notification with a one-tap Disconnect button
- Speed Test (ping, download, upload) and exit-IP display
- Yandex Mobile Ads (native cards on Home and Locations, interstitial after every 3rd disconnect), shown only after the user allows ads
- Dark theme only (matches the new design)

## Project layout

| Path | What it is |
|---|---|
| `app/` | The Android app (Kotlin, ViewBinding, Material Components) |
| `backend/` | Control API + per-VPS agent (Node.js). Also serves the website and privacy/terms pages |
| `server-setup/` | WireGuard server helper scripts |
| `OLD_FastVPN_*.md` | Old FastVPN notes, kept for reference only |
| `MANUAL_TESTING.md` | Checklist to run on a real device |

## Open in Android Studio

1. Install the current stable Android Studio and JDK 17+.
2. `File > Open` and choose this folder. Wait for Gradle sync.
3. Run on a phone or emulator (debug builds use Yandex demo ads).

Servers: see `backend/README.md`. The backend URL is set in `app/build.gradle` (`DEFAULT_BACKEND_API_URL`).
