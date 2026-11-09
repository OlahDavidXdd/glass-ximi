package io.github.devarix.kashiglass;

import android.view.*;
import android.widget.ImageView;
import java.lang.ref.WeakReference;
import java.util.function.Predicate;

/** Invalidate live glass when a recycled/scrolled icon moves, without a perpetual frame timer. */
final class LiveGlassMotion implements View.OnAttachStateChangeListener,ViewTreeObserver.OnPreDrawListener {
    private final WeakReference<View> owner;
    private final Predicate<View> active;
    private final int[] position=new int[2];private final float[] matrix=new float[9];
    private WeakReference<ViewTreeObserver> tree=new WeakReference<>(null);
    private boolean initialized;private long lastPose;
    LiveGlassMotion(View view,Predicate<View> active){this.owner=new WeakReference<>(view);this.active=active;view.addOnAttachStateChangeListener(this);if(view.isAttachedToWindow())onViewAttachedToWindow(view);}
    public void onViewAttachedToWindow(View view){remove();ViewTreeObserver observer=view.getViewTreeObserver();observer.addOnPreDrawListener(this);tree=new WeakReference<>(observer);initialized=false;}
    public void onViewDetachedFromWindow(View view){remove();initialized=false;}
    private void remove(){ViewTreeObserver observer=tree.get();if(observer!=null&&observer.isAlive())observer.removeOnPreDrawListener(this);tree.clear();}
    public boolean onPreDraw(){View view=owner.get();if(view==null){remove();return true;}
        if(!view.isShown()||view.getAlpha()==0||!active.test(view)){initialized=false;return true;}
        long pose=pose(view);if(!initialized||lastPose!=pose){initialized=true;lastPose=pose;view.invalidate();}return true;
    }
    private long pose(View view){
        view.getLocationOnScreen(position);long value=position[0]*31L+position[1];value=value*31+view.getWidth();value=value*31+view.getHeight();
        value=value*31+Float.floatToIntBits(view.getScaleX());value=value*31+Float.floatToIntBits(view.getScaleY());
        value=value*31+Float.floatToIntBits(view.getTranslationX());value=value*31+Float.floatToIntBits(view.getTranslationY());
        if(view instanceof ImageView){((ImageView)view).getImageMatrix().getValues(matrix);for(float f:matrix)value=value*31+Float.floatToIntBits(f);}return value;
    }
}
