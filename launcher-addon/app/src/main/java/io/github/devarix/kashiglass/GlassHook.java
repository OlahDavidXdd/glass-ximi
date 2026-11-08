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
    private static volatile boolean debugEnabled=true;
    private static final ExecutorService LOG_IO=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(128),new ThreadPoolExecutor.DiscardPolicy());
    private static final ArrayDeque<String[]> EARLY_LOGS=new ArrayDeque<>();
    private static final Set<String> SAMPLES=new HashSet<>();
    private static void debug(String event,String message){
        if(!debugEnabled)return;
        if(context==null){synchronized(EARLY_LOGS){if(EARLY_LOGS.size()==64)EARLY_LOGS.removeFirst();EARLY_LOGS.addLast(new String[]{event,message});}XposedBridge.log("devarix Glass "+event+": "+message);return;}
        LOG_IO.execute(()->{try{Bundle data=new Bundle();data.putString("event",event);data.putString("message",DiagnosticLog.clean(message,2000));context.getContentResolver().call(GlassProvider.SETTINGS,"log",null,data);}catch(Throwable e){XposedBridge.log("devarix Glass log transport: "+e.getClass().getSimpleName());}});
    }
    private static void error(String event,Throwable error){debug(event,android.util.Log.getStackTraceString(error));XposedBridge.log("devarix Glass "+event+": "+error);}
    private static void sample(String key,String message){if(debugEnabled&&SAMPLES.size()<60&&SAMPLES.add(key))debug("ICON_SAMPLE",message);}

    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private static final Map<View,State> STATES=new WeakHashMap<>();
    private static final Map<String,ArrayList<WeakReference<View>>> PENDING=new HashMap<>();
    private static final Set<String> FAILED=new HashSet<>();
    private static final LruCache<String,Bitmap> ICONS=new LruCache<>(32*1024*1024){@Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getAllocationByteCount();}};
    private static final ThreadLocal<Boolean> INTERNAL=ThreadLocal.withInitial(()->false);
    private static Context context;
    private static WeakReference<Activity> lastActivity=new WeakReference<>(null);
    private static boolean enabled=false,blur=false;
    private static int radius=24;
    private static String style="liquid";
    private static WallpaperBackdrop wallpaper;
    private static Set<String> packages=Collections.emptySet();
    private static String lastStatus="",lastBindings="";
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
        debug("HOOK_START","build="+BuildConfig.VERSION_NAME+" process="+param.processName);
        if(Looper.getMainLooper()==null){debug("HOOK_ABORT","No main looper; hooks skipped");return;}
        MAIN=new Handler(Looper.getMainLooper());
        XposedHelpers.findAndHookMethod(Application.class,"attach",Context.class,new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){if(context==null){context=((Context)p.args[0]).getApplicationContext();if(context==null)context=(Context)p.args[0];try{initialize();}catch(Throwable e){error("INITIALIZE_ERROR",e);}}}
        });
        try{
        XposedHelpers.findAndHookMethod(ImageView.class,"setImageDrawable",Drawable.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                if(!(p.thisObject instanceof ImageView)||!enabled||INTERNAL.get()||!isIcon(p.thisObject)||Looper.myLooper()!=Looper.getMainLooper())return;
                View view=(View)p.thisObject;State state=state(view);state.resolveCached=false;state.original=(Drawable)p.args[0];state.applied=false;
                String pkg=resolve(view);Bitmap bitmap=enabled?ICONS.get(assetKey(pkg)):null;
                if(bitmap!=null){p.args[0]=makeDrawable(view,bitmap);state.applied=true;state.packageName=pkg;}
            }
            @Override protected void afterHookedMethod(MethodHookParam p){if(enabled&&!INTERNAL.get()&&isIcon(p.thisObject))MAIN.post(()->apply((View)p.thisObject));}
        });
        Class<?> shortcutClass=XposedHelpers.findClassIfExists("com.miui.home.launcher.ShortcutIcon",param.classLoader);
        debug("HOOK_TARGET","ShortcutIcon found="+(shortcutClass!=null));
        if(shortcutClass!=null){
            XposedHelpers.findAndHookMethod(shortcutClass,"setIconDrawable",Drawable.class,Bitmap.class,new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    if(!enabled||INTERNAL.get()||Looper.myLooper()!=Looper.getMainLooper())return;
                    View view=(View)p.thisObject;State state=state(view);state.resolveCached=false;
                    state.original=(Drawable)p.args[0];state.applied=false;
                }
                @Override protected void afterHookedMethod(MethodHookParam p){if(enabled&&!INTERNAL.get())MAIN.post(()->apply((View)p.thisObject));}
            });
            for(String method:new String[]{"setTag","bindAppInfo","onLayout"})XposedBridge.hookAllMethods(shortcutClass,method,new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){if(enabled&&!INTERNAL.get())MAIN.post(()->{View v=(View)p.thisObject;state(v).resolveCached=false;apply(v);});}
            });
        }
        Class<?> iconClass=XposedHelpers.findClassIfExists("com.miui.home.icon.LauncherIconImageView",param.classLoader);
        debug("HOOK_TARGET","LauncherIconImageView found="+(iconClass!=null));
        if(iconClass!=null)XposedBridge.hookAllConstructors(iconClass,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){
            View view=(View)p.thisObject;if(XposedHelpers.getAdditionalInstanceField(view,"devarix_layout")!=null)return;
            XposedHelpers.setAdditionalInstanceField(view,"devarix_layout",true);
            view.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(enabled)apply(v);});
        }});
        XposedHelpers.findAndHookMethod(android.app.WallpaperManager.class,"setWallpaperOffsets",IBinder.class,float.class,float.class,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(wallpaper!=null)wallpaper.offsets(((Number)p.args[1]).floatValue(),((Number)p.args[2]).floatValue());}});
        XposedHelpers.findAndHookMethod(Activity.class,"onResume",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){Activity activity=(Activity)p.thisObject;lastActivity=new WeakReference<>(activity);MAIN.post(()->{if(context!=null)reload();if(enabled)scan(activity.getWindow().getDecorView());});}});
        Class<?> fragment=XposedHelpers.findClassIfExists("com.miui.home.settings.IconCustomizeFragment",param.classLoader);
        if(fragment!=null)for(String method:new String[]{"onCreate","onCreatePreferences","onResume"})XposedBridge.hookAllMethods(fragment,method,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){injectOption(p.thisObject,param.classLoader);}});
        debug("HOOK_READY","Icon, activity and wallpaper hooks installed");
        }catch(Throwable e){error("HOOK_INSTALL_ERROR",e);}
    }
    private static Drawable getIconDrawable(View view){return view instanceof ImageView?((ImageView)view).getDrawable():(Drawable)call(view,"getIconDrawable");}
    private static void setIconDrawable(View view,Drawable drawable){
        if(view instanceof ImageView)((ImageView)view).setImageDrawable(drawable);
        else {
            // ShortcutIcon's public setter rounds/rasterizes incoming artwork. Keep the live
            // Drawable in its actual drawing slot and let the launcher set compound bounds.
            XposedHelpers.setObjectField(view,"mIconDrawable",drawable);
            if(drawable!=null)XposedHelpers.callMethod(view,"applyCompoundDrawables",drawable);
            else ((android.widget.TextView)view).setCompoundDrawables(null,null,null,null);
            view.invalidate();
        }
    }
    private static void scan(View root){walk(root);diagnose();}
    private static void diagnose(){
        int seen=0,mapped=0,applied=0;
        for(View view:new ArrayList<>(STATES.keySet()))if(view!=null&&view.isAttachedToWindow()){
            seen++;State state=state(view);if(state.resolvedPackage!=null)mapped++;if(state.applied)applied++;
        }
        String message="Icon bindings: "+seen+" visible, "+mapped+" mapped, "+applied+" replaced.";
        if(enabled&&seen==0)message+=" No supported icon views found. This launcher version may need another hook.";
        else if(enabled&&mapped==0)message+=" No app mappings resolved. Open the app drawer and check again.";
        if(message.equals(lastBindings))return;lastBindings=message;
        final String value=message;
        IO.execute(()->{try{Bundle data=new Bundle();data.putString("bindings",value);context.getContentResolver().call(GlassProvider.SETTINGS,"diagnostics",null,data);}catch(Throwable e){error("DIAGNOSTICS_ERROR",e);}});
    }
    private static void walk(View view){
        if(!enabled)return;
        if(isIcon(view))apply(view);
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)walk(group.getChildAt(i));}
    }
    private static boolean isIcon(Object object){
        if(!(object instanceof View))return false;
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())if(type.getName().equals("com.miui.home.icon.LauncherIconImageView")||type.getName().equals("com.miui.home.launcher.ShortcutIcon"))return true;
        return false;
    }
    private static State state(View view){State s=STATES.get(view);if(s==null){s=new State();s.original=getIconDrawable(view);STATES.put(view,s);}return s;}
    private static String assetKey(String pkg){return style+":"+(pkg==null?"":pkg);}
    private static Drawable makeDrawable(View view,Bitmap bitmap){return "liquid".equals(style)?new LiquidGlassDrawable(view,bitmap,wallpaper,blur,radius,GlassHook::report):new BitmapDrawable(view.getResources(),bitmap);}
    private static void refreshLiquid(){if(!enabled)return;for(View view:new ArrayList<>(STATES.keySet()))if(view!=null){apply(view);view.invalidate();}}
    private static void initialize(){
        synchronized(EARLY_LOGS){while(!EARLY_LOGS.isEmpty()){String[] entry=EARLY_LOGS.removeFirst();debug(entry[0],entry[1]);}}
        debug("ATTACH","Launcher context attached; SDK="+Build.VERSION.SDK_INT);

        wallpaper=new WallpaperBackdrop(context,()->MAIN.post(GlassHook::refreshLiquid),GlassHook::report);
        try{context.getContentResolver().registerContentObserver(GlassProvider.SETTINGS,false,new ContentObserver(MAIN){@Override public void onChange(boolean selfChange){reload();}});}catch(Throwable e){error("OBSERVER_ERROR",e);}
        reload();
    }
    private static void reload(){
        IO.execute(()->{
            try{
                Bundle config=context.getContentResolver().call(GlassProvider.SETTINGS,"settings",null,null);
                if(config==null)throw new IllegalStateException("Provider unavailable");
                debugEnabled=config.getBoolean("debug_logs",true);
                debug("CONFIG","Settings received; style="+config.getString("style")+" icons="+config.getBoolean("icons")+" blur="+config.getBoolean("blur")+" mappings="+(config.getStringArrayList("packages")==null?0:config.getStringArrayList("packages").size()));
                boolean nextEnabled=config.getBoolean("icons"),nextBlur=config.getBoolean("blur");int nextRadius=GlassConfig.radius(config.getInt("radius",24));
                String nextStyle=GlassConfig.style(config.getString("style"));boolean root=config.getBoolean("root_wallpaper");String shader=config.getString("shader");
                Bitmap plate;try(var stream=context.getContentResolver().openInputStream(Uri.parse("content://"+GlassConfig.AUTHORITY+"/plate"))){plate=BitmapFactory.decodeStream(stream);}
                ArrayList<String> names=config.getStringArrayList("packages");Set<String> nextPackages=names==null?Collections.emptySet():new HashSet<>(names);
                MAIN.post(()->{
                    boolean replace=!style.equals(nextStyle)||blur!=nextBlur||radius!=nextRadius||!nextEnabled;
                    if(replace)for(View view:new ArrayList<>(STATES.keySet()))if(view!=null)restore(view,state(view));
                    enabled=nextEnabled;blur=nextBlur;radius=nextRadius;style=nextStyle;packages=nextPackages;FAILED.clear();
                    LiquidGlassDrawable.configure(shader,plate);wallpaper.configure(enabled&&"liquid".equals(style),root);
                    report(enabled?"Selected "+style+" material. Waiting for mapped icons to bind.":"Launcher connected. Glass icons are disabled.");
                    for(View v:new ArrayList<>(STATES.keySet()))if(v!=null){State state=state(v);state.resolveCached=false;state.blurFailed=false;apply(v);}
                    Activity activity=lastActivity.get();if(enabled&&activity!=null)scan(activity.getWindow().getDecorView());
                });
            }catch(Throwable e){MAIN.post(()->{enabled=false;blur=false;for(View v:new ArrayList<>(STATES.keySet()))if(v!=null)apply(v);});error("CONFIG_ERROR",e);}
        });
    }
    private static String resolve(View view){
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
        if(info instanceof Intent){ComponentName component=((Intent)info).getComponent();return component==null?GlassConfig.mapping(((Intent)info).getPackage(),null,packages):GlassConfig.mapping(component.getPackageName(),component.getClassName(),packages);}
        if(info instanceof ComponentName){ComponentName component=(ComponentName)info;return GlassConfig.mapping(component.getPackageName(),component.getClassName(),packages);}
        for(String method:new String[]{"getIntent","getComponentName","getComponent"}){Object result=call(info,method);if(result instanceof Intent||result instanceof ComponentName){String pkg=fromInfo(result);if(pkg!=null)return pkg;}}
        for(String name:new String[]{"mIntent","intent","mComponentName","componentName"}){Object result=field(info,name);if(result instanceof Intent||result instanceof ComponentName){String pkg=fromInfo(result);if(pkg!=null)return pkg;}}
        for(String method:new String[]{"getTargetPackage","getPackageName"}){Object result=call(info,method);if(result instanceof String&&packages.contains(result))return (String)result;}
        for(String name:new String[]{"mPackageName","packageName"}){Object result=field(info,name);if(result instanceof String&&packages.contains(result))return (String)result;}
        return null;
    }
    private static Object call(Object object,String method){try{return XposedHelpers.callMethod(object,method);}catch(Throwable ignored){return null;}}
    private static Object field(Object object,String name){try{return XposedHelpers.getObjectField(object,name);}catch(Throwable ignored){return null;}}
    private static void apply(View view){
        try{applyInternal(view);}catch(Throwable e){error("ICON_APPLY_ERROR",e);}
    }
    private static void applyInternal(View view){
        if(Looper.myLooper()!=Looper.getMainLooper()){MAIN.post(()->apply(view));return;}
        if(context==null||INTERNAL.get())return;
        State state=state(view);String pkg=resolve(view);
        Object tag=view.getTag();
        sample(view.getClass().getName()+":"+pkg+":"+(tag==null?"null":tag.getClass().getName()),"view="+view.getClass().getName()+" mapping="+pkg+" tagType="+(tag==null?"null":tag.getClass().getName()));
        if(!enabled||pkg==null){restore(view,state);return;}
        Bitmap bitmap=ICONS.get(assetKey(pkg));
        if(bitmap==null){if(state.applied&&!pkg.equals(state.packageName))restore(view,state);load(pkg,view);return;}
        if(!state.applied||!pkg.equals(state.packageName)){
            if(!state.applied)state.original=getIconDrawable(view);
            INTERNAL.set(true);try{setIconDrawable(view,makeDrawable(view,bitmap));}finally{INTERNAL.set(false);}
            state.applied=true;state.packageName=pkg;
            sample("applied:"+pkg,"Applied "+style+" mapping="+pkg+" view="+view.getClass().getName());
        }
        if(!state.listener){state.listener=true;view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            public void onViewAttachedToWindow(View v){MAIN.post(()->apply(v));}
            public void onViewDetachedFromWindow(View v){State s=STATES.get(v);if(s!=null)clearBlur(v,s);}
        });}
        if(blur&&view instanceof ImageView&&view.isAttachedToWindow()&&(!"liquid".equals(style)||!wallpaper.isAvailable()))nativeBlur(view,state);else clearBlur(view,state);
    }
    private static void load(String pkg,View view){
        String requestedStyle=style,key=assetKey(pkg);
        if(FAILED.contains(key))return;
        ArrayList<WeakReference<View>> waiting=PENDING.get(key);
        if(waiting!=null){boolean found=false;for(var ref:waiting)if(ref.get()==view){found=true;break;}if(!found)waiting.add(new WeakReference<>(view));return;}
        waiting=new ArrayList<>();waiting.add(new WeakReference<>(view));PENDING.put(key,waiting);
        IO.execute(()->{
            Bitmap image=null;
            try(var descriptor=context.getContentResolver().openAssetFileDescriptor(Uri.parse("content://"+GlassConfig.AUTHORITY+"/icons/"+requestedStyle+"/"+pkg),"r")){
                if(descriptor==null)throw new IllegalStateException("Missing asset");
                BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
                try(var stream=descriptor.createInputStream()){image=BitmapFactory.decodeStream(stream,null,options);}
            }catch(Throwable e){error("ASSET_ERROR "+pkg,e);}
            if(image!=null)debug("ASSET_OK",requestedStyle+" "+pkg+" "+image.getWidth()+"x"+image.getHeight());
            else debug("ASSET_FAILED",requestedStyle+" "+pkg);
            Bitmap result=image;
            MAIN.post(()->{ArrayList<WeakReference<View>> views=PENDING.remove(key);if(result!=null)ICONS.put(key,result);else FAILED.add(key);if(views!=null)for(var ref:views){View v=ref.get();if(v!=null)apply(v);}diagnose();});
        });
    }
    private static void nativeBlur(View view,State state){
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
            report("liquid".equals(style)?"Liquid Glass is using its static fallback while wallpaper capture is unavailable: "+wallpaper.failure():"Xiaomi native blur calls succeeded for Frosted Glass; physical-device rendering still needs verification.");
        }catch(Throwable e){
            error("NATIVE_BLUR_ERROR",e);
            try{invoke(view,"setMiBackgroundBlurMode",0);invoke(view,"setMiViewBlurMode",0);}catch(Throwable ignored){}
            view.setOutlineProvider(state.outline);view.setClipToOutline(state.clip);state.blurred=false;state.blurFailed=true;
            report("Native blur unavailable: "+e.getClass().getSimpleName()+". Icons still work. No simulated wallpaper blur is applied.");
        }
    }
    private static void invoke(View view,String method,int value){Object result=XposedHelpers.callMethod(view,method,value);if(Boolean.FALSE.equals(result))throw new IllegalStateException(method+" returned false");}
    private static void clearBlur(View view,State state){
        if(!state.blurred)return;
        try{invoke(view,"setMiBackgroundBlurMode",0);invoke(view,"setMiViewBlurMode",0);}catch(Throwable ignored){}
        view.setOutlineProvider(state.outline);view.setClipToOutline(state.clip);state.blurred=false;
    }
    private static void restore(View view,State state){
        clearBlur(view,state);
        if(state.applied){INTERNAL.set(true);try{setIconDrawable(view,state.original);}finally{INTERNAL.set(false);}state.applied=false;state.packageName=null;}
    }
    private static void report(String message){
        if(context==null||message.equals(lastStatus))return;lastStatus=message;
        IO.execute(()->{try{Bundle data=new Bundle();data.putString("status",message);context.getContentResolver().call(GlassProvider.SETTINGS,"report",null,data);}catch(Throwable e){error("STATUS_ERROR",e);}});
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
            XposedHelpers.callMethod(pref,"setSummary","Liquid Glass (QWEA0) or Frosted Glass");
            XposedHelpers.callMethod(pref,"setIntent",new Intent().setClassName(GlassConfig.APP,GlassConfig.APP+".SettingsActivity"));
            XposedHelpers.callMethod(screen,"addPreference",pref);
        }catch(Throwable e){error("SETTINGS_ENTRY_ERROR",e);}
    }
}
