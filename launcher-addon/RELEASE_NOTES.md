Developer: **devarix**

## 0.3.1 diagnostic test build

- Adds **Diagnostic logging**, on by default, plus **Export logs (.txt)**, **Copy logs** and **Clear logs**.
- Records startup/hook targets, settings reads, sampled icon mappings and tag classes, successful/failed asset decoding, binding counts, capture/render status, restart outcomes and caught hook exceptions.
- Export includes device/Android/launcher/add-on versions and current settings, even if the launcher never connects.
- Keeps at most two 64 KiB private log segments and uses a bounded launcher logging queue. No Internet permission, automatic upload, wallpaper images or system logcat collection.
- Retains 0.3.0's Kashi ShortcutIcon drawing fix and restart/activation controls. The normal 0.3.0 release remains available.

## Test and send logs

1. Enable **devarix Glass** in LSPosed, scoped to **System launcher (`com.miui.home`)**.
2. In devarix Glass, keep **Diagnostic logging** on; tap **Clear logs → Enable Liquid Glass → Restart launcher** and allow root.
3. Open home and the app drawer to reproduce the bug.
4. Return to the app, tap **Export logs (.txt)**, save the file and attach it to the chat/bug report. Use **Copy logs** if your ROM has no file picker.

Logs include sampled app package names and device model; export/copy only when you choose. Logging can be disabled and retained entries cleared. A missing launcher connection may need the relevant LSPosed module error as well.

Build, unit tests, lint, icon assets, shader provenance and APK signature are checked. **Physical-device behavior is not yet verified.** This is a prerelease for debugging, not a verified stable build.

If Android rejects the update because of the development signing key, uninstall **devarix Glass only**, reinstall and re-enable its LSPosed scope. Keep System launcher installed.
