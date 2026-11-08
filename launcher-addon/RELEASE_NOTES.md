Developer: **devarix**

## 0.3.0 — Icon binding fix and launcher restart

- Added support for Kashi’s main **ShortcutIcon TextView** drawing path. 0.2.0 only hooked LauncherIconImageView, which can leave the module connected while home/app-drawer icons stay stock.
- Installs the live Drawable directly in the compound-icon slot to avoid launcher rounding rasterizing the QWEA0 shader; retains the existing ImageView path.
- Added **Enable Liquid Glass**, clear LSPosed activation steps, **Restart launcher**, and **Check launcher connection**.
- Status now reports supported icon views, resolved mappings and replaced drawables. Settings reload when returning to the launcher after unlocking.
- Restart requests root only on tap, targets `com.miui.home` for the current user, times out after 25 seconds, and does not clear launcher data.
- Keeps both 700-icon sets, the upstream QWEA0 AGSL shader and the in-app preview.

## Activate

1. In LSPosed enable **devarix Glass**, scoped to **System launcher (`com.miui.home`)**.
2. Open **devarix Glass → Enable Liquid Glass**.
3. Tap **Restart launcher**, allow root, then open the home screen. Reboot instead if root is denied.
4. Return to the app and check **Launcher status**. If icons are still stock, report the three binding counts and your exact Kashi version.

Requires Android 13+ and LSPosed. Liquid refraction samples a **static wallpaper**; **Wallpaper access via root** remains a separate opt-in for denied wallpaper reads. Restart permission does not enable wallpaper access.

**No physical device verification yet.** Build, unit tests, lint, APK signature, 700 + 700 icon assets and upstream shader provenance are checked. The preview alone does not verify launcher integration.

If an update is rejected due to the development signing key, uninstall **devarix Glass only**, reinstall, re-enable its LSPosed scope and restart. Keep System launcher installed.

Source: https://github.com/OlahDavidXdd/glass-ximi/tree/main/launcher-addon
