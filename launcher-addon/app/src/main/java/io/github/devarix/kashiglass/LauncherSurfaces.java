package io.github.devarix.kashiglass;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.lang.ref.WeakReference;
import java.util.function.Consumer;
import de.robv.android.xposed.*;

/** Only decorative backgrounds are replaced; native input, source visibility and layout remain intact. */
final class LauncherSurfaces {
    private static final String MARK="devarix_search_surface";
    private static final String[] TARGETS={"com.miui.home.launcher.hotseats.HotSeats","com.miui.home.launcher.SearchBarDesktopLayout","com.miui.home.launcher.SearchBarDrawerLayout","com.miui.home.launcher.search.QuickSearchBarWidgetView","com.miui.home.launcher.SearchBarContainer","com.miui.home.launcher.pageindicators.CapsuleIndicator","com.miui.home.launcher.pageindicators.LauncherIndicator","com.miui.home.launcher.allapps.BaseAllAppsContainerView","com.miui.home.launcher.allapps.AllAppsCategoryContainer","com.miui.home.launcher.allapps.category.AllAppsCategoryListContainer","com.miui.home.launcher.allapps.AllAppsRecyclerView","com.miui.home.launcher.allapps.search.AllAppsSearchContainer"};
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Map<View,State> states=new WeakHashMap<>(),layers=new WeakHashMap<>();
    private final Map<View,WeakReference<View>> quickTargets=new WeakHashMap<>();
    private final Consumer<String> report,log;
    private final PageIndicatorGlass pages;
    private boolean enabled,dock=true,search=true,drawer=true,blur;
    private int radius=24,revision;
    private GlassBackdrop backdrop;
    private String lastCounts="",lastRenderStatus="",lastSearchStatus="";
    private static final class State {
        Drawable original;ColorStateList tint;PorterDuff.Mode mode;
        WeakReference<Drawable> ours=new WeakReference<>(null);
        WeakReference<View> decor=new WeakReference<>(null);int decorVisibility;
        Drawable indicatorImage;WeakReference<ImageView> indicator=new WeakReference<>(null);boolean nativeBlurManaged;SearchBarBlur searchBlur;
        boolean applied,decorHidden;int revision=-1,kind;
    }
    LauncherSurfaces(Consumer<String> report,Consumer<String> log){this.report=report;this.log=log;this.pages=new PageIndicatorGlass((v,method,args)->XposedHelpers.callMethod(v,method,args),log);}
    void install(ClassLoader loader){
        for(String name:TARGETS){
            Class<?> type=XposedHelpers.findClassIfExists(name,loader);log.accept("Surface target "+name+" found="+(type!=null));if(type==null)continue;
            try{
                XposedBridge.hookAllConstructors(type,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){
                    View view=(View)p.thisObject;if(XposedHelpers.getAdditionalInstanceField(view,"devarix_surface")!=null)return;
                    XposedHelpers.setAdditionalInstanceField(view,"devarix_surface",true);Watcher watcher=new Watcher(view);
                    view.addOnLayoutChangeListener(watcher);view.addOnAttachStateChangeListener(watcher);
                    if(view.isAttachedToWindow())watcher.onViewAttachedToWindow(view);
                }});
                for(String method:new String[]{"onFinishInflate","onAttachedToWindow","onResume","refreshStyle","refreshSearchBarExterior","onWallpaperColorChanged","updateContainerVisibility","resetAppsWhenLauncherModeChanged","updateLauncherSearchBarVisibility","refreshSearchBarEnable","refreshWithViewType","setBgBlurIfSupported","setBgBlurIfSupported$kx","setBgBlurIfSupportedForU","createBackgroundDrawable","onDarkModeChanged","refreshIndicator","refreshCapsuleIndicatorType","showIndicatorAndResetAccessibility"}){
                    XposedBridge.hookAllMethods(type,method,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){styleChanged((View)p.thisObject);}});
                }
            }catch(Throwable e){log.accept("Surface hook error "+name+": "+e);}
        }
    }
    private void styleChanged(View view){
        if(android.os.Looper.myLooper()!=android.os.Looper.getMainLooper()){main.post(()->styleChanged(view));return;}
        if(SearchSurfacePolicy.capsuleHost(view.getClass().getName())){
            Object body=XposedHelpers.getObjectField(view,"mIndicatorBackground");if(body instanceof View){State state=states.get(body);if(state!=null)state.nativeBlurManaged=false;}
        }
        if(SearchSurfacePolicy.pageHost(view.getClass().getName())){pageIndicator(view,true);return;}
        apply(view);
    }
    private final class Watcher implements View.OnLayoutChangeListener,View.OnAttachStateChangeListener,ViewTreeObserver.OnPreDrawListener {
        private final WeakReference<View> owner;
        private WeakReference<ViewTreeObserver> observer=new WeakReference<>(null);
        Watcher(View view){owner=new WeakReference<>(view);}
        public void onLayoutChange(View v,int l,int t,int r,int b,int ol,int ot,int or,int ob){apply(v);}
        public void onViewAttachedToWindow(View v){
            apply(v);ViewTreeObserver tree=v.getViewTreeObserver();
            if(observer.get()!=tree){remove();tree.addOnPreDrawListener(this);observer=new WeakReference<>(tree);}
        }
        public void onViewDetachedFromWindow(View v){remove();pages.detach(v);counts();}
        private void remove(){ViewTreeObserver tree=observer.get();if(tree!=null&&tree.isAlive())tree.removeOnPreDrawListener(this);observer.clear();}
        public boolean onPreDraw(){
            View view=owner.get();if(view==null){remove();return true;}
            if(enabled&&view.isShown()){
                State state=states.get(view);if(state!=null&&state.applied&&view.getBackground()!=state.ours.get())apply(view);
                if(SearchSurfacePolicy.quickHost(view.getClass().getName())||SearchSurfacePolicy.capsuleHost(view.getClass().getName())||SearchSurfacePolicy.pageHost(view.getClass().getName()))apply(view);
                State layer=layers.get(view);if(layer!=null&&layer.applied&&view.getBackground()!=layer.ours.get())clearLayer(view,layer.kind);
            }
            counts();return true;
        }
    }
    void configure(boolean enabled,boolean dock,boolean search,boolean drawer,boolean blur,int radius,GlassBackdrop backdrop){
        revision++; // GlassHook already deduplicates the complete settings snapshot, including material mode.
        this.enabled=enabled;this.dock=dock;this.search=search;this.drawer=drawer;this.blur=blur;this.radius=radius;this.backdrop=backdrop;
        pages.configure(enabled&&search,blur,radius,backdrop);
        for(View view:new ArrayList<>(states.keySet()))if(view!=null)apply(view);
        for(View view:new ArrayList<>(layers.keySet()))if(view!=null){State state=layers.get(view);if(state!=null&&(!enabled||(state.kind==GlassSurface.SEARCH?!search:!drawer)))restore(view,state);}
        if(!enabled||!search)lastSearchStatus="Disabled";
        counts();
    }
    private static int kind(View view){
        for(Class<?> type=view.getClass();type!=null;type=type.getSuperclass()){int value=GlassSurface.kind(type.getName());if(value!=GlassSurface.NONE)return value;}
        return Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(view,MARK))?GlassSurface.SEARCH:GlassSurface.NONE;
    }
    void apply(View view){
        if(android.os.Looper.myLooper()!=android.os.Looper.getMainLooper()){main.post(()->apply(view));return;}
        try{
            if(SearchSurfacePolicy.pageHost(view.getClass().getName())){pageIndicator(view,false);return;}
            if(SearchSurfacePolicy.capsuleHost(view.getClass().getName())){capsuleSearch(view);return;}
            if(SearchSurfacePolicy.quickHost(view.getClass().getName())){quickSearch(view);return;}
            int kind=kind(view);
            if(kind==GlassSurface.NONE){if(GlassSurface.drawerLayer(view.getClass().getName()))clearLayer(view,GlassSurface.DRAWER);return;}
            applySurface(view,kind);
        }catch(Throwable e){log.accept("Surface apply error: "+android.util.Log.getStackTraceString(e));State state=states.get(view);if(state!=null)restore(view,state);}
    }
    private void applySurface(View view,int kind){
        State state=states.get(view);boolean active=enabled&&backdrop!=null&&(kind==GlassSurface.DOCK?dock:kind==GlassSurface.DRAWER?drawer:search);
        if(!active){if(state!=null)restore(view,state);return;}
        if(state==null){state=new State();state.kind=kind;states.put(view,state);}
        if(!state.applied||state.revision!=revision||view.getBackground()!=state.ours.get()){
            if(!state.applied||view.getBackground()!=state.ours.get()){state.original=view.getBackground();state.tint=view.getBackgroundTintList();state.mode=view.getBackgroundTintMode();}
            Drawable glass=SharedGlassMaterial.create(view,backdrop,blur,radius,this::renderStatus,kind);
            int inset=kind==GlassSurface.DOCK?Math.round(8*view.getResources().getDisplayMetrics().density):0;
            Drawable ours=inset==0?glass:new InsetDrawable(glass,inset,Math.round(inset*.6f),inset,Math.round(inset*.6f));
            state.ours=new WeakReference<>(ours);background(view,ours);view.setBackgroundTintList(null);state.applied=true;state.revision=revision;
            log.accept("Surface applied kind="+kind+" class="+view.getClass().getName()+" size="+view.getWidth()+"x"+view.getHeight());
        }
        if(view.getBackgroundTintList()!=null){state.tint=view.getBackgroundTintList();state.mode=view.getBackgroundTintMode();view.setBackgroundTintList(null);}
        if(kind==GlassSurface.DOCK){
            Object value=XposedHelpers.getObjectField(view,"mBgBlurView");
            if(value instanceof View){View decor=(View)value;if(state.decor.get()!=decor){restoreDecor(state);state.decor=new WeakReference<>(decor);state.decorVisibility=decor.getVisibility();}
                if(decor.getVisibility()==View.VISIBLE){state.decorVisibility=View.VISIBLE;state.decorHidden=true;decor.setVisibility(View.INVISIBLE);}}
        }
        if(kind==GlassSurface.DRAWER&&view instanceof ViewGroup)clearDescendants((ViewGroup)view);
        counts();
    }
    private void pageIndicator(View host,boolean refreshed){
        Object target=XposedHelpers.getObjectField(host,"mScreenIndicator");
        Object reference=XposedHelpers.getObjectField(host,"mCapsuleIndicator");
        pages.bind(host,target instanceof View?(View)target:null,reference instanceof View?(View)reference:null,refreshed);counts();
    }
    private void capsuleSearch(View host){
        Object value=XposedHelpers.getObjectField(host,"mIndicatorBackground");
        if(!(value instanceof ImageView))return;
        ImageView body=(ImageView)value;
        XposedHelpers.setAdditionalInstanceField(body,MARK,true);
        State state=states.get(body);
        if(!enabled||!search){if(state!=null)restore(body,state);return;}
        applySurface(body,GlassSurface.SEARCH);state=states.get(body);
        if(body.getDrawable()!=null){state.indicatorImage=body.getDrawable();state.indicator=new WeakReference<>(body);body.setImageDrawable(null);}
        if(state.searchBlur==null)state.searchBlur=new SearchBarBlur(body,host,(v,method,args)->XposedHelpers.callMethod(v,method,args),message->{log.accept(message);lastSearchStatus=message;counts();});
        state.searchBlur.useSharedMaterial(!state.nativeBlurManaged);
        state.nativeBlurManaged=true;
        if(host.getBackground()!=null)clearLayer(host,GlassSurface.SEARCH);
    }
    private void quickSearch(View host){
        WeakReference<View> reference=quickTargets.get(host);View previous=reference==null?null:reference.get();
        if(!enabled||!search){if(previous!=null){State state=states.get(previous);if(state!=null)restore(previous,state);}return;}
        View selected=findSearchBody(host,host,0);
        if(previous!=null&&previous!=selected){State state=states.get(previous);if(state!=null)restore(previous,state);XposedHelpers.removeAdditionalInstanceField(previous,MARK);}
        if(previous!=selected){log.accept("Home search resolved: "+selected.getClass().getName()+" "+selected.getWidth()+"x"+selected.getHeight());quickTargets.put(host,new WeakReference<>(selected));}
        State oldLayer=layers.remove(selected);if(oldLayer!=null)restore(selected,oldLayer);
        XposedHelpers.setAdditionalInstanceField(selected,MARK,true);applySurface(selected,GlassSurface.SEARCH);
        for(ViewParent parent=selected.getParent();selected!=host&&parent instanceof View;parent=parent.getParent()){
            View container=(View)parent;if(container.getBackground()!=null&&SearchSurfacePolicy.coversBar(host.getWidth(),host.getHeight(),container.getWidth(),container.getHeight()))clearLayer(container,GlassSurface.SEARCH);
            if(container==host)break;
        }
    }
    private View findSearchBody(View host,View current,int depth){
        View best=current;if(depth>=5||!(current instanceof ViewGroup))return best;
        ViewGroup group=(ViewGroup)current;
        for(int i=0;i<group.getChildCount();i++){
            View child=group.getChildAt(i);if(child.getVisibility()!=View.VISIBLE||child instanceof TextView||child instanceof ImageView)continue;
            if(!SearchSurfacePolicy.coversBar(host.getWidth(),host.getHeight(),child.getWidth(),child.getHeight()))continue;
            View candidate=findSearchBody(host,child,depth+1);if(candidate.getBackground()!=null)best=candidate;
        }
        return best;
    }
    private void clearDescendants(ViewGroup parent){for(int i=0;i<parent.getChildCount();i++){View child=parent.getChildAt(i);if(GlassSurface.drawerLayer(child.getClass().getName()))clearLayer(child,GlassSurface.DRAWER);if(child instanceof ViewGroup)clearDescendants((ViewGroup)child);}}
    private boolean inside(View view,int kind){for(View current=view;current!=null;current=current.getParent() instanceof View?(View)current.getParent():null){if(kind==GlassSurface.SEARCH&&(SearchSurfacePolicy.quickHost(current.getClass().getName())||SearchSurfacePolicy.capsuleHost(current.getClass().getName())))return true;if(kind==GlassSurface.DRAWER&&kind(current)==GlassSurface.DRAWER)return true;}return false;}
    private void clearLayer(View view,int kind){
        State state=layers.get(view);
        if(!enabled||(kind==GlassSurface.SEARCH?!search:!drawer)||!inside(view,kind)){if(state!=null)restore(view,state);return;}
        if(state==null){state=new State();state.kind=kind;layers.put(view,state);}
        if(state.applied&&view.getBackground()==state.ours.get())return;
        state.original=view.getBackground();state.tint=view.getBackgroundTintList();state.mode=view.getBackgroundTintMode();
        Drawable ours=new ColorDrawable(Color.TRANSPARENT);state.ours=new WeakReference<>(ours);background(view,ours);view.setBackgroundTintList(null);state.applied=true;
        log.accept("Cleared structural backdrop kind="+kind+" "+view.getClass().getName());
    }
    private static void background(View view,Drawable drawable){
        boolean relative=view.isPaddingRelative();int start=relative?view.getPaddingStart():view.getPaddingLeft(),end=relative?view.getPaddingEnd():view.getPaddingRight(),top=view.getPaddingTop(),bottom=view.getPaddingBottom();
        view.setBackground(drawable);if(relative)view.setPaddingRelative(start,top,end,bottom);else view.setPadding(start,top,end,bottom);
    }
    private static void restoreDecor(State state){View decor=state.decor.get();if(decor!=null&&state.decorHidden&&decor.getVisibility()==View.INVISIBLE)decor.setVisibility(state.decorVisibility);state.decor.clear();state.decorHidden=false;}
    private void restore(View view,State state){
        if(!state.applied)return;
        if(view.getBackground()==state.ours.get()){background(view,state.original);view.setBackgroundTintList(state.tint);view.setBackgroundTintMode(state.mode);}
        if(state.searchBlur!=null){state.searchBlur.restore();state.searchBlur=null;}
        restoreDecor(state);state.applied=false;state.ours.clear();
        ImageView indicator=state.indicator.get();if(indicator!=null&&indicator.getDrawable()==null)indicator.setImageDrawable(state.indicatorImage);
        state.indicator.clear();state.indicatorImage=null;
        if(state.nativeBlurManaged){state.nativeBlurManaged=false;ViewParent parent=view.getParent();if(parent instanceof View&&SearchSurfacePolicy.capsuleHost(parent.getClass().getName()))try{XposedHelpers.callMethod(parent,"setBgBlurIfSupported",true);}catch(Throwable ignored){}}
log.accept("Surface restored: "+view.getClass().getName());counts();
    }
    private void renderStatus(String message){if(!message.equals(lastRenderStatus)){lastRenderStatus=message;log.accept(message);}}
    void invalidate(){for(View view:new ArrayList<>(states.keySet()))if(view!=null)view.invalidate();}
    private void counts(){
        int docks=0,searches=0,drawers=0;
        for(var entry:states.entrySet())if(entry.getKey()!=null&&entry.getValue().applied&&entry.getKey().isShown()){
            if(entry.getValue().kind==GlassSurface.DOCK)docks++;else if(entry.getValue().kind==GlassSurface.DRAWER)drawers++;else searches++;
        }
        String value="Glass surfaces: "+docks+" visible dock, "+searches+" visible search bar, "+drawers+" glass app drawer; "+pages.visibleCount()+" glass page pill."+(lastSearchStatus.isEmpty()?"":"\nCompact search: "+lastSearchStatus);
        if(!value.equals(lastCounts)){lastCounts=value;report.accept(value);}
    }
}
