# Glass for Kashi

Experimental LSPosed add-on for **Kashi HyperOS Launcher V7.5 on HyperOS 3**. Adds **Glass icons (devarix)** to the launcher's Icons settings, with two materials and 700 icon/activity mappings.

## 0.22.0 — Kyant navigation, native restore inputs and clock backgrounds

The floating controller navigation adapts [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass), pinned at `65ab177e90e5c1d8c62e70cf7755841982da65f6`. Its rounded-rectangle refraction shader is extracted verbatim with its SDF expansion and bundled under Apache-2.0. A Java View/RenderNode adapter uses the demo's 8dp blur / 24dp lens approach; it is not the upstream Compose tab component. Navigation overlays a separate content subtree and samples that live subtree without capturing itself. Software/shader failure uses readable dark glass without claiming blur. Icon artwork and the launcher icon renderer remain unchanged.

Kashi's `ShortcutInfo` declares a contextual artwork overload, while its no-argument getter used by delayed restores is inherited. The prior hook-all-declared-methods call missed that getter. The hook now resolves its actual declaring method. The native ShortcutIcon setter also notifies an image callback with its incoming drawable; that input is now replaced with an independent glass snapshot before the callback can cache stock artwork. Native clear/alpha/bounds behavior is retained. Verified source stop/reset callbacks also reconcile the live slot. These changes target concrete missing restore paths; end-to-end phone animations still need confirmation.

Clock binding now removes and restores Android host backgrounds on recognized MAML wrappers/render children, in addition to the existing engine decoration pass. Children, native hands/time and weather animation frames are preserved. Provider-updated backgrounds are retained/restored appropriately. Layer logs include dimensions to distinguish remaining opaque engine content. If the opaque card is baked into an unknown foreground frame, this update retains it rather than erase live clock content.

Verification: 80 Java unit tests, 229 Android runtime cases across API 33/35, lint, APK assets and signature checks. The new cases cover inherited getter resolution, native callback pixel ownership/clear/alpha behavior, clock host restoration, recursive navigation capture rejection and native Kyant shader compilation/uniforms. The original shader compiles in the API 35 native runtime. API 33 host Skia predates AGSL child.eval, so shader compilation is not tested there. Host screenshots show the software fallback; they do not prove GPU refraction, Xiaomi backdrop pixels or actual LSPosed/Kashi gestures.

Install, reboot and refresh Overview to confirm loaded hook **0.22.0-test**. Diagnostics includes `SOURCE_INPUT` in the focused animation report. For a remaining clock background, export the complete report including `MAML host`, `MAML glass rendered`, host-background and layer dimensions. See [signing/update limits](SIGNING.md).

## 0.21.0 — Shared icon material and restrained black layout

The controller replaces its separate OpticalGlass highlight painter with the actual `LiquidGlassDrawable` used by launcher icons. Controller controls use its explicit screen-matching material/rim path, with a neutral dark body, capsule geometry and no wallpaper tile. Blur is off for these plain-black controller surfaces; there is no claim of visible scene refraction or Xiaomi compositor sampling inside the controller. The launcher shader, icon artwork and launcher material settings are unchanged.

The layout follows the supplied reference more closely: plain black canvas, dark grouped settings rather than a glass tile behind every row, compact capsule actions, native switches with readable thumb state, and one floating navigation capsule with vector icons and a selected pill. The large overview hero and broad painted reflection bands are removed. The slider retains native progress/drag behavior with a thin glass track.

The supplied Weather trace shows glass-owned source fields/slots at completion and next-frame sampling. Animated `assets/weather2/.../pic_*.webp` layers remain untouched because these unnamed frames have not been established as safely removable decoration. These logs do not prove that an animation or widget blur issue is fixed; this release changes controller presentation only.

Verification: 77 unit tests, 216 Android runtime cases across API 33/35, lint, assets and signature validation. Actual native controller screenshots are attached. Refresh connection after installing/rebooting to confirm loaded hook **0.21.0-test**. See [signing/update limits](SIGNING.md).

## 0.20.0 — Black liquid-glass controller

The controller uses a pure black canvas with neutral translucent glass cards, individual option rows, reflective switch tracks/thumbs, glass buttons, blur-strength control and a floating glass bottom navigation pill. Checked and selected controls have stronger highlights; native switch behavior, ripples, labels, preference persistence and accessibility remain. This is controller glass styling, not a claim of compositor blur or optical refraction against its plain black background.

Launcher icon artwork, search/dock/drawer/widget materials and animation hooks are unchanged from 0.19.0. The supplied Weather trace confirms 0.19.0 loaded and glass-owned field/slot drawables immediately after completion and on the next frame. It does not identify any other layer displaying stock artwork or establish visual animation success.

Verification: 77 unit tests, 216 Android runtime cases across API 33/35, lint, asset and signature validation. Screenshots are rendered from the native controller UI. Install/reboot and refresh Overview to confirm loaded hook **0.20.0-test**. See [signing/update limits](SIGNING.md).

## 0.19.0 — Controller redesign and clock engine fixes

The controller now has Overview, Appearance and Diagnostics tabs, a neutral dark design, clear master/surface controls, and separate installed-app and loaded-hook versions. Cached hook information is marked unconfirmed until a fresh reply arrives. Restart feedback survives tab changes and rotation; expired requests do not report success. The focused animation report keeps return-state logs and filters unrelated widget render messages. Existing material preferences are preserved.

Clock discovery now covers AwesomeClock's reflected root and rendering child, both `miui.maml` and `com.miui.maml` engines, and dynamically loaded engine classes. Wrapper and child share a stable root binding; stale aliases cannot remove the active owner's material. Native clock hands, time, weather content and interactions remain under the provider's control. Widget materials continue to request the screen backdrop rather than a static wallpaper texture. Unsupported native blur leaves an untinted rim; this is not a claim of live refraction on unsupported firmware.

Animation target resolution now also follows Kashi's verified buddy-icon getters. Existing return-source repairs, native hide/alpha behavior and approved icon artwork remain intact. The supplied device logs did not establish the final source state, so the remaining stock-icon flash is **not claimed fixed** without a new phone trace. Native backdrop requests additionally reject supported mode getters that report an ignored request; API readback is not visual blur verification.

Verification: **77 Java unit tests**, **216 Android runtime cases across API 33/35**, lint, APK assets and signature checks. Runtime tests execute production UI, binding and graphics classes; they do not execute Kashi/LSPosed hooks or Xiaomi compositor pixels. Controller screenshots are actual host-rendered native UI, not phone blur evidence.

Install and reboot; in Overview, refresh connection and confirm loaded hook **0.19.0-test**. If animation or widget problems remain, export Diagnostics after reproducing them. See [signing/update limitations](SIGNING.md) before updating from a differently signed test build.

### Controller screenshots

Host-rendered native Android UI at phone dimensions; these do not demonstrate Xiaomi blur.

[Overview](screenshots/controller-overview.png) · [Appearance](screenshots/controller-appearance.png) · [Diagnostics](screenshots/controller-diagnostics.png)

## Materials

- **Liquid Glass** uses the actual SDF/refraction shader from [QWEA0/Liquid-Glass-Android](https://github.com/QWEA0/Liquid-Glass-Android), pinned at `16afe4d8b5d08a370d3f8c50b08d7f5c6eb5da4d`. The AGSL shader is unchanged. A Java Drawable adapter supplies wallpaper pixels and draws the silver foreground symbols separately. Grayscale is retained and chromatic dispersion is disabled.
- **Frosted Glass** retains the previous 75%-opacity icon pack and the Xiaomi native background-blur calls.

This adapts the API 33+ lens core, not the entire library/AAR or its legacy NDK pipeline. The upstream MIT license is bundled in the APK and source. This project is new add-on code, not Kashi's unpublished launcher source, and does not replace or re-sign the launcher APK.

## Build audit and regression fixes (0.12.0)

[Download 0.12.0](https://github.com/OlahDavidXdd/glass-ximi/releases/tag/kashi-glass-addon-v0.12.0-test). Fixes settings/material invalidation, stale source state on re-enable, restart queue starvation, widget restoration/async updates, provider PID handling, fresh connection status, automatic-icon capture ownership and wallpaper cleanup. Approved icon artwork/renderer parameters are preserved. See [the audit](AUDIT.md) for findings and verification limits.

Install, restart/reboot and check **Loaded hook: 0.12.0-test — matches app**, with a fresh launcher response. Host tests do not establish that Xiaomi blur, LSPosed dispatch or full Kashi gestures are correct on your firmware.

[Persistent signing support](SIGNING.md) is prepared, but configuring repository secrets is blocked by this connection’s secret-administration permissions (HTTP 403). Without those secrets, test APKs still use runner debug keys; updates from older signatures can require reinstalling the add-on. Published release assets are now immutable and validation records the public certificate fingerprint/signing mode.

## Attached compact-search blur (0.11.0)

[Download 0.11.0](https://github.com/OlahDavidXdd/glass-ximi/releases/tag/kashi-glass-addon-v0.11.0-test). The phone log showed 0.9.0 was both installed and loaded, so it did not exercise the 0.10.0 animation fixes. Those fixes are included unchanged here.

The compact home capsule now requests native blur on its existing attached decorative ImageView. That view receives a neutral clear rim and no static-wallpaper lens/gray fallback. Its neighboring text and search action are retained. This is independent of icon material, so keep your current icon settings. New `SEARCH_BLUR` logs report attachment, hardware state, dimensions, native mode/radius readback and failures. Calls are deduplicated; original state is restored on disable and rejected firmware calls roll back. Native API acceptance is not proof of visually correct blur.

Install, enable **Glass search bar** and **Backdrop blur**, restart/reboot, and verify **Loaded hook: 0.11.0-test — matches app**. Host native-graphics tests check attachment/routing/restoration and transparent-rim rendering with the firmware call boundary recorded/rejected. They do not emulate Xiaomi blur pixels or execute Kashi/LSPosed. See [release notes](RELEASE_NOTES.md).

## Animation reliability update (0.10.0)

[Download 0.10.0](https://github.com/OlahDavidXdd/glass-ximi/releases/tag/kashi-glass-addon-v0.10.0-test). Preserves the approved icon rendering and 0.9.0 search/widget surfaces. This update hardens animation hand-offs and source visibility without replacing Kashi’s springs, durations, geometry or cancel/end handling.

Native targets can now resolve through bounded, cycle-safe view/container/model links. Evicted transition snapshots rebuild from available glyphs, and live view bindings publish their app key and artwork together to avoid mixing apps during drawer recycling. Widget and folder-preview animations retain native content. Every owned snapshot clone copies from an unexposed template; native bitmap mutation/recycling cannot poison other clones. Nested initializers reuse owned inputs.

Same-app rebinding retains native source hide/fade state; actual app changes reset stale state in recycled drawer slots. Fade overshoot is clamped before native code receives it, and non-finite alpha updates are rejected. Launch/getter snapshot paths perform no new GPU/wallpaper capture or asset-provider read. Optional static-wallpaper lens mode uses a logged static fallback when no ready optical snapshot is available.

Repeated Activity resumes no longer reload assets/settings. Unchanged connection checks do not reconstruct materials. The remaining full scan is debounced 400 ms and requires launcher focus. Diagnostics count both reused and newly replaced animation hand-offs, not rendered frames or app launches.

Install, restart/reboot and check **Loaded hook: 0.10.0-test — matches app**. The build is checked against the public Kashi V7.5 native signatures and includes unit/lint/asset/signature checks, but physical-device animation smoothness is unverified. Check home/dock/drawer opening and closing, quick app switching, interrupted gestures and drawer recycling; export logs for any remaining issue. System splash artwork is outside the module’s scope. See [release notes](RELEASE_NOTES.md).

## Compact search and clock/weather widgets

[Download 0.9.0](https://github.com/OlahDavidXdd/glass-ximi/releases/tag/kashi-glass-addon-v0.9.0-test). The current icons and animation logic are preserved from 0.8.0, as requested. Validation checks hashes of their unchanged rendering/generation/transition source files.

The small home **Search** capsule is `pageindicators.CapsuleIndicator`, with an independent decorative ImageView (`mIndicatorBackground`) and live text/control (`mIndicatorContent`). Previous builds covered the larger search layouts but missed this capsule. This build replaces that decorative layer, clears its image and native color blend, and rebinds after style changes. It retains the existing text, magnifier, action, animation visibility, size and positioning. Disabling glass restores the original image/background and requests the native blur style again.

**Glass clock & weather widgets** is on by default. It targets recognized Android clock/weather AppWidget providers, replaces their separate card backgrounds with the existing glass material, and removes large background layers. Only explicitly named, non-interactive decorative images can be cleared; clock faces/hands, weather icons, text and taps are retained. No replacement weather service, location permission, timer or widget provider is added. Updates/reinflation and layout changes reapply the material, and disabling restores original backgrounds/tints/artwork.

Some providers flatten their entire card into a single image/canvas. Their background cannot be separated from live content by this approach, so opaque artwork is retained and can cover the glass. Native MAML clock/weather support is added in 0.14.0: recognized roots get the same material inside their native render pass, with large decorative background images/rectangles suppressed. Unknown or flattened images remain intact. Diagnostic logs identify recognized providers, content classes/resource IDs and the number of removed decorative layers; the status counts applied glass cards and inseparable/pending cards. Counts measure bindings, not visual success.

Install, leave **Glass search bar** and **Glass clock & weather widgets** enabled, restart/reboot, and check **Loaded hook: 0.19.0-test — matches app**. This update leaves your icon settings unchanged. Search/widget appearance is not physically verified yet.

## Screen matching and native source visibility (0.8.0)

[Download 0.8.0](https://github.com/OlahDavidXdd/glass-ximi/releases/tag/kashi-glass-addon-v0.8.0-test). **Match screen background** is on by default for Liquid Glass. It removes the wallpaper tile, desaturation and colored body wash. A dedicated background-only render node uses Xiaomi's backdrop-blur APIs where supported; glyphs and labels are not placed inside that blur node. The node is configured through an unattached plain View and rendered beneath the foreground, without adding layout/input children. Firmware may reject or fail to render this path: that is logged, and the body stays fully transparent with neutral edge reflections. An API call succeeding is not treated as proof that blur is visually correct.

This default mode is native-backdrop/transparent glass, not a claim of full-screen QWEA0 refraction. Turning screen matching off retains the static-wallpaper QWEA0 lens for comparison; that source cannot match a separate drawer scrim. Its color uniforms now preserve source saturation and add no tint/dimming. Screen matching performs no wallpaper capture/root read and no eager offscreen GPU snapshot warming. Its animation artwork is a transparent foreground/rim, so it does not carry a fixed background into a moving app transition. Native opaque-fill conversion is skipped only for the owned, transparency-preserving animation drawable. Native blur is not baked into those snapshots.

The concrete duplicate-source bug was in the old compound-drawable hook: it replaced Kashi's `mTransparentDrawable` hide placeholder with the visible glass icon. The hook now preserves that exact placeholder and tracks native hide/show/fade state across rebinding. It also honors explicit transparent/null ImageView clears, reports the true Drawable alpha, and preserves the native truncated alpha value. The launcher owns animation timing and visibility again. These changes address the identified code fault; phone verification is still required.

Home search support includes **QuickSearchBarWidgetView** in addition to the older desktop/drawer layouts. A bounded search within that known host selects its large visible bar background, excluding text/input/icon controls. Covering ancestor backgrounds are restored on disable. Logs record the resolved class and size. This does not invent a second search bar or rewrite input handlers. The new surface observers detach cleanly and keep only weak view/drawable references. Automatic icons also try Kashi's foreground layers so the original adaptive background is not converted into a white symbol tile.

Install, enable the LSPosed scope for **System launcher (`com.miui.home`)**, choose **Enable Liquid Glass**, leave **Match screen background** and **White center symbols** on, then reboot once if the running hook is old. Check **Loaded hook: 0.8.0-test**. The earlier `su: Permission denied` log means that root restart did not happen. Once the new hook is loaded, the connected-hook restart path is available without `su` in the add-on.

For the phone check, open/close apps from home and the drawer, verify that the source icon hides during the animation, and inspect the home search bar. Export logs if needed; they include native-input substitutions, native-blur support/failure and resolved search surfaces. Android system splash icons remain outside the launcher scope.

## Clearer glass, white symbols and loaded version (0.7.0)

[Download 0.7.0](https://github.com/OlahDavidXdd/glass-ximi/releases/tag/kashi-glass-addon-v0.7.0-test). Reduces the body tint, fog, dark wash and fallback plate opacity, while retaining refraction and the bright rim. **White center symbols** is on by default for Liquid Glass/automatic foregrounds. White RGB is applied only to the symbol paint, with fine shading represented by alpha so lens/details are retained; the glass background is not whitened. Switch it off to keep the original silver art. Preview follows the same option. The curated Frosted asset set remains available.

The supplied log contains `Cannot run program "su": error=13, Permission denied`, which means the root restart never ran. It also shows a surface status format from the older hook while the app reports 0.6.0. Updating the APK does not replace classes already loaded inside System launcher. The new **Loaded hook** line compares a version/PID/capability report from the launcher against the installed APK. An old hook that sends no identity is explicitly marked unreported, rather than assumed current. This identity exchange is independent of the diagnostic logging switch.

For the first update from that state: install the APK, enable the module in LSPosed for **System launcher (`com.miui.home`)**, tap **Enable Liquid Glass**, then **reboot the phone once**. Return home and check that **Loaded hook: 0.7.0-test** matches the app. If it is unreported or different, the new code is not yet confirmed loaded; the animation fix cannot be judged from the app version alone.

Once this hook is loaded, **Restart launcher** can ask it to exit its own process, then open Home. This route does not execute `su` from the add-on, but LSPosed itself still has its usual installation requirements. A five-second, one-use request is restricted to the add-on UID, acknowledged only by the authenticated launcher caller with the recorded PID, and consumed atomically. Acknowledgement is not treated as proof that a new hook version loaded: verify the version line after returning home. The original fixed-package/current-user root restart remains a fallback for hooks without this capability. Root denial, timeout and acknowledgement failure are shown persistently; they never report a completed restart as a verified module reload. No launcher database or layout is cleared.

Existing drawer, dock, search, automatic icon, animation-input and log controls remain available. New rendering and restart behavior still need a phone test.

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

In 0.15.0, compact home search uses the same LiquidGlassDrawable factory as dock/drawer. Its competing native blur plane is neutralized while glass is enabled, with original modes restored on disable. The `SEARCH_MATERIAL` diagnostic describes that path; physical rendering remains unverified.

Version 0.17 extends the search toggle to native page dots in a shared glass pill. Drawer ImageView bitmap/resource/Icon rebinding and paint boundaries retain live liquid drawables; motion-aware invalidation refreshes them during scrolling without a permanent timer. Native dot content/actions and icon source visibility remain intact.

Version 0.18 sizes/anchors page dots from the existing Search view. Clock/weather backgrounds request the live screen backdrop regardless of icon wallpaper mode; icons and Search retain their selected mode. Legacy clock/canvas-root support is extended, and animation completion refreshes sources with RETURN_STATE diagnostics. Physical-device output remains to be confirmed.
