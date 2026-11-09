package io.github.devarix.kashiglass;

import java.util.Locale;

final class MamlWidgetPolicy {
    private MamlWidgetPolicy(){}
    static boolean supportedMetadata(String metadata){String text=metadata==null?"":metadata.toLowerCase(Locale.ROOT);return text.contains("clock")||text.contains("weather");}
    static boolean decoration(String type,String name,String source,float width,float height,float cardWidth,float cardHeight,boolean interactive,boolean described){
        if(interactive||described||!Float.isFinite(width)||!Float.isFinite(height)||!Float.isFinite(cardWidth)||!Float.isFinite(cardHeight)||cardWidth<=0||cardHeight<=0||width<cardWidth*.70f||height<cardHeight*.70f||width>cardWidth*1.1f||height>cardHeight*1.1f)return false;
        if("RectangleScreenElement".equals(type))return true;
        if(!"ImageScreenElement".equals(type))return false;
        return backgroundName(name)||backgroundName(source);
    }
    private static boolean backgroundName(String value){
        if(value==null)return false;String text=value.toLowerCase(Locale.ROOT).replace('\\','/');
        int slash=text.lastIndexOf('/');String file=text.substring(slash+1);int dot=file.lastIndexOf('.');if(dot>0)file=file.substring(0,dot);
        return file.equals("bg")||file.equals("background")||file.equals("wallpaper")||file.startsWith("bg_")||file.endsWith("_bg")||file.contains("background")||text.contains("/bg/");
    }
}
