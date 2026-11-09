# 0.12.0 build audit

Scope: the add-on's Java sources, exported provider/manifest, settings/restart flow, icon generation/rendering, native search requests, widget/surface lifecycle, asset validation and release workflow. This is a bounded source/runtime audit, not a claim that every device-specific bug has been eliminated. Icon artwork and the Liquid/Frosted renderer parameters were preserved.

| Finding | Change | Verification |
| --- | --- | --- |
| Surface material could remain in its old mode after a shared setting changed | Recreate managed surfaces when the complete settings snapshot changes | Code-path review; material renderer hashes preserved; firmware appearance still unverified |
| A rapid revert could be dropped while an earlier settings application was queued | Deduplicate only on the main-thread application boundary | Queue/order code review and app compilation; native LSPosed dispatch unverified |
| Re-enabling could restore obsolete hidden/alpha state | Re-read native drawable/placeholder state on enable | Source-state regression tests; native graphics fade tests |
| Restart/control requests could sit behind icon asset reads | Separate asset, control/status and restart executors | Queue ownership review; provider restart protocol tests |
| Model tags could retain view graphs through cache state | Keep cached tag identities weakly while distinguishing collected values from explicit null | Identity/cleared-reference regression tests and reference graph review; not a measured firmware heap profile |
| Search depended on a direct-parent heuristic and failures stayed buried in logs | Track known compact background views explicitly and include their blur diagnostic in surface status | Attached-view/rim runtime tests; Xiaomi blur pixels remain unverified |
| Widget decoration stayed cleared after shrinking or becoming interactive | Restore layers that no longer qualify and restore protected images | Real Android view-state regression tests |
| A missing/changed widget root/provider could retain stale decoration | Restore prior root immediately, including zero-sized replacement roots | Real Android view-state regression tests |
| Async RemoteViews changes could overwrite glass without a layout callback | Observe owned backgrounds/protected image state before drawing, without replacing input handlers | Async background/resize regression tests using Android ViewTreeObserver |
| Surface/widget mutation could originate on a worker callback | Marshal UI state changes to the main looper | Thread ownership review and runtime view tests; actual hook threading unverified |
| Provider trusted reported PID rather than Binder PID | Use framework caller PID for identity and restart consumption | Android provider protocol tests; old provider reproduces eight expected failures across two APIs |
| UI/provider self reads could look like a launcher connection; saved versions could look fresh | Only launcher reads refresh identity; opening settings requests a fresh response | Provider and hook-version regression tests |
| Empty BitmapDrawable could throw and uncloneable adaptive layers could lose native callbacks/bounds | Skip missing bitmaps and restore original child callbacks/bounds after temporary capture | Native-graphics capture regression tests; a test-only reflective API fixture checks callback/bounds ownership |
| Wallpaper allocation failure could leave loading stuck | Handle allocation failure in the loader completion path | Code-path review; no forced whole-process memory exhaustion test |
| Root capture could start after opt-in was revoked, and timeout cleanup was weak | Check opt-in before/after process start; forcibly close a still-running process | Disabled/revoked opt-in tests; no real root manager exercised |
| Preview could overlap on short screens and claim refraction in transparent mode | Scrollable minimum canvas and mode-specific descriptions | Layout/source review and lint; not a physical screen test |
| Fresh CI debug keys can block APK updates | Managed-signing support, explicit mode/certificate reporting, immutable public releases pinned to the build commit | Four signing setup tests and both local signing configuration branches; managed CI key still requires repository secrets |

## Executed checks

The release validation records unit counts, APK signature, assets and preserved renderer hashes. The Android-runtime workflow covers bitmap cloning/recycling, transparency/fade, attached-search dispatch/restore, widget changes, provider identities and disabled root capture on API 33 and 35. It uses native graphics for bitmap/Canvas tests; firmware calls are recorded/rejected, provider Binder identities are test identities, and LSPosed hook installation is not executed. Negative controls pin the released 0.9 drawable and 0.11 provider source, so the old implementations must reproduce the corresponding failures.

## Remaining limits

- Xiaomi's compositor/native blur, live Kashi app transitions/gestures and frame pacing have not been verified on a compatible HyperOS runtime. A passing host test does not prove visual blur or smoothness.
- Clock/weather cards that flatten their content and background cannot be separated safely; MAML-only cards remain unsupported.
- System splash artwork is outside the launcher module's scope.
- Persistent update signing requires repository Actions secrets. The current GitHub connection is denied secret administration (HTTP 403); support is prepared, but the runner-debug certificate limitation remains until an administrator configures it. See [signing setup](SIGNING.md).
- The last supplied phone log had installed and loaded **0.9.0**, so it did not test the later animation changes. After updating, verify that the loaded hook matches the installed build before judging those fixes.
