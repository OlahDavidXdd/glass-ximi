package io.github.devarix.kashiglass;

final class SearchSurfacePolicy {
    private SearchSurfacePolicy(){}
    static boolean quickHost(String name){return "com.miui.home.launcher.search.QuickSearchBarWidgetView".equals(name);}
    static boolean capsuleHost(String name){return "com.miui.home.launcher.pageindicators.CapsuleIndicator".equals(name);}
    static boolean pageHost(String name){return "com.miui.home.launcher.pageindicators.LauncherIndicator".equals(name)||"com.miui.home.launcher.pageindicators.MultiSeekBarIndicator".equals(name);}
    static boolean coversBar(int hostWidth,int hostHeight,int width,int height){
        return hostWidth>0&&hostHeight>0&&width>=hostWidth*.72f&&height>=hostHeight*.35f&&width<=hostWidth*1.1f&&height<=hostHeight*1.1f;
    }
}
