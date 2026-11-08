package io.github.devarix.kashiglass;

import java.util.Locale;

/** Restricts treatment to clock/weather cards and their large decorative layers. */
final class WidgetSurfacePolicy {
    private WidgetSurfacePolicy(){}
    static boolean supported(String component,String label){
        String id=component==null?"":component.toLowerCase(Locale.ROOT);
        String title=label==null?"":label.trim().toLowerCase(Locale.ROOT);
        return id.contains("weather")||id.contains("deskclock")||id.contains(".clock")||
            title.equals("clock")||title.equals("weather")||title.equals("analog clock")||title.equals("digital clock");
    }
    static boolean coversCard(int width,int height,int childWidth,int childHeight){
        return width>0&&height>0&&childWidth>=width*.70f&&childHeight>=height*.70f&&childWidth<=width*1.1f&&childHeight<=height*1.1f;
    }
    static boolean decorativeImage(String name,boolean interactive,boolean described){
        if(name==null||interactive||described)return false;
        String id=name.toLowerCase(Locale.ROOT);
        return id.equals("bg")||id.equals("background")||id.equals("wallpaper")||id.equals("widget_bg")||
            id.endsWith("_background")||id.endsWith("_bg")||id.equals("background_image")||id.equals("bg_image");
    }
}
