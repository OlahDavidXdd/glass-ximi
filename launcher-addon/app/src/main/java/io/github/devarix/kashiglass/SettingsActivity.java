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
        ScrollView scroll=new ScrollView(this);LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);int pad=dp(24);page.setPadding(pad,pad,pad,pad);scroll.addView(page);setContentView(scroll);
        text(page,"devarix Glass",30,Color.WHITE);text(page,"For Kashi launcher · HyperOS 3",16,0xffcccccc);
        text(page,"1. LSPosed → Modules → devarix Glass → enable and select System launcher (com.miui.home).\n2. Tap Enable Liquid Glass below.\n3. Tap Restart launcher and allow root, then open your home screen.\nThe icon switch alone cannot enable the LSPosed module.",15,0xffcccccc);
        text(page,"Material",18,Color.WHITE);
        Spinner material=new Spinner(this);material.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Liquid Glass · QWEA0 renderer","Frosted Glass · previous style"}));material.setSelection("frosted".equals(GlassConfig.style(prefs.getString("style","liquid")))?1:0);page.addView(material);
        material.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,android.view.View v,int position,long id){String style=position==0?"liquid":"frosted";if(!style.equals(GlassConfig.style(prefs.getString("style","liquid")))){prefs.edit().putString("style",style).commit();changed();}}});
        iconsSwitch=toggle(page,"Use built-in glass icons","700 grayscale mappings · 512 px symbols · Liquid or Frosted material","icons");
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
        text(page,"Experimental build. Hooks were checked against Kashi V7.5. Liquid Glass currently samples static wallpapers; live wallpapers need the native Frosted mode. A device test is still required. If blur is unavailable, it will not be replaced by a fake blur image.",14,0xffb8b8b8);
        text(page,"Developer: devarix\nOriginal base symbols: 一凡UI / Guofeng 26 Lite. Liquid Glass renderer: QWEA0 / pandadog (MIT). Additional icon sources are listed in the source repository.",13,0xffaaaaaa);
        getContentResolver().registerContentObserver(android.net.Uri.parse("content://"+GlassConfig.AUTHORITY+"/status"),false,observer);
    }
    private void changed(){getContentResolver().notifyChange(GlassProvider.SETTINGS,null);}
    private void updateStatus(){
        if(status==null)return;
        long connected=prefs.getLong("connected_at",0);
        String connection=connected==0?"Launcher has not connected. Check LSPosed scope and restart launcher.":"Last launcher settings read: "+android.text.format.DateFormat.format("HH:mm:ss",connected);
        if(checkStarted>0&&connected<checkStarted)connection=checking?"Checking… Open the home screen now.":"No fresh launcher response. Check LSPosed scope, unlock the phone, and restart launcher.";
        status.setText(connection+"\n"+prefs.getString("bindings","No icon binding counts received yet. Open the home screen or app drawer.")+"\n"+prefs.getString("status","Waiting for launcher."));
    }
    private void restartLauncher(Button button){
        button.setEnabled(false);button.setText("Waiting for root…");
        new Thread(()->{
            java.lang.Process process=null;String result;
            try{
                String command=LauncherRestart.command(android.os.Process.myUid()/100000);
                process=new ProcessBuilder("su","-c",command).redirectOutput(new java.io.File("/dev/null")).redirectError(new java.io.File("/dev/null")).start();
                if(!process.waitFor(25,java.util.concurrent.TimeUnit.SECONDS)){process.destroyForcibly();result="Root request timed out. Open the home screen. If needed, reboot to apply the module.";}
                else result=process.exitValue()==0?"Launcher restarted. Open the home screen, then return here to check icon counts.":"Restart failed or root denied. Allow root for devarix Glass and retry, or reboot the phone.";
            }catch(Exception e){result="Could not restart launcher: "+e.getClass().getSimpleName()+". Reboot the phone instead.";}
            finally{if(process!=null&&process.isAlive())process.destroyForcibly();}
            final String message=result;
            main.post(()->{if(isDestroyed())return;button.setEnabled(true);button.setText("Restart launcher");Toast.makeText(this,message,Toast.LENGTH_LONG).show();updateStatus();});
        },"glass-launcher-restart").start();
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView text(LinearLayout page,String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(10),0,dp(12));page.addView(t);return t;}
    private Switch toggle(LinearLayout page,String title,String subtitle,String key){Switch s=new Switch(this);s.setText(title);s.setTextColor(Color.WHITE);s.setTextSize(18);s.setPadding(0,dp(18),0,dp(8));s.setChecked(prefs.getBoolean(key,false));page.addView(s);text(page,subtitle,14,0xffb8b8b8);s.setOnCheckedChangeListener((button,checked)->{prefs.edit().putBoolean(key,checked).commit();changed();});return s;}
    @Override protected void onResume(){super.onResume();updateStatus();}
    @Override protected void onDestroy(){main.removeCallbacksAndMessages(null);getContentResolver().unregisterContentObserver(observer);super.onDestroy();}
}
