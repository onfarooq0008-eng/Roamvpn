# Manual testing checklist -- required before relying on this in production

Some behavior here depends on real OS/OEM process-management decisions
(when Android kills a process, whether it restarts a service, how
battery-management skins like MIUI/ColorOS/EMUI treat foreground VPN
services) that cannot be verified by reading source code or running in a
sandbox. These need an actual physical device. Do not consider this app's
disconnect/notification behavior proven correct until this checklist has
been run and passed.

## Tunnel lifecycle after Activity/process death

Covers: `VpnTunnelManager`, `VpnTunnelManagerHolder`, `VpnActionReceiver`,
`NotificationHelper`. Logcat tags to filter on while testing:
`Roam VPN-Tunnel` and `Roam VPN-Disconnect`.

**Steps:**
1. Connect to any server from the app. Confirm the "Connected" notification
   appears with a Disconnect action.
2. Kill the Activity -- swipe Roam VPN away from Recents (this is the common,
   everyday case: the underlying VPN service should keep running and the
   process should stay alive, since Android gives elevated priority to a
   process running a real foreground VPN service).
3. Open the notification shade and tap **Disconnect**.
4. **Confirm the WireGuard interface actually disappears** -- check
   `adb shell ip link` or `adb shell wg show` (if available) before and
   after step 3; the `roamvpn` interface/tunnel should be gone after.
5. **Confirm Android's own VPN state clears** -- the status bar VPN key icon
   should disappear, and Settings -> Network -> VPN should no longer show
   Roam VPN as connected.
6. **Confirm the backend peer was actually removed** -- check the dashboard
   at `https://api.fastvpnn.pp.ua/` (registered device count should drop),
   or query the VPS node directly (`wg show` on the VPS itself should no
   longer list this device's public key as a peer).
7. Check logcat for the full expected sequence with no gaps:
   `onReceive: disconnect action from notification` -> `state before
   disconnect: UP` -> `disconnect: tunnel torn down` -> `backend peer
   unregistered for server ...` -> `notification cleared`.

**If any step fails:** the logcat trail from step 7 tells you which stage
broke (local teardown vs. backend unregister vs. notification) -- fix that
specific stage rather than assuming the whole flow needs a rewrite.

## Harder variant -- true process death

Repeat the above, but instead of just swiping from Recents, force the
process to die completely before testing Disconnect:
```
adb shell am kill com.fastvpnn.app
```
or, for a closer simulation of aggressive OEM battery management, disable
battery optimization exceptions for the app and let the OS kill it under
memory pressure naturally (harder to trigger on demand, but the most
realistic real-world case for aggressive OEM skins).

Expected: since the in-process wireguard-go native runtime dies with the
process, there should be no real tunnel left for `getRunningTunnelNames()`
to find on restart -- the app should just come up in a DOWN state. Confirm:
- The WireGuard interface is actually gone (step 4 above) -- if the OS
  auto-restarted the VpnService before your check, this may show
  differently than expected; note what you observe.
- The backend peer registration will be left stale in this scenario, since
  no disconnect/unregister call ever fires from a fully-dead process.
  **This is now handled server-side**: `backend/api/server.js` runs a daily
  sweep (`pruneStaleRegistrations`) that removes any registration not
  refreshed in 30+ days, calling the VPS agent's `/remove-peer` first so the
  real WireGuard peer is actually removed, not just the bookkeeping. This
  was added as part of this same fix -- verify it's actually deployed
  (`grep pruneStaleRegistrations backend/api/server.js` on the VPS, and
  check `journalctl -u fastvpn-api` a day later for a "Pruning N stale
  registration(s)" line if you have any to prune). 30 days means this
  won't reclaim a slot quickly -- if you need faster reclaim, lower
  `STALE_REGISTRATION_MAX_AGE_MS` in server.js.

## Sign-off

Do not check this off from code review alone. Run it on at least one real
device, ideally one with an aggressive OEM battery-management skin (MIUI,
ColorOS, EMUI, or similar), before considering this production-ready.

- [ ] Steps 1-7 (Activity death, foreground service survives) -- passed
- [ ] True process death variant -- observed and documented, gap
      (if any) understood and accepted or mitigated

---

## Added for the Roam VPN redesign -- run these on a real device too

Nothing below could be run when the redesign was produced (no Android SDK or device was available).

1. **First launch:** Splash -> Welcome ("Get Started") -> Your Privacy Matters ("Agree & Continue") -> Home. Privacy Policy / Terms of Service links open. The X button closes the app.
2. **Connect:** tap the ring. Status must go Connecting (with step text: Contacting server / Establishing secure tunnel / Verifying connection) -> Connected only after verification. Timer, data used and server IP fill in.
3. **Cancel:** tap the ring while it says Connecting. It must return to Not Connected within a couple of seconds, with no key icon left in the status bar, and the button must work again.
4. **Failure / retry:** turn on airplane mode, tap connect. A red message with "Tap to retry" must appear. Turn it off and tap again.
5. **Rotate the phone** while Connecting. The attempt must not be cancelled.
6. **Server switching:** connect, open Locations, pick another country. Rapid double taps must not start two connections.
7. **Locations:** search, All / Fastest / Favorites chips (star a country first), pull to refresh, expand a country with 2+ servers, "Optimal Location" row.
8. **Settings:** Auto Connect, Notifications (Android 13+ asks for permission; refusing flips the switch back), Split Tunneling, DNS (changing it while connected reconnects), Kill Switch opens Android VPN settings, Rate / Share / Help / Privacy / Terms / About.
9. **Notification:** Disconnect button works with the app closed.
10. **Wi-Fi <-> mobile data switch** while connected: traffic keeps flowing (or the app shows Not Connected once the tunnel drops).
11. Android 7 (API 24/25) device or emulator: spacing looks right.

## Speed Test, IP tile, Clear Logs, ads (added with the India VPN sync)
- [ ] Bottom nav > Speed: Start shows ping, then download, then upload; Stop cancels; works with VPN on and off.
- [ ] Connected: the Home "IP Address" tile shows the VPN exit IP, not your own.
- [ ] Settings > Clear Logs > Clear: toast "Cleared"; the server list reloads next time.
- [ ] Server rows show Online / Offline / Checking, no ms values.
- [ ] Switching servers does not hang on the old connection.
- [ ] Ads (debug = demo): native card in Home and Locations; interstitial only after the 3rd manual disconnect; none while connecting.
- [ ] Rotate during a connection: timer keeps the real time.
