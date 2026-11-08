#!/usr/bin/env python3
from pathlib import Path
import zipfile,struct,re,json,sys
apk=Path(sys.argv[1]);targets={'Lcom/miui/home/icon/LauncherIconImageView;','Lcom/miui/home/settings/IconCustomizeFragment;','Lcom/miui/home/launcher/ShortcutInfo;','Lcom/miui/home/launcher/ShortcutIcon;','Lcom/miui/home/launcher/hotseats/HotSeats;','Lcom/miui/home/launcher/SearchBarDesktopLayout;','Lcom/miui/home/launcher/SearchBarDrawerLayout;','Lcom/miui/home/launcher/allapps/BaseAllAppsContainerView;','Lcom/miui/home/recents/views/FloatingIconView2;','Lcom/miui/home/recents/views/FloatingIconLayer2;'};found={}
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
  protocount,protooff=struct.unpack_from('<II',d,72)
  methodcount,methodoff=struct.unpack_from('<II',d,88);classcount,classoff=struct.unpack_from('<II',d,96)
  for i in range(classcount):
   off=classoff+32*i;cls=types[u32(off)]
   if cls not in targets:continue
   pos=u32(off+24);counts=[]
   for _ in range(4):n,pos=leb(pos);counts.append(n)
   for _ in range(counts[0]+counts[1]):_,pos=leb(pos);_,pos=leb(pos)
   names=[];animation_inputs=[]
   for total in counts[2:]:
    idx=0
    for _ in range(total):
     delta,pos=leb(pos);idx+=delta;_,pos=leb(pos);_,pos=leb(pos);methodname=strings[u32(methodoff+idx*8+4)];names.append(methodname)
     proto=struct.unpack_from('<H',d,methodoff+idx*8+2)[0];params=u32(protooff+12*proto+8)
     parameters=[types[struct.unpack_from('<H',d,params+4+2*j)[0]] for j in range(u32(params))] if params else []
     for j in range(len(parameters)-1):
      if parameters[j:j+2]==['Lcom/miui/home/common/animate/LaunchAppAndBackHomeAnimTarget;','Landroid/graphics/drawable/Drawable;']:animation_inputs.append({'method':methodname,'drawable_parameter':j+1})
   found[cls]={'super':types[u32(off+8)],'methods':names,'animation_inputs':animation_inputs}
assert targets<=found.keys(),'Missing hook targets: '+str(targets-found.keys())
assert any(n in found['Lcom/miui/home/settings/IconCustomizeFragment;']['methods'] for n in ['onCreate','onCreatePreferences','onResume'])
assert any(n in found['Lcom/miui/home/launcher/ShortcutInfo;']['methods'] for n in ['getPackageName','getTargetPackage','getIntent','getComponentName'])
assert {'setIconDrawable','getIconDrawable','applyCompoundDrawables','getShortcutInfo','setOriginalDrawableAndRoundedDrawable','getContentDrawable','getBackAnimPreviewDrawable','getBoomAnimBitmap','onTouchEvent'}<=set(found['Lcom/miui/home/launcher/ShortcutIcon;']['methods'])
for target in ['Lcom/miui/home/recents/views/FloatingIconView2;','Lcom/miui/home/recents/views/FloatingIconLayer2;']:
 assert found[target]['animation_inputs'],'No supported animation input pair: '+target
print(json.dumps(found,indent=2))
