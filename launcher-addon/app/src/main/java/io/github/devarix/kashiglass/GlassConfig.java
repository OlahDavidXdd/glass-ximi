package io.github.devarix.kashiglass;

public final class GlassConfig {
    public static final String APP = "io.github.devarix.kashiglass";
    public static final String LAUNCHER = "com.miui.home";
    public static final String AUTHORITY = APP + ".provider";
    public static final String PREFS = "glass";
    public static final int DEFAULT_RADIUS_DP = 24;
    private GlassConfig() {}
    public static boolean validStyle(String value) { return "liquid".equals(value)||"frosted".equals(value); }
    public static String style(String value) { return validStyle(value)?value:"liquid"; }
    public static String assetDirectory(String style) { return "frosted".equals(style)?"icons":"glyphs"; }
    public static String wallpaperPath(int user) { if(user<0||user>99999)throw new IllegalArgumentException("Invalid user"); return "/data/system/users/"+user+"/wallpaper"; }
    public static String mapping(String pkg,String activity,java.util.Set<String> available){
        if(activity!=null&&available.contains(activity))return activity;
        return pkg!=null&&available.contains(pkg)?pkg:null;
    }
    public static String iconKey(String pkg,String activity,java.util.Set<String> available,boolean automatic){
        String mapped=mapping(pkg,activity,available);
        if(mapped!=null)return mapped;
        return automatic&&validPackage(pkg)?"auto:"+pkg+":"+(activity==null?"":activity):null;
    }
    public static boolean automaticKey(String key){return key!=null&&key.startsWith("auto:");}
    public static int radius(int value) { return Math.max(8, Math.min(60, value)); }
    public static boolean validPackage(String value) {
        if ("android".equals(value)) return true;
        return value != null && value.length() <= 240 && value.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+");
    }
    public static boolean allowedCaller(int callingUid, int ownUid, String[] packages) {
        if (callingUid == ownUid) return true;
        if (packages != null) for (String name : packages) if (LAUNCHER.equals(name)) return true;
        return false;
    }
}
