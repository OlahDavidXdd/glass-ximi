#!/usr/bin/env python3
import hashlib,json,struct,sys,zipfile,xml.etree.ElementTree as ET
from pathlib import Path
root=Path(__file__).resolve().parents[1];apk=Path(sys.argv[1]);report={}
with zipfile.ZipFile(apk) as z:
 assert z.testzip() is None
 names=[n for n in z.namelist() if n.startswith('assets/icons/') and n.endswith('.png')];assert len(names)==700
 for name in names:
  assert z.getinfo(name).compress_type==zipfile.ZIP_STORED,name
  data=z.read(name);assert data==(root/'app/src/main'/name).read_bytes(),name
  assert struct.unpack_from('>II',data,16)==(512,512),name
 assert z.read('assets/xposed_init').decode().strip()=='io.github.devarix.kashiglass.GlassHook'
 report.update(icon_mappings=700,source_assets_match=True,asset_descriptors_supported=True,apk_sha256=hashlib.sha256(apk.read_bytes()).hexdigest())
unit=ET.parse(root/'app/build/test-results/testDebugUnitTest/TEST-io.github.devarix.kashiglass.GlassConfigTest.xml').getroot()
assert unit.get('failures')=='0' and unit.get('errors')=='0'
report.update(unit_tests=int(unit.get('tests')),physical_device_tested=False,launcher='Kashi V7.5 / HyperOS 3',requires='Root and LSPosed',developer='devarix')
text=json.dumps(report,indent=2);print(text)
if len(sys.argv)>2:Path(sys.argv[2]).write_text(text+'\n')
