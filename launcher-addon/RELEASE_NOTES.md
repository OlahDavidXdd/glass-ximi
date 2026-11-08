Developer: **devarix**

## Liquid Glass using the requested repository

- Added **Liquid Glass** and retained **Frosted Glass** as selectable materials inside the Kashi add-on.
- Uses the actual, unchanged AGSL refraction shader from **QWEA0/Liquid-Glass-Android**, with MIT license and pinned source attribution included.
- Bundles 700 transparent symbol mappings for the lens and the existing 700 Frosted mappings. Foreground symbols stay sharp and grayscale.
- Added **Preview Liquid Glass**: drag icons over a striped background using the actual shader, without enabling LSPosed or granting root.
- Added activity-specific icon lookup and narrower icon layout hooks.

## Launcher requirements and limits

Requires LSPosed scoped to `com.miui.home` for Kashi integration. The lens currently samples **static wallpapers**, not live-wallpaper frames or the fully composed launcher. An optional, explicit root-wallpaper switch is available if normal wallpaper access is denied; it only reads the current user's wallpaper file. Root is not requested while that switch is off.

**Physical-device rendering is not verified.** Build, unit tests, lint, both asset sets, upstream shader provenance, uniform coverage and APK signature are checked. The preview and status screen report unsupported drivers or denied capture; a successful command is not a device verification.

Install → open **Preview Liquid Glass** → enable the module in LSPosed → restart the launcher → **Icons → Glass icons (devarix)** → select Liquid Glass and enable the pack. Backdrop blur is optional.

If updating 0.1.0 fails because of the development signing key, uninstall **devarix Glass only**, install this version, and re-enable its scope. Do not uninstall Kashi/System launcher.

Source: https://github.com/OlahDavidXdd/glass-ximi/tree/main/launcher-addon
