Developer: **devarix**

## 0.12.0 — Build audit and regression fixes

Preserves the approved icon artwork and renderer parameters. See [the audit](AUDIT.md) for scope, evidence and remaining limits.

- Rebuilds dock/drawer/search surfaces when shared material settings change; avoids dropping a rapid settings revert while a previous update is queued.
- Refreshes real source visibility/alpha when the module is re-enabled, clears stale animation artwork on restore and avoids strong cached tag references.
- Separates restart, control/status and asset queues so icon loading cannot hold up a restart request.
- Restores widget decoration after shrinking, becoming interactive or losing/replacing its provider/root; reapplies async background changes before drawing. UI mutations are marshalled to the main looper.
- Uses Binder’s actual caller PID for loaded-hook identity and restart consumption. UI reads no longer count as a launcher connection, and settings requests a fresh response instead of presenting a saved version as live.
- Handles empty automatic-icon bitmaps and restores uncloneable foreground layers’ original callbacks/bounds after capture.
- Handles wallpaper allocation errors without leaving loading stuck; checks root opt-in before/after starting a read and forcibly closes a still-running reader process on cleanup.
- Makes the icon preview scrollable on short screens and describes its selected material accurately. Compact-search blur diagnostics are visible in surface status. Diagnostic exports include widget options/status and the public APK signing certificate fingerprint.
- Supports persistent signing from repository Actions secrets and records signing mode/certificate in `validation.json`. Published release assets are immutable; rerunning a job does not replace them with a new debug certificate.

## Apply

Install, keep LSPosed scoped to **System launcher (`com.miui.home`)**, restart/reboot and verify **Loaded hook: 0.12.0-test — matches app**, with a fresh launcher response. Keep your preferred icon material. The last phone log was still 0.9.0, so it did not exercise the later animation fixes.

**Signing remains a configuration limitation:** this GitHub connection cannot administer Actions secrets (HTTP 403). Without the managed-signing secrets, CI still uses an ephemeral debug key and clearly reports that mode. Android can reject an in-place update from an earlier differently signed test APK. See [signing setup](SIGNING.md); if reinstalling is required, save your settings report, reinstall **devarix Glass only**, restore its scope/options and reboot. Keep System launcher installed.

## Checks and limits

The package is checked with unit tests, lint, APK signature/asset validation and preserved renderer hashes. The host Android suite runs on API 33/35 and covers bitmap ownership, rendering, search dispatch/restore, widget changes, provider identities and disabled root capture. Old-source negative controls reproduce the earlier snapshot and provider failures. Signing setup branches have separate tests. Counts are recorded in CI reports.

These tests do **not** execute LSPosed hook dispatch, Xiaomi’s real native blur compositor, full Kashi gestures or frame pacing. No claim is made that every firmware-specific animation issue is fixed. MAML-only/flattened widgets and Android system splash artwork retain the limits described in the audit.
