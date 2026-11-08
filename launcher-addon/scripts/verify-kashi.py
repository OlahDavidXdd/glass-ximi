#!/usr/bin/env python3
from pathlib import Path
import zipfile,struct,re,json,sys
apk=Path(sys.argv[1]);targets={'Lcom/miui/home/icon/LauncherIconImageView;','Lcom/miui/home/settings/IconCustomizeFragment;','Lcom/miui/home/launcher/ShortcutInfo;','Lcom/miui/home/launcher/ShortcutIcon;'};found={}
with zipfile.ZipFile(apk) as z:
 for name in z.namelist():
  if not re.match(r'classes\d*\.dex$',name):continue
  d=z.read(name);u32=lambda off:struct.unpack_from('<I',d,off)[0]
  def leb(off):
   n=0;shift=0
   while True:
    b=d[off];off+=1;n|=(b&127)<<shift
    if not b&128:return n,off
    shift+=7
  count,offset=struct.unpack_from('<II',d,56);strings=[]
  for i in range(count):
   pos=u32(offset+4*i);_,pos=leb(pos);strings.append(d[pos:d.find(b'\0',pos)].decode('utf8','replace'))
  count,offset=struct.unpack_from('<II',d,64);types=[strings[u32(offset+4*i)] for i in range(count)]
  methodcount,methodoff=struct.unpack_from('<II',d,88);classcount,classoff=struct.unpack_from('<II',d,96)
  for i in range(classcount):
   off=classoff+32*i;cls=types[u32(off)]
   if cls not in targets:continue
   pos=u32(off+24);counts=[]
   for _ in range(4):n,pos=leb(pos);counts.append(n)
   for _ in range(counts[0]+counts[1]):_,pos=leb(pos);_,pos=leb(pos)
   names=[]
   for total in counts[2:]:
    idx=0
    for _ in range(total):
     delta,pos=leb(pos);idx+=delta;_,pos=leb(pos);_,pos=leb(pos);names.append(strings[u32(methodoff+idx*8+4)])
   found[cls]={'super':types[u32(off+8)],'methods':names}
assert targets<=found.keys(),'Missing hook targets: '+str(targets-found.keys())
assert any(n in found['Lcom/miui/home/settings/IconCustomizeFragment;']['methods'] for n in ['onCreate','onCreatePreferences','onResume'])
assert any(n in found['Lcom/miui/home/launcher/ShortcutInfo;']['methods'] for n in ['getPackageName','getTargetPackage','getIntent','getComponentName'])
assert {'setIconDrawable','getIconDrawable','applyCompoundDrawables','getShortcutInfo'}<=set(found['Lcom/miui/home/launcher/ShortcutIcon;']['methods'])
print(json.dumps(found,indent=2))
