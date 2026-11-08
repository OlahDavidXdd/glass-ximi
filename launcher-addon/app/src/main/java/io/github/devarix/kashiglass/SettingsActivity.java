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
        text(page,"Material",18,Color.WHITE);
        Spinner material=new Spinner(this);material.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Liquid Glass · QWEA0 renderer","Frosted Glass · previous style"}));material.setSelection("frosted".equals(GlassConfig.style(prefs.getString("style","liquid")))?1:0);page.addView(material);
        material.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,android.view.View v,int position,long id){String style=position==0?"liquid":"frosted";if(!style.equals(GlassConfig.style(prefs.getString("style","liquid")))){prefs.edit().putString("style",style).commit();changed();}}});
        toggle(page,"Use built-in glass icons","700 grayscale mappings · 512 px symbols · Liquid or Frosted material","icons");
        toggle(page,"Backdrop blur","Liquid Glass uses the QWEA0 shader; Frosted Glass uses Xiaomi native blur.","blur");
        toggle(page,"Wallpaper access via root","Optional. If normal wallpaper access is denied, requests root for System launcher to read the current static wallpaper only.","root_wallpaper");
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
    private void updateStatus(){if(status!=null)status.setText(prefs.getString("status","Waiting for launcher. Enable the LSPosed module and restart System launcher after unlocking the phone."));}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView text(LinearLayout page,String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(10),0,dp(12));page.addView(t);return t;}
    private void toggle(LinearLayout page,String title,String subtitle,String key){Switch s=new Switch(this);s.setText(title);s.setTextColor(Color.WHITE);s.setTextSize(18);s.setPadding(0,dp(18),0,dp(8));s.setChecked(prefs.getBoolean(key,false));page.addView(s);text(page,subtitle,14,0xffb8b8b8);s.setOnCheckedChangeListener((button,checked)->{prefs.edit().putBoolean(key,checked).commit();changed();});}
    @Override protected void onDestroy(){getContentResolver().unregisterContentObserver(observer);super.onDestroy();}
}
