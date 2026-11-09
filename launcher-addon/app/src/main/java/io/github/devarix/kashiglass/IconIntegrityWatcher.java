package io.github.devarix.kashiglass;

import android.view.View;
import android.view.ViewTreeObserver;
import java.lang.ref.WeakReference;
import java.util.function.Consumer;

/** Check before rendering, without timers, visibility changes, or a retained detached tree. */
final class IconIntegrityWatcher implements View.OnAttachStateChangeListener,ViewTreeObserver.OnPreDrawListener {
    private final WeakReference<View> view;
    private final Consumer<View> verify;
    private ViewTreeObserver observer;
    IconIntegrityWatcher(View target,Consumer<View> verify){this.view=new WeakReference<>(target);this.verify=verify;target.addOnAttachStateChangeListener(this);if(target.isAttachedToWindow())onViewAttachedToWindow(target);}
    public void onViewAttachedToWindow(View target){detach();observer=target.getViewTreeObserver();observer.addOnPreDrawListener(this);}
    public void onViewDetachedFromWindow(View target){detach();}
    private void detach(){if(observer!=null&&observer.isAlive())observer.removeOnPreDrawListener(this);observer=null;}
    public boolean onPreDraw(){View target=view.get();if(target!=null)verify.accept(target);else detach();return true;}
}
