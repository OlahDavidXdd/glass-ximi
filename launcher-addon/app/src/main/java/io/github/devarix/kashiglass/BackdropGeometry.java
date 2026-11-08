package io.github.devarix.kashiglass;

import android.graphics.*;
import android.view.View;
import android.widget.ImageView;

final class BackdropGeometry {
    private final Matrix localToScreen=new Matrix(),screenToLocal=new Matrix();
    private final int[] location=new int[2];
    boolean concatScreenToNode(Canvas target,View owner,Rect bounds,int margin){
        localToScreen.reset();
        if(owner instanceof ImageView)localToScreen.set(((ImageView)owner).getImageMatrix());
        if(owner instanceof android.widget.TextView){
            // Top compound icon is horizontally centered in TextView's content box.
            float x=owner.getPaddingLeft()+(owner.getWidth()-owner.getPaddingLeft()-owner.getPaddingRight()-bounds.width())*.5f;
            localToScreen.postTranslate(x,owner.getPaddingTop());
        }else localToScreen.postTranslate(owner.getPaddingLeft(),owner.getPaddingTop());
        localToScreen.postScale(owner.getScaleX(),owner.getScaleY());
        owner.getLocationOnScreen(location);
        localToScreen.postTranslate(location[0],location[1]);
        if(!localToScreen.invert(screenToLocal))return false;
        screenToLocal.postTranslate(margin-bounds.left,margin-bounds.top);
        target.concat(screenToLocal);return true;
    }
    static float displayScale(View owner){
        if(!(owner instanceof ImageView))return Math.max(.05f,Math.abs(owner.getScaleX()));
        float[] values=new float[9];((ImageView)owner).getImageMatrix().getValues(values);
        return Math.max(.05f,(float)Math.hypot(values[Matrix.MSCALE_X],values[Matrix.MSKEW_Y])*Math.abs(owner.getScaleX()));
    }
}
