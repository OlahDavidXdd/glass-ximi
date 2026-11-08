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
    private final ContentObserver observer=new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override public void onChange(boolean selfChange) { updateStatus(); }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);prefs=getSharedPreferences(GlassConfig.PREFS,0);
        ScrollView scroll=new ScrollView(this);LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);int pad=dp(24);page.setPadding(pad,pad,pad,pad);scroll.addView(page);setContentView(scroll);
        text(page,"devarix Glass",30,Color.WHITE);text(page,"For Kashi launcher · HyperOS 3",16,0xffcccccc);
        text(page,"Enable this module in LSPosed, scope it to System launcher (com.miui.home), then restart the launcher. The same controls are available from its Icons settings entry.",15,0xffcccccc);
        toggle(page,"Use built-in glass icons","700 grayscale mappings · 512 px · 75% background opacity","icons");
        toggle(page,"Frosted backdrop","Use Xiaomi native background blur behind the glass icons. Requires compatible firmware.","blur");
        TextView radius=text(page,"Blur radius: "+GlassConfig.radius(prefs.getInt("radius",24))+" dp",18,Color.WHITE);
        SeekBar seek=new SeekBar(this);seek.setMax(52);seek.setProgress(GlassConfig.radius(prefs.getInt("radius",24))-8);page.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int value,boolean user){radius.setText("Blur radius: "+(value+8)+" dp");}
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){prefs.edit().putInt("radius",bar.getProgress()+8).commit();changed();}
        });
        text(page,"Launcher status",18,Color.WHITE);status=text(page,"",14,0xffcccccc);updateStatus();
        text(page,"Experimental build. Native APIs and hooks were checked against Kashi V7.5; a device test is still required. If blur is unavailable, it will not be replaced by a fake blur image.",14,0xffb8b8b8);
        text(page,"Developer: devarix\nOriginal base symbols: 一凡UI / Guofeng 26 Lite. Additional icon sources are listed in the source repository.",13,0xffaaaaaa);
        getContentResolver().registerContentObserver(android.net.Uri.parse("content://"+GlassConfig.AUTHORITY+"/status"),false,observer);
    }
    private void changed(){getContentResolver().notifyChange(GlassProvider.SETTINGS,null);}
    private void updateStatus(){if(status!=null)status.setText(prefs.getString("status","Waiting for launcher. Enable the LSPosed module and restart System launcher after unlocking the phone."));}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView text(LinearLayout page,String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(10),0,dp(12));page.addView(t);return t;}
    private void toggle(LinearLayout page,String title,String subtitle,String key){Switch s=new Switch(this);s.setText(title);s.setTextColor(Color.WHITE);s.setTextSize(18);s.setPadding(0,dp(18),0,dp(8));s.setChecked(prefs.getBoolean(key,false));page.addView(s);text(page,subtitle,14,0xffb8b8b8);s.setOnCheckedChangeListener((button,checked)->{prefs.edit().putBoolean(key,checked).commit();changed();});}
    @Override protected void onDestroy(){getContentResolver().unregisterContentObserver(observer);super.onDestroy();}
}
