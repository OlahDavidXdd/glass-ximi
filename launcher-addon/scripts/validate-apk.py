#!/usr/bin/env python3
import hashlib,json,struct,sys,zipfile,re,xml.etree.ElementTree as ET
from pathlib import Path
root=Path(__file__).resolve().parents[1];apk=Path(sys.argv[1]);report={}
with zipfile.ZipFile(apk) as z:
 assert z.testzip() is None
 names=[n for n in z.namelist() if n.startswith('assets/icons/') and n.endswith('.png')];assert len(names)==700
 for name in names:
  assert z.getinfo(name).compress_type==zipfile.ZIP_STORED,name
  data=z.read(name);assert data==(root/'app/src/main'/name).read_bytes(),name
  assert struct.unpack_from('>II',data,16)==(512,512),name
 glyphs=[n for n in z.namelist() if n.startswith('assets/glyphs/') and n.endswith('.png')];assert len(glyphs)==700
 assert {Path(n).name for n in names}=={Path(n).name for n in glyphs}
 for name in glyphs:
  assert z.getinfo(name).compress_type==zipfile.ZIP_STORED,name
  data=z.read(name);assert data==(root/'app/src/main'/name).read_bytes(),name
  assert struct.unpack_from('>II',data,16)==(512,512),name
 upstream=json.loads((root/'compatibility/liquidglass-upstream.json').read_text())
 shader=z.read('assets/shaders/qwea0_lens.agsl');assert hashlib.sha256(shader).hexdigest()==upstream['extracted_shader_sha256']
 assert b'pandadog' in z.read('assets/licenses/Liquid-Glass-Android-MIT.txt')
 expected=set(re.findall(r'uniform\s+float[234]?\s+(\w+)',shader.decode()))
 actual=set(re.findall(r'setFloatUniform\("([^"]+)"',(root/'app/src/main/java/io/github/devarix/kashiglass/LiquidGlassDrawable.java').read_text()))
 assert expected==actual,(expected-actual,actual-expected)
 report.update(liquid_glyphs=700,qwea0_shader_unmodified=True,shader_uniforms_complete=True)
 assert z.read('assets/xposed_init').decode().strip()=='io.github.devarix.kashiglass.GlassHook'
 report.update(icon_mappings=700,source_assets_match=True,asset_descriptors_supported=True,apk_sha256=hashlib.sha256(apk.read_bytes()).hexdigest())
units=list((root/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
assert {'TEST-io.github.devarix.kashiglass.GlassConfigTest.xml','TEST-io.github.devarix.kashiglass.LauncherRestartTest.xml','TEST-io.github.devarix.kashiglass.DiagnosticLogTest.xml','TEST-io.github.devarix.kashiglass.AutoIconPixelsTest.xml','TEST-io.github.devarix.kashiglass.GlassSurfaceTest.xml'}<={p.name for p in units}
count=0
for unit_path in units:
 unit=ET.parse(unit_path).getroot()
 assert unit.get('failures')=='0' and unit.get('errors')=='0' and int(unit.get('skipped','0'))==0
 count+=int(unit.get('tests'))
report.update(unit_tests=count,physical_device_tested=False,launcher='Kashi V7.5 / HyperOS 3',requires='Root and LSPosed',developer='devarix')
text=json.dumps(report,indent=2);print(text)
if len(sys.argv)>2:Path(sys.argv[2]).write_text(text+'\n')
