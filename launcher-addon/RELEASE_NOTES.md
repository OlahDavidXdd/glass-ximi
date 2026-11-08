Developer: **devarix**

## 0.6.0 — iPhone reference glass, frosted drawer and native animation inputs

- Softer gray frost, a curved gloss cap, finer double rim and brighter silver automatic icons, tuned from the supplied iPhone photo. No blue tint or dispersion.
- Adds **Glass app drawer**, on by default: a frosted wallpaper-backed sheet behind the native grid and search results. Only identified structural backgrounds are cleared; they are restored on disable.
- Replaces direct Drawable inputs to **FloatingIconView2.init** and **FloatingIconLayer2.init/initDrawable**, which can bypass the icon getters hooked in 0.5.0. Narrow class/signature matching is checked against the current public Kashi V7.5 APK.
- Hands each animation its own mutable bitmap, keeping native fill/recycle operations away from the snapshot cache.
- Keeps snapshots across wallpaper-offset changes, warms them at staggered times, and captures a detached icon at its original geometry. A cold worker-thread fallback uses static glass artwork, not a stock icon; it is logged separately.
- Adds **Logged animation replacements** and last-event details to status/log export. Includes automatic icons, matching dock/search, preview, restart and logging controls. Previous releases remain available.

## Apply and check the flash

Install, enable **devarix Glass** in LSPosed scoped to **System launcher (`com.miui.home`)**, then choose **Enable Liquid Glass → Restart launcher**. Leave **Glass app drawer** and the other glass surface switches on.

For a clean test: **Diagnostic logging on → Clear logs → Restart launcher → open/close apps → return to status**. If the flash remains, export the log file and report **Logged animation replacements**, affected apps, whether it occurs opening/closing, and launcher version. A nonzero counter establishes that an input hook ran; it does not prove every animation frame is correct. The previous fix did not resolve the reported phone behavior, and this new fix still needs phone verification.

Real refraction/blur samples the static wallpaper. Animation snapshots stay static; GPU readback/cold-start failures can use a glass artwork fallback. Android system splash icons are outside this launcher's scope. No wallpaper images, system logcat, automatic uploads or launcher data clearing are added.

Build, unit tests, lint, APK signature, curated assets and unchanged shader provenance are checked. **New material/drawer/transition behavior has not been verified on a physical device.**

If the development key prevents updating, uninstall **devarix Glass only**, reinstall and re-enable its LSPosed scope. Keep System launcher installed.
