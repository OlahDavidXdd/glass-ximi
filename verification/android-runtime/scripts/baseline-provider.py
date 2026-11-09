#!/usr/bin/env python3
import hashlib,json,sys,xml.etree.ElementTree as ET
from pathlib import Path
root=Path(__file__).resolve().parents[1]
if len(sys.argv)==1:
    fixture=root/'fixtures/GlassProvider-0.11.0.java.txt'
    assert hashlib.sha256(fixture.read_bytes()).hexdigest()==json.loads((root/'fixtures/provider-provenance.json').read_text())['sha256']
    target=root/'results/provider-baseline-source/io/github/devarix/kashiglass/GlassProvider.java'
    target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(fixture.read_bytes());print(target.parents[4])
else:
    cases=[]
    for file in (root/'provider-baseline-build/test-results/test').glob('TEST-*.xml'):cases.extend(ET.parse(file).getroot().findall('testcase'))
    assert len(cases)==10,len(cases)
    failures=[c.get('name') for c in cases if c.find('failure') is not None or c.find('error') is not None]
    expected=['reportedPidComesFromBinderNotUntrustedExtras','uiReadDoesNotPretendLauncherConnected','spoofedPidCannotConsumeAnotherProcessesRestart','actualConnectedPidCanConsumeOnlyOnce']
    assert len(failures)==8,failures
    assert all(any(e in name for e in expected) for name in failures),failures
    result={'expected_failures_reproduced':True,'android_apis':[33,35],'tests':10,'expected_failures':8,'failure_names':failures,'fixture':json.loads((root/'fixtures/provider-provenance.json').read_text())}
    (root/'results/provider-baseline-verification.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
