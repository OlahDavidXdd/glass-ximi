# APK update signing

The early test workflow built on a fresh GitHub runner without a persistent signing key. Android Gradle generated a debug keystore for each run. Different certificates prevent normal in-place updates, even when the package name is the same. Re-running a workflow could also replace a public release with a differently signed APK; 0.12.0 now preserves already-published release assets.

The workflow supports these repository **Actions secrets** for persistent signing:

- `DEVARIX_KEYSTORE_B64`: Base64-encoded JKS/PKCS12 keystore bytes.
- `DEVARIX_STORE_PASSWORD`: Store password.
- `DEVARIX_KEY_PASSWORD`: Key password.
- `DEVARIX_KEY_ALIAS`: Alias of the signing key.

A repository administrator must create/manage the key securely and add these through repository Settings → Secrets and variables → Actions. Keep a secure backup of the private key; never commit it to this repository or attach it to an issue. The workflow decodes it into the runner's private temporary directory and does not package it into the source ZIP or APK. Partial/invalid configuration fails instead of silently falling back.

If no key is configured, the workflow explicitly warns that it is using an ephemeral debug certificate. `validation.json` records the signing mode and the public certificate SHA-256, and exported app diagnostics include the installed certificate fingerprint. **Persistent signing is not configured by adding this support alone.** The current connected GitHub token cannot administer Actions secrets (HTTP 403), so that configuration remains an administrator task.

A new persistent key cannot match old runner keys that were never retained. The first transition can require reinstalling **devarix Glass only**. That resets the add-on's settings/scope, so save its diagnostic settings report first, re-enable its LSPosed scope after reinstalling, and restore your preferred icon material/options. Keep System launcher installed. Subsequent versions signed with the same managed key can update in place. If a user already has a newer source of the module with a securely retained key, prefer reusing that key to avoid another transition.
