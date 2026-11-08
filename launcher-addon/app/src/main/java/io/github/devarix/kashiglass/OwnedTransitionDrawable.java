package io.github.devarix.kashiglass;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

/** Native animation code may mutate/recycle its bitmap. Never hand it the cache's bitmap. */
final class OwnedTransitionDrawable extends BitmapDrawable {
    OwnedTransitionDrawable(Resources resources,Bitmap cached){super(resources,copy(cached));}
    static Bitmap copy(Bitmap cached){
        if(cached==null||cached.isRecycled())throw new IllegalArgumentException("Unavailable transition snapshot");
        Bitmap copy=cached.copy(Bitmap.Config.ARGB_8888,true);
        if(copy==null)throw new IllegalStateException("Cannot copy transition snapshot");return copy;
    }
}
