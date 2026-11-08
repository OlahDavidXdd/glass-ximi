package io.github.devarix.kashiglass;

public final class GlassConfig {
    public static final String APP = "io.github.devarix.kashiglass";
    public static final String LAUNCHER = "com.miui.home";
    public static final String AUTHORITY = APP + ".provider";
    public static final String PREFS = "glass";
    public static final int DEFAULT_RADIUS_DP = 24;
    private GlassConfig() {}
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
