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
    private static final ExecutorService CONTROL_IO=Executors.newSingleThreadExecutor();
    private static final ExecutorService RESTART_IO=Executors.newSingleThreadExecutor();
    private static final Map<View,State> STATES=new WeakHashMap<>();
    private static final Map<String,ArrayList<WeakReference<View>>> PENDING=new HashMap<>();
    private static final Set<String> WARMING=new HashSet<>();
    private static long snapshotWallpaperVersion=-1,nextWarmAt;
    private static final Set<String> FAILED=new HashSet<>();
    private static final LruCache<String,Bitmap> ICONS=new LruCache<>(32*1024*1024){@Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getAllocationByteCount();}};
    private static final LruCache<String,Bitmap> TRANSITIONS=new LruCache<>(8*1024*1024){@Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getAllocationByteCount();}};
    private static final ThreadLocal<Boolean> INTERNAL=ThreadLocal.withInitial(()->false);
    private static Context context;
    private static WeakReference<Activity> lastActivity=new WeakReference<>(null);
    private static volatile boolean enabled=false,blur=false,automatic=true,whiteSymbols=true,screenMatch=true;
    private static int radius=24;
    private static volatile String style="liquid";
    private static WallpaperBackdrop wallpaper;
    private static LauncherSurfaces surfaces;
    private static LauncherWidgets widgets;
    private static volatile boolean selfRestartReady;
    private static boolean settingsLoaded;
    private static volatile String renderConfiguration;
    private static final Runnable RESUME_WORK=()->{Activity activity=lastActivity.get();if(activity!=null&&!activity.isFinishing()&&!activity.isDestroyed()&&activity.hasWindowFocus()&&enabled)scan(activity.getWindow().getDecorView());};
    private static volatile Set<String> packages=Collections.emptySet();
    private static String lastStatus="",lastBindings="";
    private static final class Artwork {final String key;final Bitmap glyph;Artwork(String key,Bitmap glyph){this.key=key;this.glyph=glyph;}}
    private static final class State {
        Drawable original;
        final SourceVisibility visibility=new SourceVisibility();
        final LiveIconBinding binding=new LiveIconBinding();
        boolean applied,blurred,listener;
        ViewOutlineProvider outline;
        boolean clip;
        String packageName, resolvedPackage, originalKey;
        final WeakIdentity lastTag=new WeakIdentity(),lastParentTag=new WeakIdentity();
        boolean resolveCached,blurFailed;
        int lastRadius,lastWidth,lastHeight;
    }
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) throws Throwable {
        if(!GlassConfig.LAUNCHER.equals(param.packageName)||!GlassConfig.LAUNCHER.equals(param.processName))return;
        debug("HOOK_START","build="+BuildConfig.VERSION_NAME+" process="+param.processName);
        if(Looper.getMainLooper()==null){debug("HOOK_ABORT","No main looper; hooks skipped");return;}
        MAIN=new Handler(Looper.getMainLooper());
        surfaces=new LauncherSurfaces(GlassHook::surfaceReport,message->debug("SURFACE",message));
        surfaces.install(param.classLoader);
        widgets=new LauncherWidgets(GlassHook::widgetReport,message->debug("WIDGET",message));
        try{widgets.install(param.classLoader);}catch(Throwable e){error("WIDGET_HOOK_ERROR",e);}
        new LauncherAnimations(()->enabled&&!INTERNAL.get(),GlassHook::animationSnapshot,message->debug("ANIMATION",message)).install(param.classLoader);
        XposedHelpers.findAndHookMethod(Application.class,"attach",Context.class,new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){if(context==null){context=((Context)p.args[0]).getApplicationContext();if(context==null)context=(Context)p.args[0];try{initialize();}catch(Throwable e){error("INITIALIZE_ERROR",e);}}}
        });
        try{
        XposedHelpers.findAndHookMethod(ImageView.class,"setImageDrawable",Drawable.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                if(!(p.thisObject instanceof ImageView)||!enabled||INTERNAL.get()||!isIcon(p.thisObject)||Looper.myLooper()!=Looper.getMainLooper())return;
                View view=(View)p.thisObject;State state=state(view);
                Drawable incoming=(Drawable)p.args[0];if(incoming==null||incoming.getOpacity()==android.graphics.PixelFormat.TRANSPARENT){state.visibility.hidden=true;state.applied=false;return;}
                state.visibility.hidden=false;state.visibility.alpha=incoming.getAlpha();state.resolveCached=false;rememberCurrent(view,state);captureOriginal(view,state,(Drawable)p.args[0]);state.applied=false;
                String pkg=resolve(view);Bitmap bitmap=enabled?ICONS.get(assetKey(pkg)):null;
                if(bitmap!=null&&!bitmap.isRecycled()){p.args[0]=makeDrawable(view,bitmap);state.binding.installed(assetKey(pkg),bitmap,(Drawable)p.args[0]);state.applied=true;state.packageName=pkg;}
            }
            @Override protected void afterHookedMethod(MethodHookParam p){if(enabled&&!INTERNAL.get()&&isIcon(p.thisObject))apply((View)p.thisObject);}
        });
        Class<?> shortcutClass=XposedHelpers.findClassIfExists("com.miui.home.launcher.ShortcutIcon",param.classLoader);
        debug("HOOK_TARGET","ShortcutIcon found="+(shortcutClass!=null));
        if(shortcutClass!=null){
            XposedHelpers.findAndHookMethod(shortcutClass,"setIconDrawable",Drawable.class,Bitmap.class,new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    if(!enabled||INTERNAL.get()||Looper.myLooper()!=Looper.getMainLooper())return;
                    View view=(View)p.thisObject;State state=state(view);state.resolveCached=false;rememberCurrent(view,state);
                    captureOriginal(view,state,(Drawable)p.args[0]);state.applied=false;
                }
                @Override protected void afterHookedMethod(MethodHookParam p){if(enabled&&!INTERNAL.get())apply((View)p.thisObject);}
            });
            XposedBridge.hookAllMethods(shortcutClass,"setOriginalDrawableAndRoundedDrawable",new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){if(enabled&&!INTERNAL.get())apply((View)p.thisObject);}
            });
            XposedBridge.hookAllMethods(shortcutClass,"setIconVisibility",new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){if(enabled&&!INTERNAL.get()){SourceVisibility visibility=sourceVisibility((View)p.thisObject);if(visibility!=null)visibility.visibility(((Number)p.args[0]).intValue());}}
            });
            XposedBridge.hookAllMethods(shortcutClass,"setIconAlpha",new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){if(enabled&&!INTERNAL.get()){
                        SourceVisibility visibility=sourceVisibility((View)p.thisObject);float alpha=SourceVisibility.normalized(((Number)p.args[0]).floatValue());
                        if(!Float.isFinite(alpha)){p.setResult(null);return;}
                        if(visibility!=null)visibility.opacity(alpha);p.args[0]=alpha;
                    }}
            });
            XposedBridge.hookAllMethods(shortcutClass,"applyCompoundDrawables",new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    if(!enabled||INTERNAL.get()||Looper.myLooper()!=Looper.getMainLooper())return;
                    View view=(View)p.thisObject;State state=state(view);Object transparent=field(view,"mTransparentDrawable");
                    if(p.args[0]==null)return;
                    if(transparent instanceof Drawable&&p.args[0]==transparent){state.visibility.transparentPlaceholder();return;}
                    if(p.args[0] instanceof ColorDrawable&&((Drawable)p.args[0]).getOpacity()==PixelFormat.TRANSPARENT)return;
                    apply(view);
                    if(state.applied)p.args[0]=state.visibility.hidden&&transparent instanceof Drawable?transparent:getIconDrawable(view);
                }
            });
            for(String method:new String[]{"getContentDrawable","getBackAnimPreviewDrawable","getBoomAnimBitmap"}){
                final String name=method;
                XposedBridge.hookAllMethods(shortcutClass,method,new XC_MethodHook(){
                    @Override protected void afterHookedMethod(MethodHookParam p){
                        if(!enabled||INTERNAL.get()||Looper.myLooper()!=Looper.getMainLooper())return;
                        View view=(View)p.thisObject;Bitmap bitmap=animationSnapshot(view);
                        if(bitmap!=null){p.setResult("getBoomAnimBitmap".equals(name)?OwnedTransitionDrawable.copy(bitmap):new OwnedTransitionDrawable(view.getResources(),bitmap));sample("transition:"+name,"Styled transition supplied via "+name);}
                    }
                });
            }
            XposedBridge.hookAllMethods(shortcutClass,"onTouchEvent",new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    if(enabled&&p.args.length>0&&p.args[0] instanceof MotionEvent&&((MotionEvent)p.args[0]).getActionMasked()==MotionEvent.ACTION_DOWN){View view=(View)p.thisObject;apply(view);animationSnapshot(view);}
                }
            });
            XposedBridge.hookAllMethods(shortcutClass,"setTag",new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){if(enabled&&!INTERNAL.get()&&Looper.myLooper()==Looper.getMainLooper())state((View)p.thisObject);}
            });
            for(String method:new String[]{"setTag","bindAppInfo","onLayout"})XposedBridge.hookAllMethods(shortcutClass,method,new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){if(enabled&&!INTERNAL.get()){View v=(View)p.thisObject;state(v).resolveCached=false;apply(v);}}
            });
        }
        Class<?> infoClass=XposedHelpers.findClassIfExists("com.miui.home.launcher.ShortcutInfo",param.classLoader);
        if(infoClass!=null)XposedBridge.hookAllMethods(infoClass,"getIconDrawable",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){
                if(!enabled||INTERNAL.get())return;
                String key=fromInfo(p.thisObject);Bitmap bitmap=snapshotForKey(key);
                if(bitmap!=null)p.setResult(new OwnedTransitionDrawable(context.getResources(),bitmap));
            }
        });
        Class<?> iconClass=XposedHelpers.findClassIfExists("com.miui.home.icon.LauncherIconImageView",param.classLoader);
        debug("HOOK_TARGET","LauncherIconImageView found="+(iconClass!=null));
        if(iconClass!=null)XposedBridge.hookAllConstructors(iconClass,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){
            View view=(View)p.thisObject;if(XposedHelpers.getAdditionalInstanceField(view,"devarix_layout")!=null)return;
            XposedHelpers.setAdditionalInstanceField(view,"devarix_layout",true);
            view.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(enabled)apply(v);});
        }});
        XposedHelpers.findAndHookMethod(android.app.WallpaperManager.class,"setWallpaperOffsets",IBinder.class,float.class,float.class,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(wallpaper!=null)wallpaper.offsets(((Number)p.args[1]).floatValue(),((Number)p.args[2]).floatValue());}});
        XposedHelpers.findAndHookMethod(Activity.class,"onResume",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){Activity activity=(Activity)p.thisObject;lastActivity=new WeakReference<>(activity);MAIN.post(()->{if(context!=null&&!settingsLoaded)reload();MAIN.removeCallbacks(RESUME_WORK);MAIN.postDelayed(RESUME_WORK,400);});}});
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
            if(drawable!=null){
                Drawable current=((android.widget.TextView)view).getCompoundDrawables()[1];Object mask=field(view,"mTransparentDrawable");
                Drawable visible=LiveIconBinding.drawingSlot(drawable,current,mask instanceof Drawable?(Drawable)mask:null,state(view).visibility.hidden);
                if(visible!=null)XposedHelpers.callMethod(view,"applyCompoundDrawables",visible);
            }
            else ((android.widget.TextView)view).setCompoundDrawables(null,null,null,null);
            view.invalidate();
        }
    }
    private static void scan(View root){walk(root);diagnose();}
    private static void diagnose(){
        int seen=0,mapped=0,applied=0,generated=0;
        for(View view:new ArrayList<>(STATES.keySet()))if(view!=null&&view.isAttachedToWindow()){
            seen++;State state=state(view);if(state.resolvedPackage!=null)mapped++;if(state.applied){applied++;if(GlassConfig.automaticKey(state.packageName))generated++;}
        }
        String message="Icon bindings: "+seen+" visible, "+mapped+" mapped, "+applied+" replaced ("+generated+" automatic).";
        if(enabled&&seen==0)message+=" No supported icon views found. This launcher version may need another hook.";
        else if(enabled&&mapped==0)message+=" No app mappings resolved. Open the app drawer and check again.";
        if(message.equals(lastBindings))return;lastBindings=message;
        final String value=message;
        CONTROL_IO.execute(()->{try{Bundle data=new Bundle();data.putString("bindings",value);context.getContentResolver().call(GlassProvider.SETTINGS,"diagnostics",null,data);}catch(Throwable e){error("DIAGNOSTICS_ERROR",e);}});
    }
    private static void walk(View view){
        if(!enabled)return;
        if(surfaces!=null)surfaces.apply(view);
        if(widgets!=null)widgets.apply(view);
        if(isIcon(view))apply(view);
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)walk(group.getChildAt(i));}
    }
    private static boolean isIcon(Object object){
        if(!(object instanceof View))return false;
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())if(type.getName().equals("com.miui.home.icon.LauncherIconImageView")||type.getName().equals("com.miui.home.launcher.ShortcutIcon"))return true;
        return false;
    }
    private static State state(View view){State s=STATES.get(view);if(s==null){s=new State();s.original=getIconDrawable(view);if(s.original!=null)s.visibility.alpha=s.original.getAlpha();
            Object mask=field(view,"mTransparentDrawable");if(view instanceof android.widget.TextView&&mask instanceof Drawable&&((android.widget.TextView)view).getCompoundDrawables()[1]==mask)s.visibility.hidden=true;
            STATES.put(view,s);XposedHelpers.setAdditionalInstanceField(view,"devarix_visibility",s.visibility);s.originalKey=resolve(view);s.visibility.bind(s.originalKey);}return s;}
    private static SourceVisibility sourceVisibility(View view){
        Object existing=XposedHelpers.getAdditionalInstanceField(view,"devarix_visibility");
        if(existing instanceof SourceVisibility)return (SourceVisibility)existing;
        return Looper.myLooper()==Looper.getMainLooper()?state(view).visibility:null;
    }
    private static void captureOriginal(View view,State state,Drawable incoming){
        if(state.binding.owns(incoming))return;
        if(incoming instanceof OwnedTransitionDrawable){Object raw=field(view.getTag(),"mIconDrawable");if(raw instanceof Drawable&&!(raw instanceof OwnedTransitionDrawable)&&!(raw instanceof LiquidGlassDrawable))state.original=(Drawable)raw;}
        else if(!(incoming instanceof LiquidGlassDrawable))state.original=incoming;
        state.originalKey=resolve(view);
    }
    private static void warmTransition(View view,String pkg){
        String key=assetKey(pkg);if(TRANSITIONS.get(key)!=null||WARMING.size()>=64||!WARMING.add(key))return;
        WeakReference<View> reference=new WeakReference<>(view);
        nextWarmAt=Math.max(SystemClock.uptimeMillis()+32,nextWarmAt+24);
        MAIN.postAtTime(()->{WARMING.remove(key);View current=reference.get();if(enabled&&current!=null&&current.isAttachedToWindow()&&pkg.equals(resolve(current)))transition(current);},nextWarmAt);
    }
    private static Bitmap animationSnapshot(Object target){
        return AnimationTargetResolver.find(target,GlassHook::snapshotForTarget,node->{
            ArrayList<Object> links=new ArrayList<>();
            for(String method:new String[]{"getAnimTargetContainerView","getTargetRootView","getMBuddyInfo","getShortcutInfo","getItemInfo"})links.add(call(node,method));
            for(String name:new String[]{"mAnimTargetRef","mLaunchAppAndBackHomeAnimTargetRef","mInfo","mItemInfo","mShortcutInfo"}){
                Object value=field(node,name);links.add(value instanceof java.lang.ref.Reference?((java.lang.ref.Reference<?>)value).get():value);
            }
            return links;
        });
    }
    private static Bitmap snapshotForTarget(Object target){
        if(target instanceof View){
            if(!isIcon(target))return null;View view=(View)target;
            Object value=XposedHelpers.getAdditionalInstanceField(view,"devarix_artwork");
            if(Looper.myLooper()==Looper.getMainLooper()){
                apply(view);value=XposedHelpers.getAdditionalInstanceField(view,"devarix_artwork");
            }
            if(!(value instanceof Artwork))return null;Artwork artwork=(Artwork)value;
            Bitmap ready=snapshotForKey(artwork.key);return ready!=null?ready:snapshotForGlyph(artwork.key,artwork.glyph);
        }
        boolean appInfo=false;for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())if(type.getName().equals("com.miui.home.launcher.ShortcutInfo")||type.getName().equals("com.miui.home.launcher.AppInfo")){appInfo=true;break;}
        return appInfo?snapshotForKey(fromInfo(target)):null;
    }
    private static Bitmap snapshotForKey(String pkg){
        if(pkg==null)return null;String key=assetKey(pkg);Bitmap cached=TRANSITIONS.get(key);
        if(cached!=null&&!cached.isRecycled())return cached;
        Bitmap glyph=ICONS.get(key);if(glyph==null||glyph.isRecycled())return null;
        return snapshotForGlyph(pkg,glyph);
    }
    private static Bitmap snapshotForGlyph(String pkg,Bitmap glyph){
        if(glyph==null||glyph.isRecycled())return null;
        String key=assetKey(pkg);
        // No wallpaper/GPU readback or provider I/O on a transition's critical path.
        Bitmap ready="frosted".equals(style)&&!GlassConfig.automaticKey(pkg)?glyph:LiquidGlassDrawable.coldTransition(glyph);
        TRANSITIONS.put(key,ready);debug(screenMatch?"ANIMATION_CACHE_REBUILT":"ANIMATION_STATIC_FALLBACK",key);return ready;
    }
    private static void rememberCurrent(View view,State state){
        String key=resolve(view);Bitmap trusted=state.binding.trusted(assetKey(key));
        if(key!=null&&trusted!=null)ICONS.put(assetKey(key),trusted);
    }
    private static String assetKey(String pkg){return style+":"+(pkg==null?"":pkg);}
    private static Drawable makeDrawable(View view,Bitmap bitmap){Drawable result="liquid".equals(style)||GlassConfig.automaticKey(resolve(view))?new LiquidGlassDrawable(view,bitmap,wallpaper,blur,radius,GlassHook::report):new BitmapDrawable(view.getResources(),OwnedTransitionDrawable.copy(bitmap));result.setAlpha(state(view).visibility.alpha);return result;}
    private static void refreshLiquid(){if(wallpaper!=null&&snapshotWallpaperVersion!=wallpaper.version()){snapshotWallpaperVersion=wallpaper.version();TRANSITIONS.evictAll();debug("TRANSITION_CACHE","Wallpaper content changed; snapshots cleared");}if(surfaces!=null)surfaces.invalidate();if(!enabled)return;for(View view:new ArrayList<>(STATES.keySet()))if(view!=null){apply(view);view.invalidate();}}
    private static void initialize(){
        synchronized(EARLY_LOGS){while(!EARLY_LOGS.isEmpty()){String[] entry=EARLY_LOGS.removeFirst();debug(entry[0],entry[1]);}}
        debug("ATTACH","Launcher context attached; SDK="+Build.VERSION.SDK_INT);

        NativeScreenBackdrop.setAccess(new NativeScreenBackdrop.Access(){
            public Object call(Object target,String name,Object... args){return XposedHelpers.callMethod(target,name,args);}
            public Object field(Object target,String name){return XposedHelpers.getObjectField(target,name);}
        });
        wallpaper=new WallpaperBackdrop(context,()->MAIN.post(GlassHook::refreshLiquid),GlassHook::report);
        try{context.getContentResolver().registerContentObserver(GlassProvider.SETTINGS,false,new ContentObserver(MAIN){@Override public void onChange(boolean selfChange){reload();}});}catch(Throwable e){error("OBSERVER_ERROR",e);}
        try{context.getContentResolver().registerContentObserver(GlassProvider.RESTART,false,new ContentObserver(MAIN){@Override public void onChange(boolean selfChange){selfRestart();}});selfRestartReady=true;}catch(Throwable e){error("RESTART_OBSERVER_ERROR",e);}
        reload();
    }
    private static void selfRestart(){
        RESTART_IO.execute(()->{
            try{Bundle request=new Bundle();request.putInt("pid",android.os.Process.myPid());
                Bundle response=context.getContentResolver().call(GlassProvider.RESTART,"consume_restart",null,request);
                if(response!=null&&response.getString("token")!=null){debug("SELF_RESTART","Acknowledged; exiting own launcher process");MAIN.postDelayed(()->android.os.Process.killProcess(android.os.Process.myPid()),150);}
            }catch(Throwable e){error("SELF_RESTART_ERROR",e);}
        });
    }
    private static void reload(){
        CONTROL_IO.execute(()->{
            try{
                Bundle identity=new Bundle();identity.putInt("hook_code",BuildConfig.VERSION_CODE);identity.putString("hook_version",BuildConfig.VERSION_NAME);identity.putInt("hook_pid",android.os.Process.myPid());identity.putString("hook_capabilities",selfRestartReady?HookVersion.RESTART_CAPABILITY:"");
                Bundle config=context.getContentResolver().call(GlassProvider.SETTINGS,"settings",null,identity);
                if(config==null)throw new IllegalStateException("Provider unavailable");
                debugEnabled=config.getBoolean("debug_logs",true);
                debug("CONFIG","Settings received; style="+config.getString("style")+" icons="+config.getBoolean("icons")+" blur="+config.getBoolean("blur")+" screen_match="+config.getBoolean("screen_match",true)+" search="+config.getBoolean("search_glass",true)+" loaded="+BuildConfig.VERSION_NAME+" mappings="+(config.getStringArrayList("packages")==null?0:config.getStringArrayList("packages").size()));
                boolean nextEnabled=config.getBoolean("icons"),nextBlur=config.getBoolean("blur"),nextAutomatic=config.getBoolean("automatic",true);int nextRadius=GlassConfig.radius(config.getInt("radius",24));
                boolean nextWhite=config.getBoolean("white_symbols",true),nextScreen=config.getBoolean("screen_match",true);
                boolean nextDock=config.getBoolean("dock_glass",true),nextSearch=config.getBoolean("search_glass",true),nextDrawer=config.getBoolean("drawer_glass",true),nextWidgets=config.getBoolean("widget_glass",true);
                String nextStyle=GlassConfig.style(config.getString("style"));boolean root=config.getBoolean("root_wallpaper");String shader=config.getString("shader");
                // Identity/log checks may repeat; unchanged settings must not rebuild every surface mid-animation.
                String configuration=nextStyle+":"+nextEnabled+":"+nextBlur+":"+nextAutomatic+":"+nextRadius+":"+nextWhite+":"+nextScreen+":"+nextDock+":"+nextSearch+":"+nextDrawer+":"+nextWidgets+":"+root;
                Bitmap plate;try(var stream=context.getContentResolver().openInputStream(Uri.parse("content://"+GlassConfig.AUTHORITY+"/plate"))){plate=BitmapFactory.decodeStream(stream);}
                ArrayList<String> names=config.getStringArrayList("packages");Set<String> nextPackages=names==null?Collections.emptySet():new HashSet<>(names);
                MAIN.post(()->{
                    if(configuration.equals(renderConfiguration)){settingsLoaded=true;return;}
                    boolean enabling=nextEnabled&&!enabled;
                    boolean replace=!style.equals(nextStyle)||blur!=nextBlur||radius!=nextRadius||automatic!=nextAutomatic||whiteSymbols!=nextWhite||screenMatch!=nextScreen||!nextEnabled;
                    if(replace)TRANSITIONS.evictAll();
                    if(replace)for(View view:new ArrayList<>(STATES.keySet()))if(view!=null)restore(view,state(view));
                    if(enabling)for(View view:new ArrayList<>(STATES.keySet()))if(view!=null){
                        State state=state(view);Drawable nativeDrawable=getIconDrawable(view);
                        Object mask=field(view,"mTransparentDrawable");
                        boolean hidden=view instanceof android.widget.TextView?mask instanceof Drawable&&((android.widget.TextView)view).getCompoundDrawables()[1]==mask:nativeDrawable==null||nativeDrawable.getOpacity()==PixelFormat.TRANSPARENT;
                        state.visibility.observe(hidden,nativeDrawable==null?255:nativeDrawable.getAlpha());
                    }
                    renderConfiguration=configuration;settingsLoaded=true;
                    enabled=nextEnabled;blur=nextBlur;radius=nextRadius;style=nextStyle;automatic=nextAutomatic;whiteSymbols=nextWhite;screenMatch=nextScreen;packages=nextPackages;FAILED.clear();
                    LiquidGlassDrawable.configure(shader,plate,whiteSymbols,screenMatch);wallpaper.configure(enabled&&!screenMatch&&("liquid".equals(style)||nextDock||nextSearch||nextDrawer),root);
                    if(surfaces!=null)surfaces.configure(enabled,nextDock,nextSearch,nextDrawer,blur,radius,wallpaper);
                    if(widgets!=null)widgets.configure(enabled&&nextWidgets,blur,radius,wallpaper);
                    report(enabled?"Selected "+style+" material. Waiting for mapped icons to bind.":"Launcher connected. Glass icons are disabled.");
                    for(View v:new ArrayList<>(STATES.keySet()))if(v!=null){State state=state(v);state.resolveCached=false;state.blurFailed=false;apply(v);}
                    Activity activity=lastActivity.get();if(enabled&&activity!=null)scan(activity.getWindow().getDecorView());
                });
            }catch(Throwable e){error("CONFIG_ERROR_KEEPING_LAST_SETTINGS",e);}
        });
    }
    private static String resolve(View view){
        State state=state(view);Object tag=view.getTag();Object parentTag=view.getParent() instanceof View?((View)view.getParent()).getTag():null;
        if(state.resolveCached&&state.lastTag.matches(tag)&&state.lastParentTag.matches(parentTag))return state.resolvedPackage;
        state.lastTag.set(tag);state.lastParentTag.set(parentTag);state.resolveCached=true;state.resolvedPackage=null;
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
        if(info instanceof Intent){ComponentName component=((Intent)info).getComponent();return component==null?GlassConfig.iconKey(((Intent)info).getPackage(),null,packages,automatic):GlassConfig.iconKey(component.getPackageName(),component.getClassName(),packages,automatic);}
        if(info instanceof ComponentName){ComponentName component=(ComponentName)info;return GlassConfig.iconKey(component.getPackageName(),component.getClassName(),packages,automatic);}
        for(String method:new String[]{"getIntent","getComponentName","getComponent"}){Object result=call(info,method);if(result instanceof Intent||result instanceof ComponentName){String pkg=fromInfo(result);if(pkg!=null)return pkg;}}
        for(String name:new String[]{"mIntent","intent","mComponentName","componentName"}){Object result=field(info,name);if(result instanceof Intent||result instanceof ComponentName){String pkg=fromInfo(result);if(pkg!=null)return pkg;}}
        for(String method:new String[]{"getTargetPackage","getPackageName"}){Object result=call(info,method);if(result instanceof String){String key=GlassConfig.iconKey((String)result,null,packages,automatic);if(key!=null)return key;}}
        for(String name:new String[]{"mPackageName","packageName"}){Object result=field(info,name);if(result instanceof String){String key=GlassConfig.iconKey((String)result,null,packages,automatic);if(key!=null)return key;}}
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
        if(state.visibility.bind(pkg))sample("rebind:"+pkg,"Reset recycled source visibility for "+pkg);
        if(view instanceof ImageView&&state.visibility.hidden)return;
        Object previous=XposedHelpers.getAdditionalInstanceField(view,"devarix_artwork");
        if(previous instanceof Artwork&&!Objects.equals(((Artwork)previous).key,pkg))XposedHelpers.removeAdditionalInstanceField(view,"devarix_artwork");
        if(pkg!=null)XposedHelpers.setAdditionalInstanceField(view,"devarix_icon_key",pkg);
        Object tag=view.getTag();
        sample(view.getClass().getName()+":"+pkg+":"+(tag==null?"null":tag.getClass().getName()),"view="+view.getClass().getName()+" mapping="+pkg+" tagType="+(tag==null?"null":tag.getClass().getName()));
        if(!enabled||pkg==null){restore(view,state);return;}
        Bitmap bitmap=state.binding.trusted(assetKey(pkg));
        if(bitmap!=null)ICONS.put(assetKey(pkg),bitmap);
        else bitmap=ICONS.get(assetKey(pkg));
        if(bitmap!=null&&bitmap.isRecycled()){ICONS.remove(assetKey(pkg));bitmap=null;}
        if(bitmap==null&&GlassConfig.automaticKey(pkg)&&pkg.equals(state.originalKey)){
            Drawable original=state.original!=null?state.original:getIconDrawable(view);
            try{Bitmap captured=AutoGlyph.capture(original);if(captured!=null){bitmap=AutoGlyph.generate(captured);captured.recycle();ICONS.put(assetKey(pkg),bitmap);sample("generated:"+pkg,"Automatic silver glyph generated for "+pkg);}}
            catch(Throwable e){error("AUTO_GLYPH_ERROR",e);}
        }
        if(bitmap==null){if(state.applied&&!pkg.equals(state.packageName))restore(view,state);if(!GlassConfig.automaticKey(pkg))load(pkg,view);return;}
        XposedHelpers.setAdditionalInstanceField(view,"devarix_icon_glyph",bitmap);
        if(!(previous instanceof Artwork)||((Artwork)previous).glyph!=bitmap||!pkg.equals(((Artwork)previous).key))XposedHelpers.setAdditionalInstanceField(view,"devarix_artwork",new Artwork(pkg,bitmap));
        if(!state.applied&&state.original==null)state.original=getIconDrawable(view);
        boolean repaired;
        INTERNAL.set(true);try{
            repaired=state.binding.repair(assetKey(pkg),bitmap,new LiveIconBinding.Slot(){
                public Drawable get(){return getIconDrawable(view);}
                public void set(Drawable drawable){setIconDrawable(view,drawable);}
            },source->makeDrawable(view,source));
            repairDrawingSlot(view,state);
        }finally{INTERNAL.set(false);}
        state.applied=true;state.packageName=pkg;
        if(repaired){
            debug("ICON_REPAIRED","Verified artwork restored: "+pkg);
            if(!screenMatch)warmTransition(view,pkg);
        }
        if(!state.listener){state.listener=true;new IconIntegrityWatcher(view,GlassHook::verifyLiveIcon);view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            public void onViewAttachedToWindow(View v){MAIN.post(()->apply(v));}
            public void onViewDetachedFromWindow(View v){State s=STATES.get(v);if(s!=null)clearBlur(v,s);}
        });}
        if(state.applied&&!screenMatch)warmTransition(view,pkg);
        if(!screenMatch&&blur&&view instanceof ImageView&&view.isAttachedToWindow()&&(!"liquid".equals(style)||!wallpaper.isAvailable()))nativeBlur(view,state);else clearBlur(view,state);
    }
    private static void verifyLiveIcon(View view){
        if(!enabled||INTERNAL.get())return;State state=STATES.get(view);
        if(state==null||!state.applied)return;
        try{
            if(!state.binding.owns(getIconDrawable(view))){apply(view);return;}
            INTERNAL.set(true);try{repairDrawingSlot(view,state);}finally{INTERNAL.set(false);}
        }catch(Throwable e){error("ICON_INTEGRITY_ERROR",e);}
    }
    private static void repairDrawingSlot(View view,State state){
        if(!(view instanceof android.widget.TextView)||view instanceof ImageView)return;
        Drawable owned=getIconDrawable(view);if(!state.binding.owns(owned))return;
        Drawable current=((android.widget.TextView)view).getCompoundDrawables()[1];Object mask=field(view,"mTransparentDrawable");
        Drawable desired=LiveIconBinding.drawingSlot(owned,current,mask instanceof Drawable?(Drawable)mask:null,state.visibility.hidden);
        if(desired!=current&&desired!=null)XposedHelpers.callMethod(view,"applyCompoundDrawables",desired);
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
    private static Bitmap transition(View view){
        if(context==null||Looper.myLooper()!=Looper.getMainLooper())return null;
        State state=state(view);if(!state.applied||state.packageName==null)return null;
        String key=assetKey(state.packageName);Bitmap cached=TRANSITIONS.get(key);if(cached!=null&&!cached.isRecycled())return cached;
        Drawable drawable=getIconDrawable(view);if(drawable==null)return null;
        try{
            Bitmap result;
            if(drawable instanceof LiquidGlassDrawable)result=((LiquidGlassDrawable)drawable).transitionBitmap();
            else{Bitmap glyph=ICONS.get(key);if(glyph==null||glyph.isRecycled())return null;result=glyph;}
            TRANSITIONS.put(key,result);debug("TRANSITION_READY",key);return result;
        }catch(Throwable e){error("TRANSITION_ERROR",e);return null;}
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
        XposedHelpers.removeAdditionalInstanceField(view,"devarix_artwork");
        if(state.applied){INTERNAL.set(true);try{if(view instanceof ImageView)setIconDrawable(view,state.original);else{XposedHelpers.callMethod(view,"setIconDrawable",state.original,null);if(state.original!=null)state.original.setAlpha(state.visibility.alpha);Object mask=field(view,"mTransparentDrawable");if(state.visibility.hidden&&mask instanceof Drawable)XposedHelpers.callMethod(view,"applyCompoundDrawables",mask);}}finally{INTERNAL.set(false);}state.applied=false;state.packageName=null;}
        state.binding.clear();
    }
    private static void widgetReport(String message){
        if(context==null)return;
        CONTROL_IO.execute(()->{try{Bundle data=new Bundle();data.putString("widgets",message);context.getContentResolver().call(GlassProvider.SETTINGS,"widgets",null,data);}catch(Throwable e){error("WIDGET_STATUS_ERROR",e);}});
    }
    private static void surfaceReport(String message){
        if(context==null)return;
        CONTROL_IO.execute(()->{try{Bundle data=new Bundle();data.putString("surfaces",message);context.getContentResolver().call(GlassProvider.SETTINGS,"surfaces",null,data);}catch(Throwable e){error("SURFACE_STATUS_ERROR",e);}});
    }
    private static void report(String message){
        if(context==null||message.equals(lastStatus))return;lastStatus=message;
        CONTROL_IO.execute(()->{try{Bundle data=new Bundle();data.putString("status",message);context.getContentResolver().call(GlassProvider.SETTINGS,"report",null,data);}catch(Throwable e){error("STATUS_ERROR",e);}});
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
