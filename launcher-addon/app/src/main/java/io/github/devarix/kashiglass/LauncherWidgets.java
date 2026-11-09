package io.github.devarix.kashiglass;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;
import de.robv.android.xposed.*;

/** Styles RemoteViews decoration without replacing the provider or its live content. */
final class LauncherWidgets {
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Map<View,Change> changes=new WeakHashMap<>();
    private final Map<View,Host> hosts=new WeakHashMap<>();
    private final Consumer<String> log,report;
    private final MamlWidgetGlass maml;
    private final SearchBarBlur.Calls underlayCalls;
    private boolean enabled,blur,applying;
    private int strength=24,revision;
    private GlassBackdrop backdrop;
    private String lastCounts="",lastRender="";
    private static final class Host {String diagnostic="";boolean supported,watching;WeakReference<View> root=new WeakReference<>(null);}
    private static final class Change {
        WeakReference<View> host;Drawable background,image;ColorStateList tint;PorterDuff.Mode mode;
        SearchBarBlur underlay;WeakReference<Drawable> ours=new WeakReference<>(null);boolean imageCleared,applied,glass;int revision;
    }
    LauncherWidgets(Consumer<String> report,Consumer<String> log){this(report,log,(view,method,args)->XposedHelpers.callMethod(view,method,args));}
    LauncherWidgets(Consumer<String> report,Consumer<String> log,SearchBarBlur.Calls calls){this.report=report;this.log=log;this.underlayCalls=calls;this.maml=new MamlWidgetGlass(log,message->counts(),calls);}
    void install(ClassLoader loader){
        Class<?> mamlHost=XposedHelpers.findClassIfExists("com.miui.home.launcher.maml.MaMlHostView",loader);
        log.accept("MAML widget host found="+(mamlHost!=null));
        if(mamlHost!=null){
            XposedBridge.hookAllConstructors(mamlHost,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){track((View)p.thisObject);}});
            Class<?> mamlView=XposedHelpers.findClassIfExists("com.miui.maml.component.MamlView",loader);
            if(mamlView!=null)XposedBridge.hookAllMethods(mamlView,"initMamlview",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){View view=(View)p.thisObject;view.post(()->apply(view));}});
            Class<?> group=XposedHelpers.findClassIfExists("com.miui.maml.elements.ElementGroup",loader);
            if(group!=null)XposedBridge.hookAllMethods(group,"doRender",new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){try{if(p.args.length==1&&p.args[0] instanceof android.graphics.Canvas)maml.drawBackground(p.thisObject,(android.graphics.Canvas)p.args[0]);}catch(Throwable error){log.accept("MAML background hook error: "+error.getClass().getSimpleName());}}});
            Class<?> element=XposedHelpers.findClassIfExists("com.miui.maml.elements.AnimatedScreenElement",loader);
            if(element!=null)XposedBridge.hookAllMethods(element,"render",new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){try{if(maml.skipDecoration(p.thisObject))p.setResult(null);}catch(Throwable error){log.accept("MAML decoration hook error: "+error.getClass().getSimpleName());}}});
        }
        Class<?> type=XposedHelpers.findClassIfExists("com.miui.home.launcher.LauncherAppWidgetHostView",loader);
        log.accept("Widget host found="+(type!=null));if(type==null)return;
        XposedBridge.hookAllConstructors(type,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){
            track((View)p.thisObject);
        }});
        for(String method:new String[]{"updateAppWidget","setAppWidget","reInflate","onAttachedToWindow"})
            XposedBridge.hookAllMethods(type,method,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){View view=(View)p.thisObject;view.post(()->apply(view));}});
    }
    void track(View view){
        if(android.os.Looper.myLooper()!=android.os.Looper.getMainLooper()){main.post(()->track(view));return;}
        Host state=hosts.get(view);if(state==null){state=new Host();hosts.put(view,state);}if(state.watching)return;state.watching=true;
        Watcher watcher=new Watcher(view);view.addOnLayoutChangeListener(watcher);view.addOnAttachStateChangeListener(watcher);if(view.isAttachedToWindow())watcher.onViewAttachedToWindow(view);
    }
    private final class Watcher implements View.OnAttachStateChangeListener,View.OnLayoutChangeListener,android.view.ViewTreeObserver.OnPreDrawListener {
        private final WeakReference<View> owner;private WeakReference<android.view.ViewTreeObserver> tree=new WeakReference<>(null);
        Watcher(View view){owner=new WeakReference<>(view);}
        public void onLayoutChange(View v,int l,int t,int r,int b,int ol,int ot,int or,int ob){apply(v);}
        public void onViewAttachedToWindow(View v){remove();android.view.ViewTreeObserver observer=v.getViewTreeObserver();observer.addOnPreDrawListener(this);tree=new WeakReference<>(observer);v.post(()->apply(v));}
        public void onViewDetachedFromWindow(View v){remove();maml.detach(v);counts();}
        private void remove(){android.view.ViewTreeObserver observer=tree.get();if(observer!=null&&observer.isAlive())observer.removeOnPreDrawListener(this);tree.clear();}
        public boolean onPreDraw(){
            View view=owner.get();if(view==null){remove();return true;}
            if(enabled)maml.apply(view);Host state=hosts.get(view);
            if(!enabled||!view.isShown()||state==null||!state.supported)return true;
            View root=view instanceof ViewGroup&&((ViewGroup)view).getChildCount()>0?((ViewGroup)view).getChildAt(0):null;
            boolean dirty=root!=state.root.get();
            if(!dirty)for(var entry:changes.entrySet()){
                View child=entry.getKey();Change change=entry.getValue();if(child==null||change.host.get()!=view)continue;
                if(!change.glass&&root!=null&&!WidgetSurfacePolicy.coversCard(root.getWidth(),root.getHeight(),child.getWidth(),child.getHeight())||child.getBackground()!=change.ours.get()||change.imageCleared&&child instanceof ImageView&&(((ImageView)child).getDrawable()!=null||child.isClickable()||child.isLongClickable()||child.getContentDescription()!=null&&child.getContentDescription().length()>0)){dirty=true;break;}
            }
            if(dirty)apply(view);return true;
        }
    }
    void configure(boolean enabled,boolean blur,int strength,GlassBackdrop backdrop){
        revision++; // Recreate cards when shared material options change as well.
        this.enabled=enabled;this.blur=blur;this.strength=strength;this.backdrop=backdrop;
        maml.configure(enabled,blur,strength,backdrop);
        if(!enabled){for(View view:new ArrayList<>(changes.keySet()))restore(view);for(Host host:hosts.values())host.supported=false;}
        else for(View view:new ArrayList<>(hosts.keySet()))apply(view);
        counts();
    }
    void apply(View view){
        if(android.os.Looper.myLooper()!=android.os.Looper.getMainLooper()){main.post(()->apply(view));return;}
        if(applying||!enabled||backdrop==null)return;
        maml.apply(view);
        if(!(view instanceof AppWidgetHostView))return;
        track(view);
        applying=true;
        try{
            AppWidgetHostView host=(AppWidgetHostView)view;Host state=hosts.get(view);if(state==null){state=new Host();hosts.put(view,state);}
            AppWidgetProviderInfo info=host.getAppWidgetInfo();if(info==null||info.provider==null){restoreHost(view);state.supported=false;state.root.clear();return;}
            String provider=info.provider.flattenToShortString();String label=String.valueOf(info.loadLabel(view.getContext().getPackageManager()));
            boolean supported=WidgetSurfacePolicy.supported(provider,label);
            if(!supported){restoreHost(view);state.supported=false;return;}
            state.supported=true;
            View root=host.getChildCount()>0?host.getChildAt(0):null;
            if(state.root.get()!=root){restoreHost(view);state.root=new WeakReference<>(root);if(root!=null){int[] remaining={32};describe(root,0,remaining);}}
            if(root==null||root.getWidth()==0||root.getHeight()==0)return;
            // Never paint across the launcher's widget label or outer padding.
            boolean layered=root instanceof ViewGroup;
            int removed=0;
            Set<View> touched=Collections.newSetFromMap(new IdentityHashMap<>());
            if(layered){replace(view,root,true,false);touched.add(root);removed=clearDecoration(view,root,root,0,touched);}
            for(View changed:new ArrayList<>(changes.keySet())){Change change=changes.get(changed);if(change!=null&&change.host.get()==view&&!touched.contains(changed))restore(changed);}
            String diagnostic="Widget provider="+provider+" root="+root.getClass().getName()+" id="+id(root)+" size="+root.getWidth()+"x"+root.getHeight()+" decorative_layers="+removed+(layered?"; live views retained":"; flattened content retained, background cannot be separated safely");
            if(!diagnostic.equals(state.diagnostic)){state.diagnostic=diagnostic;log.accept(diagnostic);}
        }catch(Throwable e){log.accept("Widget styling error: "+e.getClass().getSimpleName());restoreHost(view);}
        finally{applying=false;counts();}
    }
    private int clearDecoration(View host,View root,View view,int depth,Set<View> touched){
        if(depth>10)return 0;int count=0;
        if(view!=root&&WidgetSurfacePolicy.coversCard(root.getWidth(),root.getHeight(),view.getWidth(),view.getHeight())){
            boolean image=view instanceof ImageView&&WidgetSurfacePolicy.decorativeImage(id(view),view.isClickable()||view.isLongClickable(),view.getContentDescription()!=null&&view.getContentDescription().length()>0);
            if(view.getBackground()!=null||image){replace(host,view,false,image);touched.add(view);count++;}
        }
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)count+=clearDecoration(host,root,group.getChildAt(i),depth+1,touched);}
        return count;
    }
    private void replace(View host,View view,boolean glass,boolean clearImage){
        Change change=changes.get(view);if(change==null){change=new Change();change.host=new WeakReference<>(host);changes.put(view,change);}
        if(change.underlay!=null)change.underlay.useSharedMaterial(false);
        if(change.imageCleared&&!clearImage&&view instanceof ImageView){
            if(((ImageView)view).getDrawable()==null)((ImageView)view).setImageDrawable(change.image);
            change.imageCleared=false;change.image=null;
        }
        if(!change.applied||view.getBackground()!=change.ours.get()){
            change.background=view.getBackground();change.tint=view.getBackgroundTintList();change.mode=view.getBackgroundTintMode();
        }else if(change.revision==revision&&change.glass==glass&&(!clearImage||((ImageView)view).getDrawable()==null))return;
        if(change.underlay==null)change.underlay=new SearchBarBlur(view,glass?host:view,underlayCalls,log,"WIDGET");
        change.underlay.useSharedMaterial(true);
        Drawable ours=glass?SharedGlassMaterial.create(view,backdrop,blur,strength,this::renderStatus,GlassSurface.WIDGET):new ColorDrawable(Color.TRANSPARENT);
        change.ours=new WeakReference<>(ours);change.applied=true;change.glass=glass;change.revision=revision;
        background(view,ours);view.setBackgroundTintList(null);
        if(clearImage){ImageView image=(ImageView)view;if(image.getDrawable()!=null){change.image=image.getDrawable();change.imageCleared=true;image.setImageDrawable(null);}}
    }
    private static void background(View view,Drawable drawable){
        boolean relative=view.isPaddingRelative();int start=relative?view.getPaddingStart():view.getPaddingLeft(),end=relative?view.getPaddingEnd():view.getPaddingRight(),top=view.getPaddingTop(),bottom=view.getPaddingBottom();
        view.setBackground(drawable);if(relative)view.setPaddingRelative(start,top,end,bottom);else view.setPadding(start,top,end,bottom);
    }
    private void restoreHost(View host){for(View view:new ArrayList<>(changes.keySet())){Change change=changes.get(view);if(change!=null&&change.host.get()==host)restore(view);}}
    private void restore(View view){
        Change change=changes.remove(view);if(view==null||change==null||!change.applied)return;
        if(change.underlay!=null)change.underlay.restore();
        if(view.getBackground()==change.ours.get()){background(view,change.background);view.setBackgroundTintList(change.tint);view.setBackgroundTintMode(change.mode);}
        if(change.imageCleared&&view instanceof ImageView&&((ImageView)view).getDrawable()==null)((ImageView)view).setImageDrawable(change.image);
    }
    private void describe(View view,int depth,int[] remaining){
        if(depth>4||remaining[0]--<=0)return;
        log.accept("Widget layer depth="+depth+" class="+view.getClass().getName()+" id="+id(view)+" size="+view.getWidth()+"x"+view.getHeight()+" background="+(view.getBackground()!=null));
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount()&&remaining[0]>0;i++)describe(group.getChildAt(i),depth+1,remaining);}
    }
    private static String id(View view){try{return view.getResources().getResourceEntryName(view.getId());}catch(Exception ignored){return "no_id";}}
    private void renderStatus(String message){if(!message.equals(lastRender)){lastRender=message;log.accept(message);}}
    private void counts(){
        int cards=0,unsupported=0;for(var entry:hosts.entrySet())if(entry.getKey()!=null&&entry.getKey().isShown()&&entry.getValue().supported){
            View root=entry.getValue().root.get();Change change=changes.get(root);if(change!=null&&change.applied&&root.getWidth()>0&&root.getHeight()>0&&root.isShown())cards++;else unsupported++;
        }
        String value="Widgets: "+cards+" glass clock/weather cards, "+unsupported+" inseparable or pending cards; "+maml.summary()+".";
        if(!value.equals(lastCounts)){lastCounts=value;report.accept(value);}
    }
}
