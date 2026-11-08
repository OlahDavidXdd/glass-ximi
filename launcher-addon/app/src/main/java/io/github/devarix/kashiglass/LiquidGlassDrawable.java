/*
 * Lens shader/effect chain adapted from QWEA0/Liquid-Glass-Android,
 * Copyright (c) 2025-2026 pandadog, MIT. See assets/licenses/Liquid-Glass-Android-MIT.txt.
 * Wallpaper-backed Drawable adapter Copyright (c) 2026 devarix, MIT.
 */
package io.github.devarix.kashiglass;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.os.SystemClock;
import java.lang.ref.WeakReference;
import java.util.function.Consumer;

final class LiquidGlassDrawable extends Drawable {
    private static String source;
    private static Bitmap plate;
    static void configure(String shader,Bitmap fallback){source=shader;plate=fallback;}
    private final WeakReference<View> owner;
    private final Bitmap glyph;
    private final GlassBackdrop backdrop;
    private final Consumer<String> status;
    private final boolean blur;
    private final int strength;
    private final RenderNode node=new RenderNode("devarix-QWEA0-Lens");
    private final Paint foreground=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF target=new RectF();
    private RuntimeShader shader;
    private boolean broken,pollQueued,reportedSoftware;
    private final int[] position=new int[2];
    private final float[] matrixValues=new float[9];
    private long lastPose;
    private final Runnable poll=new Runnable(){@Override public void run(){
        pollQueued=false;View view=owner.get();if(view==null||!view.isAttachedToWindow()||view.getWindowVisibility()!=View.VISIBLE||!isVisible()||getCallback()==null)return;
        long pose=pose(view);if(pose!=lastPose){lastPose=pose;invalidateSelf();}
        queuePoll(view);
    }};
    private int alpha=255,lastW,lastH,lastMargin;
    private float lastBlur=-1,lastPress=-1;
    LiquidGlassDrawable(View view,Bitmap glyph,GlassBackdrop backdrop,boolean blur,int strength,Consumer<String> status){this.owner=new WeakReference<>(view);this.glyph=glyph;this.backdrop=backdrop;this.blur=blur;this.strength=strength;this.status=status;}
    @Override public void draw(Canvas canvas){
        Rect b=getBounds();if(b.isEmpty())return;View view=owner.get();if(view==null)return;
        if(!canvas.isHardwareAccelerated()&&!reportedSoftware){reportedSoftware=true;status.accept("QWEA0 lens unavailable on a software canvas. Static fallback only.");}
        queuePoll(view);lastPose=pose(view);
        int saved=alpha==255?canvas.save():canvas.saveLayerAlpha(new RectF(b),alpha);
        try{
            boolean drawn=false;
            if(!broken&&canvas.isHardwareAccelerated()&&backdrop.isAvailable())try{drawn=drawLens(canvas,view,b);}catch(Throwable e){broken=true;status.accept("QWEA0 lens unavailable on this driver: "+e.getClass().getSimpleName()+". Showing the clearly marked static fallback.");}
            if(!drawn&&plate!=null){target.set(b);canvas.drawBitmap(plate,null,target,foreground);}
            target.set(b);canvas.drawBitmap(glyph,null,target,foreground);
        }finally{canvas.restoreToCount(saved);}
    }
    private boolean drawLens(Canvas canvas,View view,Rect b){
        if(source==null)return false;
        if(shader==null)shader=new RuntimeShader(source);
        int w=b.width(),h=b.height();float small=Math.min(w,h),scale=BackdropGeometry.displayScale(view);
        float blurRadius=blur?Math.min(small*.12f,strength*view.getResources().getDisplayMetrics().density*.18f/scale):0;
        int margin=(int)Math.ceil(Math.max(small*.28f,blurRadius*3+2));
        float press=0;for(int state:getState())if(state==android.R.attr.state_pressed)press=1;
        if(w!=lastW||h!=lastH||margin!=lastMargin||lastBlur!=blurRadius||press!=lastPress){
            float r=small*.27f;
            shader.setFloatUniform("margin",(float)margin);shader.setFloatUniform("viewSize",(float)w,(float)h);
            shader.setFloatUniform("shape1",w*.5f,h*.5f,w*.5f-.5f,h*.5f-.5f);shader.setFloatUniform("shape1L",w*.5f,h*.5f,w*.5f-.5f,h*.5f-.5f);
            shader.setFloatUniform("radii1",r,r,r,r);shader.setFloatUniform("rimSoft",.7f);
            shader.setFloatUniform("shape2",0,0,0,0);shader.setFloatUniform("radius2",0);shader.setFloatUniform("blendK",0);
            shader.setFloatUniform("bevel",small*.14f);shader.setFloatUniform("refractPx",small*.20f);shader.setFloatUniform("falloff",2);shader.setFloatUniform("refractDir",-1);
            shader.setFloatUniform("sampleLo",margin+1f,margin+1f);shader.setFloatUniform("sampleHi",margin+w-1f,margin+h-1f);
            shader.setFloatUniform("dispersion",0);shader.setFloatUniform("lightDir",-.866f,-.5f);shader.setFloatUniform("specStrength",.9f);shader.setFloatUniform("rimBandMax",Math.max(2,small*.03f));
            shader.setFloatUniform("tintColor",1,1,1,.06f);shader.setFloatUniform("adaptiveTint",0);shader.setFloatUniform("glassTint",1,1,1,.05f);shader.setFloatUniform("dimAmount",.1f);shader.setFloatUniform("satFactor",0);
            shader.setFloatUniform("press",press);shader.setFloatUniform("touchPos",w*.5f,h*.5f);shader.setFloatUniform("touchAmp",0);
            RenderEffect lens=RenderEffect.createRuntimeShaderEffect(shader,"content");
            node.setRenderEffect(blurRadius>.01f?RenderEffect.createChainEffect(lens,RenderEffect.createBlurEffect(blurRadius,blurRadius,Shader.TileMode.CLAMP)):lens);
            lastW=w;lastH=h;lastMargin=margin;lastBlur=blurRadius;lastPress=press;
        }
        int rw=w+2*margin,rh=h+2*margin;
        node.setPosition(b.left-margin,b.top-margin,b.right+margin,b.bottom+margin);
        Canvas recording=node.beginRecording(rw,rh);boolean captured;
        try{captured=backdrop.draw(recording,view,b,margin);}finally{node.endRecording();}
        if(!captured)return false;
        canvas.drawRenderNode(node);
        status.accept("QWEA0 refraction/blur render commands submitted using a static wallpaper. Physical-device appearance still needs verification.");return true;
    }
    private void queuePoll(View view){if(!pollQueued&&view.isAttachedToWindow()&&view.getWindowVisibility()==View.VISIBLE&&isVisible()&&getCallback()!=null){pollQueued=true;scheduleSelf(poll,SystemClock.uptimeMillis()+33);}}
    private long pose(View view){view.getLocationOnScreen(position);long hash=position[0]*31L+position[1];hash=hash*31+Float.floatToIntBits(view.getScaleX());hash=hash*31+Float.floatToIntBits(view.getScaleY());if(view instanceof android.widget.ImageView){((android.widget.ImageView)view).getImageMatrix().getValues(matrixValues);for(float v:matrixValues)hash=hash*31+Float.floatToIntBits(v);}return hash;}
    @Override public boolean setVisible(boolean visible,boolean restart){if(!visible){unscheduleSelf(poll);pollQueued=false;}return super.setVisible(visible,restart);}
    @Override public int getIntrinsicWidth(){return glyph.getWidth();}
    @Override public int getIntrinsicHeight(){return glyph.getHeight();}
    @Override public boolean isStateful(){return true;}
    @Override protected boolean onStateChange(int[] state){invalidateSelf();return true;}
    @Override public void setAlpha(int alpha){this.alpha=alpha;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){foreground.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
