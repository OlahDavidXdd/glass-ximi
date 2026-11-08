package io.github.devarix.kashiglass;

import android.graphics.*;
import android.graphics.drawable.*;

final class AutoGlyph {
    private AutoGlyph(){}
    static Bitmap capture(Drawable original){
        if(original==null||original instanceof LiquidGlassDrawable)return null;
        Drawable source=original;
        if(original instanceof AdaptiveIconDrawable){
            AdaptiveIconDrawable adaptive=(AdaptiveIconDrawable)original;
            source=adaptive.getMonochrome()!=null?adaptive.getMonochrome():adaptive.getForeground();
        }
        if(source==null)return null;
        Drawable.ConstantState constant=source.getConstantState();if(constant!=null)source=constant.newDrawable().mutate();
        if(source instanceof BitmapDrawable&&((BitmapDrawable)source).getBitmap().getConfig()==Bitmap.Config.HARDWARE){
            Bitmap copy=((BitmapDrawable)source).getBitmap().copy(Bitmap.Config.ARGB_8888,false);if(copy==null)return null;source=new BitmapDrawable(copy);
        }
        Rect old=new Rect(source.getBounds());Bitmap bitmap=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);
        try{source.setBounds(0,0,128,128);source.draw(new Canvas(bitmap));}finally{source.setBounds(old);}
        return bitmap;
    }
    static Bitmap generate(Bitmap source){
        int size=source.getWidth();int[] pixels=new int[size*size];source.getPixels(pixels,0,size,0,0,size,size);
        pixels=AutoIconPixels.silver(pixels,size);
        Bitmap clean=Bitmap.createBitmap(pixels,size,size,Bitmap.Config.ARGB_8888);
        int left=size,top=size,right=0,bottom=0;
        for(int y=0;y<size;y++)for(int x=0;x<size;x++)if((pixels[y*size+x]>>>24)>12){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x+1);bottom=Math.max(bottom,y+1);}
        if(right<=left||bottom<=top){left=top=0;right=bottom=size;}
        Bitmap glyph=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(glyph);
        float scale=174f/Math.max(right-left,bottom-top),w=(right-left)*scale,h=(bottom-top)*scale;
        canvas.drawBitmap(clean,new Rect(left,top,right,bottom),new RectF((256-w)*.5f,(256-h)*.5f,(256+w)*.5f,(256+h)*.5f),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
        clean.recycle();return glyph;
    }
}
