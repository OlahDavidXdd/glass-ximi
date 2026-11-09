package io.github.devarix.kashiglass;

import android.graphics.drawable.Drawable;
import android.view.View;
import java.util.function.Consumer;

/** One material pipeline for dock, search and drawer; only their outline geometry differs. */
final class SharedGlassMaterial {
    private SharedGlassMaterial(){}
    static Drawable create(View owner,GlassBackdrop backdrop,boolean blur,int radius,Consumer<String> status,int kind){
        return kind==GlassSurface.WIDGET?new LiquidGlassDrawable(owner,null,backdrop,blur,radius,message->status.accept(message.equals("QWEA0 lens unavailable on a software canvas. Static fallback only.")?"Software canvas cannot sample the live widget backdrop; a clear rim is drawn, not a wallpaper texture.":message),kind,true):new LiquidGlassDrawable(owner,null,backdrop,blur,radius,status,kind);
    }
}
