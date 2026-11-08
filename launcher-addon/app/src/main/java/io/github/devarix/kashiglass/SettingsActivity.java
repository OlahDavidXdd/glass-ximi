package io.github.devarix.kashiglass;

import android.app.Activity;
import android.content.*;
import android.database.ContentObserver;
import android.graphics.Color;
import android.os.*;
import android.widget.*;

public final class SettingsActivity extends Activity {
    private android.content.SharedPreferences prefs;
    private TextView status;
    private Switch iconsSwitch,blurSwitch;
    private final Handler main=new Handler(Looper.getMainLooper());
    private long checkStarted;
    private boolean checking;
    private final Runnable checkResult=()->{checking=false;updateStatus();};
    private final ContentObserver observer=new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override public void onChange(boolean selfChange) { updateStatus(); }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);prefs=getSharedPreferences(GlassConfig.PREFS,0);
        DebugLog.event(this,"APP_OPEN",BuildConfig.VERSION_NAME);
        ScrollView scroll=new ScrollView(this);LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);int pad=dp(24);page.setPadding(pad,pad,pad,pad);scroll.addView(page);setContentView(scroll);
        text(page,"devarix Glass",30,Color.WHITE);text(page,"For Kashi launcher · HyperOS 3",16,0xffcccccc);
        text(page,"1. LSPosed → Modules → devarix Glass → enable and select System launcher (com.miui.home).\n2. Tap Enable Liquid Glass below.\n3. Tap Restart launcher and allow root, then open your home screen.\nThe icon switch alone cannot enable the LSPosed module.",15,0xffcccccc);
        text(page,"Material",18,Color.WHITE);
        Spinner material=new Spinner(this);material.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Liquid Glass · QWEA0 renderer","Frosted Glass · previous style"}));material.setSelection("frosted".equals(GlassConfig.style(prefs.getString("style","liquid")))?1:0);page.addView(material);
        material.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,android.view.View v,int position,long id){String style=position==0?"liquid":"frosted";if(!style.equals(GlassConfig.style(prefs.getString("style","liquid")))){prefs.edit().putString("style",style).commit();changed();}}});
        iconsSwitch=toggle(page,"Use built-in glass icons","700 grayscale mappings · 512 px symbols · Liquid or Frosted material","icons");
        toggle(page,"Automatic glass icons","Creates a silver foreground and glass finish for apps without a bundled mapping. Bundled artwork always takes priority.","automatic");
        toggle(page,"Glass dock","Rounded gray glass behind the dock icons. Uses the same lens, reflections and blur strength as Liquid Glass icons.","dock_glass");
        toggle(page,"Glass search bar","Matching glass capsule for Kashi’s built-in home and drawer search bars. Their controls and native width are retained.","search_glass");
        blurSwitch=toggle(page,"Backdrop blur","Liquid Glass uses the QWEA0 shader; Frosted Glass uses Xiaomi native blur.","blur");
        toggle(page,"Wallpaper access via root","Optional. If normal wallpaper access is denied, requests root for System launcher to read the current static wallpaper only.","root_wallpaper");
        Button enable=new Button(this);enable.setText("Enable Liquid Glass");page.addView(enable);
        enable.setOnClickListener(v->{
            prefs.edit().putString("style","liquid").putBoolean("icons",true).putBoolean("blur",true).commit();
            material.setSelection(0);iconsSwitch.setChecked(true);blurSwitch.setChecked(true);changed();
            Toast.makeText(this,"Icons enabled. Restart launcher, then open your home screen.",Toast.LENGTH_LONG).show();
        });
        Button restart=new Button(this);restart.setText("Restart launcher");page.addView(restart);
        text(page,"Requests root only when tapped. Restarts System launcher for your current Android user; your home layout is kept. If root is denied, reboot the phone to apply the LSPosed module.",14,0xffb8b8b8);
        restart.setOnClickListener(v->restartLauncher(restart));
        Button check=new Button(this);check.setText("Check launcher connection");page.addView(check);
        check.setOnClickListener(v->{checkStarted=System.currentTimeMillis();checking=true;changed();updateStatus();
            main.removeCallbacks(checkResult);main.postDelayed(checkResult,4000);
            Toast.makeText(this,"Open your home screen, then return here to read the status.",Toast.LENGTH_LONG).show();});
        Button preview=new Button(this);preview.setText("Preview Liquid Glass");page.addView(preview);preview.setOnClickListener(v->startActivity(new Intent(this,PreviewActivity.class)));
        TextView radius=text(page,"Blur strength: "+GlassConfig.radius(prefs.getInt("radius",24))+"",18,Color.WHITE);
        SeekBar seek=new SeekBar(this);seek.setMax(52);seek.setProgress(GlassConfig.radius(prefs.getInt("radius",24))-8);page.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int value,boolean user){radius.setText("Blur strength: "+(value+8)+"");}
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){prefs.edit().putInt("radius",bar.getProgress()+8).commit();changed();}
        });
        text(page,"Launcher status",18,Color.WHITE);status=text(page,"",14,0xffcccccc);updateStatus();
        text(page,"Test build logs",18,Color.WHITE);
        toggle(page,"Diagnostic logging","Keeps up to 128 KiB locally: hook startup, icon mapping samples, binding counts, blur/capture errors and launcher restart results. Includes device/launcher versions and sampled app package names. No wallpaper images or system logcat. Export happens only when you choose a file.","debug_logs");
        Button export=new Button(this);export.setText("Export logs (.txt)");page.addView(export);
        export.setOnClickListener(v->{
            DebugLog.event(this,"EXPORT_REQUEST","User selected Export logs");
            Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain").putExtra(Intent.EXTRA_TITLE,"devarix-glass-test-log.txt");
            try{startActivityForResult(save,71);}catch(ActivityNotFoundException e){Toast.makeText(this,"No document picker available. Use Copy logs.",Toast.LENGTH_LONG).show();}
        });
        Button copy=new Button(this);copy.setText("Copy logs");page.addView(copy);
        copy.setOnClickListener(v->new Thread(()->{try{String report=DebugLog.snapshot(this);main.post(()->{if(isDestroyed())return;((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("devarix Glass logs",report));Toast.makeText(this,"Logs copied",Toast.LENGTH_SHORT).show();});}catch(Exception e){logFailure(e);}},"glass-copy-log").start());
        Button clear=new Button(this);clear.setText("Clear logs");page.addView(clear);
        clear.setOnClickListener(v->{try{DebugLog.clear(this);Toast.makeText(this,"Logs cleared",Toast.LENGTH_SHORT).show();}catch(Exception e){logFailure(e);}});
        text(page,"Experimental build. Hooks were checked against Kashi V7.5. Liquid Glass currently samples static wallpapers; live wallpapers need the native Frosted mode. A device test is still required. If blur is unavailable, it will not be replaced by a fake blur image.",14,0xffb8b8b8);
        text(page,"Developer: devarix\nOriginal base symbols: 一凡UI / Guofeng 26 Lite. Liquid Glass renderer: QWEA0 / pandadog (MIT). Additional icon sources are listed in the source repository.",13,0xffaaaaaa);
        getContentResolver().registerContentObserver(android.net.Uri.parse("content://"+GlassConfig.AUTHORITY+"/status"),false,observer);
    }
    private void changed(){DebugLog.event(this,"SETTINGS_CHANGED","style="+prefs.getString("style","liquid")+" icons="+prefs.getBoolean("icons",false)+" blur="+prefs.getBoolean("blur",false)+" root_wallpaper="+prefs.getBoolean("root_wallpaper",false));getContentResolver().notifyChange(GlassProvider.SETTINGS,null);}
    private void updateStatus(){
        if(status==null)return;
        long connected=prefs.getLong("connected_at",0);
        String connection=connected==0?"Launcher has not connected. Check LSPosed scope and restart launcher.":"Last launcher settings read: "+android.text.format.DateFormat.format("HH:mm:ss",connected);
        if(checkStarted>0&&connected<checkStarted)connection=checking?"Checking… Open the home screen now.":"No fresh launcher response. Check LSPosed scope, unlock the phone, and restart launcher.";
        status.setText(connection+"\n"+prefs.getString("bindings","No icon binding counts received yet. Open the home screen or app drawer.")+"\n"+prefs.getString("surfaces","Waiting for dock/search surface bindings.")+"\n"+prefs.getString("status","Waiting for launcher."));
    }
    private void restartLauncher(Button button){
        DebugLog.event(this,"RESTART_REQUEST","Current user="+android.os.Process.myUid()/100000);
        button.setEnabled(false);button.setText("Waiting for root…");
        new Thread(()->{
            java.lang.Process process=null;String result;
            try{
                String command=LauncherRestart.command(android.os.Process.myUid()/100000);
                process=new ProcessBuilder("su","-c",command).redirectOutput(new java.io.File("/dev/null")).redirectError(new java.io.File("/dev/null")).start();
                if(!process.waitFor(25,java.util.concurrent.TimeUnit.SECONDS)){process.destroyForcibly();result="Root request timed out. Open the home screen. If needed, reboot to apply the module.";}
                else result=process.exitValue()==0?"Launcher restarted. Open the home screen, then return here to check icon counts.":"Restart failed or root denied. Allow root for devarix Glass and retry, or reboot the phone.";
            }catch(Exception e){DebugLog.event(this,"RESTART_ERROR",android.util.Log.getStackTraceString(e));result="Could not restart launcher: "+e.getClass().getSimpleName()+". Reboot the phone instead.";}
            finally{if(process!=null&&process.isAlive())process.destroyForcibly();}
            DebugLog.event(this,"RESTART_RESULT",result);
            final String message=result;
            main.post(()->{if(isDestroyed())return;button.setEnabled(true);button.setText("Restart launcher");Toast.makeText(this,message,Toast.LENGTH_LONG).show();updateStatus();});
        },"glass-launcher-restart").start();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=71||result!=RESULT_OK||data==null||data.getData()==null)return;
        android.net.Uri destination=data.getData();
        new Thread(()->{
            try{
                String report=DebugLog.snapshot(this);
                try(var stream=getContentResolver().openOutputStream(destination,"wt")){
                    if(stream==null)throw new java.io.IOException("Document unavailable");
                    stream.write(report.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                main.post(()->{if(!isDestroyed())Toast.makeText(this,"Log file saved. You can attach it to this chat.",Toast.LENGTH_LONG).show();});
            }catch(Exception e){logFailure(e);}
        },"glass-export-log").start();
    }
    private void logFailure(Exception e){DebugLog.event(this,"EXPORT_ERROR",android.util.Log.getStackTraceString(e));main.post(()->{if(!isDestroyed())Toast.makeText(this,"Log action failed: "+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();});}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView text(LinearLayout page,String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(10),0,dp(12));page.addView(t);return t;}
    private Switch toggle(LinearLayout page,String title,String subtitle,String key){Switch s=new Switch(this);s.setText(title);s.setTextColor(Color.WHITE);s.setTextSize(18);s.setPadding(0,dp(18),0,dp(8));s.setChecked(prefs.getBoolean(key,("debug_logs".equals(key)||"automatic".equals(key)||"dock_glass".equals(key)||"search_glass".equals(key))));page.addView(s);text(page,subtitle,14,0xffb8b8b8);s.setOnCheckedChangeListener((button,checked)->{prefs.edit().putBoolean(key,checked).commit();changed();});return s;}
    @Override protected void onResume(){super.onResume();updateStatus();}
    @Override protected void onDestroy(){main.removeCallbacksAndMessages(null);getContentResolver().unregisterContentObserver(observer);super.onDestroy();}
}
