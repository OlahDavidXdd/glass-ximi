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
    private final int surface;
    private final GlassBackdrop backdrop;
    private final Consumer<String> status;
    private final boolean blur;
    private final int strength;
    private final RenderNode node=new RenderNode("devarix-QWEA0-Lens");
    private final Paint foreground=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF target=new RectF();
    private RuntimeShader shader;
    private final Paint glass=new Paint(Paint.ANTI_ALIAS_FLAG);
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
    LiquidGlassDrawable(View view,Bitmap glyph,GlassBackdrop backdrop,boolean blur,int strength,Consumer<String> status){this(view,glyph,backdrop,blur,strength,status,GlassSurface.ICON);}
    LiquidGlassDrawable(View view,Bitmap glyph,GlassBackdrop backdrop,boolean blur,int strength,Consumer<String> status,int surface){this.surface=surface;this.owner=new WeakReference<>(view);this.glyph=glyph;this.backdrop=backdrop;this.blur=blur;this.strength=strength;this.status=status;}
    @Override public void draw(Canvas canvas){
        Rect b=getBounds();if(b.isEmpty())return;View view=owner.get();if(view==null)return;
        if(!canvas.isHardwareAccelerated()&&!reportedSoftware){reportedSoftware=true;status.accept("QWEA0 lens unavailable on a software canvas. Static fallback only.");}
        queuePoll(view);lastPose=pose(view);
        int saved=alpha==255?canvas.save():canvas.saveLayerAlpha(new RectF(b),alpha);
        try{
            boolean drawn=false;
            if(!broken&&canvas.isHardwareAccelerated()&&backdrop.isAvailable())try{drawn=drawLens(canvas,view,b);}catch(Throwable e){broken=true;status.accept("QWEA0 lens unavailable on this driver: "+e.getClass().getSimpleName()+". Showing the clearly marked static fallback.");}
            if(!drawn){
                if(surface==GlassSurface.ICON&&plate!=null){target.set(b);canvas.drawBitmap(plate,null,target,foreground);}
                else if(surface!=GlassSurface.ICON){glass.setShader(null);glass.setColor(0x60636363);float r=GlassSurface.radius(b.width(),b.height(),surface);canvas.drawRoundRect(new RectF(b),r,r,glass);glass.setColor(Color.WHITE);}
            }
            drawSurface(canvas,b,view.isPressed());
            if(glyph!=null){target.set(b);canvas.drawBitmap(glyph,null,target,foreground);}
        }finally{canvas.restoreToCount(saved);}
    }
    private boolean drawLens(Canvas canvas,View view,Rect b){
        if(source==null)return false;
        if(shader==null)shader=new RuntimeShader(source);
        int w=b.width(),h=b.height();float small=Math.min(w,h),scale=BackdropGeometry.displayScale(view);
        float blurRadius=blur?Math.min(small*.075f,strength*view.getResources().getDisplayMetrics().density*.12f/scale):0;
        int margin=(int)Math.ceil(Math.max(small*.38f,blurRadius*3+2));
        float press=0;for(int state:getState())if(state==android.R.attr.state_pressed)press=1;
        if(w!=lastW||h!=lastH||margin!=lastMargin||lastBlur!=blurRadius||press!=lastPress){
            float r=GlassSurface.radius(w,h,surface);
            shader.setFloatUniform("margin",(float)margin);shader.setFloatUniform("viewSize",(float)w,(float)h);
            shader.setFloatUniform("shape1",w*.5f,h*.5f,w*.5f-.5f,h*.5f-.5f);shader.setFloatUniform("shape1L",w*.5f,h*.5f,w*.5f-.5f,h*.5f-.5f);
            shader.setFloatUniform("radii1",r,r,r,r);shader.setFloatUniform("rimSoft",.7f);
            shader.setFloatUniform("shape2",0,0,0,0);shader.setFloatUniform("radius2",0);shader.setFloatUniform("blendK",0);
            shader.setFloatUniform("bevel",small*.19f);shader.setFloatUniform("refractPx",small*.32f);shader.setFloatUniform("falloff",2.3f);shader.setFloatUniform("refractDir",-1);
            shader.setFloatUniform("sampleLo",margin+1f,margin+1f);shader.setFloatUniform("sampleHi",margin+w-1f,margin+h-1f);
            shader.setFloatUniform("dispersion",0);shader.setFloatUniform("lightDir",-.866f,-.5f);shader.setFloatUniform("specStrength",1.35f);shader.setFloatUniform("rimBandMax",Math.max(2,small*.045f));
            shader.setFloatUniform("tintColor",1,1,1,.035f);shader.setFloatUniform("adaptiveTint",0);shader.setFloatUniform("glassTint",1,1,1,.025f);shader.setFloatUniform("dimAmount",.055f);shader.setFloatUniform("satFactor",0);
            shader.setFloatUniform("press",press);shader.setFloatUniform("touchPos",w*.5f,h*.5f);shader.setFloatUniform("touchAmp",0);
            RenderEffect lens=RenderEffect.createRuntimeShaderEffect(shader,"content");
            node.setRenderEffect(blurRadius>.01f?RenderEffect.createChainEffect(lens,RenderEffect.createBlurEffect(blurRadius,blurRadius,Shader.TileMode.CLAMP)):lens);
            lastW=w;lastH=h;lastMargin=margin;lastBlur=blurRadius;lastPress=press;
        }
        int rw=w+2*margin,rh=h+2*margin;
        node.setPosition(b.left-margin,b.top-margin,b.right+margin,b.bottom+margin);
        Canvas recording=node.beginRecording(rw,rh);boolean captured;
        try{captured=surface==GlassSurface.ICON?backdrop.draw(recording,view,b,margin):backdrop.drawSurface(recording,view,b,margin);}finally{node.endRecording();}
        if(!captured)return false;
        canvas.drawRenderNode(node);
        status.accept("QWEA0 refraction/blur render commands submitted using a static wallpaper. Physical-device appearance still needs verification.");return true;
    }
    private void drawSurface(Canvas canvas,Rect bounds,boolean pressed){
        float size=Math.min(bounds.width(),bounds.height()),r=GlassSurface.radius(bounds.width(),bounds.height(),surface);
        RectF body=new RectF(bounds);int saved=canvas.save();Path clip=new Path();clip.addRoundRect(body,r,r,Path.Direction.CW);canvas.clipPath(clip);
        glass.setStyle(Paint.Style.FILL);
        glass.setShader(new LinearGradient(body.left,body.top,body.right,body.bottom,new int[]{0x35ffffff,0x04ffffff,0x00ffffff,0x17000000},new float[]{0,.24f,.65f,1},Shader.TileMode.CLAMP));
        canvas.drawRoundRect(body,r,r,glass);
        glass.setShader(new RadialGradient(body.left+size*.16f,body.top+size*.02f,size*.66f,new int[]{pressed?0x68ffffff:0x48ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));
        canvas.drawOval(body.left-size*.18f,body.top-size*.35f,body.right+size*.12f,body.top+size*.35f,glass);
        glass.setStyle(Paint.Style.STROKE);glass.setStrokeWidth(Math.max(.8f,size*.012f));body.inset(glass.getStrokeWidth()*.5f,glass.getStrokeWidth()*.5f);
        glass.setShader(new LinearGradient(body.left,body.top,body.right,body.bottom,new int[]{0xc0ffffff,0x20ffffff,0x70ffffff,0x24000000},new float[]{0,.40f,.76f,1},Shader.TileMode.CLAMP));
        canvas.drawRoundRect(body,r,r,glass);
        body.inset(size*.023f,size*.023f);glass.setStrokeWidth(Math.max(.6f,size*.006f));
        glass.setShader(new LinearGradient(body.left,body.top,body.left,body.bottom,0x10ffffff,0x72ffffff,Shader.TileMode.CLAMP));canvas.drawRoundRect(body,r-size*.02f,r-size*.02f,glass);
        glass.setShader(null);glass.setStyle(Paint.Style.FILL);canvas.restoreToCount(saved);
    }
    Bitmap glyph(){return glyph;}
    Bitmap transitionBitmap(){
        int size=192;Rect old=new Rect(getBounds());setBounds(0,0,size,size);
        try{
            RenderNode capture=new RenderNode("devarix-transition");capture.setPosition(0,0,size,size);
            Canvas recording=capture.beginRecording(size,size);try{draw(recording);}finally{capture.endRecording();}
            HardwareRenderer renderer=new HardwareRenderer();
            try(android.media.ImageReader reader=android.media.ImageReader.newInstance(size,size,PixelFormat.RGBA_8888,2,android.hardware.HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE|android.hardware.HardwareBuffer.USAGE_GPU_COLOR_OUTPUT)){
                renderer.setOpaque(false);renderer.setSurface(reader.getSurface());renderer.setContentRoot(capture);
                renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw();
                try(android.media.Image image=reader.acquireLatestImage()){
                    if(image!=null)try(android.hardware.HardwareBuffer buffer=image.getHardwareBuffer()){
                        if(buffer!=null){Bitmap hardware=Bitmap.wrapHardwareBuffer(buffer,ColorSpace.get(ColorSpace.Named.SRGB));
                            if(hardware!=null){Bitmap copy=hardware.copy(Bitmap.Config.ARGB_8888,false);hardware.recycle();if(copy!=null)return copy;}
                        }
                    }
                }
            }catch(RuntimeException ignored){
                // Keep a recognizable glass transition when GPU readback is unavailable.
            }finally{renderer.destroy();capture.discardDisplayList();}
            Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);Canvas fallback=new Canvas(bitmap);
            if(plate!=null)fallback.drawBitmap(plate,null,new Rect(0,0,size,size),foreground);
            drawSurface(fallback,new Rect(0,0,size,size),false);if(glyph!=null)fallback.drawBitmap(glyph,null,new Rect(0,0,size,size),foreground);return bitmap;
        }finally{setBounds(old);}
    }
    private void queuePoll(View view){if(!pollQueued&&view.isAttachedToWindow()&&view.getWindowVisibility()==View.VISIBLE&&isVisible()&&getCallback()!=null){pollQueued=true;scheduleSelf(poll,SystemClock.uptimeMillis()+33);}}
    private long pose(View view){view.getLocationOnScreen(position);long hash=position[0]*31L+position[1];hash=hash*31+Float.floatToIntBits(view.getScaleX());hash=hash*31+Float.floatToIntBits(view.getScaleY());if(view instanceof android.widget.ImageView){((android.widget.ImageView)view).getImageMatrix().getValues(matrixValues);for(float v:matrixValues)hash=hash*31+Float.floatToIntBits(v);}return hash;}
    @Override public boolean setVisible(boolean visible,boolean restart){if(!visible){unscheduleSelf(poll);pollQueued=false;}return super.setVisible(visible,restart);}
    @Override public int getIntrinsicWidth(){return glyph==null?-1:glyph.getWidth();}
    @Override public int getIntrinsicHeight(){return glyph==null?-1:glyph.getHeight();}
    @Override public boolean isStateful(){return true;}
    @Override protected boolean onStateChange(int[] state){invalidateSelf();return true;}
    @Override public void setAlpha(int alpha){this.alpha=alpha;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){foreground.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
