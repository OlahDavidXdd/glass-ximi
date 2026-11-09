package io.github.devarix.kashiglass;

import android.content.res.Resources;
import android.graphics.Bitmap;
import java.util.function.Function;

/** Every handoff is resolved for the current target, even when the previous input was glass. */
final class AnimationArtwork {
    static OwnedTransitionDrawable fresh(Object target,Resources resources,Function<Object,Bitmap> snapshots){
        Bitmap bitmap=snapshots.apply(target);
        return bitmap==null||bitmap.isRecycled()?null:new OwnedTransitionDrawable(resources,bitmap);
    }
}
