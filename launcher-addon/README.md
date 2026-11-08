# Glass for Kashi

Experimental LSPosed add-on for **Kashi HyperOS Launcher V7.5 on HyperOS 3**. Adds **Glass icons (devarix)** to the launcher's Icons settings, with two materials and 700 icon/activity mappings.

## Materials

- **Liquid Glass** uses the actual SDF/refraction shader from [QWEA0/Liquid-Glass-Android](https://github.com/QWEA0/Liquid-Glass-Android), pinned at `16afe4d8b5d08a370d3f8c50b08d7f5c6eb5da4d`. The AGSL shader is unchanged. A Java Drawable adapter supplies wallpaper pixels and draws the silver foreground symbols separately. Grayscale is retained and chromatic dispersion is disabled.
- **Frosted Glass** retains the previous 75%-opacity icon pack and the Xiaomi native background-blur calls.

This adapts the API 33+ lens core, not the entire library/AAR or its legacy NDK pipeline. The upstream MIT license is bundled in the APK and source. This project is new add-on code, not Kashi's unpublished launcher source, and does not replace or re-sign the launcher APK.

## Preview before activating

Install the APK and open **devarix Glass → Preview Liquid Glass**. Drag an icon over the striped background. This screen runs the actual AGSL shader in the app; it needs Android 13+ and hardware acceleration, but no root or LSPosed activation. A driver failure is reported and the static fallback is identified.

A successful preview does not establish that launcher wallpaper capture works on the phone.

## Enable in Kashi

1. Enable the add-on in LSPosed, scoped only to **System launcher (`com.miui.home`)**.
2. Open **devarix Glass → Enable Liquid Glass**. This turns on the icon pack and blur together. It does not enable LSPosed scope for you.
3. Tap **Restart launcher**, allow root for **devarix Glass**, then open the home screen. This restarts only `com.miui.home` for the current user without clearing its data. If root is denied, reboot the phone.
4. Return to **devarix Glass → Launcher status**. The binding counts show supported icon views, resolved mappings and replaced drawables. **Check launcher connection** requests a fresh settings read; open the home screen and return to see the result.

If 0.2.0 stayed on “Waiting for mapped icons to bind”, update to 0.3.0. The old hook only handled `LauncherIconImageView`; Kashi also draws its main icons through the `ShortcutIcon` TextView. This release supports that path without rasterizing the live glass drawable. The wallpaper crop adapter now accounts for its centered top compound icon. Device appearance remains unverified.

Both feature switches are off by default. Turning the icon option off restores the captured original drawables. Applications without a bundled mapping keep their existing icons; activity-specific mappings take priority over package fallbacks, so dialer and contacts activities can retain distinct icons.

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
