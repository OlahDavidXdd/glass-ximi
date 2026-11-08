package io.github.devarix.kashiglass;

import android.content.res.ColorStateList;
import android.graphics.drawable.*;
import android.graphics.PorterDuff;
import android.view.*;
import java.util.*;
import java.lang.ref.WeakReference;
import java.util.function.Consumer;
import de.robv.android.xposed.*;

/** Replaces decorative backgrounds only; the launcher's input and layout code stays in charge. */
final class LauncherSurfaces {
    private static final String[] TARGETS={"com.miui.home.launcher.hotseats.HotSeats","com.miui.home.launcher.SearchBarDesktopLayout","com.miui.home.launcher.SearchBarDrawerLayout"};
    private final Map<View,State> states=new WeakHashMap<>();
    private final Consumer<String> report,log;
    private boolean enabled,dock=true,search=true,blur;
    private int radius=24,revision;
    private GlassBackdrop backdrop;
    private String lastCounts="",lastRenderStatus="";
    private static final class State {
        Drawable original,ours;ColorStateList tint;PorterDuff.Mode mode;
        WeakReference<View> decor=new WeakReference<>(null);int decorVisibility;
        boolean applied,decorHidden;int revision=-1,kind;
    }
    LauncherSurfaces(Consumer<String> report,Consumer<String> log){this.report=report;this.log=log;}
    void install(ClassLoader loader){
        for(String name:TARGETS){
            Class<?> type=XposedHelpers.findClassIfExists(name,loader);log.accept("Target "+name+" found="+(type!=null));if(type==null)continue;
            try{
                XposedBridge.hookAllConstructors(type,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){
                    View view=(View)p.thisObject;
                    if(XposedHelpers.getAdditionalInstanceField(view,"devarix_surface")!=null)return;
                    XposedHelpers.setAdditionalInstanceField(view,"devarix_surface",true);
                    view.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->apply(v));
                    view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){apply(v);}public void onViewDetachedFromWindow(View v){counts();}});
                }});
                for(String method:new String[]{"onFinishInflate","onAttachedToWindow","onResume","refreshStyle","refreshSearchBarExterior","onWallpaperColorChanged"}){
                    XposedBridge.hookAllMethods(type,method,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){apply((View)p.thisObject);}});
                }
            }catch(Throwable e){log.accept("Hook error "+name+": "+e);}
        }
    }
    void configure(boolean enabled,boolean dock,boolean search,boolean blur,int radius,GlassBackdrop backdrop){
        if(this.enabled!=enabled||this.dock!=dock||this.search!=search||this.blur!=blur||this.radius!=radius)revision++;
        this.enabled=enabled;this.dock=dock;this.search=search;this.blur=blur;this.radius=radius;this.backdrop=backdrop;
        for(View view:new ArrayList<>(states.keySet()))if(view!=null)apply(view);
        counts();
    }
    void apply(View view){
        int kind=GlassSurface.NONE;
        for(Class<?> type=view.getClass();type!=null;type=type.getSuperclass()){kind=GlassSurface.kind(type.getName());if(kind!=GlassSurface.NONE)break;}
        if(kind==GlassSurface.NONE)return;
        State state=states.get(view);boolean active=enabled&&backdrop!=null&&(kind==GlassSurface.DOCK?dock:search);
        if(!active){if(state!=null)restore(view,state);return;}
        if(state==null){state=new State();state.kind=kind;states.put(view,state);}
        try{
            if(!state.applied||state.revision!=revision||view.getBackground()!=state.ours){
                if(!state.applied||view.getBackground()!=state.ours){state.original=view.getBackground();state.tint=view.getBackgroundTintList();state.mode=view.getBackgroundTintMode();}
                Drawable glass=new LiquidGlassDrawable(view,null,backdrop,blur,radius,this::renderStatus,kind);
                int inset=kind==GlassSurface.DOCK?Math.round(8*view.getResources().getDisplayMetrics().density):0;
                state.ours=inset==0?glass:new InsetDrawable(glass,inset,Math.round(inset*.6f),inset,Math.round(inset*.6f));
                background(view,state.ours);view.setBackgroundTintList(null);state.applied=true;state.revision=revision;
                log.accept("Applied "+(kind==GlassSurface.DOCK?"dock":"search pill")+" "+view.getClass().getName()+" "+view.getWidth()+"x"+view.getHeight());
            }
            if(kind==GlassSurface.DOCK){
                Object value=XposedHelpers.getObjectField(view,"mBgBlurView");
                if(value instanceof View){View decor=(View)value;
                    if(state.decor.get()!=decor){restoreDecor(state);state.decor=new WeakReference<>(decor);state.decorVisibility=decor.getVisibility();}
                    if(decor.getVisibility()==View.VISIBLE){state.decorVisibility=View.VISIBLE;state.decorHidden=true;decor.setVisibility(View.INVISIBLE);}
                }
            }
            view.invalidate();counts();
        }catch(Throwable e){log.accept("Surface apply error: "+e);restore(view,state);}
    }
    private static void background(View view,Drawable drawable){
        // setBackground may apply drawable padding; preserve the launcher's current layout.
        boolean relative=view.isPaddingRelative();int start=relative?view.getPaddingStart():view.getPaddingLeft(),end=relative?view.getPaddingEnd():view.getPaddingRight(),top=view.getPaddingTop(),bottom=view.getPaddingBottom();
        view.setBackground(drawable);
        if(relative)view.setPaddingRelative(start,top,end,bottom);else view.setPadding(start,top,end,bottom);
    }
    private static void restoreDecor(State state){View decor=state.decor.get();if(decor!=null&&state.decorHidden&&decor.getVisibility()==View.INVISIBLE)decor.setVisibility(state.decorVisibility);state.decor.clear();state.decorHidden=false;}
    private void restore(View view,State state){
        if(state.applied){if(view.getBackground()==state.ours){background(view,state.original);view.setBackgroundTintList(state.tint);view.setBackgroundTintMode(state.mode);}restoreDecor(state);state.applied=false;state.ours=null;log.accept("Restored "+view.getClass().getName());counts();}
    }
    private void renderStatus(String message){if(!message.equals(lastRenderStatus)){lastRenderStatus=message;log.accept(message);}}
    void invalidate(){for(View view:new ArrayList<>(states.keySet()))if(view!=null)view.invalidate();}
    private void counts(){
        int docks=0,searches=0;
        for(var entry:states.entrySet())if(entry.getKey()!=null&&entry.getValue().applied&&entry.getKey().isShown()){
            if(entry.getValue().kind==GlassSurface.DOCK)docks++;else searches++;
        }
        String value="Glass surfaces: "+docks+" visible dock, "+searches+" visible search bar.";
        if(!value.equals(lastCounts)){lastCounts=value;report.accept(value);}
    }
}
