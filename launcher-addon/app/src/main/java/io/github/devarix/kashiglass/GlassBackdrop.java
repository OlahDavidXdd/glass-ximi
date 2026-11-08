package io.github.devarix.kashiglass;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

interface GlassBackdrop {
    boolean isAvailable();
    boolean draw(Canvas canvas, View owner, Rect bounds, int margin);
    default boolean drawSurface(Canvas canvas,View owner,Rect bounds,int margin){return draw(canvas,owner,bounds,margin);}
}
