package io.github.devarix.kashiglass;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.DrawableWrapper;

/** Native animation code may mutate/recycle its bitmap. Never hand it the cache's bitmap. */
final class OwnedTransitionDrawable extends DrawableWrapper {
    final boolean preserveTransparency;
    OwnedTransitionDrawable(Resources resources,Bitmap cached){this(resources,cached,LiquidGlassDrawable.isScreenMatching());}
    private OwnedTransitionDrawable(Resources resources,Bitmap cached,boolean preserve){super(new BitmapDrawable(resources,copy(cached)));preserveTransparency=preserve;}
    @Override public ConstantState getConstantState(){
        if(!preserveTransparency)return getDrawable().getConstantState();
        Bitmap bitmap=getBitmap();return new ConstantState(){
            @Override public android.graphics.drawable.Drawable newDrawable(){return new OwnedTransitionDrawable(null,bitmap,true);}
            @Override public android.graphics.drawable.Drawable newDrawable(Resources resources){return new OwnedTransitionDrawable(resources,bitmap,true);}
            @Override public int getChangingConfigurations(){return 0;}
        };
    }
    Bitmap getBitmap(){return ((BitmapDrawable)getDrawable()).getBitmap();}
    static Bitmap copy(Bitmap cached){
        if(cached==null||cached.isRecycled())throw new IllegalArgumentException("Unavailable transition snapshot");
        Bitmap copy=cached.copy(Bitmap.Config.ARGB_8888,true);
        if(copy==null)throw new IllegalStateException("Cannot copy transition snapshot");return copy;
    }
}
