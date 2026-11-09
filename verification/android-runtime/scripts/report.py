#!/usr/bin/env python3
"""Summarize executed tests without presenting host runtime tests as firmware verification."""
import json,hashlib,re,xml.etree.ElementTree as ET
from pathlib import Path
root=Path(__file__).resolve().parents[1]
reports=list((root/'build/test-results/test').glob('TEST-*.xml'))
assert reports,'No Android runtime test results'
cases=[]
for file in reports:
 for case in ET.parse(file).getroot().findall('testcase'):
  cases.append({'class':case.get('classname'),'name':case.get('name'),'passed':case.find('failure') is None and case.find('error') is None and case.find('skipped') is None})
assert len(cases)==114,(len(cases),cases)
assert all(c['passed'] for c in cases),cases
production=root.parents[1]/'launcher-addon/app/src/main/java/io/github/devarix/kashiglass'
release=json.loads((root/'fixtures/current-release-provenance.json').read_text())
baseline=json.loads((root/'results/baseline-verification.json').read_text())
provider_baseline=json.loads((root/'results/provider-baseline-verification.json').read_text())
assert baseline['expected_failures_reproduced'] and provider_baseline['expected_failures_reproduced']
inputs=set(re.findall(r"\*\*/([A-Za-z0-9]+\.java)",(root/'build.gradle').read_text()))
result={'scope':'Production Android classes executed by Robolectric; native graphics where applicable; not full APK/HyperOS execution','robolectric':'4.14.1','android_apis':[33,35],'tests':len(cases),'all_passed':True,'physical_phone_used':False,'lsposed_hook_dispatch_verified':False,'kashi_end_to_end_animation_verified':False,'xiaomi_native_blur_verified':False,'apk_or_existing_release_changed':False,'production_source_sha256':{file.name:hashlib.sha256(file.read_bytes()).hexdigest() for file in production.glob('*.java') if file.name in inputs},'cases':cases}
result['search_test_limit']='Search firmware calls are recorded/rejected to verify attached-view routing and restoration. Actual Xiaomi blur pixels are not emulated or verified.'
result['search_source_sha256']={name:hashlib.sha256((production/name).read_bytes()).hexdigest() for name in ['SearchBarBlur.java','SearchGlassDrawable.java']}
result['maml_test_limit']='Production selection/drawing/binding runs against fixtures with Kashi-verified MAML getters/fields. The real MAML engine and Xposed dispatch are not executed.'
result['provider_test_limit']='Provider tests use Robolectric Binder/package identities. They do not run kernel Binder, LSPosed hooks or real firmware.'
result['reference_release']=release
result['production_sources_match_released_0100']=all(result['production_source_sha256'].get(name)==digest for name,digest in release['production_source_sha256'].items())
result['visual_renderer_reference_match']=all(result['production_source_sha256'].get(name)==release['production_source_sha256'][name] for name in ['LiquidGlassDrawable.java','NativeScreenBackdrop.java','GlassSurface.java','BackdropGeometry.java'])
result['baseline_negative_control']=baseline
result['provider_negative_control']=provider_baseline
result['native_canvas_outputs']=[str(file.relative_to(root/'results')) for file in sorted((root/'results/canvas').glob('*.png'))]
assert len(result['native_canvas_outputs'])==2,'Missing native Canvas output'
(root/'results').mkdir(exist_ok=True)
(root/'results/SUMMARY.md').write_text('''# Phone-free Android runtime verification

- **114 / 114 current-code tests passed** on Android APIs 33 and 35, including native graphics, widget restoration and provider protocol cases.
- **4 expected failures reproduced** in released 0.9.0 source: shared Frosted pixels and cloning after recycled native pixels, on both APIs.
- Visual renderer references remain unchanged: **'''+str(result['visual_renderer_reference_match'])+'''**.
- Native MAML selection/binding/material drawing uses fixtures for verified native getters/fields; the real MAML engine and Xposed callbacks are not executed.
- New search tests record/reject the native-call boundary to verify routing and restoration; they do not emulate Xiaomi blur pixels.
- **8 provider protocol failures reproduced** with the released 0.11.0 provider, across APIs 33/35; the current provider passes.
- Native Canvas PNGs and XML/HTML test reports are attached in the workflow artifact.

This executes production Java classes in Robolectric's native-graphics Android test runtime. It does not boot HyperOS or execute Kashi/LSPosed hooks. Xiaomi native blur, end-to-end gestures, source hiding inside Kashi and frame pacing are **not verified**. This verification workflow does not rebuild or replace APK releases.
''')
(root/'results/android-runtime-verification.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
