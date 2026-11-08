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
            prefs.edit().putLong("connected_at",System.currentTimeMillis()).apply();
            String style=GlassConfig.style(prefs.getString("style","liquid"));
            result.putString("style",style);
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
        } else if ("log".equals(method)) {
            if(extras!=null)DebugLog.event(getContext(),"LAUNCHER/"+DiagnosticLog.clean(extras.getString("event"),32),extras.getString("message"));
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
