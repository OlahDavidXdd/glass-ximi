Developer: **devarix**

## 0.8.0 — Screen-matching glass and source-icon animation fix

- **Match screen background**, on by default for Liquid Glass: no fixed wallpaper tile, forced grayscale, body tint or dark wash. Keeps white center symbols and neutral glass edges. Uses a dedicated Xiaomi backdrop-blur node where supported; unsupported blur leaves a fully clear body. Native appearance still needs a phone test.
- Fixes the identified source-visibility fault: the old hook replaced Kashi’s transparent hide placeholder with a visible glass icon. Preserves native placeholder/show/fade state across rebinds, respects transparent/null ImageView clears and reports the actual drawable alpha.
- Transparent animation snapshots preserve alpha instead of running native opaque-fill conversion. Default screen mode removes eager offscreen GPU readback/warming and position polling; native animation timing is retained.
- Adds Kashi’s newer **QuickSearchBarWidgetView** home search host, selects its bar background without altering its controls, and logs the resolved class/size. Improves surface observer cleanup and automatic adaptive-foreground extraction.
- Keeps the glass drawer/dock, automatic icons, loaded-hook check, connected-hook restart and exported logs. Previous releases remain available.

## Apply

Install → enable **devarix Glass** in LSPosed scoped to **System launcher (`com.miui.home`)** → **Enable Liquid Glass** → leave **Match screen background**, **White center symbols**, and surface switches on → **reboot the phone once** if the loaded hook is old. Verify **Loaded hook: 0.8.0-test — matches app**.

The earlier log’s `su: Permission denied` means the launcher was not restarted. Installing a newer APK alone cannot update the classes already running inside it. The connected-hook restart route becomes available after loading a supporting hook.

Check home and drawer app opening/closing, source icon hiding, and the home search bar. If an issue remains, export logs with the loaded version, animation replacement count and native blur/search messages.

Screen matching is native blur/transparent glass; it does not claim full-screen shader refraction. Native blur may be unavailable on a firmware, in which case no substitute colored background is painted. Static-wallpaper QWEA0 refraction remains an optional comparison mode with source colors preserved; it cannot capture the fully composed screen. System splash icons are outside this launcher’s scope.

Build, tests, lint, signature, curated assets and unchanged shader provenance are checked. **No new animation/blur/search behavior is physically verified yet.** If an update is rejected, reinstall **devarix Glass only**, re-enable its LSPosed scope and reboot. Keep System launcher installed.
