# Phone-free Android runtime verification

Runs the production drawable/snapshot classes under Robolectric 4.14.1 with **native graphics** on Android API 33 and 35. These are executable Android bitmap/Canvas, widget-state and provider protocol tests, separate from the browser motion simulation and the add-on's pure Java tests.

The tests use the original production Java files directly; they do not substitute a web animation or a duplicate snapshot implementation. They check independent pixel ownership, cloning after native-copy recycling, Frosted clone isolation, transparent holes/fade alpha, invalid templates, real bundled glyph rendering, hidden-drawable rendering and snapshot independence from live source alpha.

Requires Java 17, Android SDK platform 35, and Maven/Google Maven access:

```sh
export ANDROID_HOME=/path/to/android-sdk
./launcher-addon/gradlew -p verification/android-runtime test --no-daemon --console=plain
```

The report is `verification/android-runtime/build/reports/tests/test/index.html`.

This verifies Android graphics code in a host-side Android test runtime. It does **not** boot the APK in HyperOS, load LSPosed into Kashi, execute Xiaomi's proprietary backdrop/compositor APIs, or establish that the on-device open/close gestures, source hiding and frame timing are correct. Those require a compatible HyperOS runtime, which is not present in this execution environment. No APK or existing release is changed by running this suite.

The separate **Verify Android snapshot runtime** GitHub workflow runs this suite and an expected-failure negative control using the pinned 0.9.0 drawable source in `fixtures/`. Four expected failures (two bugs across two Android versions) must reproduce in the older implementation while the current 100 tests pass. This guards against a test suite that simply passes regardless of the fix. The workflow uploads XML/HTML reports, a structured coverage report and native-Canvas PNG output. It does not rebuild or replace the released APK.

Search tests additionally check routing to an attached decorative view, frame request deduplication, restoration, rejected calls, unattached views and a transparent search rim independent of icon mode. The Xiaomi firmware call boundary is recorded/rejected, not emulated; these tests do not verify actual native blur pixels.

The audit suite also runs the real provider with test Binder identities and package metadata, exercises widget background restoration using Android views, protects disabled root capture, and checks automatic-capture callback/bounds ownership with a test-only shape of Kashi’s reflective foreground API. Xposed API classes are supplied for declarations; hook installation is never invoked in these host tests. `BuildConfig` is generated from the actual app version metadata. A second pinned negative control must reproduce eight provider identity/protocol failures in 0.11.0. See the [build audit](../../launcher-addon/AUDIT.md) for tested versus code-reviewed findings.

The 0.13.0 ownership regression cases exercise the production LiveIconBinding, IconIntegrityWatcher and AnimationArtwork classes. On each API they replay 500 field replacements and 500 recycled transition handoffs, then test mutation, app/style changes, native clear/hide and detach/reattach. Hook wiring and Kashi layer refresh boundaries are source-reviewed rather than executed in this host runtime.

Version 0.14.0 adds 22 cases across APIs 33/35 for native MAML root/material binding, localized metadata, async root readiness, unknown/flattened content, interactive layers, root replacement and disable/detach. Fixtures model the verified MAML getter/field boundary; the native engine and Xposed callbacks are not executed.
