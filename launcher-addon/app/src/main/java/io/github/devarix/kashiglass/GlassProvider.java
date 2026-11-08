package io.github.devarix.kashiglass;

import android.content.*;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.*;
import java.io.*;
import java.util.*;

public final class GlassProvider extends ContentProvider {
    public static final Uri SETTINGS = Uri.parse("content://" + GlassConfig.AUTHORITY + "/settings");
    public static final Uri RESTART=Uri.parse("content://"+GlassConfig.AUTHORITY+"/restart");
    private void enforceCaller() {
        int uid = Binder.getCallingUid();
        if (!GlassConfig.allowedCaller(uid, android.os.Process.myUid(), getContext().getPackageManager().getPackagesForUid(uid)))
            throw new SecurityException("Only the launcher and this add-on may access glass assets");
    }
    @Override public boolean onCreate() { return true; }
    @Override public Bundle call(String method, String arg, Bundle extras) {
        enforceCaller();
        var prefs = getContext().getSharedPreferences(GlassConfig.PREFS, 0);
        Bundle result = new Bundle();
        if ("settings".equals(method)) {
            long connected=System.currentTimeMillis();
            var edit=prefs.edit().putLong("connected_at",connected);
            if(Binder.getCallingUid()!=android.os.Process.myUid()){
                int code=extras==null?0:Math.max(0,extras.getInt("hook_code",0));
                String version=extras==null?"unreported":DiagnosticLog.clean(extras.getString("hook_version"),64);
                int pid=extras==null?0:Math.max(0,extras.getInt("hook_pid",0));
                String capabilities=extras==null?"":DiagnosticLog.clean(extras.getString("hook_capabilities"),128);
                edit.putInt("hook_code",code).putString("hook_version",version).putInt("hook_pid",pid).putString("hook_capabilities",capabilities);
                DebugLog.event(getContext(),"HOOK_IDENTITY","loaded="+version+" code="+code+" pid="+pid+" installed="+BuildConfig.VERSION_NAME);
            }
            edit.apply();
            String style=GlassConfig.style(prefs.getString("style","liquid"));
            result.putString("style",style);
            result.putBoolean("screen_match",prefs.getBoolean("screen_match",true));
            result.putBoolean("white_symbols",prefs.getBoolean("white_symbols",true));
            result.putBoolean("widget_glass",prefs.getBoolean("widget_glass",true));
            result.putBoolean("drawer_glass",prefs.getBoolean("drawer_glass",true));
            result.putBoolean("dock_glass",prefs.getBoolean("dock_glass",true));
            result.putBoolean("search_glass",prefs.getBoolean("search_glass",true));
            result.putBoolean("automatic",prefs.getBoolean("automatic",true));
            result.putBoolean("debug_logs",prefs.getBoolean("debug_logs",true));
            DebugLog.event(getContext(),"SETTINGS_READ","caller="+Binder.getCallingUid()+" style="+style+" icons="+prefs.getBoolean("icons",false)+" blur="+prefs.getBoolean("blur",false));
            result.putBoolean("root_wallpaper",prefs.getBoolean("root_wallpaper",false));
            try(var stream=getContext().getAssets().open("shaders/qwea0_lens.agsl")){result.putString("shader",new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}catch(IOException e){result.putString("error",e.toString());}
            result.putBoolean("icons", prefs.getBoolean("icons", false));
            result.putBoolean("blur", prefs.getBoolean("blur", false));
            result.putInt("radius", GlassConfig.radius(prefs.getInt("radius", GlassConfig.DEFAULT_RADIUS_DP)));
            try { ArrayList<String> names=new ArrayList<>(); for (String n:getContext().getAssets().list(GlassConfig.assetDirectory(style))) if(n.endsWith(".png"))names.add(n.substring(0,n.length()-4)); result.putStringArrayList("packages",names); }
            catch (IOException e) { result.putString("error", e.toString()); }
        } else if ("request_restart".equals(method)) {
            if(Binder.getCallingUid()!=android.os.Process.myUid())throw new SecurityException("Only the add-on UI may request a restart");
            synchronized(GlassProvider.class){
                int pid=prefs.getInt("hook_pid",0);
                if(!HookVersion.canRestart(prefs.getString("hook_capabilities",""),pid))throw new IllegalStateException("Loaded hook cannot restart; reboot the phone once");
                String token=UUID.randomUUID().toString();
                prefs.edit().putString("restart_token",token).putLong("restart_created",SystemClock.elapsedRealtime()).putInt("restart_pid",pid).remove("restart_ack").commit();
                result.putString("token",token);
            }
            DebugLog.event(getContext(),"SELF_RESTART_REQUEST","Explicit user request for connected launcher PID");
            getContext().getContentResolver().notifyChange(RESTART,null);
        } else if ("consume_restart".equals(method)) {
            if(Binder.getCallingUid()==android.os.Process.myUid())throw new SecurityException("Only the launcher may consume a restart");
            synchronized(GlassProvider.class){
                String token=prefs.getString("restart_token",null);int pid=extras==null?0:extras.getInt("pid",0);
                if(RestartHandshake.valid(token,prefs.getLong("restart_created",-1),SystemClock.elapsedRealtime(),prefs.getInt("restart_pid",0),pid)){
                    prefs.edit().remove("restart_token").putString("restart_ack",token).commit();result.putString("token",token);
                    DebugLog.event(getContext(),"SELF_RESTART_ACK","Launcher PID="+pid);
                }
            }
        } else if ("log".equals(method)) {
            if(extras!=null){
                String event=extras.getString("event"),message=extras.getString("message");
                DebugLog.event(getContext(),"LAUNCHER/"+DiagnosticLog.clean(event,32),message);
                if("ANIMATION".equals(event)&&message!=null&&message.startsWith("Animation input replaced:")){
                    synchronized(GlassProvider.class){prefs.edit().putInt("animation_replacements",Math.min(1000000,prefs.getInt("animation_replacements",0)+1)).putString("last_animation",DiagnosticLog.clean(message,600)).apply();}
                    getContext().getContentResolver().notifyChange(Uri.parse("content://"+GlassConfig.AUTHORITY+"/status"),null);
                }
            }
        } else if ("widgets".equals(method)) {
            String value=extras==null?null:extras.getString("widgets");
            if(value!=null){prefs.edit().putString("widgets",DiagnosticLog.clean(value,600)).apply();DebugLog.event(getContext(),"WIDGETS",value);}
            getContext().getContentResolver().notifyChange(Uri.parse("content://"+GlassConfig.AUTHORITY+"/status"),null);
        } else if ("surfaces".equals(method)) {
            String value=extras==null?null:extras.getString("surfaces");
            if(value!=null){prefs.edit().putString("surfaces",DiagnosticLog.clean(value,600)).apply();DebugLog.event(getContext(),"SURFACES",value);}
            getContext().getContentResolver().notifyChange(Uri.parse("content://"+GlassConfig.AUTHORITY+"/status"),null);
        } else if ("diagnostics".equals(method)) {
            String value=extras==null?null:extras.getString("bindings");
            if(value!=null)DebugLog.event(getContext(),"BINDINGS",value);
            if(value!=null)prefs.edit().putString("bindings",value.substring(0,Math.min(600,value.length()))).apply();
            getContext().getContentResolver().notifyChange(Uri.parse("content://"+GlassConfig.AUTHORITY+"/status"),null);
        } else if ("report".equals(method)) {
            String status = extras == null ? null : extras.getString("status");
            if(status!=null)DebugLog.event(getContext(),"STATUS",status);
            if (status != null) prefs.edit().putString("status", status.substring(0,Math.min(600,status.length()))).apply();
            getContext().getContentResolver().notifyChange(Uri.parse("content://"+GlassConfig.AUTHORITY+"/status"),null);
        } else throw new IllegalArgumentException("Unknown provider method");
        return result;
    }
    @Override public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        enforceCaller();
        if(!"r".equals(mode))throw new FileNotFoundException();
        if(uri.getPathSegments().size()==1&&"plate".equals(uri.getLastPathSegment())){try{return getContext().getAssets().openFd("liquid_plate.png");}catch(IOException e){throw new FileNotFoundException();}}
        if(uri.getPathSegments().size()!=3||!"icons".equals(uri.getPathSegments().get(0)))throw new FileNotFoundException();
        String style=uri.getPathSegments().get(1);if(!GlassConfig.validStyle(style))throw new FileNotFoundException("Invalid style");
        String pkg=uri.getLastPathSegment();
        if (!GlassConfig.validPackage(pkg)) throw new FileNotFoundException("Invalid package");
        try { return getContext().getAssets().openFd(GlassConfig.assetDirectory(style)+"/"+pkg+".png"); }
        catch (IOException e) { throw new FileNotFoundException(pkg); }
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,String sortOrder) { enforceCaller();return new MatrixCursor(new String[]{"status"}); }
    @Override public String getType(Uri uri) { enforceCaller();return "image/png"; }
    @Override public Uri insert(Uri uri,ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri,ContentValues values,String where,String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri,String where,String[] args) { throw new UnsupportedOperationException(); }
}
