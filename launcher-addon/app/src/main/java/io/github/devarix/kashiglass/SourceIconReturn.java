package io.github.devarix.kashiglass;

import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

/** Intercept native return-to-home artwork before it enters the TextView's drawing slot. */
final class SourceIconReturn {
    private SourceIconReturn(){}
    static Drawable replacement(Drawable incoming,Drawable owned,Drawable mask,boolean hidden){
        if(incoming==null||incoming==mask||incoming instanceof ColorDrawable&&incoming.getOpacity()==PixelFormat.TRANSPARENT)return incoming;
        if(owned==null)return incoming;
        if(hidden&&mask!=null)return mask;
        if(incoming!=owned&&!incoming.getBounds().isEmpty())owned.setBounds(incoming.getBounds());
        return owned;
    }
    static boolean repair(TextView view,Drawable owned,Drawable mask,boolean hidden){
        Drawable[] slots=view.getCompoundDrawables();Drawable next=replacement(slots[1],owned,mask,hidden);
        if(next==slots[1])return false;
        view.setCompoundDrawables(slots[0],next,slots[2],slots[3]);return true;
    }
}
