Developer: **devarix**

Experimental **LSPosed add-on** for Kashi HyperOS Launcher V7.5 on HyperOS 3. This is not a rebuilt Kashi APK.

- Adds **Glass icons (devarix)** to the launcher's Icons settings.
- Bundles the 700 grayscale glass icon mappings at 512 × 512 and 75% background opacity.
- Separate opt-in controls for the icon pack and Xiaomi native background blur, plus an 8–60 dp radius control.
- Both switches are off by default. Disable them to restore the captured original icon drawables.
- No launcher APK, database, or global system property is modified. No Internet permission.

**Requirements:** rooted phone, LSPosed scoped to `com.miui.home`, compatible Xiaomi native blur support. Install the APK, enable the module, restart the launcher after unlocking, then open its Icons settings or the standalone devarix Glass app.

**Validation:** source builds, unit tests, Android lint, APK signature, bundled assets, and hook target classes checked. **Native blur has not yet been verified on a physical HyperOS phone.** The app reports native API success/failure; success still requires a visual check on-device. No fake blurred wallpaper is substituted if unsupported.

The standalone source archive and validation report are attached. Source and setup details: https://github.com/OlahDavidXdd/glass-ximi/tree/main/launcher-addon

Base icon attribution: Guofeng 26 Lite Edition by 一凡UI. Additional artwork sources are listed in the source README.
