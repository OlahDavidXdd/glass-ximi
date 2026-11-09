package io.github.devarix.kashiglass;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
/** Protect native setter callbacks as well as the final live drawing slot. */
final class NativeIconInput {
    private NativeIconInput(){}
    static Drawable replace(Drawable incoming,Bitmap ready,Resources resources){
        if(SourceIconReturn.isClear(incoming)||incoming instanceof OwnedTransitionDrawable||ready==null||ready.isRecycled())return incoming;
        Drawable result=new OwnedTransitionDrawable(resources,ready);result.setAlpha(incoming.getAlpha());result.setBounds(incoming.getBounds());return result;
    }
}
