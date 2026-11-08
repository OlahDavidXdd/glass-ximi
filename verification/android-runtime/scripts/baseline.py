#!/usr/bin/env python3
import hashlib,json,sys,xml.etree.ElementTree as ET
from pathlib import Path
root=Path(__file__).resolve().parents[1]
if len(sys.argv)==1:
 fixture=root/'fixtures/OwnedTransitionDrawable-0.9.0.java.txt'
 assert hashlib.sha256(fixture.read_bytes()).hexdigest()==json.loads((root/'fixtures/provenance.json').read_text())['sha256']
 target=root/'results/baseline-source/io/github/devarix/kashiglass/OwnedTransitionDrawable.java'
 target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(fixture.read_bytes());print(target.parents[4])
else:
 cases=[]
 for file in (root/'baseline-build/test-results/test').glob('TEST-*.xml'):
  cases.extend(ET.parse(file).getroot().findall('testcase'))
 assert len(cases)==10,len(cases)
 failures=[case.get('name') for case in cases if case.find('failure') is not None or case.find('error') is not None]
 assert len(failures)==4,failures
 assert all('constantStateStillClonesAfterNativeBitmapWasRecycled' in name or 'frostedClonesAlsoOwnIndependentPixels' in name for name in failures),failures
 result={'scope':'Negative control using released 0.9.0 production drawable source with identical Android runtime tests','expected_failures_reproduced':True,'android_apis':[33,35],'tests':10,'expected_failures':4,'failure_names':failures,'fixture':json.loads((root/'fixtures/provenance.json').read_text())}
 (root/'results/baseline-verification.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
