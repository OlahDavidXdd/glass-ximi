# 0.17.0: page pill and live drawer material

Kashi's native page dots use LauncherIndicator.mScreenIndicator separately from CapsuleIndicator's Search body. PageIndicatorGlass decorates only that view's background, shares the approved material/underlay helper and retains all dot children and native visibility/actions. Native target/style replacement, disable and detach restore original backgrounds/tints/modes.

Drawer ImageView binding now covers bitmap/resource/Icon setters and verifies live material before painting. LiveGlassMotion invalidates moved/scaled/resized icon views, including image-matrix changes, without perpetual redraws. It uses weak ownership and removes observers on detach. Artwork, renderer parameters and source animation guards are retained. Android tests exercise real views and production state/observation helpers; Xposed hook dispatch, Xiaomi blur pixels and fling performance remain unverified.

# 0.16.0: widget material and source-return guards

Both widget paths construct their material via SharedGlassMaterial and reset native underlays with the approved search helper, preserving the renderer parameters. Native underlay modes are restored on disable/detach/root replacement. Pending Android widget resets activate after attachment and deduplicate unchanged reapplication. Existing separation rules continue to retain flattened/unknown live content.

Native direct compound-drawable restores could reach TextView outside the private ShortcutIcon setter/pre-draw coverage. The module now intercepts source-icon compound assignments, verifies the backing field at native getter access and checks source integrity immediately before TextView painting. Native clear/mask/hide/alpha behavior is retained. Host tests execute production SourceIconReturn/LiveIconBinding against real TextView/Bitmap objects, including repeated native restore sequences. Actual Xposed dispatch and firmware exit animation are not executed by these tests.

# 0.15.0: common search material

The compact search branch used SearchGlassDrawable plus SearchBarBlur's attached native plane, unlike dock/drawer. Search is now constructed by SharedGlassMaterial using the existing LiquidGlassDrawable, with capsule outline geometry. The additional body/host native planes are disabled while this material is installed, native search images are removed, and reintroduced background tint is cleared. Disable restores saved native modes and decoration; no search text or tap handlers are replaced.

Native graphics tests compare material center pixels for dock/search/drawer in both material modes and check transparent/hidden output. Recorded firmware boundaries verify underlay reset, style refresh, restoration, attachment and rejection handling. Actual Xiaomi compositor pixels remain unverified. The icon renderer, shader, PNGs and dock/drawer rendering code/parameters are unchanged.

# 0.14.0: native MAML widget path

The prior Android-only widget guard skipped Kashi's separate MaMlHostView cards. The native path is now discovered and logged, with clock/weather metadata filtering. The existing material is drawn in the registered root's inner ElementGroup render pass, after the engine clears its canvas. Only large, noninteractive named background images or rectangle decoration is skipped. Clock hands, weather symbols, text, touch handlers, unrelated roots and flattened images are retained. Disable/detach/root replacement remove the binding without altering native element state.

`compatibility/maml-widget-api-v75.json` records the native signatures checked against the APK. Runtime tests execute the production drawing/binding/policy using API-shaped fixtures; actual MAML/Xposed dispatch and device output are not emulated. The new status reports bound roots, successful material drawing and background replacements separately, including a zero count where opaque/unknown content remains.

# 0.13.0 targeted follow-up: repeated transitions

Phone report on 0.12.0: the first transitions used glass, later ones used stock, and repeated transitions left stock artwork on the launcher. Settings did not recover it.

Source review found that `State.applied` did not prove the live drawable was still owned, and `rememberCurrent` could import an arbitrary native BitmapDrawable into the canonical glyph cache. The new `LiveIconBinding` retains only validated decoded/generated artwork, checks live drawable identity and bitmap generation/recycling, and repairs native replacements. The pre-draw watcher repairs bypassed setters and restores stale visible compound slots while preserving native hides, transparent masks and clears. Frosted live bitmaps are copied before exposure. Animation handoffs resolve the current target every time and refresh before FloatingIconView2/Layer2 derive their layers.

Production ownership/watcher/handoff classes are exercised in the Android host suite: 500 stock replacements and 500 recycled/alternating-target handoffs per API, mutation/recycling, style/app rebinding, native hide/clear, detach/reattach. The host executes Android classes and native Bitmap graphics, but not Xposed hook dispatch or the full launcher. Phone confirmation is still required. Preserved renderer hashes and all 1,400 packaged PNGs remain unchanged. No new search/blur/clock/weather styling is claimed in this follow-up.

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
- Clock/weather cards that flatten their content and background cannot be separated safely; MAML-only cards were unsupported through 0.13.0. Version 0.14.0 adds scoped native-root rendering for separable MAML backgrounds; flattened/unknown image content is still retained.
- System splash artwork is outside the launcher module's scope.
- Persistent update signing requires repository Actions secrets. The current GitHub connection is denied secret administration (HTTP 403); support is prepared, but the runner-debug certificate limitation remains until an administrator configures it. See [signing setup](SIGNING.md).
- The last supplied phone log had installed and loaded **0.9.0**, so it did not test the later animation changes. After updating, verify that the loaded hook matches the installed build before judging those fixes.
