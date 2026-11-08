package io.github.devarix.kashiglass;

final class AnimationPolicy {
    static final String[] TARGETS={"com.miui.home.recents.views.FloatingIconView","com.miui.home.recents.views.FloatingIconView2","com.miui.home.recents.views.FloatingIconLayer2","com.miui.home.recents.FloatingIconLayer"};
    private AnimationPolicy(){}
    static int drawableIndex(String[] types){
        for(int i=0;i+1<types.length;i++)if("com.miui.home.common.animate.LaunchAppAndBackHomeAnimTarget".equals(types[i])&&"android.graphics.drawable.Drawable".equals(types[i+1]))return i+1;
        return -1;
    }
}
