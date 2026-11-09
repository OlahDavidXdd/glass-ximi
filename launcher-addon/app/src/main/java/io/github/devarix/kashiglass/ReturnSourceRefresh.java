package io.github.devarix.kashiglass;

import android.view.View;
import android.view.ViewParent;
import java.util.function.Consumer;

/** Flush source and parent display lists after native animation cleanup, preserving hide/alpha. */
final class ReturnSourceRefresh {
    private ReturnSourceRefresh(){}
    static int matching(View source,Iterable<View> known,java.util.function.Function<View,String> key,Consumer<View> reconcile){
        String requested=key.apply(source);refresh(source,reconcile);int count=1;
        if(requested!=null)for(View view:known)if(view!=null&&view!=source&&view.isAttachedToWindow()&&requested.equals(key.apply(view))){refresh(view,reconcile);count++;}
        return count;
    }
    static void refresh(View source,Consumer<View> reconcile){
        reconcile.accept(source);source.invalidate();ViewParent parent=source.getParent();if(parent instanceof View)((View)parent).invalidate();
    }
}
