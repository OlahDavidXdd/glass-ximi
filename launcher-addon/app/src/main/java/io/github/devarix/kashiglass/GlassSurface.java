package io.github.devarix.kashiglass;

final class GlassSurface {
    static final int NONE=-1,ICON=0,DOCK=1,SEARCH=2,DRAWER=3;
    private GlassSurface(){}
    static int kind(String name){
        if("com.miui.home.launcher.hotseats.HotSeats".equals(name))return DOCK;
        if("com.miui.home.launcher.SearchBarDesktopLayout".equals(name)||"com.miui.home.launcher.SearchBarDrawerLayout".equals(name))return SEARCH;
        if("com.miui.home.launcher.allapps.BaseAllAppsContainerView".equals(name)||"com.miui.home.launcher.allapps.AllAppsContainerView".equals(name))return DRAWER;
        return NONE;
    }
    static boolean drawerLayer(String name){
        return "com.miui.home.launcher.allapps.AllAppsCategoryContainer".equals(name)||"com.miui.home.launcher.allapps.category.AllAppsCategoryListContainer".equals(name)||"com.miui.home.launcher.allapps.AllAppsRecyclerView".equals(name)||"com.miui.home.launcher.allapps.search.AllAppsSearchContainer".equals(name);
    }
    static float radius(int width,int height,int kind){return radius(width,height,kind,1);}
    static float radius(int width,int height,int kind,float density){
        float size=Math.max(0,Math.min(width,height));
        if(kind==DRAWER)return Math.min(size*.12f,36*Math.max(.5f,density));
        return size*(kind==SEARCH?.5f:kind==DOCK?.32f:.27f);
    }
}
