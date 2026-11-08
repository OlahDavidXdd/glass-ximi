package io.github.devarix.kashiglass;

final class GlassSurface {
    static final int NONE=-1,ICON=0,DOCK=1,SEARCH=2;
    private GlassSurface(){}
    static int kind(String name){
        if("com.miui.home.launcher.hotseats.HotSeats".equals(name))return DOCK;
        if("com.miui.home.launcher.SearchBarDesktopLayout".equals(name)||"com.miui.home.launcher.SearchBarDrawerLayout".equals(name))return SEARCH;
        return NONE;
    }
    static float radius(int width,int height,int kind){
        float size=Math.max(0,Math.min(width,height));
        return size*(kind==SEARCH?.5f:kind==DOCK?.32f:.27f);
    }
}
