package io.github.devarix.kashiglass;

import android.app.Application;
import android.app.Activity;
import android.content.*;
import android.database.ContentObserver;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.util.LruCache;
import android.view.*;
import android.widget.ImageView;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class GlassHook implements IXposedHookLoadPackage {
    private static Handler MAIN;
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private static final Map<ImageView,State> STATES=new WeakHashMap<>();
    private static final Map<String,ArrayList<WeakReference<ImageView>>> PENDING=new HashMap<>();
    private static final Set<String> FAILED=new HashSet<>();
    private static final LruCache<String,Bitmap> ICONS=new LruCache<>(32*1024*1024){@Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getAllocationByteCount();}};
    private static final ThreadLocal<Boolean> INTERNAL=ThreadLocal.withInitial(()->false);
    private static Context context;
    private static WeakReference<Activity> lastActivity=new WeakReference<>(null);
    private static boolean enabled=false,blur=false;
    private static int radius=24;
    private static Set<String> packages=Collections.emptySet();
    private static String lastStatus="";
    private static final class State {
        Drawable original;
        boolean applied,blurred,listener;
        ViewOutlineProvider outline;
        boolean clip;
        String packageName, resolvedPackage;
        Object lastTag,lastParentTag;
        boolean resolveCached,blurFailed;
        int lastRadius,lastWidth,lastHeight;
    }
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) throws Throwable {
        if(!GlassConfig.LAUNCHER.equals(param.packageName)||!GlassConfig.LAUNCHER.equals(param.processName))return;
        if(Looper.getMainLooper()==null){XposedBridge.log("devarix Glass: no main looper; hooks skipped");return;}
        MAIN=new Handler(Looper.getMainLooper());
        XposedHelpers.findAndHookMethod(Application.class,"attach",Context.class,new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){if(context==null){context=((Context)p.args[0]).getApplicationContext();if(context==null)context=(Context)p.args[0];initialize();}}
        });
        XposedHelpers.findAndHookMethod(ImageView.class,"setImageDrawable",Drawable.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                if(!enabled||INTERNAL.get()||!isIcon(p.thisObject)||Looper.myLooper()!=Looper.getMainLooper())return;
                ImageView view=(ImageView)p.thisObject;State state=state(view);state.resolveCached=false;state.original=(Drawable)p.args[0];state.applied=false;
                String pkg=resolve(view);Bitmap bitmap=enabled?ICONS.get(pkg==null?"":pkg):null;
                if(bitmap!=null){p.args[0]=new BitmapDrawable(view.getResources(),bitmap);state.applied=true;state.packageName=pkg;}
            }
            @Override protected void afterHookedMethod(MethodHookParam p){if(enabled&&!INTERNAL.get()&&isIcon(p.thisObject))MAIN.post(()->apply((ImageView)p.thisObject));}
        });
        XposedBridge.hookAllMethods(View.class,"setTag",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(enabled&&isIcon(p.thisObject))MAIN.post(()->apply((ImageView)p.thisObject));}});
        XposedHelpers.findAndHookMethod(View.class,"layout",int.class,int.class,int.class,int.class,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(enabled&&isIcon(p.thisObject))apply((ImageView)p.thisObject);}});
        XposedHelpers.findAndHookMethod(Activity.class,"onResume",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){Activity activity=(Activity)p.thisObject;lastActivity=new WeakReference<>(activity);MAIN.post(()->{if(enabled)walk(activity.getWindow().getDecorView());});}});
        Class<?> fragment=XposedHelpers.findClassIfExists("com.miui.home.settings.IconCustomizeFragment",param.classLoader);
        if(fragment!=null)for(String method:new String[]{"onCreate","onCreatePreferences","onResume"})XposedBridge.hookAllMethods(fragment,method,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){injectOption(p.thisObject,param.classLoader);}});
    }
    private static void walk(View view){
        if(!enabled)return;
        if(isIcon(view))apply((ImageView)view);
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)walk(group.getChildAt(i));}
    }
    private static boolean isIcon(Object object){
        if(!(object instanceof ImageView))return false;
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())if(type.getName().equals("com.miui.home.icon.LauncherIconImageView"))return true;
        return false;
    }
    private static State state(ImageView view){State s=STATES.get(view);if(s==null){s=new State();s.original=view.getDrawable();STATES.put(view,s);}return s;}
    private static void initialize(){
        try{context.getContentResolver().registerContentObserver(GlassProvider.SETTINGS,false,new ContentObserver(MAIN){@Override public void onChange(boolean selfChange){reload();}});}catch(Throwable e){XposedBridge.log("devarix Glass: "+e);}
        reload();
    }
    private static void reload(){
        IO.execute(()->{
            try{
                Bundle config=context.getContentResolver().call(GlassProvider.SETTINGS,"settings",null,null);
                if(config==null)throw new IllegalStateException("Provider unavailable");
                boolean nextEnabled=config.getBoolean("icons"),nextBlur=config.getBoolean("blur");int nextRadius=GlassConfig.radius(config.getInt("radius",24));
                ArrayList<String> names=config.getStringArrayList("packages");Set<String> nextPackages=names==null?Collections.emptySet():new HashSet<>(names);
                MAIN.post(()->{enabled=nextEnabled;blur=nextBlur;radius=nextRadius;packages=nextPackages;FAILED.clear();report(enabled?"Glass pack enabled. Waiting for a mapped app icon to bind.":"Launcher connected. Glass icons are disabled.");for(ImageView v:new ArrayList<>(STATES.keySet()))if(v!=null){State state=state(v);state.resolveCached=false;state.blurFailed=false;apply(v);}Activity activity=lastActivity.get();if(enabled&&activity!=null)walk(activity.getWindow().getDecorView());});
            }catch(Throwable e){MAIN.post(()->{enabled=false;blur=false;for(ImageView v:new ArrayList<>(STATES.keySet()))if(v!=null)apply(v);});XposedBridge.log("devarix Glass config: "+e);}
        });
    }
    private static String resolve(ImageView view){
        State state=state(view);Object tag=view.getTag();Object parentTag=view.getParent() instanceof View?((View)view.getParent()).getTag():null;
        if(state.resolveCached&&tag==state.lastTag&&parentTag==state.lastParentTag)return state.resolvedPackage;
        state.lastTag=tag;state.lastParentTag=parentTag;state.resolveCached=true;state.resolvedPackage=null;
        View current=view;
        for(int depth=0;current!=null&&depth<5;depth++){
            String result=fromInfo(current.getTag());if(result!=null){state.resolvedPackage=result;return result;}
            for(String method:new String[]{"getShortcutInfo","getItemInfo"}){result=fromInfo(call(current,method));if(result!=null){state.resolvedPackage=result;return result;}}
            for(String field:new String[]{"mInfo","mItemInfo","mShortcutInfo"}){result=fromInfo(field(current,field));if(result!=null){state.resolvedPackage=result;return result;}}
            current=current.getParent() instanceof View?(View)current.getParent():null;
        }
        return null;
    }
    private static String fromInfo(Object info){
        if(info==null)return null;
        if(info instanceof Intent){ComponentName component=((Intent)info).getComponent();String pkg=component==null?((Intent)info).getPackage():component.getPackageName();return packages.contains(pkg)?pkg:null;}
        if(info instanceof ComponentName){String pkg=((ComponentName)info).getPackageName();return packages.contains(pkg)?pkg:null;}
        for(String method:new String[]{"getTargetPackage","getPackageName"}){Object result=call(info,method);if(result instanceof String&&packages.contains(result))return (String)result;}
        for(String method:new String[]{"getIntent","getComponentName","getComponent"}){Object result=call(info,method);if(result instanceof Intent||result instanceof ComponentName){String pkg=fromInfo(result);if(pkg!=null)return pkg;}}
        for(String name:new String[]{"mIntent","intent","mComponentName","componentName"}){Object result=field(info,name);if(result instanceof Intent||result instanceof ComponentName){String pkg=fromInfo(result);if(pkg!=null)return pkg;}}
        for(String name:new String[]{"mPackageName","packageName"}){Object result=field(info,name);if(result instanceof String&&packages.contains(result))return (String)result;}
        return null;
    }
    private static Object call(Object object,String method){try{return XposedHelpers.callMethod(object,method);}catch(Throwable ignored){return null;}}
    private static Object field(Object object,String name){try{return XposedHelpers.getObjectField(object,name);}catch(Throwable ignored){return null;}}
    private static void apply(ImageView view){
        if(Looper.myLooper()!=Looper.getMainLooper()){MAIN.post(()->apply(view));return;}
        if(context==null||INTERNAL.get())return;
        State state=state(view);String pkg=resolve(view);
        if(!enabled||pkg==null){restore(view,state);return;}
        Bitmap bitmap=ICONS.get(pkg);
        if(bitmap==null){if(state.applied&&!pkg.equals(state.packageName))restore(view,state);load(pkg,view);return;}
        if(!state.applied||!pkg.equals(state.packageName)){
            if(!state.applied)state.original=view.getDrawable();
            INTERNAL.set(true);try{view.setImageDrawable(new BitmapDrawable(view.getResources(),bitmap));}finally{INTERNAL.set(false);}
            state.applied=true;state.packageName=pkg;
        }
        if(!state.listener){state.listener=true;view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            public void onViewAttachedToWindow(View v){MAIN.post(()->apply((ImageView)v));}
            public void onViewDetachedFromWindow(View v){State s=STATES.get(v);if(s!=null)clearBlur((ImageView)v,s);}
        });}
        if(blur&&view.isAttachedToWindow())nativeBlur(view,state);else clearBlur(view,state);
    }
    private static void load(String pkg,ImageView view){
        if(FAILED.contains(pkg))return;
        ArrayList<WeakReference<ImageView>> waiting=PENDING.get(pkg);
        if(waiting!=null){boolean found=false;for(var ref:waiting)if(ref.get()==view){found=true;break;}if(!found)waiting.add(new WeakReference<>(view));return;}
        waiting=new ArrayList<>();waiting.add(new WeakReference<>(view));PENDING.put(pkg,waiting);
        IO.execute(()->{
            Bitmap image=null;
            try(var descriptor=context.getContentResolver().openAssetFileDescriptor(Uri.parse("content://"+GlassConfig.AUTHORITY+"/icons/"+pkg),"r")){
                if(descriptor==null)throw new IllegalStateException("Missing asset");
                BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
                try(var stream=descriptor.createInputStream()){image=BitmapFactory.decodeStream(stream,null,options);}
            }catch(Throwable e){XposedBridge.log("devarix Glass asset "+pkg+": "+e);}
            Bitmap result=image;
            MAIN.post(()->{ArrayList<WeakReference<ImageView>> views=PENDING.remove(pkg);if(result!=null)ICONS.put(pkg,result);else FAILED.add(pkg);if(views!=null)for(var ref:views){ImageView v=ref.get();if(v!=null)apply(v);}});
        });
    }
    private static void nativeBlur(ImageView view,State state){
        int pixels=Math.round(radius*view.getResources().getDisplayMetrics().density);
        if(state.blurFailed||(state.blurred&&state.lastRadius==pixels&&state.lastWidth==view.getWidth()&&state.lastHeight==view.getHeight()))return;
        try{
            if(!state.blurred){state.outline=view.getOutlineProvider();state.clip=view.getClipToOutline();}
            view.setOutlineProvider(new ViewOutlineProvider(){@Override public void getOutline(View v,Outline out){out.setRoundRect(0,0,v.getWidth(),v.getHeight(),Math.min(v.getWidth(),v.getHeight())*.27f);}});
            view.setClipToOutline(true);
            invoke(view,"setMiBackgroundBlurMode",1);
            invoke(view,"setMiBackgroundBlurRadius",pixels);
            invoke(view,"setMiViewBlurMode",1);
            state.blurred=true;state.lastRadius=pixels;state.lastWidth=view.getWidth();state.lastHeight=view.getHeight();
            report("Xiaomi native background-blur calls succeeded. Check the wallpaper through the icons; this build has not been device-verified.");
        }catch(Throwable e){
            try{invoke(view,"setMiBackgroundBlurMode",0);invoke(view,"setMiViewBlurMode",0);}catch(Throwable ignored){}
            view.setOutlineProvider(state.outline);view.setClipToOutline(state.clip);state.blurred=false;state.blurFailed=true;
            report("Native blur unavailable: "+e.getClass().getSimpleName()+". Icons still work. No simulated wallpaper blur is applied.");
        }
    }
    private static void invoke(View view,String method,int value){Object result=XposedHelpers.callMethod(view,method,value);if(Boolean.FALSE.equals(result))throw new IllegalStateException(method+" returned false");}
    private static void clearBlur(ImageView view,State state){
        if(!state.blurred)return;
        try{invoke(view,"setMiBackgroundBlurMode",0);invoke(view,"setMiViewBlurMode",0);}catch(Throwable ignored){}
        view.setOutlineProvider(state.outline);view.setClipToOutline(state.clip);state.blurred=false;
    }
    private static void restore(ImageView view,State state){
        clearBlur(view,state);
        if(state.applied){INTERNAL.set(true);try{view.setImageDrawable(state.original);}finally{INTERNAL.set(false);}state.applied=false;state.packageName=null;}
    }
    private static void report(String message){
        if(context==null||message.equals(lastStatus))return;lastStatus=message;
        IO.execute(()->{try{Bundle data=new Bundle();data.putString("status",message);context.getContentResolver().call(GlassProvider.SETTINGS,"report",null,data);}catch(Throwable e){XposedBridge.log("devarix Glass status: "+e);}});
    }
    private static void injectOption(Object fragment,ClassLoader loader){
        try{
            Object screen=XposedHelpers.callMethod(fragment,"getPreferenceScreen");if(screen==null)return;
            if(XposedHelpers.callMethod(fragment,"findPreference","devarix_glass")!=null)return;
            Context ctx=(Context)XposedHelpers.callMethod(fragment,"getContext");if(ctx==null)return;
            Class<?> prefClass=XposedHelpers.findClass("androidx.preference.Preference",loader);
            Object pref=XposedHelpers.newInstance(prefClass,ctx);
            XposedHelpers.callMethod(pref,"setKey","devarix_glass");
            XposedHelpers.callMethod(pref,"setTitle","Glass icons (devarix)");
            XposedHelpers.callMethod(pref,"setSummary","Optional gray icon pack and frosted backdrop");
            XposedHelpers.callMethod(pref,"setIntent",new Intent().setClassName(GlassConfig.APP,GlassConfig.APP+".SettingsActivity"));
            XposedHelpers.callMethod(screen,"addPreference",pref);
        }catch(Throwable e){XposedBridge.log("devarix Glass setting entry: "+e);}
    }
}
