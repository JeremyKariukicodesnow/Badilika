# Badilika for Android — Java edition

## Version 1.8

On first launch after updating, users with saved app timers can confirm **Clear all app timers** to clear app limits, minimum goals and app-timer cooldowns. Choose **Later** to keep the option in **App timers → Review one-time reset**. Usage history and porn and escort protection settings are preserved. Set up daily or repeating limits afterward.

## Version 1.7

**Settings → Uninstall protection** opens Android's optional device administrator activation screen. Once enabled, deactivate Badilika in Android Settings under **Device admin apps** before uninstalling. This adds a deliberate step without requesting device policies.

**App timers → Change timer → Choose cooldown** accepts waits from one minute to 30 days. Set days to zero for an hours-and-minutes wait. Increasing or removing an allowance still requires the configured wait. Porn and escort protection retain their separate one-day minimum.

Enable **Repeat every few hours** in a timer editor to choose a repeating window. For example, choose 30 minutes of allowed time and a five-hour window. The first window starts when you save its schedule. Unused time expires at each reset. Switching between daily and repeating limits or changing the window length requires a cooldown.

## Version 1.6: interactive setup

Open **Settings → Setup guide** for seven visual steps with live status, numbered instructions, progress buttons and direct actions. Usage access, Accessibility, VPN, network, notification and app settings open the relevant Android controls where supported. Protection and timer actions open the real Badilika screens; **Continue setup guide** returns to the saved step.

The guide can be skipped or reopened. It does not enable protection, change saved allowances, start cooldowns or grant permissions on the user's behalf. Permissions are checked again when the app resumes; Android screen names vary by manufacturer.

Download the latest APK from https://badilika.sparkfield.co.ke. Install over the existing app with the same signing key to preserve local records.

One combined app targeting **Android 6.0 and newer**:

| APK | Purpose |
| --- | --- |
| `dist/Badilika.apk` | Local DNS content protection, separate porn/escort locks, daily app timers with automatic closing, goals, notifications and all Insights reports |

The forest-green, sage and cream interface is native Android, implemented in Java without third-party runtime libraries. **Version 1.4 keeps the mobile interface unchanged and strengthens the background protection service:** Android 6–7 foreground-service compatibility, IPv6 DNS handling, interception of common public resolver endpoints, and a safely validated adult-list refresh with an offline bundled fallback. Protection, Timers and Insights remain in one app. Whole-phone interactive/unlock events require Android 9+; Android 6–8 shows app time and data reports and explains that system-metric limitation instead of displaying misleading zero charts. The APK contains no processor-specific native libraries and does not require Google Play services. That does not guarantee every manufacturer's firmware or security policy will accept it. The separate companion is no longer built or needed. Installing this APK over an earlier Badilika preserves its app ID, local history, rules and protection locks when it is signed with the same key.

## Install and share

Send an APK as a **document/file** in WhatsApp. On the receiving Android phone, download it, open it, and allow installation from that source when Android asks. APKs cannot install on iPhones. Do not send the signing key.

Download **dist/Badilika-1.3.apk** directly and install it as an **update**. Do not uninstall Badilika or clear its data: doing so deletes saved timers and history. The package and signing key are unchanged. An optional **dist/Badilika-1.3.zip** contains the same APK if a transfer service requires a ZIP. The app is signed with v1, v2 and v3 schemes and ZIP alignment is checked. The user confirmed the preceding build installed and worked on their Nothing Phone (3a).

If Android reports “App not installed”, preserve the exact error. On an authorized connected device, adb install --no-incremental -r mobile/dist/Badilika-1.3.apk supplies a more specific INSTALL_FAILED diagnostic. Do not uninstall an existing installation to guess at the cause.

1. In **Settings**, grant **Usage access** and **Notifications**.
2. Tap **Start tracking** to save daily history and enable app reminders. On phones that aggressively stop background apps, allow unrestricted battery use in Android's app settings.
3. In the main app, open **Protect** and enable the desired categories. Accept Android's VPN permission dialog.
4. Turn off system Private DNS and browser Secure DNS for this DNS-only filter to receive those queries. This app cannot coexist with another active VPN.
5. Reopen browsers to close cached connections. Check protection on the actual receiving phone before relying on it.
6. For app closing, open **App timers → Enable automatic closing**, read the disclosure, and enable **Daily app timers and automatic return to Home** in Android Accessibility settings. Usage access is also required. If Android restricts a sideloaded app, its App info menu may offer **Allow restricted settings** before the Accessibility switch can be enabled.
7. Choose an app, tap its **Choose on clock** button and use the alarm-style clock to select **hours and minutes**, then save the timer. The clock sets a duration, not a time of day.

## Charging analog clock

Open **Tools → Analog clock → Open screen saver settings**, select **Badilika Analog Clock**, and choose **while charging** in Android's screen saver settings. Enable the system screen saver switch if present. Android starts it after the phone becomes idle while charging; the display must remain powered. Tap to dismiss. Availability and setting names depend on the phone manufacturer.

The black clock face has sage hands, a date and an optional seconds hand. Its position shifts slightly over time. This uses Android's native DreamService, with no overlay permission or wake lock. It cannot display while the phone is powered off and does not replace the manufacturer's always-on display. Existing protection and timers work independently of this feature.
## Protection and the 1–30 day waiting period

Porn and escort protection have **separate locks**. Use **Choose disable delay** to select **days**, then use the alarm-style clock for additional **hours and minutes**. For example, choose 1 day and 02:15 for a 26-hour, 15-minute wait. The minimum is **one full day**, and the maximum is **30 full days** (extra hours/minutes must be zero at 30 days). A pending countdown cannot be shortened by changing the selected duration. Protection remains active until the entire delay passes **and** the user explicitly confirms turning that category off.

**Cancel countdown** removes the request and keeps the category enabled. A subsequent request starts the complete selected delay again. Changing the device date cannot accelerate the countdown: it consumes monotonic elapsed device time. After a reboot it resumes from the last persisted remaining amount; powered-off time does not count. This is deliberately conservative, so the real calendar wait may be longer. Open the protection screen to see the current timer. Optional tracking sends a notification when the delay has completed.

This is a voluntary normal Android app, **not device-owner management**. Android Settings can revoke VPN permissions, switch VPNs, force-stop, clear data, or uninstall it. The app cannot guarantee a 24-hour uninstall/system-settings lock. Always-on VPN can help restart it, but **do not enable “Block connections without VPN”** for this implementation: only DNS traffic is routed through the tunnel.

### Filtering implementation

`GuardService` routes local IPv4 and IPv6 DNS through a `VpnService` interface. It parses direct UDP DNS queries, applies the bundled lists and subdomain matching, and returns NXDOMAIN for blocked names. It also traps traffic to a limited set of common public resolver endpoints while protection is connected. With porn protection enabled, other queries go to Cloudflare Family DNS at `1.1.1.3`; otherwise they go to `1.1.1.1`. Forwarded DNS is UDP, not encrypted DNS. Those resolvers receive queried domain names; Badilika does not log browsing domains or proxy web traffic. The built-in list is used offline, and a downloaded update is accepted only after size and format validation. Escort-only protection uses the curated escort list, not a guarantee of complete category coverage. The curated seed comes from `desktop/adult_domains.txt`.

An arbitrary browser DoH provider, another VPN/proxy, direct IP connections, cached connections and explicit content inside general-purpose apps can still bypass a DNS-only filter. Turn off system Private DNS and browser Secure DNS as described above. No claim of universal content detection or SafeSearch enforcement is made for the mobile version. Additional domain rules apply whenever the protection VPN is connected.

## Daily app timers, closing and notifications

**App timers → Choose an app → Choose on clock** opens an alarm-style, 24-hour clock picker. Select any duration from **00:01 to 23:59**, such as 00:30, 01:00, 02:00 or 01:17. There are no preset menus. Turn **Use a daily time limit** off for no limit. An optional minimum goal has its own clock picker and switch. Its duration cannot exceed an enabled allowance.

### Cooldown before loosening or removing app limits

Every daily allowance has a change/removal cooldown, initially **one full day** for existing timers. Choose **1–30 days**, plus clock-selected hours and minutes, when setting a timer. The 30-day maximum must have zero extra hours/minutes.

Increasing an allowance, turning its limit off, removing it, or shortening its selected cooldown requires a completed wait. Reducing the allowance or lengthening its cooldown can apply immediately. Minimum-goal-only rules do not close apps and are not locked.

Use **Request limit removal / unlock** on the timer card, then **Start cooldown**. The current allowance keeps working throughout the wait. At completion, return and confirm the change or removal; nothing is automatically removed. A successful change consumes the unlock. Saving an unchanged rule preserves an existing request. Cancel keeps the limit; a new request starts the full wait again. Powered-off time does not count, and changing the device date does not shorten the monotonic countdown.

**Pause automatic closing** in Settings requires all current limited apps to complete their own cooldowns. A single action can start missing requests without restarting existing waits. This is an in-app safeguard: Android settings, revoked Accessibility access, clearing storage and uninstalling remain outside its control.

App timers and the Home preview read today's usage directly from Android UsageEvents in the background, without requiring the journal to be running. Visible counters refresh about every 15 seconds and on returning to the app. Both used and remaining time are shown with a progress bar. Reports still use the saved long-term journal and its existing filters.

Daily usage is cumulative across sessions and includes usage earlier that same local day, before the timer was added. The allowance resets at **local midnight**. With automatic closing enabled, `AppLimitService` checks the active selected app about every **two seconds**, using Android's usage events. At the limit it performs Android's **Home** action; reopening an exhausted app returns to Home again. It does not require the separate journal foreground service to run, although **Usage access and Accessibility permission must remain enabled**.

This is **return-to-Home enforcement, not process force-stop**: background playback/downloads may continue. It is best effort, with a small event/polling delay. OEM restrictions, force-stop, revoked permissions, clock/timezone changes, missing usage events and multi-window behavior can affect it. Users can reduce allowances immediately, request cooldowns before loosening/removing timers or pausing closing, or disable the Accessibility service in Android settings. Calling apps, keyboards, system settings, the default Home app and Badilika itself are excluded so system controls remain accessible.

The Accessibility service receives package/window-change events only. Its configuration explicitly **cannot retrieve window content**; it does not read screen text, passwords or messages. The app explains this before taking the user to the system permission screen. Settings contains a **Pause automatic closing** control protected by each app’s cooldown.

The optional journal still checks roughly once per minute for goal-reached and limit-reached notifications, plus an unfinished minimum-goal reminder after 20:00 local time, once per rule/day. Android battery restrictions and denied notification permission can delay or prevent these notifications. Notification permission alone does not enable closing.

## Reports and long-term storage

- Today, yesterday, 7 days, 30 days, year, all history and custom date ranges.
- Search by app label/package; sort by foreground time, data or activity resumes.
- Wi-Fi/mobile/all data filters and per-app upload+download totals.
- Device screen-active time, unlocks, daily trends, and hourly screen-time distribution.
- Per-app daily drilldown and CSV export with one row per app/day for the selected dates.
- SQLite history has no automatic expiry. Collection revisits recent days and preserves archived maxima if Android has discarded older raw events. CSV exports are available through Android's document picker.

App “opens” counts activity resumes, not unique process launches. Multi-window usage may overlap across apps. Network usage comes from Android's historical buckets and may be delayed or approximate; it is not a real-time billing meter. Shared UIDs are shown together. Some system/uninstalled packages appear as UID identifiers. Filtered app totals and whole-phone screen/unlock metrics are labeled separately.

Historical data before installation is limited to whatever Android still retains. Force-stop, permission removal, clock/timezone changes, missing events and battery restrictions affect completeness. No data is synthesized to fill gaps. Long-term reporting requires the journal to remain enabled; uninstalling or clearing storage deletes its history. Backups are disabled.

**Router/provider limitation:** Android does not expose retrospective per-router or per-SIM-provider byte counts to this ordinary app. Wi-Fi combines routers, and mobile combines SIMs/providers. The UI explicitly says so. Exact router/provider accounting would require a different privileged/device-management or router-side integration; this build does not fabricate that attribution.

## Rebuild on Windows

Requires a JDK (the Android Studio bundled JBR works), Android SDK platform 36, and build-tools 36.0.0. No Gradle or Maven download is needed.

```powershell
./mobile/test.ps1
./mobile/build.ps1
# Or specify installed paths:
./mobile/build.ps1 -Sdk 'C:/Android/Sdk' -JavaHome 'C:/Program Files/Java/jdk-21'
```

The script compiles Java, desugars through D8, packages resources with AAPT2, aligns the combined APK and signs/verifies it with Android's apksigner. Default build version is 1.4 (code 6); -VersionName and -VersionCode can override it for future releases. Local output is in `mobile/dist/` and is ignored by Git. A previous companion APK, if present in that output folder, is moved to ignored `mobile/build/legacy/` to avoid distributing it accidentally. The local testing key lives outside the repository at `%USERPROFILE%/.android/badilika-local.jks`, with the standard local-test password `android`. **Keep the same key to install future updates over this build.** This is a locally signed build for direct testing/sharing, not a Play Store release with a production signing pipeline.

## Relevant Android documentation

- [VPN service and consent](https://developer.android.com/develop/connectivity/vpn)
- [NetworkStatsManager and subscriber-ID restrictions](https://developer.android.com/reference/android/app/usage/NetworkStatsManager)
- [Usage events](https://developer.android.com/reference/android/app/usage/UsageEvents.Event)
- [Foreground service types](https://developer.android.com/about/versions/14/changes/fgs-types-required)
- [Accessibility service and Home action](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)

See `VALIDATION.md` for tests actually performed and remaining device checks.
