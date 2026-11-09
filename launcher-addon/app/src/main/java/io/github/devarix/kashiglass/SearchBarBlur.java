package io.github.devarix.kashiglass;

import android.view.*;
import android.graphics.Outline;
import java.lang.ref.WeakReference;
import java.util.function.Consumer;

/** Requests blur on the existing attached decorative ImageView, never a detached proxy or text view. */
final class SearchBarBlur {
    interface Calls {Object call(View view,String method,Object... args) throws Throwable;}
    private final WeakReference<View> body,host;
    private final Calls calls;
    private final Consumer<String> log;
    private final String role;
    private ViewOutlineProvider outline;
    private boolean clip,saved,active,failed,shared;
    private Integer bodyBackground,bodyView,bodyRadius,hostBackground,hostView;
    private int width=-1,height=-1,radius=-1;
    private String last="";
    SearchBarBlur(View body,View host,Calls calls,Consumer<String> log){this(body,host,calls,log,"SEARCH");}
    SearchBarBlur(View body,View host,Calls calls,Consumer<String> log,String role){this.body=new WeakReference<>(body);this.host=new WeakReference<>(host);this.calls=calls;this.log=log;this.role=role;}
    void update(boolean enabled,int pixels,boolean force){
        View view=body.get(),container=host.get();if(view==null||container==null)return;
        if(!enabled){restore();return;}
        if(!view.isAttachedToWindow()||view.getWidth()<=0||view.getHeight()<=0){status("SEARCH_BLUR pending attached="+view.isAttachedToWindow()+" size="+view.getWidth()+"x"+view.getHeight());return;}
        if((active||failed)&&!force&&radius==pixels&&width==view.getWidth()&&height==view.getHeight())return;
        capture(view,container);
        try{
            // The host owns text/actions; only its separate decorative child receives blur.
            optional(container,"setMiBackgroundBlurMode",0);optional(container,"setMiViewBlurMode",0);optional(container,"clearMiBackgroundBlendColor");
            view.setOutlineProvider(new ViewOutlineProvider(){@Override public void getOutline(View v,Outline out){out.setRoundRect(0,0,v.getWidth(),v.getHeight(),Math.min(v.getWidth(),v.getHeight())*.5f);}});
            view.setClipToOutline(true);
            require(view,"setMiBackgroundBlurMode",1);require(view,"setMiBackgroundBlurRadius",pixels);require(view,"setMiViewBlurMode",1);optional(view,"clearMiBackgroundBlendColor");
            failed=false;active=true;radius=pixels;width=view.getWidth();height=view.getHeight();view.invalidate();
            status("SEARCH_BLUR requested on attached "+view.getClass().getName()+" size="+width+"x"+height+" radius_px="+pixels+" hardware="+view.isHardwareAccelerated()+" background_mode="+read(view,"getMiBackgroundBlurMode")+" view_mode="+read(view,"getMiViewBlurMode")+" radius_readback="+read(view,"getMiBackgroundBlurRadius")+". Native API accepted; visual blur not verified.");
        }catch(Throwable error){
            restore();failed=true;width=view.getWidth();height=view.getHeight();radius=pixels;status("SEARCH_BLUR unavailable: "+error.getClass().getSimpleName()+" "+error.getMessage()+". Transparent rim retained; no wallpaper/gray fill substituted.");
        }
    }
    // The common renderer owns its own backdrop node. Do not stack a second native blur plane.
    void useSharedMaterial(boolean force){
        View view=body.get(),container=host.get();if(view==null||container==null)return;
        if(!view.isAttachedToWindow()||view.getWidth()<=0||view.getHeight()<=0){status(role+"_MATERIAL pending attachment/layout");return;}
        if(shared&&!force)return;
        capture(view,container);shared=true;
        try{
            require(view,"setMiBackgroundBlurMode",0);require(view,"setMiViewBlurMode",0);optional(view,"clearMiBackgroundBlendColor");
            require(container,"setMiBackgroundBlurMode",0);require(container,"setMiViewBlurMode",0);optional(container,"clearMiBackgroundBlendColor");
            status(role+"_MATERIAL shared LiquidGlassDrawable, same pipeline as dock/drawer; native underlay disabled. Device rendering needs verification.");
        }catch(Throwable error){status(role+"_MATERIAL shared renderer installed; native underlay reset unavailable: "+error.getClass().getSimpleName()+" "+error.getMessage());}
        view.invalidate();
    }
    private void capture(View view,View container){
        if(!saved){
            outline=view.getOutlineProvider();clip=view.getClipToOutline();
            bodyBackground=read(view,"getMiBackgroundBlurMode");bodyView=read(view,"getMiViewBlurMode");bodyRadius=read(view,"getMiBackgroundBlurRadius");
            hostBackground=read(container,"getMiBackgroundBlurMode");hostView=read(container,"getMiViewBlurMode");saved=true;
        }
    }
    void restore(){
        View view=body.get(),container=host.get();active=false;failed=false;shared=false;width=height=radius=-1;if(!saved)return;
        if(view!=null){optional(view,"setMiBackgroundBlurMode",bodyBackground==null?0:bodyBackground);optional(view,"setMiViewBlurMode",bodyView==null?0:bodyView);if(bodyRadius!=null)optional(view,"setMiBackgroundBlurRadius",bodyRadius);view.setOutlineProvider(outline);view.setClipToOutline(clip);view.invalidate();}
        if(container!=null){if(hostBackground!=null)optional(container,"setMiBackgroundBlurMode",hostBackground);if(hostView!=null)optional(container,"setMiViewBlurMode",hostView);}
        active=false;saved=false;width=height=radius=-1;
    }
    private Integer read(View view,String method){try{Object value=calls.call(view,method);return value instanceof Number?((Number)value).intValue():null;}catch(Throwable ignored){return null;}}
    private void require(View view,String method,Object... args) throws Throwable {if(Boolean.FALSE.equals(calls.call(view,method,args)))throw new IllegalStateException(method+" rejected");}
    private void optional(View view,String method,Object... args){try{calls.call(view,method,args);}catch(Throwable ignored){}}
    private void status(String message){if(!message.equals(last)){last=message;log.accept(message);}}
}
