package io.github.devarix.kashiglass;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.io.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

final class WallpaperBackdrop implements GlassBackdrop {
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService loader=Executors.newSingleThreadExecutor();
    private final Runnable changed;
    private final Consumer<String> status;
    private final BackdropGeometry geometry=new BackdropGeometry();
    private final Matrix wallpaperMatrix=new Matrix();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private volatile Bitmap bitmap;
    private volatile boolean active,rootAllowed,loading;
    private volatile long generation;
    private volatile Process process;
    private float offsetX=.5f,offsetY=.5f;
    private String failure="Wallpaper is not loaded yet.";
    WallpaperBackdrop(Context context,Runnable changed,Consumer<String> status){
        this.context=context;this.changed=changed;this.status=status;
        context.registerReceiver(new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(active)refresh();}},new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED),Context.RECEIVER_NOT_EXPORTED);
    }
    void configure(boolean enabled,boolean allowRoot){
        boolean different=active!=enabled||rootAllowed!=allowRoot;
        active=enabled;rootAllowed=allowRoot;
        if(different){generation++;cancelProcess();bitmap=null;if(enabled)refresh();else changed.run();}
    }
    void offsets(float x,float y){if(Math.abs(x-offsetX)<.001f&&Math.abs(y-offsetY)<.001f)return;offsetX=Math.max(0,Math.min(1,x));offsetY=Math.max(0,Math.min(1,y));if(active&&bitmap!=null)changed.run();}
    String failure(){return failure;}
    @Override public boolean isAvailable(){return bitmap!=null;}
    void refresh(){
        if(!active)return;
        if(loading){generation++;cancelProcess();return;}
        loading=true;long request=generation;boolean allowRoot=rootAllowed;
        loader.execute(()->{
            Bitmap result=null;String problem=null;
            try{result=readWallpaper(allowRoot);if(result==null)problem="Static wallpaper could not be decoded.";}
            catch(Exception e){problem=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
            Bitmap image=result;String message=problem;
            main.post(()->{
                loading=false;
                if(!active)return;
                if(generation!=request){refresh();return;}
                bitmap=image;failure=message==null?"":message;
                status.accept(image!=null?"Static wallpaper available for the QWEA0 lens. Rendering still needs a device check.":"Liquid Glass capture unavailable: "+failure);
                changed.run();
            });
        });
    }
    @SuppressLint("MissingPermission") // Runs inside the scoped launcher, catches denied APIs; root reads are explicitly opt-in.
    private Bitmap readWallpaper(boolean allowRoot) throws Exception {
        WallpaperManager manager=WallpaperManager.getInstance(context);
        if(manager.getWallpaperInfo()!=null)throw new IOException("Live wallpapers are not sampled by this adapter. Use Frosted Glass/native blur for a live wallpaper.");
        try(var file=manager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)){
            if(file!=null){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFileDescriptor(file.getFileDescriptor(),null,o);o.inSampleSize=sample(o.outWidth,o.outHeight);o.inJustDecodeBounds=false;Bitmap b=BitmapFactory.decodeFileDescriptor(file.getFileDescriptor(),null,o);if(b!=null)return b;}
        }catch(SecurityException|IOException ignored){}
        try{
            Drawable d=manager.getDrawable();
            if(d instanceof BitmapDrawable){Bitmap b=((BitmapDrawable)d).getBitmap();if(b!=null)return b;}
        }catch(SecurityException ignored){}
        if(!allowRoot)throw new IOException("Normal wallpaper access was denied. Optionally enable Wallpaper access via root in devarix Glass; no root request is made while it is off.");
        return readRootWallpaper();
    }
    private static int sample(int w,int h){int n=1;while(Math.max(w,h)/n>3072)n*=2;return n;}
    private Bitmap readRootWallpaper() throws Exception {
        int user=android.os.Process.myUid()/100000;
        Process p=new ProcessBuilder("su","-c","cat "+GlassConfig.wallpaperPath(user)).start();process=p;p.getOutputStream().close();
        CompletableFuture<byte[]> bytes=new CompletableFuture<>();
        Thread reader=new Thread(()->{
            try(InputStream in=p.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[8192];int n;
                while((n=in.read(buffer))!=-1){if(out.size()+n>24*1024*1024)throw new IOException("Wallpaper exceeds the 24 MiB capture limit.");out.write(buffer,0,n);}
                bytes.complete(out.toByteArray());
            }catch(Exception e){bytes.completeExceptionally(e);p.destroyForcibly();}
        },"devarix-wallpaper-read");reader.setDaemon(true);reader.start();
        try{
            if(!p.waitFor(25,TimeUnit.SECONDS))throw new IOException("Root wallpaper read timed out or was not approved.");
            if(p.exitValue()!=0)throw new IOException("Root wallpaper access was denied or the current user's wallpaper file is unavailable.");
            byte[] data=bytes.get(3,TimeUnit.SECONDS);BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,o);o.inSampleSize=sample(o.outWidth,o.outHeight);o.inJustDecodeBounds=false;
            return BitmapFactory.decodeByteArray(data,0,data.length,o);
        }finally{p.destroy();try{p.getErrorStream().close();}catch(IOException ignored){}process=null;}
    }
    private void cancelProcess(){Process p=process;if(p!=null)p.destroyForcibly();}
    @Override public boolean draw(Canvas canvas,View owner,Rect bounds,int margin){return drawInternal(canvas,owner,bounds,margin,true);}
    @Override public boolean drawSurface(Canvas canvas,View owner,Rect bounds,int margin){return drawInternal(canvas,owner,bounds,margin,false);}
    private boolean drawInternal(Canvas canvas,View owner,Rect bounds,int margin,boolean content){
        Bitmap source=bitmap;if(source==null)return false;
        View root=owner.getRootView();int width=root.getWidth(),height=root.getHeight();if(width<1||height<1)return false;
        float scale=Math.max((float)width/source.getWidth(),(float)height/source.getHeight());
        wallpaperMatrix.setScale(scale,scale);wallpaperMatrix.postTranslate(-(source.getWidth()*scale-width)*offsetX,-(source.getHeight()*scale-height)*offsetY);
        int saved=canvas.save();try{if(!geometry.concatScreenToNode(canvas,owner,bounds,margin,content))return false;canvas.drawBitmap(source,wallpaperMatrix,paint);return true;}finally{canvas.restoreToCount(saved);}
    }
}
