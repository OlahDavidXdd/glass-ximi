package io.github.devarix.kashiglass;

import android.content.Context;
import android.os.Build;

final class DebugLog {
    private static DiagnosticLog log;
    private DebugLog(){}
    private static synchronized DiagnosticLog store(Context context){
        if(log==null)log=new DiagnosticLog(context.getFilesDir());return log;
    }
    static boolean enabled(Context context){return context.getSharedPreferences(GlassConfig.PREFS,0).getBoolean("debug_logs",true);}
    static void event(Context context,String event,String message){
        if(!enabled(context))return;
        try{store(context).append(event,message);}catch(Exception e){android.util.Log.w("devarixGlass","Diagnostic write failed: "+e.getClass().getSimpleName());}
    }
    private static String certificate(Context context){
        try{
            var info=context.getPackageManager().getPackageInfo(context.getPackageName(),android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES);
            var signers=info.signingInfo==null?null:info.signingInfo.getApkContentsSigners();if(signers==null||signers.length==0)return "unavailable";
            byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(signers[0].toByteArray());StringBuilder out=new StringBuilder();
            for(byte value:digest){int b=value&255;out.append("0123456789abcdef".charAt(b>>>4)).append("0123456789abcdef".charAt(b&15));}return out.toString();
        }catch(Exception ignored){return "unavailable";}
    }
    static String snapshot(Context context) throws java.io.IOException {
        var prefs=context.getSharedPreferences(GlassConfig.PREFS,0);
        String launcher;
        try{var info=context.getPackageManager().getPackageInfo(GlassConfig.LAUNCHER,0);launcher=info.versionName+" ("+info.getLongVersionCode()+")";}catch(Exception e){launcher="unavailable: "+e.getClass().getSimpleName();}
        return "devarix Glass diagnostic report\nBuild: "+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")\nAPK certificate SHA-256: "+certificate(context)+"\nDevice: "+Build.MANUFACTURER+" "+Build.MODEL+" / Android "+Build.VERSION.RELEASE+" / SDK "+Build.VERSION.SDK_INT+
            "\nLast reported hook (refresh connection to confirm): "+prefs.getString("hook_version","unreported")+" code="+prefs.getInt("hook_code",0)+" pid="+prefs.getInt("hook_pid",0)+" capabilities="+prefs.getString("hook_capabilities","")+"\nRestart result: "+prefs.getString("restart_status","none")+"\nLauncher: "+launcher+"\nLogging: "+enabled(context)+"\nSettings: icons="+prefs.getBoolean("icons",false)+", screen_match="+prefs.getBoolean("screen_match",true)+", white_symbols="+prefs.getBoolean("white_symbols",true)+", blur="+prefs.getBoolean("blur",false)+", style="+GlassConfig.style(prefs.getString("style","liquid"))+", radius="+prefs.getInt("radius",24)+", drawer_glass="+prefs.getBoolean("drawer_glass",true)+", dock_glass="+prefs.getBoolean("dock_glass",true)+", search_glass="+prefs.getBoolean("search_glass",true)+", widget_glass="+prefs.getBoolean("widget_glass",true)+", automatic="+prefs.getBoolean("automatic",true)+", root_wallpaper="+prefs.getBoolean("root_wallpaper",false)+
            "\nLast settings read (epoch ms): "+prefs.getLong("connected_at",0)+"\nBindings: "+prefs.getString("bindings","not received")+"\nLogged glass animation hand-offs: "+prefs.getInt("animation_replacements",0)+"\nLast animation: "+prefs.getString("last_animation","not received")+"\nSurfaces: "+prefs.getString("surfaces","not received")+"\nWidgets: "+prefs.getString("widgets","not received")+"\nStatus: "+prefs.getString("status","not received")+"\n\n"+store(context).snapshot();
    }
    static void clear(Context context) throws java.io.IOException {store(context).clear();context.getSharedPreferences(GlassConfig.PREFS,0).edit().putInt("animation_replacements",0).remove("last_animation").apply();}
}
