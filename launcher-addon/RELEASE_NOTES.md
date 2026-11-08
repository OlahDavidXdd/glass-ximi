Developer: **devarix**

## 0.7.0 — Clear glass, white centers and loaded-hook verification

- More transparent-looking glass: lighter tint, reduced fog/dark wash and a clearer fallback plate. Keeps gray refraction, curved highlights and bright borders.
- **White center symbols**, on by default. Foregrounds are white while fine shading is retained through alpha; the glass body is not tinted white. Turn off for the original silver artwork. Preview follows the selection.
- Adds **Loaded hook** version/PID reporting. The installed app version is no longer treated as evidence that System launcher has loaded the update. Missing/old reports explicitly request a reboot.
- Adds connected-hook restart without executing `su` from the add-on, once a supporting hook is loaded. An explicit, authenticated, single-use request expires after five seconds and targets only the reported launcher process. Root remains a fallback for older hooks; denial/unconfirmed restarts stay visible.
- Retains the glass drawer/dock/search, automatic icons, native animation-input fixes and log export. Earlier releases remain available.

## Important for the supplied log

`Cannot run program "su": error=13, Permission denied` means the launcher was **not restarted**. The old surface status format also indicates an older hook may still be running despite `APP_OPEN 0.6.0-test`.

Install this APK → enable devarix Glass in LSPosed scoped to **System launcher (`com.miui.home`)** → **Enable Liquid Glass** → **reboot the phone once**. Then open home and verify **Loaded hook: 0.7.0-test — matches app**. The new connected-hook restart route becomes available only after the new hook loads.

If the icon flash remains after that version check, use **Clear logs → open/close apps → Export logs (.txt)** and include **Logged animation replacements**. The earlier 0.6.0 animation changes still need a test with that code actually loaded on the phone. No new universal animation-fix claim is made in this release.

LSPosed requirements and static-wallpaper capture limits remain. The restart acknowledgement is not proof of successful reload; check the loaded version. Logs stay local until exported; no Internet permission or launcher data clearing is added.

Build, tests, lint, signature, assets and unchanged shader provenance are checked. **New phone rendering/restart behavior is not physically verified.** If a development-key update is rejected, uninstall **devarix Glass only**, reinstall, re-enable LSPosed scope and reboot. Keep System launcher installed.
