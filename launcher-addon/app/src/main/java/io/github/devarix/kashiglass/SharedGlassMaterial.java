package io.github.devarix.kashiglass;

import android.graphics.drawable.Drawable;
import android.view.View;
import java.util.function.Consumer;

/** One material pipeline for dock, search and drawer; only their outline geometry differs. */
final class SharedGlassMaterial {
    private SharedGlassMaterial(){}
    static Drawable create(View owner,GlassBackdrop backdrop,boolean blur,int radius,Consumer<String> status,int kind){
        return kind==GlassSurface.WIDGET?new LiquidGlassDrawable(owner,null,backdrop,blur,radius,status,kind,true):new LiquidGlassDrawable(owner,null,backdrop,blur,radius,status,kind);
    }
}
