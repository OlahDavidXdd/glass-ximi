package io.github.devarix.kashiglass;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Only neutral edges. The attached native background view supplies blur, independent of icon mode. */
final class SearchGlassDrawable extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF outer=new RectF(),inner=new RectF();
    private final Rect cached=new Rect();
    private final float density;
    private Shader edge,inside;
    private int alpha=255;
    SearchGlassDrawable(float density){this.density=density;}
    @Override public void draw(Canvas canvas){
        Rect b=getBounds();if(b.isEmpty()||alpha==0)return;
        float width=Math.max(.7f,Math.min(Math.min(b.width(),b.height())*.009f,1.5f*density));
        if(!cached.equals(b)){
            cached.set(b);outer.set(b);outer.inset(width*.5f,width*.5f);inner.set(outer);inner.inset(Math.min(b.height()*.018f,2.5f*density),Math.min(b.height()*.018f,2.5f*density));
            edge=new LinearGradient(b.left,b.top,b.right,b.bottom,new int[]{0xb8ffffff,0x22ffffff,0x94ffffff},new float[]{0,.5f,1},Shader.TileMode.CLAMP);
            inside=new LinearGradient(b.left,b.top,b.left,b.bottom,0x08ffffff,0x62ffffff,Shader.TileMode.CLAMP);
        }
        int save=canvas.saveLayerAlpha(new RectF(b),alpha);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);paint.setShader(edge);canvas.drawRoundRect(outer,b.height()*.5f,b.height()*.5f,paint);
        paint.setStrokeWidth(Math.max(.5f,width*.5f));paint.setShader(inside);canvas.drawRoundRect(inner,b.height()*.5f,b.height()*.5f,paint);
        canvas.restoreToCount(save);
    }
    @Override public void setAlpha(int value){alpha=Math.max(0,Math.min(255,value));invalidateSelf();}
    @Override public int getAlpha(){return alpha;}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
