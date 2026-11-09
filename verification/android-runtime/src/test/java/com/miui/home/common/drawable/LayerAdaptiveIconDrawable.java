package com.miui.home.common.drawable;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.*;

/** Test-only shape of Kashi's reflective foreground API; not its renderer or hook runtime. */
public final class LayerAdaptiveIconDrawable extends Drawable {
    public static final class Layer {
        private final Drawable drawable;
        Layer(Drawable drawable){this.drawable=drawable;}
        public Drawable getCurrentShowDrawable(){return drawable;}
    }
    private final List<Layer> layers;
    public LayerAdaptiveIconDrawable(Drawable source){layers=Collections.singletonList(new Layer(source));}
    public List<Layer> getForegroundLayers(){return layers;}
    public void draw(Canvas canvas){}
    public void setAlpha(int value){}
    public void setColorFilter(ColorFilter filter){}
    public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
