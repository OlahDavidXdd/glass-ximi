Developer: **devarix**

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

## 0.18.0 — Search-sized page pill, live widgets and return cleanup

Page dots now use the native Search sibling's measured width/height, gravity and margins. Hidden Search can be measured without showing it; the page pill follows Search text/size changes and restores its native layout params when disabled. Dot content and native show/hide behavior remain intact.

Widgets explicitly request the screen backdrop instead of the static-wallpaper lens, independently of the selected icon mode. This removes the module's baked wallpaper texture from widget rendering. When Xiaomi's native backdrop is unavailable, the fallback is an untinted rim, not a substituted wallpaper tile. Legacy `MiAdvancedView` clock gadgets are now discovered, and separate backgrounds on non-image Android clock canvas roots can be styled while retaining native hands/text/time drawing. Flattened/opaque provider content is still retained. Repeated MAML render-status messages are deduplicated.

The supplied animation log confirmed styled inputs but did not show the final home-screen source. Native completion/release/cancel callbacks now reconcile the source and matching home/drawer representations and invalidate their display lists. A next-frame check runs without forcing visibility or alpha. `RETURN_STATE` reports build, key, field/slot ownership, hide/alpha, layer and visibility after cleanup. This targets stale return-to-home content; the remaining phone flash is not claimed fixed without a new device trace.

The material constructor now supports an explicit per-widget live flag. All drawing methods and icon/search mode selection are preserved and checked by hash/runtime tests. Full source hashes intentionally differ from 0.17 because of that constructor policy.

Install and reboot; confirm **Loaded hook: 0.18.0-test**. If the flash remains, export the new `RETURN_STATE` and `SOURCE_RETURN` lines after exiting. For clock support, export `MAML host` / `MAML layer` or `Widget provider` lines. Native blur appearance and final Kashi animation output require phone confirmation.

## 0.17.0 — Page-indicator pill and live drawer icons

The approved Search material is retained. The native LauncherIndicator page-dot view now receives the same capsule material and underlay reset as Search. Its child dots, current-page selection, native show/hide transitions, accessibility and tap actions are retained. The setting is named **Glass search & page pill** and uses the existing search preference. Native style refresh, target replacement, disable and detach restore/reapply the background without rewriting page content.

Drawer icon binding now handles ImageView bitmap, resource and Icon setters as well as direct drawable assignment. The ImageView paint boundary verifies the live material; a faded bitmap is treated as artwork rather than an explicit clear. Live liquid drawables invalidate when their position, scale, geometry or image matrix changes during scrolling/recycling. Unchanged/hidden/inactive icons do not request continuous frames, and detached views stop observing. Native source visibility/animation guards and the approved material parameters/artwork remain unchanged.

Restart/reboot and verify **Loaded hook: 0.18.0-test**. Page diagnostics use `PAGE_MATERIAL`. Android host tests cover page content/selection preservation, target/style changes, padding, attachment, restoration and live-motion invalidation. Actual Kashi mode switching, GPU blur and drawer fling frame pacing still need phone confirmation; no full-launcher emulation is claimed.

## 0.16.0 — Widget material and return-to-home source protection

The search material approved on the phone is retained. Both Android and native MAML widget backgrounds now go through the same SharedGlassMaterial factory and native-underlay reset used by search; widget outline geometry and live content are preserved. Pending Android widget underlays activate after attachment, without repeating requests on unchanged frames. Opaque/flattened provider content still requires the existing per-card background detection and is not blindly erased.

The phone report identifies stock artwork returning on the home-screen source after exiting an app. This build intercepts native TextView compound-drawable assignment for launcher icons, verifies the backing drawable at native getter access and repairs the source immediately before TextView paints. These guards supplement the existing pre-draw and animation input protections, so late source restores do not have to wait for another pre-draw pass. Explicit clears, transparent masks, hidden source state, fade alpha and unrelated drawable slots remain native-controlled. Search, icon material parameters, artwork and animation timing are unchanged.

Install, restart/reboot, and verify **Loaded hook: 0.18.0-test**. Logs include `Protected source drawing slot` when a native restore was replaced. Android host regressions repeat 500 source restores, late slot writes and backing-field replacements on each API. The actual Kashi/LSPosed dispatch and physical exit animation still need phone confirmation.

## 0.15.0 — Search uses the dock/drawer material

The compact home search pill previously used a separate rim-only drawable and an attached native blur plane. The phone screenshot shows an opaque black pill despite working glass on nearby surfaces.

Search now uses the same `LiquidGlassDrawable` material factory as the dock and drawer. The separate native body/host blur planes are disabled while glass is active, and native image/background tint updates cannot add a black search fill. Search retains capsule geometry, native text, tap actions, layout and source visibility. Native modes/artwork are restored when the option is disabled. The icon renderer, artwork, dock/drawer material parameters, animation fixes and 0.14 widget support remain unchanged.

Enable **Glass search bar**, restart/reboot, and verify **Loaded hook: 0.18.0-test**. `SEARCH_MATERIAL` logs identify the common renderer and whether native underlay reset was accepted. Host tests check real drawable pixels for all three surface kinds in screen-matching/static modes, underlay reset/restore, style refresh, attachment, rejection and hidden state. Xiaomi's compositor output still needs phone confirmation.

## 0.14.0 — Native MAML clock and weather cards

The earlier widget implementation only recognized Android AppWidgetHostView cards. Kashi V7.5 also renders clock/weather through `MaMlHostView` and the native MAML element tree; those widgets were never reached by that implementation.

This build discovers native MAML clock/weather metadata and roots, inserts the existing Liquid Glass material inside the root's render pass, and suppresses large, noninteractive decorative background images/rectangles. Native time, weather data, hands, condition symbols, text and touch behavior are retained. Unknown or flattened images remain intact. Unrelated MAML widgets and app icons are excluded. Disabling the option stops interception without rewriting theme assets.

Widget status now includes bound/rendered MAML cards and replaced background-layer counts. Logs include host metadata, pending roots, layer names/sources and replacement decisions. Enable **Glass clock & weather widgets**, restart/reboot, and confirm **Loaded hook: 0.18.0-test**. If a card still looks stock, export the new `MAML host`, `MAML layer` and `MAML glass rendered` lines.

The API boundary was checked against Kashi V7.5's APK. Android host tests execute production material drawing, selection, binding, root replacement and disable/detach logic using fixtures for MAML's getter/field boundary. They do not execute the actual MAML engine or LSPosed hook dispatch; the appearance of your specific stock cards still needs phone verification. Approved icon renderers and PNG assets are unchanged.

## 0.13.0 — Recover artwork after repeated app animations

Fixes a verified recovery/cache-ownership defect matching the 0.12.0 phone report: native drawable replacement could leave the applied flag set, and an unverified stock bitmap could then overwrite cached glass artwork. Per-view bindings now retain trusted glyphs, verify the actual drawable, repair native field/drawing-slot replacement before rendering, and isolate Frosted bitmap pixels from native mutation/recycling. Detached views stop their integrity watcher. Native null/transparent slots, source hiding and alpha remain authoritative.

Every animation input is resolved afresh for its current target, including reused glass inputs. FloatingIconView2 and FloatingIconLayer2 refresh artwork before deriving native layers/surfaces. Widget/folder exclusions and native animation timing are unchanged. `ICON_REPAIRED` and `Animation layers refreshed` identify these paths in logs.

Approved icon artwork, material shader/parameters, dock/search/widget styles are unchanged. The host regression suite runs 500 native drawable replacements and 500 recycled animation handoffs on **each** of APIs 33 and 35, plus mutation, style/app rebinding, hide/clear and watcher detach/reattach cases. Actual Kashi hook dispatch and phone animation rendering remain unverified; this is a targeted experimental fix, not a claim that all firmware bugs are eliminated.

## 0.12.0 — Build audit and regression fixes

Preserves the approved icon artwork and renderer parameters. See [the audit](AUDIT.md) for scope, evidence and remaining limits.

- Rebuilds dock/drawer/search surfaces when shared material settings change; avoids dropping a rapid settings revert while a previous update is queued.
- Refreshes real source visibility/alpha when the module is re-enabled, clears stale animation artwork on restore and avoids strong cached tag references.
- Separates restart, control/status and asset queues so icon loading cannot hold up a restart request.
- Restores widget decoration after shrinking, becoming interactive or losing/replacing its provider/root; reapplies async background changes before drawing. UI mutations are marshalled to the main looper.
- Uses Binder’s actual caller PID for loaded-hook identity and restart consumption. UI reads no longer count as a launcher connection, and settings requests a fresh response instead of presenting a saved version as live.
- Handles empty automatic-icon bitmaps and restores uncloneable foreground layers’ original callbacks/bounds after capture.
- Handles wallpaper allocation errors without leaving loading stuck; checks root opt-in before/after starting a read and forcibly closes a still-running reader process on cleanup.
- Makes the icon preview scrollable on short screens and describes its selected material accurately. Compact-search blur diagnostics are visible in surface status. Diagnostic exports include widget options/status and the public APK signing certificate fingerprint.
- Supports persistent signing from repository Actions secrets and records signing mode/certificate in `validation.json`. Published release assets are immutable; rerunning a job does not replace them with a new debug certificate.

## Apply

Install, keep LSPosed scoped to **System launcher (`com.miui.home`)**, restart/reboot and verify **Loaded hook: 0.14.0-test — matches app**, with a fresh launcher response. Keep your preferred icon material. The latest phone report confirmed the repeated-transition issue on 0.12.0. Restarting the launcher is required to load the new hook and discard the old process caches.

**Signing remains a configuration limitation:** this GitHub connection cannot administer Actions secrets (HTTP 403). Without the managed-signing secrets, CI still uses an ephemeral debug key and clearly reports that mode. Android can reject an in-place update from an earlier differently signed test APK. See [signing setup](SIGNING.md); if reinstalling is required, save your settings report, reinstall **devarix Glass only**, restore its scope/options and reboot. Keep System launcher installed.

## Checks and limits

The package is checked with unit tests, lint, APK signature/asset validation and preserved renderer hashes. The host Android suite runs on API 33/35 and covers bitmap ownership, rendering, search dispatch/restore, widget changes, provider identities and disabled root capture. Old-source negative controls reproduce the earlier snapshot and provider failures. Signing setup branches have separate tests. Counts are recorded in CI reports.

These tests do **not** execute LSPosed hook dispatch, Xiaomi’s real native blur compositor, full Kashi gestures or frame pacing. No claim is made that every firmware-specific animation issue is fixed. MAML-only/flattened widgets and Android system splash artwork retain the limits described in the audit.
