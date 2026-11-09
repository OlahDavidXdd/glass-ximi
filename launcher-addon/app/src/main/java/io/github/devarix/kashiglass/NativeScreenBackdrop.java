package io.github.devarix.kashiglass;

import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import java.lang.reflect.*;
import java.util.function.Consumer;

/** A dedicated blur node beneath the glyph, without clipping labels or adding layout children. */
final class NativeScreenBackdrop {
    interface Access {Object call(Object target,String name,Object... args) throws Throwable;Object field(Object target,String name) throws Throwable;}
    private static Access access=new ReflectiveAccess();
    static void setAccess(Access value){access=value;}
    private View host;
    private RenderNode node;
    private boolean failed,reported;
    private int width,height,blur=-1;
    boolean draw(Canvas canvas,View owner,Rect bounds,float corner,int radius,int alpha,Consumer<String> status){
        if(failed||!canvas.isHardwareAccelerated()||!owner.isAttachedToWindow())return false;
        try{
            if(host==null){
                host=new View(owner.getContext().getApplicationContext());host.setBackground(new ColorDrawable(Color.TRANSPARENT));
                NativeBackdropRequest.configure(access,host);
                Object candidate=access.field(host,"mRenderNode");if(!(candidate instanceof RenderNode))throw new IllegalStateException("Native blur render node unavailable");node=(RenderNode)candidate;
            }
            if(width!=bounds.width()||height!=bounds.height()||blur!=radius){
                width=bounds.width();height=bounds.height();blur=radius;host.layout(0,0,width,height);
                require(access.call(host,"setMiBackgroundBlurRadius",radius));
                Outline outline=new Outline();outline.setRoundRect(0,0,width,height,corner);node.setOutline(outline);node.setClipToOutline(true);node.setClipToBounds(true);
                Canvas recording=node.beginRecording(width,height);try{recording.drawColor(Color.TRANSPARENT,BlendMode.SRC_OVER);}finally{node.endRecording();}
            }
            node.setPosition(bounds.left,bounds.top,bounds.right,bounds.bottom);node.setAlpha(alpha/255f);canvas.drawRenderNode(node);
            if(!reported){reported=true;status.accept("Native backdrop requested; supported mode getters were checked. Proxy is a dedicated render node. Actual scene sampling still needs phone verification; no wallpaper tile is substituted.");}return true;
        }catch(Throwable e){failed=true;host=null;node=null;status.accept("Native screen blur unavailable: "+e.getClass().getSimpleName()+". Using an untinted transparent glass rim; no mismatched wallpaper fill.");return false;}
    }
    private static void require(Object result){if(Boolean.FALSE.equals(result))throw new IllegalStateException("Native blur request rejected");}
    private static final class ReflectiveAccess implements Access {
        public Object field(Object target,String name) throws Throwable {for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{Field field=type.getDeclaredField(name);field.setAccessible(true);return field.get(target);}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
        public Object call(Object target,String name,Object... args) throws Throwable {
            Class<?>[] types=new Class<?>[args.length];for(int i=0;i<args.length;i++)types[i]=args[i] instanceof Integer?int.class:args[i].getClass();
            for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{Method method=type.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(target,args);}catch(NoSuchMethodException ignored){}throw new NoSuchMethodException(name);
        }
    }
}
