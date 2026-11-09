#!/usr/bin/env python3
import json,re,sys,os
from pathlib import Path
report=Path(sys.argv[1]);certificate=Path(sys.argv[2]).read_text()
match=re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-fA-F]{64})',certificate)
assert match,'APK signing certificate fingerprint not found'
state=json.loads(Path(sys.argv[3]).read_text())
result=json.loads(report.read_text());result['apk_certificate_sha256']=match.group(1).lower();result['signing']=state
if os.environ.get('GITHUB_SHA'):result['build_commit']=os.environ['GITHUB_SHA']
report.write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps({'apk_certificate_sha256':result['apk_certificate_sha256'],'signing':state},indent=2))
