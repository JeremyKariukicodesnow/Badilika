# Badilika validation — combined Java Android app

## Current 1.3 app-timer cooldown and UI build

Validated on 22 October 2026 with the Android 15 (API 35) disposable emulator.

- **63 host policy/DNS checks passed**.
- **64 Android integration checks passed** on the final APK. These cover existing VPN/history/notifications/navigation and real minute-boundary app closing, plus 24 cooldown checks: early increase/disable/removal/pause refusal, unchanged-rule preservation, cancellation/restart, monotonic persistence, shortening refusal, immediate tightening, explicit confirmation, one-use unlocks and all-app pause gating.
- **13 upgrade checks passed**, installing 1.3 over 1.2 without clearing data: saved goals, settings, protection locks and all daily history rows matched. A running protection countdown and its custom duration remained intact.
- Manually tested the revised radial picker: selected hour 1 and dragged the minute hand to 17. “Use duration” returned 01 h 17 min to the editor. Saving this increase over a 30-minute rule offered a cooldown and left the original rule active.
- Started that cooldown through the real UI. After force-stop and reopening, the 30-minute allowance, usage/remaining counters and pending countdown remained visible.
- Live time reads use the same DailyUsage event calculator as enforcement, on a background thread. Usage access is required; the long-term journal need not be running for today's timer counters.
- Core LockState, AppLimitService, History, DailyUsage, TrackerService and GuardService files still match the pre-redesign baseline. New TimerLocks gates saves/removal and global pause; no database schema change or new permission.
- APK v1/v2/v3 signatures and ZIP alignment passed. Same package and local certificate; version code 5, minimum API 26, target API 35.
- Cooldown completion tests simulate persisted zero remaining time. Actual day/month soak tests and further physical/OEM device checks remain outstanding.

- Final Home, App timers, Protect and Insights screens were visually inspected. Shared radial and day pickers were also checked at font scale 1.3. Screenshots: screenshots/v13-*.png.

Artifact: Badilika-1.3.apk, version code 5.
SHA-256: 2135FAB62D6A29716609B46F1CC5A4459DD95F76E8C021575D60D12BFB97358D

## Earlier 1.2 UI and charging-clock build

Validated on 22 October 2026 using an isolated Android 15 (API 35) emulator.

- **40 Android integration checks passed** on the final APK, including real app-limit enforcement, reopening prevention, increasing allowances, pause/removal, VPN filtering, notifications, history and navigation.
- **63 host policy/DNS checks passed**.
- **13 upgrade checks passed** after installing 1.2 over 1.1.1 without clearing data. All saved goals, rules, settings and locks matched; daily history rows matched; a pending protection countdown remained active with its custom duration.
- Core LockState, AppLimitService, History, DailyUsage, TrackerService and GuardService sources matched their pre-redesign hashes. The UI uses existing preferences and database schema.
- Android Settings listed Badilika Analog Clock. Both system preview and automatic charging/idle activation displayed the clock. dumpsys confirmed the automatic session was not a preview; touch dismissed it.
- Visually inspected the dashboard, timers, settings, utility menu, clock setup and full-screen analog clock. Screenshots are in screenshots/v12-*.png.
- Same package and signing certificate as 1.1.1; version code increased from 3 to 4. APK signing and ZIP alignment passed. Minimum API remains 26; target API remains 35.
- Manufacturer screen saver availability and physical-device behavior of 1.2 remain receiving-phone checks. No claim of testing every Android phone.

Artifact: Badilika-1.2.apk, version code 4.
SHA-256: 9583F39BB479239EF7E471D7932608BF46FE7E36592279FA20708F691E73C0A6
## Earlier 1.1.1 compatibility build

- Minimum Android API lowered to **26 (Android 8.0)**, with D8 targeting the same minimum. Whole-phone screen/unlock charts are gated to Android 9+, where those events exist. Android 8 retains app-time, data, timers and protection views, with an explicit metric-availability notice.
- Target API remains 35. No processor-specific native libraries or Play-services dependencies are packaged; this is one universal DEX APK, not architecture splits.
- Explicit v1/v2/v3 signing verified across schemes with apksigner's verification minimum override. This override is a signing check only; the application still requires Android 8+. ZIP alignment verified.
- **Clean install on Android 15 succeeded**, and the updated integration suite passed **40 checks**. The fresh-install test now prepares VPN consent after the emulator app-op grant; the old test had relied on consent left by an earlier install. Production already uses the VPN consent flow.
- All **63 host checks** passed on the current sources.
- The transfer ZIP contains exactly one APK. Its extracted SHA-256 was checked against the distributable, byte for byte.
- **The user subsequently confirmed installation and normal operation on their Nothing Phone (3a).** The original generic installation error was not diagnosed on that handset. The original APK also passed all signing checks. This build and the ZIP transfer are a compatibility/re-transfer attempt, not proof that a specific signing fault was fixed. If the fresh package still fails, obtain the exact PackageInstaller/ADB error from the phone.
- Android 8 emulator verification is recorded below when completed. Other manufacturer firmware/security policies and physical ARM devices remain untested.

Current artifact: Badilika-1.1.1.apk, version code 3.

SHA-256: CDB72C4EF6647AE1533E112F079BC057E1BB37FFC755ABAE4CDB281310D1AFE8

## Earlier 1.1 validation

Performed on 22 October 2026 using SDK 36/build-tools 36.0.0 and an isolated Android 15 (API 35), x86_64 AOSP emulator.

## Passed

- Production Java compiled into one combined APK: version code 2 / version name 1.1, minimum API 29, target API 35.
- APK v3 signature verified. Same package/signing key as Badilika 1.0; update installation retained its local history.
- **63 host policy/DNS checks**, including 10,000 malformed inputs: full-day boundary, reboot/backward ticks, 30-day maximum, custom days/hours/minutes, daily budget boundaries, disabled/malformed rules, saved-rule compatibility, accumulated sessions, overlapping activity intervals, midnight clipping, screen-off/shutdown behavior, current open sessions and DNS packet construction.
- **39 Android integration checks**: separate protection locks, persisted requests, early-disable denial, cancellation, immutable pending duration, full 30-day state, explicit disable confirmation, combined navigation, real Usage access/SQLite reports/hourly bins, actual VPN establishment, blocked porn/escort DNS, allowed example.com resolution, background saving and foreground notification.
- The integration runner launched a separate harmless test app and waited for a **real daily minute boundary**. It stayed usable under its allowance, returned to Home after exhausting it, and returned Home again on reopening. Increasing its allowance restored access. Pausing enforcement and removing the timer both left it usable. Badilika and Android Settings are excluded.
- **Actual radial-clock UI tested**: selected hour 1, dragged the minute hand to 17, confirmed and saved. The timer card rendered **1h 17m per day**. There is no preset duration menu.
- **Protection picker UI tested**: selected 1 day and clock time 02:15, saved, and confirmed **Locked · 1d 02:15:00 disable delay**. Minimum/maximum clamps are covered by policy tests.
- Visually inspected current screenshots: screenshots/timer-clock.png, screenshots/protection-clock.png, screenshots/timers.png and screenshots/protection.png. Companion screenshots belong to the earlier version.
- No AndroidRuntime crash observed during the final runs.

## Test isolation

The device script builds an instrumentation runner and harmless timer fixture under ignored build/integration/. Neither is included in the distributable. It changes rules and grants permissions only on the isolated emulator. Production code never enables its own Accessibility permission; users opt in through Android settings after the disclosure.

Instrumentation restarts its target process, which Android marks as a stopped Accessibility connection. The runner re-enables it after that test-only restart and uses FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES. VPN tests wait for network propagation. Protection-expiration tests simulate the final persisted state; they do not wait an actual day/month.

## Remaining receiving-phone checks

- OEM battery restrictions, locked-phone reminders, recovery after reboot/force-stop, multi-window apps, background audio and Android versions other than 15.
- Actual day/month soak tests and real midnight/timezone transitions. Midnight clipping is unit-tested.
- Carrier accounting, multiple SIMs, roaming and delayed network buckets.
- Additional domain coverage, custom/secure DNS, cached connections and browser configurations.
- CSV document-picker flow and installation after WhatsApp transfer.
- Accessibility/font-scale testing beyond the standard emulator setup.

App closing uses Home navigation, not force-stop; background audio/downloads may continue. Users can edit/pause/remove timers or revoke Accessibility permission. Router/provider attribution remains unavailable. DNS filtering is not universal content detection. These limits are explained in Settings and README.

## Distributable SHA-256

Badilika.apk (1.1): 78A912F9A4F66344965B22F442BA06AFDF8CAA1F36680CC3A14A43C12E878D05

Run ./mobile/test.ps1 for host checks. Build, connect an isolated Android test device and run ./mobile/test-device.ps1 for integration checks. Do not run device tests against a personal installation with important rules or history.
