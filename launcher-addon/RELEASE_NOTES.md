Developer: **devarix**

## 0.10.0 — Animation ownership, cache and visibility fixes

Keeps the approved icon artwork/material, compact search glass and supported widget background treatment from 0.9.0. No stronger refraction or mockup-only widget redesign is added.

- Resolves glass animation artwork through the source view, native target-container/root, buddy model and weak target links. Traversal is bounded and cycle-safe. Widget/folder-preview targets retain their native animation content.
- Rebuilds evicted transition artwork from available glyphs, including live-view artwork when the glyph LRU has evicted it. A single immutable binding holder keeps the app key and glyph together so a recycled drawer slot cannot pair a new key with its previous app’s symbol.
- Native animation inputs and all ConstantState clones receive independent mutable pixel copies from an unexposed template. This applies to both transparent and Frosted snapshots; recycling/mutating one native copy does not invalidate its siblings or future clones. Nested initializers reuse already-owned inputs rather than repeatedly copying them.
- Keeps native hide/show for the same app during rebinding, resets stale hidden/fade state when a drawer view is actually rebound to another app, clamps finite fade overshoot before the native setter, and rejects NaN/infinite alpha updates.
- Opening/getter hand-offs no longer perform wallpaper/GPU readback or synchronous asset-provider I/O. Transparent mode keeps its existing rim/white-symbol snapshots. Optional static-wallpaper mode uses a logged static fallback if a ready lens snapshot is absent; it does not generate a new optical capture during launch.
- Avoids repeated settings/material rebuilds on every Activity resume and on unchanged connection checks. Debounces the remaining full scan by 400 ms and runs it only while the launcher has focus. Native animation clocks, durations, springs, source geometry, cancel/end callbacks and launcher visibility remain in charge.
- Logs reused as well as replaced glass animation hand-offs. The counter measures hand-offs, not launches, frames or proof of smoothness.

## Apply

Install and keep LSPosed enabled for **System launcher (`com.miui.home`)**. Restart the launcher or reboot, then verify **Loaded hook: 0.10.0-test — matches app**. Previous releases remain available. If the APK signature prevents an update, reinstall **devarix Glass only**, restore its LSPosed scope and reboot; keep System launcher installed.

Build, unit tests, lint, signature, native Kashi V7.5 hook signatures, unchanged visual rendering sources, 700 icon/glyph assets and shader provenance are checked. **Animation appearance and smoothness have not been verified on a physical device.** These changes fix identified code paths; they do not establish that every firmware/gesture/launcher animation issue is gone. Android system splash artwork remains outside this launcher module’s scope.

For verification: check opening and closing from home, dock and drawer, quickly launch different apps, cancel a home gesture, and scroll/reuse drawer slots. If a symptom remains, export logs after reproducing it; they include loaded version, cache fallbacks and native hand-offs. Clock/weather backgrounds remain provider-dependent, as in 0.9.0.
