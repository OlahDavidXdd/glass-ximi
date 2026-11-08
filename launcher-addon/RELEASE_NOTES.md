Developer: **devarix**

## 0.9.0 — Compact search and clock/weather widget glass

- Keeps the current icon artwork, icon renderer, automatic glyph generation and animation code unchanged from 0.8.0. Their source hashes are checked during APK validation.
- Targets Kashi’s compact **CapsuleIndicator** search pill, not just the large desktop/QuickSearch layouts. Replaces its separate `mIndicatorBackground`, clears the decorative image and its native color/blur blend, and retains the search text, magnifier, taps, layout and native visibility. Reapplies after native theme/style updates and restores the original decoration on disable.
- Adds **Glass clock & weather widgets**, enabled by default. For recognized Android clock/weather widget providers, styles the content card beneath its live views, removes large separate background layers, and clears only explicitly named, non-interactive decorative background images. Clock faces/hands, weather symbols, text, refresh logic and taps remain native.
- Widget support and resolved card class/resource names are included in diagnostic logs and the app status. Widgets that draw their background and content together cannot safely have that background removed. Those layers are retained; glass can remain covered by opaque provider artwork. MAML-only/non-AppWidget cards are not supported in this build.

## Apply

Install the APK, keep the module enabled in LSPosed for **System launcher (`com.miui.home`)**, and keep **Glass search bar** and **Glass clock & weather widgets** on. Restart the launcher or reboot once, then verify **Loaded hook: 0.9.0-test — matches app**. Your existing icon settings remain intact.

Build, unit tests, lint, signature, curated assets, preserved icon/animation sources and unchanged shader provenance are checked. Search/widget appearance still needs verification on your phone; a successful hook or background binding does not prove that a provider’s opaque artwork is separable. Logs distinguish recognized widget bindings from unsupported/pending cards. Native backdrop blur remains firmware-dependent, with the existing transparent fallback and no extra colored fill.

Previous releases remain available. If Android rejects the update signature, reinstall **devarix Glass only**, re-enable its LSPosed scope and reboot. Keep System launcher installed.
