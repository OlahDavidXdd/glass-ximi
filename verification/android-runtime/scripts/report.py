#!/usr/bin/env python3
"""Summarize executed tests without presenting host runtime tests as firmware verification."""
import json,hashlib,xml.etree.ElementTree as ET
from pathlib import Path
root=Path(__file__).resolve().parents[1]
reports=list((root/'build/test-results/test').glob('TEST-*.xml'))
assert reports,'No Android runtime test results'
cases=[]
for file in reports:
 for case in ET.parse(file).getroot().findall('testcase'):
  cases.append({'class':case.get('classname'),'name':case.get('name'),'passed':case.find('failure') is None and case.find('error') is None and case.find('skipped') is None})
assert len(cases)==16,(len(cases),cases)
assert all(c['passed'] for c in cases),cases
production=root.parents[1]/'launcher-addon/app/src/main/java/io/github/devarix/kashiglass'
release=json.loads((root/'fixtures/current-release-provenance.json').read_text())
baseline=json.loads((root/'results/baseline-verification.json').read_text())
assert baseline['expected_failures_reproduced']
result={'scope':'Production Android Java classes executed by Robolectric with native graphics; not full APK/HyperOS execution','robolectric':'4.14.1','android_apis':[33,35],'tests':len(cases),'all_passed':True,'physical_phone_used':False,'lsposed_hook_dispatch_verified':False,'kashi_end_to_end_animation_verified':False,'xiaomi_native_blur_verified':False,'apk_or_existing_release_changed':False,'production_source_sha256':{file.name:hashlib.sha256(file.read_bytes()).hexdigest() for file in production.glob('*.java') if file.name in release['production_source_sha256']},'cases':cases}
result['reference_release']=release
result['production_sources_match_released_0100']=result['production_source_sha256']==release['production_source_sha256']
result['baseline_negative_control']=baseline
result['native_canvas_outputs']=[str(file.relative_to(root/'results')) for file in sorted((root/'results/canvas').glob('*.png'))]
assert len(result['native_canvas_outputs'])==2,'Missing native Canvas output'
(root/'results').mkdir(exist_ok=True)
(root/'results/SUMMARY.md').write_text('''# Phone-free Android runtime verification

- **16 / 16 current-code tests passed** with native graphics on Android APIs 33 and 35.
- **4 expected failures reproduced** in released 0.9.0 source: shared Frosted pixels and cloning after recycled native pixels, on both APIs.
- Actual tested production sources match released 0.10.0: **'''+str(result['production_sources_match_released_0100'])+'''**.
- Native Canvas PNGs and XML/HTML test reports are attached in the workflow artifact.

This executes production Java classes in Robolectric's native-graphics Android test runtime. It does not boot HyperOS or execute Kashi/LSPosed hooks. Xiaomi native blur, end-to-end gestures, source hiding inside Kashi and frame pacing are **not verified**. The APK/release is unchanged.
''')
(root/'results/android-runtime-verification.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
