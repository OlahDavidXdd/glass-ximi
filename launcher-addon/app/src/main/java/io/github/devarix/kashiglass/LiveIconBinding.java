package io.github.devarix.kashiglass;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.lang.ref.WeakReference;
import java.util.function.Function;

/** Trusted artwork survives native field replacement; native pixels never become cache input. */
final class LiveIconBinding {
    interface Slot { Drawable get(); void set(Drawable drawable); }
    private String key;
    private Bitmap glyph;
    private WeakReference<Drawable> live=new WeakReference<>(null);
    private int generation;
    Bitmap trusted(String requested){return requested!=null&&requested.equals(key)&&glyph!=null&&!glyph.isRecycled()?glyph:null;}
    boolean owns(Drawable drawable){
        if(drawable==null||drawable!=live.get())return false;
        if(drawable instanceof BitmapDrawable){Bitmap bitmap=((BitmapDrawable)drawable).getBitmap();return !bitmap.isRecycled()&&bitmap.getGenerationId()==generation;}
        return true;
    }
    void installed(String requested,Bitmap trusted,Drawable drawable){
        key=requested;glyph=trusted;live=new WeakReference<>(drawable);
        generation=drawable instanceof BitmapDrawable?((BitmapDrawable)drawable).getBitmap().getGenerationId():0;
    }
    boolean repair(String requested,Bitmap fallback,Slot slot,Function<Bitmap,Drawable> factory){
        Bitmap source=trusted(requested);if(source==null)source=fallback;
        if(source==null||source.isRecycled())return false;
        if(trusted(requested)!=null&&owns(slot.get()))return false;
        Drawable replacement=factory.apply(source);slot.set(replacement);installed(requested,source,replacement);return true;
    }
    void clear(){key=null;glyph=null;live.clear();}
    static Drawable drawingSlot(Drawable owned,Drawable current,Drawable mask,boolean hidden){
        // Native clear/hide is authoritative, even while the backing icon field is repaired.
        if(hidden&&mask!=null)return mask;
        if(current==null||current==mask||current instanceof android.graphics.drawable.ColorDrawable&&current.getOpacity()==PixelFormat.TRANSPARENT)return current;
        return owned;
    }
}
