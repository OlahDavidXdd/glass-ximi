# Glass for Kashi

Experimental LSPosed add-on for **Kashi HyperOS Launcher V7.5 on HyperOS 3**. Adds **Glass icons (devarix)** to the launcher's Icons settings, with two materials and 700 icon/activity mappings.

## Materials

- **Liquid Glass** uses the actual SDF/refraction shader from [QWEA0/Liquid-Glass-Android](https://github.com/QWEA0/Liquid-Glass-Android), pinned at `16afe4d8b5d08a370d3f8c50b08d7f5c6eb5da4d`. The AGSL shader is unchanged. A Java Drawable adapter supplies wallpaper pixels and draws the silver foreground symbols separately. Grayscale is retained and chromatic dispersion is disabled.
- **Frosted Glass** retains the previous 75%-opacity icon pack and the Xiaomi native background-blur calls.

This adapts the API 33+ lens core, not the entire library/AAR or its legacy NDK pipeline. The upstream MIT license is bundled in the APK and source. This project is new add-on code, not Kashi's unpublished launcher source, and does not replace or re-sign the launcher APK.

## iPhone reference finish, drawer and native animation fix (0.6.0)

[Download 0.6.0](https://github.com/OlahDavidXdd/glass-ximi/releases/tag/kashi-glass-addon-v0.6.0-test). The finish now has softer neutral frost, a curved upper highlight, a fine double rim and brighter silver automatic glyphs, based on the supplied iPhone photo. Grayscale remains enabled and dispersion remains off. The unchanged QWEA0 shader still performs the optical refraction; the adapter supplies the material settings and reflection overlay.

**Glass app drawer** is on by default. It adds a frosted sheet to Kashi's BaseAllAppsContainerView/AllAppsContainerView, clears only four identified structural drawer backdrops (never individual app/input/selection backgrounds), and restores their backgrounds/tints on disable. Scrolling, categories, search, click/focus handlers and layout remain native. Full-screen corner size and optical bevel are bounded rather than scaled to hundreds of pixels. Drawer sampling still uses the static wallpaper; it does not capture the complete launcher or live wallpaper surface. Test performance and appearance on the phone.

The previous transition build could miss the direct Drawable parameter passed to **FloatingIconView2.init** and **FloatingIconLayer2.init/initDrawable**. These inputs now receive glass snapshots before the native code splits/copies the icon. Only exact adjacent LaunchAppAndBackHomeAnimTarget + Drawable parameters inside a four-class animation whitelist are patched; widgets without a known app snapshot keep their native behavior. The public V7.5 Kashi APK was checked for these signatures (V7.5 is the current public release at build time).

Each animation/getter/model hand-off receives its own mutable bitmap copy: native fill/recycle code cannot modify the cache's bitmap. Wallpaper-position updates no longer empty that cache; wallpaper content/material changes still invalidate it. Snapshots are warmed at staggered times, and capture uses a detached drawable with the live icon's original geometry. A worker-thread cache miss can use a clearly logged static glass/glyph fallback, which is not wallpaper refraction. This addresses concrete gaps in 0.5.0; the remaining flash is not considered verified fixed until the phone test.

Leave **Diagnostic logging** on. After **Clear logs → Restart launcher**, open and close apps, return to the app, and check **Logged animation replacements**. Export logs if the flash remains; the report now names the exact native input hooks, missing/cold-fallback snapshots and substitution events. This counter counts input replacements, not app launches or rendered frames.

## Matching glass dock and search (0.5.0)

[Download 0.5.0](https://github.com/OlahDavidXdd/glass-ximi/releases/tag/kashi-glass-addon-v0.5.0-test). Includes all 0.4.0 icon changes plus **Glass dock** and **Glass search bar**, both on by default under the master icon switch.

The existing Kashi HotSeats background becomes a rounded gray lens, inset slightly to form a floating dock. Its separate decorative blur view is hidden while active and restored on disable. Home and drawer search layouts use a capsule lens. The surfaces share the icons' QWEA0 shader, grayscale tint, optical bevel, silver reflection overlay and blur strength. Native padding, child layout, search input, focus, click and gesture handlers stay in charge; no extra touch layer is inserted. Background colors/tints are restored when a surface switch is disabled. Only the three explicitly identified Kashi classes are targeted; external Google/third-party search widgets are not restyled or resized.

**Preview Liquid Glass** now shows the icon grid, a glass search pill and a glass dock on the same striped background. **Launcher status** and exported logs include bound dock/search counts and surface errors. If a control is absent in your selected launcher layout, the add-on does not create a second one. Select Kashi's built-in search bar in launcher settings to style it. Static-wallpaper capture limits still apply, including in the drawer; the fully composed drawer background is not captured.

This implements the reference's shared material, while retaining your launcher layout and control widths. The new surface hooks still need a phone test. Older releases remain available.

## Liquid Glass test build (0.4.0)

[Download 0.4.0](https://github.com/OlahDavidXdd/glass-ximi/releases/tag/kashi-glass-addon-v0.4.0-test). Adds stronger optical refraction, broader silver bevels, curved highlights and finer inner edge reflections. Blur is lighter so the lens distortion is more visible; icons remain grayscale.

**Automatic glass icons** is on by default. The 700 bundled mappings take priority. Unmapped app icons are converted locally into silver glyphs using adaptive monochrome/foreground layers where available. Flat edge-connected backgrounds are removed when safe; complex artwork keeps its silhouette or an inset gray tile. This is automatic styling of the app's own artwork, not a hand-drawn replacement. It runs once on demand and uses a bounded bitmap cache, with no package scan, downloads or storage permission. Restart the launcher after an app icon changes to regenerate its cached variant. Turning this switch off restores the existing behavior for unmapped apps.

The bind hook now replaces the drawable synchronously before compound drawing. Known Kashi animation getters (`getContentDrawable`, `getBackAnimPreviewDrawable`, `getBoomAnimBitmap`) and cached model icon requests receive standalone glass snapshots rather than the stock original or the live drawable. A snapshot is prepared on touch and reused from an 8 MiB cache. The public HardwareRenderer/ImageReader path captures the lens where supported; a glass plate/glyph snapshot is used if GPU readback is unavailable. Snapshot capture is static during the transition and initial capture may add work to the first touch. The live icon remains tied to its own view; animation code cannot steal its callback or bounds. This targets the observed stock flash; a device check must confirm every ROM-specific transition path. Transient settings IPC failure keeps the last working configuration.

Diagnostic export/copy/clear controls remain available. The normal 0.3.0 release and 0.3.1 logging build are retained as backups. These new changes pass build checks but have not yet been verified on the phone.

## Diagnostic test build (0.3.1)

[Download the logging test build](https://github.com/OlahDavidXdd/glass-ximi/releases/tag/kashi-glass-addon-v0.3.1-test). This earlier build updates the same add-on package and keeps the 0.3.0 glass behavior.

Diagnostic logging is on by default in this test build. To report a problem:

1. Open devarix Glass and leave **Diagnostic logging** on.
2. Tap **Clear logs**, then **Restart launcher**. Open home and the app drawer to reproduce the issue.
3. Return to the app and tap **Export logs (.txt)**. Choose a save location and attach that file to the bug report/chat. **Copy logs** is available if your ROM has no document picker.

The report includes app/device/launcher versions, settings, hook installation and errors, sampled icon package names and tag class names, asset decode results, mapping counts, render/capture status and restart results. It never collects wallpaper pixels, arbitrary Intent contents, system logcat, account identifiers or a full installed-app inventory. It stays in private app storage (two rotating 64 KiB segments) until you export or copy it. Turning logging off stops new file entries; **Clear logs** removes retained entries. A bounded background transport queue may discard messages during bursts. Log collection/export itself does not require root; launcher restart and LSPosed have their usual requirements.

If the add-on never hooks into the launcher, its exported report will lack launcher events. In that case include the module error from LSPosed's own logs too. Logging is best-effort and not a crash tombstone collector.

## Preview before activating

Install the APK and open **devarix Glass → Preview Liquid Glass**. Drag an icon over the striped background. This screen runs the actual AGSL shader in the app; it needs Android 13+ and hardware acceleration, but no root or LSPosed activation. A driver failure is reported and the static fallback is identified.

A successful preview does not establish that launcher wallpaper capture works on the phone.

## Enable in Kashi

1. Enable the add-on in LSPosed, scoped only to **System launcher (`com.miui.home`)**.
2. Open **devarix Glass → Enable Liquid Glass**. This turns on the icon pack and blur together. It does not enable LSPosed scope for you.
3. Tap **Restart launcher**, allow root for **devarix Glass**, then open the home screen. This restarts only `com.miui.home` for the current user without clearing its data. If root is denied, reboot the phone.
4. Return to **devarix Glass → Launcher status**. The binding counts show supported icon views, resolved mappings and replaced drawables. **Check launcher connection** requests a fresh settings read; open the home screen and return to see the result.

If 0.2.0 stayed on “Waiting for mapped icons to bind”, update to 0.3.0. The old hook only handled `LauncherIconImageView`; Kashi also draws its main icons through the `ShortcutIcon` TextView. This release supports that path without rasterizing the live glass drawable. The wallpaper crop adapter now accounts for its centered top compound icon. Device appearance remains unverified.

Both feature switches are off by default. Turning the icon option off restores the captured original drawables. With Automatic glass icons disabled, applications without a bundled mapping keep their existing icons; activity-specific mappings take priority over package fallbacks, so dialer and contacts activities can retain distinct icons.

## Wallpaper capture and limits

Android draws the home wallpaper in a separate surface. The QWEA0 library's usual parent-View capture cannot simply be dropped into the launcher to capture that surface. This adapter samples the current **static wallpaper**, tracks launcher wallpaper-offset calls and icon position changes, and feeds it to the upstream shader. The lens pipeline uses a GPU blur effect when enabled; the foreground glyph is not blurred.

If normal wallpaper APIs deny access, the status explains the failure. **Wallpaper access via root** is a separate opt-in switch, off by default. Only when selected may the launcher request `su` access to read `/data/system/users/<current-user>/wallpaper`. Reads are time-limited and size-limited; wallpaper pixels stay in launcher memory and are not exported through the asset provider or uploaded. Disabling the feature cancels a pending root process.

Live-wallpaper frames are not captured. Use Frosted Glass/native blur for a live wallpaper. Wallpaper-based refraction does not sample dock/folder overlays or the fully composed screen; crop alignment, special wallpaper scaling, dark-mode dimming and window-level animations may differ. Hardware-driver support and Xiaomi firmware still need physical-device verification.

The root switch is not necessary for the in-app preview. The separate **Restart launcher** button requests root only when tapped; its process is time-limited to 25 seconds and targets only the current user’s System launcher. The add-on does not modify system properties, launcher APKs or launcher databases.

## Validation

Local builds and unit tests passed. Checks cover provider caller restrictions, URI/material validation, current-user wallpaper paths, blur bounds, activity-specific icon selection, both sets of 700 assets, upstream shader provenance and uniform coverage. Android lint completes without errors. APK signatures are verified during release publishing.

**No physical HyperOS device has verified this build yet.** Status messages distinguish capture failures and shader/driver failures; submitting render commands is not a visual verification. Disable the add-on in LSPosed and restart the launcher if it behaves incorrectly.

## Updating the experimental build

These releases use development signing keys. If Android rejects an update due to a signature mismatch, uninstall **devarix Glass only**, install the latest version, re-enable its LSPosed scope and restart the launcher. Do not uninstall the System launcher.

## Build and inspect

Use JDK 17, Android SDK platform 35 and Build Tools 35.0.0:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
python3 scripts/verify-kashi.py /path/to/MiuiHome.apk
```

The Xposed API is compile-only, from its official Maven repository. No Internet permission is requested. Asset/settings IPC is restricted by calling UID to the add-on itself and `com.miui.home`. Image decoding runs off the UI thread; the bitmap cache is bounded to 32 MiB. Layout listeners are attached to icon views rather than hooking every View's layout method. The lens watches icon pose changes while visible; device performance has not been benchmarked.

## Credits

Developer: **devarix**. Target launcher: [Kashi / Mods Center](https://github.com/Mods-Center/HyperOS-Launcher).

Lens shader: **pandadog / QWEA0**, MIT; see `app/src/main/assets/licenses/Liquid-Glass-Android-MIT.txt` and `compatibility/liquidglass-upstream.json`.

New adapter code is MIT licensed. Artwork is not relicensed by that code license: base icon symbols are from Guofeng 26 Lite Edition by **一凡UI**, with additions from [Simple Icons](https://github.com/simple-icons/simple-icons), [Shizuku](https://github.com/RikkaApps/Shizuku), [MicroG RE](https://github.com/MorpheApp/MicroG-RE), and custom glass assets.
