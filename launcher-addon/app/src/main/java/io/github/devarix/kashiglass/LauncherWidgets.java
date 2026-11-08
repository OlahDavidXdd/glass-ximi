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
    private final Map<View,Change> changes=new WeakHashMap<>();
    private final Map<View,Host> hosts=new WeakHashMap<>();
    private final Consumer<String> log,report;
    private boolean enabled,blur,applying;
    private int strength=24,revision;
    private GlassBackdrop backdrop;
    private String lastCounts="",lastRender="";
    private static final class Host {String diagnostic="";boolean supported;WeakReference<View> root=new WeakReference<>(null);}
    private static final class Change {
        WeakReference<View> host;Drawable background,image;ColorStateList tint;PorterDuff.Mode mode;
        WeakReference<Drawable> ours=new WeakReference<>(null);boolean imageCleared,applied,glass;int revision;
    }
    LauncherWidgets(Consumer<String> report,Consumer<String> log){this.report=report;this.log=log;}
    void install(ClassLoader loader){
        Class<?> type=XposedHelpers.findClassIfExists("com.miui.home.launcher.LauncherAppWidgetHostView",loader);
        log.accept("Widget host found="+(type!=null));if(type==null)return;
        XposedBridge.hookAllConstructors(type,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){
            View view=(View)p.thisObject;if(hosts.containsKey(view))return;hosts.put(view,new Host());
            view.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->apply(v));
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){v.post(()->apply(v));}public void onViewDetachedFromWindow(View v){counts();}});
        }});
        for(String method:new String[]{"updateAppWidget","setAppWidget","reInflate","onAttachedToWindow"})
            XposedBridge.hookAllMethods(type,method,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){View view=(View)p.thisObject;view.post(()->apply(view));}});
    }
    void configure(boolean enabled,boolean blur,int strength,GlassBackdrop backdrop){
        revision++; // Recreate cards when shared material options change as well.
        this.enabled=enabled;this.blur=blur;this.strength=strength;this.backdrop=backdrop;
        if(!enabled){for(View view:new ArrayList<>(changes.keySet()))restore(view);for(Host host:hosts.values())host.supported=false;}
        else for(View view:new ArrayList<>(hosts.keySet()))apply(view);
        counts();
    }
    void apply(View view){
        if(applying||!enabled||backdrop==null||!(view instanceof AppWidgetHostView))return;
        applying=true;
        try{
            AppWidgetHostView host=(AppWidgetHostView)view;Host state=hosts.get(view);if(state==null){state=new Host();hosts.put(view,state);}
            AppWidgetProviderInfo info=host.getAppWidgetInfo();if(info==null||info.provider==null)return;
            String provider=info.provider.flattenToShortString();String label=String.valueOf(info.loadLabel(view.getContext().getPackageManager()));
            boolean supported=WidgetSurfacePolicy.supported(provider,label);
            if(!supported){restoreHost(view);state.supported=false;return;}
            state.supported=true;
            View root=host.getChildCount()>0?host.getChildAt(0):null;
            if(root==null||root.getWidth()==0||root.getHeight()==0)return;
            if(state.root.get()!=root){restoreHost(view);state.root=new WeakReference<>(root);int[] remaining={32};describe(root,0,remaining);}
            // Never paint across the launcher's widget label or outer padding.
            boolean layered=root instanceof ViewGroup;
            int removed=0;
            if(layered){replace(view,root,true,false);removed=clearDecoration(view,root,root,0);}
            String diagnostic="Widget provider="+provider+" root="+root.getClass().getName()+" id="+id(root)+" size="+root.getWidth()+"x"+root.getHeight()+" decorative_layers="+removed+(layered?"; live views retained":"; flattened content retained, background cannot be separated safely");
            if(!diagnostic.equals(state.diagnostic)){state.diagnostic=diagnostic;log.accept(diagnostic);}
        }catch(Throwable e){log.accept("Widget styling error: "+e.getClass().getSimpleName());restoreHost(view);}
        finally{applying=false;counts();}
    }
    private int clearDecoration(View host,View root,View view,int depth){
        if(depth>10)return 0;int count=0;
        if(view!=root&&WidgetSurfacePolicy.coversCard(root.getWidth(),root.getHeight(),view.getWidth(),view.getHeight())){
            boolean image=view instanceof ImageView&&WidgetSurfacePolicy.decorativeImage(id(view),view.isClickable()||view.isLongClickable(),view.getContentDescription()!=null&&view.getContentDescription().length()>0);
            if(view.getBackground()!=null||image){replace(host,view,false,image);count++;}
        }
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)count+=clearDecoration(host,root,group.getChildAt(i),depth+1);}
        return count;
    }
    private void replace(View host,View view,boolean glass,boolean clearImage){
        Change change=changes.get(view);if(change==null){change=new Change();change.host=new WeakReference<>(host);changes.put(view,change);}
        if(!change.applied||view.getBackground()!=change.ours.get()){
            change.background=view.getBackground();change.tint=view.getBackgroundTintList();change.mode=view.getBackgroundTintMode();
        }else if(change.revision==revision&&change.glass==glass&&(!clearImage||((ImageView)view).getDrawable()==null))return;
        Drawable ours=glass?new LiquidGlassDrawable(view,null,backdrop,blur,strength,this::renderStatus,GlassSurface.WIDGET):new ColorDrawable(Color.TRANSPARENT);
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
            View root=entry.getValue().root.get();Change change=changes.get(root);if(change!=null&&change.applied)cards++;else unsupported++;
        }
        String value="Widgets: "+cards+" glass clock/weather cards, "+unsupported+" inseparable or pending cards.";
        if(!value.equals(lastCounts)){lastCounts=value;report.accept(value);}
    }
}
