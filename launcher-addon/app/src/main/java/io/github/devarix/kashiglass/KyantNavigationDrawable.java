/* Java View adapter for Kyant/AndroidLiquidGlass's Apache-2.0 refraction shader.
 * Copyright 2025 Kyant; adapter Copyright 2026 devarix. See bundled license. */
package io.github.devarix.kashiglass;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.*;
import java.lang.ref.WeakReference;

/** Samples a separate content subtree. The navigation is never captured into itself. */
final class KyantNavigationDrawable extends Drawable {
    private final WeakReference<View> owner,scene;
    private final RenderNode node=new RenderNode("Kyant-navigation-backdrop");
    private final Paint paint=new Paint(3);private final Path outline=new Path();
    private final int[] a=new int[2],b=new int[2];
    private RuntimeShader lens;private boolean unavailable;private int alpha=255,lastW,lastH;
    KyantNavigationDrawable(View owner,View scene){this.owner=new WeakReference<>(owner);this.scene=new WeakReference<>(scene);}
    static boolean safeSource(View owner,View source){for(View v=owner;v!=null;v=v.getParent() instanceof View?(View)v.getParent():null)if(v==source)return false;return source!=null;}
    @Override public void draw(Canvas canvas){
        View view=owner.get(),source=scene.get();Rect bounds=getBounds();if(view==null||source==null||bounds.isEmpty()||alpha==0)return;
        float radius=Math.min(bounds.width(),bounds.height())*.5f,density=view.getResources().getDisplayMetrics().density;
        int saved=canvas.saveLayerAlpha(new RectF(bounds),alpha);outline.reset();outline.addRoundRect(new RectF(bounds),radius,radius,Path.Direction.CW);canvas.clipPath(outline);
        try{
            if(safeSource(view,source)){
                view.getLocationOnScreen(a);source.getLocationOnScreen(b);
                if(canvas.isHardwareAccelerated()&&!unavailable)try{
                    int margin=Math.round(32*density),w=bounds.width()+margin*2,h=bounds.height()+margin*2;
                    if(lens==null)try(var stream=view.getContext().getAssets().open("shaders/kyant_refraction.agsl")){lens=new RuntimeShader(new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}
                    if(w!=lastW||h!=lastH){lastW=w;lastH=h;lens.setFloatUniform("size",(float)bounds.width(),(float)bounds.height());lens.setFloatUniform("offset",-margin,-margin);lens.setFloatUniform("cornerRadii",radius,radius,radius,radius);lens.setFloatUniform("refractionHeight",Math.min(24*density,radius));lens.setFloatUniform("refractionAmount",-24*density);lens.setFloatUniform("depthEffect",0);node.setRenderEffect(RenderEffect.createChainEffect(RenderEffect.createRuntimeShaderEffect(lens,"content"),RenderEffect.createBlurEffect(8*density,8*density,Shader.TileMode.CLAMP)));}
                    Canvas recording=node.beginRecording(w,h);try{recording.drawColor(Color.BLACK);recording.translate(margin+b[0]-a[0]-bounds.left,margin+b[1]-a[1]-bounds.top);source.draw(recording);}finally{node.endRecording();}
                    node.setPosition(bounds.left-margin,bounds.top-margin,bounds.right+margin,bounds.bottom+margin);canvas.drawRenderNode(node);
                }catch(Exception error){unavailable=true;DebugLog.event(view.getContext(),"NAV_BACKDROP_ERROR",error.getClass().getSimpleName());}
                // Software rendering cannot apply this effect. Do not redraw the scene into
                // the navigation: that would expose unblurred text and reenter View drawing.
            }
            paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(!canvas.isHardwareAccelerated()||unavailable?0xf2121212:0x99121212);canvas.drawRoundRect(new RectF(bounds),radius,radius,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(density);paint.setShader(new LinearGradient(bounds.left,bounds.top,bounds.right,bounds.bottom,new int[]{0x70ffffff,0x0cffffff,0x40ffffff},null,Shader.TileMode.CLAMP));RectF edge=new RectF(bounds);edge.inset(density*.5f,density*.5f);canvas.drawRoundRect(edge,radius,radius,paint);paint.setShader(null);
        }finally{canvas.restoreToCount(saved);}
    }
    @Override public void setAlpha(int value){alpha=Math.max(0,Math.min(255,value));invalidateSelf();}@Override public int getAlpha(){return alpha;}
    @Override public void setColorFilter(ColorFilter filter){}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
