#  Glass for Kashi

An experimental, optional LSPosed add-on for **Kashi HyperOS Launcher V7.5 on HyperOS 3**. It bundles the 700 gray glass icon mappings from the theme project and calls Xiaomi's native per-view background-blur APIs behind the icon images.

This is new add-on source code. It is **not** Kashi's launcher source and does not replace or re-sign the launcher APK. Kashi's public repository currently contains documentation and release binaries.

## Install and enable

1. Install the signed experimental add-on APK from this repository's release.
2. Enable **devarix Glass** in LSPosed and scope it only to **System launcher (`com.miui.home`)**.
3. Restart the launcher after unlocking the phone.
4. Open **Home screen settings → Icons → Glass icons (devarix)**. The same controls are also in the standalone **devarix Glass** app.
5. Enable **Use built-in glass icons**, then **Frosted backdrop**. Start with a blur radius of 24 dp.

Both switches are off by default. Switching the icon option off restores the drawable captured from the launcher. The icon asset pack uses 75% background opacity and neutral grayscale pixels. Applications outside the bundled package mappings keep their existing icons.

## Important validation limit

The launcher hook targets and package lookup methods have been checked against the published Kashi V7.5 APK. An APK build and unit tests do **not** establish that Xiaomi's native blur actually renders on a particular phone. HyperOS firmware, system blur settings, renderer limits, LSPosed compatibility and other launcher modules can affect it. This project has not yet been tested on a physical HyperOS phone.

The settings screen reports whether the native method calls succeeded or failed. A successful call is not a visual verification. If unsupported, the module keeps the icon pack available and reports that native blur is unavailable; it does not substitute a fake blurred wallpaper image.

If the launcher behaves incorrectly, disable this add-on in LSPosed and restart the launcher. No system properties, launcher APKs or launcher databases are modified.

## Implementation

- Adds an ordinary preference to `com.miui.home.settings.IconCustomizeFragment` linking to the add-on controls.
- Intercepts the standard `ImageView.setImageDrawable` path for `com.miui.home.icon.LauncherIconImageView` only.
- Resolves application packages from bound shortcut data, checks the bundled mapping, loads images off the UI thread, and bounds the bitmap cache to 32 MiB.
- Applies `View.setMiBackgroundBlurMode`, `setMiBackgroundBlurRadius` and `setMiViewBlurMode`, with rounded outline clipping. It does not blur the foreground symbol.
- Uses an exported asset/settings provider restricted by calling UID to the add-on itself and `com.miui.home`. The APK requests no Internet permission.

## Build

Use JDK 17, Android SDK platform 35 and Build Tools 35.0.0:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The experimental APK is signed with the standard local debug signing configuration. Do not treat it as a production-tested launcher replacement. The Xposed API is a compile-only dependency from its official Maven repository; it is not bundled into the module.

Compatibility inspection can be repeated against a locally obtained launcher APK:

```sh
python3 scripts/verify-kashi.py /path/to/MiuiHome.apk
```

## Credits and license

Developer: **devarix**. Launcher compatibility target: [Kashi / Mods Center](https://github.com/Mods-Center/HyperOS-Launcher).

New add-on code is MIT licensed. The bundled artwork retains its original attribution and is not relicensed by the code license: base icon symbols from Guofeng 26 Lite Edition by **一凡UI**, additions using [Simple Icons](https://github.com/simple-icons/simple-icons), [Shizuku](https://github.com/RikkaApps/Shizuku), [MicroG RE](https://github.com/MorpheApp/MicroG-RE), and custom glass assets. See the parent theme repository's source notes for the icon pack history.
