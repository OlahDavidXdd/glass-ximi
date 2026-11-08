Developer: **devarix**

## 0.5.0 — Matching Liquid Glass dock and search

- Adds **Glass dock** and **Glass search bar**, on by default with the icon pack.
- Uses the same QWEA0 lens, gray tint, stronger refraction, silver rim reflections and blur strength as the icons. The dock has broad rounded corners; search uses a capsule shape.
- Styles Kashi HotSeats and its home/drawer search layouts. Restores replaced backgrounds/tints and the existing dock decoration when disabled. Native layout and input handlers are retained.
- Updates **Preview Liquid Glass** to show icons, dock and search together over the striped test background.
- Adds surface binding counts and errors to Launcher status and exported logs.
- Includes 0.4.0's automatic silver/glass artwork for unmapped apps, synchronous icon binding and glass snapshots for known app animation paths. Logging/restart controls and older release backups remain available.

## Apply

Install the APK, enable the module in LSPosed scoped to **System launcher (`com.miui.home`)**, then open **devarix Glass → Enable Liquid Glass → Restart launcher**. Leave **Automatic glass icons**, **Glass dock**, and **Glass search bar** on. Each can be disabled independently.

Uses the existing launcher geometry; this does not replace the whole home screen with an iOS layout. Search styling requires Kashi's built-in search bar; external search widgets are not altered. Hidden/disabled controls are not created or forced visible. Liquid refraction samples the current static wallpaper; animation snapshots remain static, and GPU readback can use a glass fallback.

If a stock-icon flash remains or a surface fails, use **Clear logs → Restart launcher**, reproduce it, then **Export logs (.txt)** and send the file with your exact Kashi version and affected app/control.

Build, unit tests, lint, signature, curated assets and upstream shader provenance are checked. **The new surface and transition changes still require phone verification.**

If the development signing key prevents updating, uninstall **devarix Glass only**, reinstall and re-enable its LSPosed scope. Keep System launcher installed.
