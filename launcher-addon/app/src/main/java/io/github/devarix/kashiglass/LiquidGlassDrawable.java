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
    private static boolean whiteSymbols=true;
    private static final ColorFilter WHITE=new ColorMatrixColorFilter(new ColorMatrix(new float[]{0,0,0,0,255, 0,0,0,0,255, 0,0,0,0,255, .2126f,.7152f,.0722f,1,-255}));
    static void configure(String shader,Bitmap fallback){configure(shader,fallback,true);}
    static void configure(String shader,Bitmap fallback,boolean white){source=shader;plate=fallback;whiteSymbols=white;}
    private final WeakReference<View> owner;
    private final Bitmap glyph;
    private final int surface;
    private final GlassBackdrop backdrop;
    private final Consumer<String> status;
    private final boolean blur;
    private final int strength;
    private final RenderNode node=new RenderNode("devarix-QWEA0-Lens");
    private final Paint foreground=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint symbol=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final boolean white=whiteSymbols;
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
    LiquidGlassDrawable(View view,Bitmap glyph,GlassBackdrop backdrop,boolean blur,int strength,Consumer<String> status,int surface){this.surface=surface;this.owner=new WeakReference<>(view);this.glyph=glyph;this.backdrop=backdrop;this.blur=blur;this.strength=strength;this.status=status;symbol.setColorFilter(white?WHITE:null);}
    @Override public void draw(Canvas canvas){
        Rect b=getBounds();if(b.isEmpty())return;View view=owner.get();if(view==null)return;
        if(!canvas.isHardwareAccelerated()&&!reportedSoftware){reportedSoftware=true;status.accept("QWEA0 lens unavailable on a software canvas. Static fallback only.");}
        queuePoll(view);lastPose=pose(view);
        int saved=alpha==255?canvas.save():canvas.saveLayerAlpha(new RectF(b),alpha);
        try{
            boolean drawn=false;
            if(!broken&&canvas.isHardwareAccelerated()&&backdrop.isAvailable())try{drawn=drawLens(canvas,view,b);}catch(Throwable e){broken=true;status.accept("QWEA0 lens unavailable on this driver: "+e.getClass().getSimpleName()+". Showing the clearly marked static fallback.");}
            if(!drawn){
                if(surface==GlassSurface.ICON&&plate!=null){target.set(b);int savedAlpha=foreground.getAlpha();foreground.setAlpha(160);canvas.drawBitmap(plate,null,target,foreground);foreground.setAlpha(savedAlpha);}
                else if(surface!=GlassSurface.ICON){glass.setShader(null);glass.setColor(0x38636363);float r=GlassSurface.radius(b.width(),b.height(),surface,view.getResources().getDisplayMetrics().density);canvas.drawRoundRect(new RectF(b),r,r,glass);glass.setColor(Color.WHITE);}
            }
            drawSurface(canvas,b,view.isPressed());
            if(glyph!=null){target.set(b);canvas.drawBitmap(glyph,null,target,symbol);}
        }finally{canvas.restoreToCount(saved);}
    }
    private boolean drawLens(Canvas canvas,View view,Rect b){
        if(source==null)return false;
        if(shader==null)shader=new RuntimeShader(source);
        int w=b.width(),h=b.height();float small=Math.min(w,h),density=view.getResources().getDisplayMetrics().density,edge=surface==GlassSurface.DRAWER?Math.min(small,160*density):small,scale=BackdropGeometry.displayScale(view);
        float blurRadius=blur?(surface==GlassSurface.DRAWER?Math.min(28*density,strength*density*.55f)/scale:Math.min(small*.065f,strength*density*.11f/scale)):0;
        int margin=(int)Math.ceil(Math.max(edge*.35f,blurRadius*3+2));
        float press=0;for(int state:getState())if(state==android.R.attr.state_pressed)press=1;
        if(w!=lastW||h!=lastH||margin!=lastMargin||lastBlur!=blurRadius||press!=lastPress){
            float r=GlassSurface.radius(w,h,surface,density);
            shader.setFloatUniform("margin",(float)margin);shader.setFloatUniform("viewSize",(float)w,(float)h);
            shader.setFloatUniform("shape1",w*.5f,h*.5f,w*.5f-.5f,h*.5f-.5f);shader.setFloatUniform("shape1L",w*.5f,h*.5f,w*.5f-.5f,h*.5f-.5f);
            shader.setFloatUniform("radii1",r,r,r,r);shader.setFloatUniform("rimSoft",.7f);
            shader.setFloatUniform("shape2",0,0,0,0);shader.setFloatUniform("radius2",0);shader.setFloatUniform("blendK",0);
            shader.setFloatUniform("bevel",edge*.15f);shader.setFloatUniform("refractPx",edge*.24f);shader.setFloatUniform("falloff",2.1f);shader.setFloatUniform("refractDir",-1);
            shader.setFloatUniform("sampleLo",margin+1f,margin+1f);shader.setFloatUniform("sampleHi",margin+w-1f,margin+h-1f);
            shader.setFloatUniform("dispersion",0);shader.setFloatUniform("lightDir",-.866f,-.5f);shader.setFloatUniform("specStrength",1.12f);shader.setFloatUniform("rimBandMax",Math.max(2,edge*.032f));
            shader.setFloatUniform("tintColor",1,1,1,.025f);shader.setFloatUniform("adaptiveTint",0);shader.setFloatUniform("glassTint",1,1,1,.012f);shader.setFloatUniform("dimAmount",.02f);shader.setFloatUniform("satFactor",0);
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
        float size=Math.min(bounds.width(),bounds.height()),r=GlassSurface.radius(bounds.width(),bounds.height(),surface,owner.get()==null?1:owner.get().getResources().getDisplayMetrics().density);
        RectF body=new RectF(bounds);int saved=canvas.save();Path clip=new Path();clip.addRoundRect(body,r,r,Path.Direction.CW);canvas.clipPath(clip);
        glass.setStyle(Paint.Style.FILL);
        glass.setShader(new LinearGradient(body.left,body.top,body.right,body.bottom,new int[]{0x2cffffff,0x08ffffff,0x03484848,0x14000000},new float[]{0,.24f,.65f,1},Shader.TileMode.CLAMP));
        canvas.drawRoundRect(body,r,r,glass);
        glass.setShader(new RadialGradient(body.left+size*.16f,body.top+size*.02f,size*.66f,new int[]{pressed?0x68ffffff:0x48ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));
        if(surface==GlassSurface.DRAWER){glass.setShader(null);glass.setColor(0x22484848);canvas.drawRoundRect(body,r,r,glass);glass.setColor(Color.WHITE);}
        else{
            float w=body.width(),h=body.height();Path reflection=new Path();reflection.moveTo(body.left-w*.05f,body.top-h*.05f);reflection.lineTo(body.right+w*.05f,body.top-h*.05f);reflection.lineTo(body.right+w*.05f,body.top+h*.12f);
            reflection.cubicTo(body.left+w*.72f,body.top+h*.36f,body.left+w*.30f,body.top+h*.37f,body.left-w*.05f,body.top+h*.19f);reflection.close();
            glass.setShader(new LinearGradient(body.left,body.top,body.left,body.top+h*.40f,new int[]{pressed?0x50ffffff:0x38ffffff,0x08ffffff},null,Shader.TileMode.CLAMP));canvas.drawPath(reflection,glass);
        }
        glass.setStyle(Paint.Style.STROKE);glass.setStrokeWidth(Math.max(.8f,surface==GlassSurface.DRAWER?Math.min(size*.009f,4.5f):size*.009f));body.inset(glass.getStrokeWidth()*.5f,glass.getStrokeWidth()*.5f);
        glass.setShader(new LinearGradient(body.left,body.top,body.right,body.bottom,new int[]{0xd8ffffff,0x40ffffff,0xa0ffffff,0x38000000},new float[]{0,.40f,.76f,1},Shader.TileMode.CLAMP));
        canvas.drawRoundRect(body,r,r,glass);
        float inner=surface==GlassSurface.DRAWER?Math.min(size*.018f,7.5f):size*.018f;body.inset(inner,inner);glass.setStrokeWidth(Math.max(.6f,size*.005f));
        glass.setShader(new LinearGradient(body.left,body.top,body.left,body.bottom,0x10ffffff,0x72ffffff,Shader.TileMode.CLAMP));canvas.drawRoundRect(body,r-size*.02f,r-size*.02f,glass);
        glass.setShader(null);glass.setStyle(Paint.Style.FILL);canvas.restoreToCount(saved);
    }
    Bitmap glyph(){return glyph;}
    static Bitmap coldTransition(Bitmap glyph){
        LiquidGlassDrawable detached=new LiquidGlassDrawable(null,glyph,null,false,24,message->{});
        Bitmap bitmap=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);Rect bounds=new Rect(0,0,192,192);
        if(plate!=null){detached.foreground.setAlpha(160);canvas.drawBitmap(plate,null,bounds,detached.foreground);detached.foreground.setAlpha(255);}
        detached.drawSurface(canvas,bounds,false);canvas.drawBitmap(glyph,null,bounds,detached.symbol);return bitmap;
    }
    Bitmap transitionBitmap(){
        View view=owner.get();if(view==null)return null;
        LiquidGlassDrawable frozen=new LiquidGlassDrawable(view,glyph,backdrop,blur,strength,status,surface);
        Rect bounds=new Rect(getBounds());if(bounds.isEmpty())bounds.set(0,0,192,192);frozen.setBounds(bounds);return frozen.renderSnapshot();
    }
    private Bitmap renderSnapshot(){
        int size=192;Rect old=new Rect(getBounds());
        try{
            RenderNode capture=new RenderNode("devarix-transition");capture.setPosition(0,0,size,size);
            Canvas recording=capture.beginRecording(size,size);try{recording.scale((float)size/old.width(),(float)size/old.height());recording.translate(-old.left,-old.top);draw(recording);}finally{capture.endRecording();}
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
            Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);Canvas fallback=new Canvas(bitmap);fallback.scale((float)size/old.width(),(float)size/old.height());fallback.translate(-old.left,-old.top);
            if(plate!=null){int savedAlpha=foreground.getAlpha();foreground.setAlpha(160);fallback.drawBitmap(plate,null,old,foreground);foreground.setAlpha(savedAlpha);}
            drawSurface(fallback,old,false);if(glyph!=null)fallback.drawBitmap(glyph,null,old,symbol);return bitmap;
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
