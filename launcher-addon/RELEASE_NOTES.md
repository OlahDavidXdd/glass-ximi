Developer: **devarix**

## 0.4.0 — Stronger Liquid Glass, transitions and automatic icons

- Stronger QWEA0 refraction with a wider optical bevel, brighter silver highlights, curved surface reflections and an inner rim. Reduced blur hides less of the distortion. No blue tint or chromatic dispersion.
- Replaces icons synchronously during Kashi binding and compound drawing to avoid a posted stock frame.
- Supplies independent glass snapshots through known opening/closing animation getters and cached ShortcutInfo icon requests. Snapshots are prepared on touch and bounded to 8 MiB. GPU readback uses public Android APIs; a static glass snapshot is the fallback. This aims to fix the reported flash but still needs your phone test.
- Adds **Automatic glass icons**, on by default. Unmapped apps use adaptive monochrome/foreground artwork or a cleaned silver rendering of their existing icon. Curated mappings always win. Complex images may remain inset grayscale tiles.
- Recycled drawer cells wait for their matching source artwork before generating a new fallback. Temporary settings IPC failure retains the last working configuration.
- Keeps **Restart launcher**, **Export logs**, **Copy logs**, **Clear logs**, both curated asset sets and the live shader preview.

## Apply and test

Install the APK, keep LSPosed scope set to **System launcher (`com.miui.home`)**, then open **devarix Glass → Enable Liquid Glass → Restart launcher**. **Automatic glass icons** starts on; it can be disabled independently.

Check an unlisted app, then open and close a few apps using both gesture and button navigation if available. If a stock flash remains, use **Clear logs → Restart launcher**, reproduce it, then **Export logs (.txt)** and send the file with the affected app and exact launcher version.

Liquid refraction still samples the current static wallpaper. Animation snapshots are static; they do not refract app-window contents. First-time glyph generation and snapshot capture have not been benchmarked on the phone. Prior releases remain available as backups.

Build, tests, lint, signature, asset and shader-provenance checks are included. **New 0.4.0 behavior has not yet been verified on a physical device.**

If Android rejects the development-key update, uninstall **devarix Glass only**, reinstall and re-enable its LSPosed scope. Keep System launcher installed.
