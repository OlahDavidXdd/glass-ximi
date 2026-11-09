Developer: **devarix**

## 0.11.0 — Attached background blur for compact home search

The supplied phone log reports **installed=0.9.0-test and loaded=0.9.0-test**. The 0.10.0 animation changes were not running. This update includes those changes intact; it does not assume an unspecified remaining animation symptom has been identified or fixed.

- Compact home search uses its existing attached `CapsuleIndicator.mIndicatorBackground` ImageView for Xiaomi native background blur. The older implementation disabled blur on that real view and delegated rendering to a detached proxy or static-wallpaper drawable.
- Gives that decorative child a clear, neutral capsule rim. It no longer selects the icon material's static-wallpaper lens or gray fallback. **Keep your existing icon settings**: search requests live blur independently of Match screen background. Search text, magnifier, taps, size and native visibility stay intact.
- Captures/restores the background view's outline, clip setting and readable native blur modes/radius. Requests are deduplicated, wait for attachment/layout, and rejected firmware calls are rolled back without retrying every frame.
- New `SEARCH_BLUR` diagnostics report attachment, size, hardware acceleration, requested radius, readable native modes/radius, or rejection. API acceptance is not presented as visual verification. Config logs include running version and material options.
- Keeps the 0.10.0 icon/animation code, curated art, automatic glyphs and widget/drawer/dock behavior. No stronger-refraction proposal is added.

## Apply

Install **0.11.0-test**, keep LSPosed scoped to **System launcher (`com.miui.home`)**, and restart launcher/reboot. Verify **Loaded hook: 0.11.0-test — matches app**. Leave **Glass search bar** and **Backdrop blur** on; keep your preferred icon material settings. The provided 0.9.0 log cannot verify the later animation fixes.

Build, 63 unit tests, lint, signature/assets and native Kashi V7.5 signatures are checked. The separate host-side native-graphics suite covers 30 cases on API 33/35, including attached-view routing, restoration, rejected requests and the clear rim. Search firmware calls in those routing tests are recorded/rejected, **not emulated**; real Xiaomi blur pixels, LSPosed dispatch and Kashi gesture smoothness still require checking on a compatible runtime.

If an issue remains after the loaded version matches, export the new `SEARCH_BLUR`/animation logs. The animation symptom itself still needs a description. Previous releases remain available. If Android rejects the update signature, reinstall **devarix Glass only**, re-enable its scope and reboot; keep System launcher installed.
