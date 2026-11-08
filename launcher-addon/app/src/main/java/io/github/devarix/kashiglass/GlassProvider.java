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
            result.putBoolean("icons", prefs.getBoolean("icons", false));
            result.putBoolean("blur", prefs.getBoolean("blur", false));
            result.putInt("radius", GlassConfig.radius(prefs.getInt("radius", GlassConfig.DEFAULT_RADIUS_DP)));
            try { ArrayList<String> names=new ArrayList<>(); for (String n:getContext().getAssets().list("icons")) if(n.endsWith(".png"))names.add(n.substring(0,n.length()-4)); result.putStringArrayList("packages",names); }
            catch (IOException e) { result.putString("error", e.toString()); }
        } else if ("report".equals(method)) {
            String status = extras == null ? null : extras.getString("status");
            if (status != null) prefs.edit().putString("status", status.substring(0,Math.min(600,status.length()))).apply();
            getContext().getContentResolver().notifyChange(Uri.parse("content://"+GlassConfig.AUTHORITY+"/status"),null);
        } else throw new IllegalArgumentException("Unknown provider method");
        return result;
    }
    @Override public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        enforceCaller();
        if (!"r".equals(mode) || uri.getPathSegments().size()!=2 || !"icons".equals(uri.getPathSegments().get(0))) throw new FileNotFoundException();
        String pkg=uri.getLastPathSegment();
        if (!GlassConfig.validPackage(pkg)) throw new FileNotFoundException("Invalid package");
        try { return getContext().getAssets().openFd("icons/"+pkg+".png"); }
        catch (IOException e) { throw new FileNotFoundException(pkg); }
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,String sortOrder) { enforceCaller();return new MatrixCursor(new String[]{"status"}); }
    @Override public String getType(Uri uri) { enforceCaller();return "image/png"; }
    @Override public Uri insert(Uri uri,ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri,ContentValues values,String where,String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri,String where,String[] args) { throw new UnsupportedOperationException(); }
}
