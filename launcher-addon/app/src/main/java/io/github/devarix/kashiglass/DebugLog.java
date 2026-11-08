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
    static String snapshot(Context context) throws java.io.IOException {
        var prefs=context.getSharedPreferences(GlassConfig.PREFS,0);
        String launcher;
        try{var info=context.getPackageManager().getPackageInfo(GlassConfig.LAUNCHER,0);launcher=info.versionName+" ("+info.getLongVersionCode()+")";}catch(Exception e){launcher="unavailable: "+e.getClass().getSimpleName();}
        return "devarix Glass diagnostic report\nBuild: "+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")\nDevice: "+Build.MANUFACTURER+" "+Build.MODEL+" / Android "+Build.VERSION.RELEASE+" / SDK "+Build.VERSION.SDK_INT+
            "\nLoaded hook: "+prefs.getString("hook_version","unreported")+" code="+prefs.getInt("hook_code",0)+" pid="+prefs.getInt("hook_pid",0)+" capabilities="+prefs.getString("hook_capabilities","")+"\nRestart result: "+prefs.getString("restart_status","none")+"\nLauncher: "+launcher+"\nLogging: "+enabled(context)+"\nSettings: icons="+prefs.getBoolean("icons",false)+", screen_match="+prefs.getBoolean("screen_match",true)+", white_symbols="+prefs.getBoolean("white_symbols",true)+", blur="+prefs.getBoolean("blur",false)+", style="+GlassConfig.style(prefs.getString("style","liquid"))+", radius="+prefs.getInt("radius",24)+", drawer_glass="+prefs.getBoolean("drawer_glass",true)+", dock_glass="+prefs.getBoolean("dock_glass",true)+", search_glass="+prefs.getBoolean("search_glass",true)+", automatic="+prefs.getBoolean("automatic",true)+", root_wallpaper="+prefs.getBoolean("root_wallpaper",false)+
            "\nLast settings read (epoch ms): "+prefs.getLong("connected_at",0)+"\nBindings: "+prefs.getString("bindings","not received")+"\nLogged animation replacements: "+prefs.getInt("animation_replacements",0)+"\nLast animation: "+prefs.getString("last_animation","not received")+"\nSurfaces: "+prefs.getString("surfaces","not received")+"\nStatus: "+prefs.getString("status","not received")+"\n\n"+store(context).snapshot();
    }
    static void clear(Context context) throws java.io.IOException {store(context).clear();context.getSharedPreferences(GlassConfig.PREFS,0).edit().putInt("animation_replacements",0).remove("last_animation").apply();}
}
