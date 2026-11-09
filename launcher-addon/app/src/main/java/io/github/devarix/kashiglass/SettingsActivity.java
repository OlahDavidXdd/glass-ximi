package io.github.devarix.kashiglass;

import android.app.Activity;
import android.content.*;
import android.database.ContentObserver;
import android.graphics.Color;
import android.os.*;
import android.widget.*;
import android.view.*;

public final class SettingsActivity extends Activity {
    private android.content.SharedPreferences prefs;
    private TextView status,restartResult;
    private Switch iconsSwitch,blurSwitch;
    private ControllerUi ui;private LinearLayout body;private ScrollView scroll;private int selectedTab;
    private TextView connectionTitle,connectionDetail,loadedVersion;
    private Button restartButton;
    private final Runnable restartExpiry=this::updateRestartUi;
    private final Button[] navigation=new Button[3];
    private final Handler main=new Handler(Looper.getMainLooper());
    private long checkStarted;
    private boolean checking;
    private final Runnable checkResult=()->{checking=false;updateStatus();};
    private final ContentObserver observer=new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override public void onChange(boolean selfChange) { updateStatus(); }
    };
    @Override public void onCreate(Bundle state) {
        setTheme(android.R.style.Theme_Material_NoActionBar);super.onCreate(state);prefs=getSharedPreferences(GlassConfig.PREFS,0);ui=new ControllerUi(this);
        DebugLog.event(this,"APP_OPEN",BuildConfig.VERSION_NAME);getWindow().setStatusBarColor(ControllerUi.BG);getWindow().setNavigationBarColor(ControllerUi.BG);
        LinearLayout root=ui.column();root.setBackgroundColor(ControllerUi.BG);root.setFitsSystemWindows(true);
        LinearLayout header=ui.row();header.setPadding(dp(20),dp(18),dp(20),dp(12));LinearLayout brand=ui.column();ui.text(brand,"devarix Glass",22,ControllerUi.INK,true);ui.text(brand,"KASHI · HYPEROS",10,ControllerUi.DIM,true);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));TextView version=ui.label(BuildConfig.VERSION_NAME,12,ControllerUi.MUTED,false);header.addView(version);root.addView(header);
        scroll=new ScrollView(this);scroll.setClipToPadding(false);scroll.setFillViewport(true);body=ui.column();int margin=Math.max(20,(getResources().getConfiguration().screenWidthDp-620)/2);body.setPadding(dp(margin),dp(8),dp(margin),dp(24));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=ui.row();nav.setPadding(dp(12),dp(10),dp(12),dp(10));nav.setBackgroundColor(0xff111318);String[] titles={"Overview","Appearance","Diagnostics"};for(int i=0;i<3;i++){final int tab=i;Button button=ui.button(titles[i],false);button.setTextSize(12);button.setTag("tab:"+i);button.setContentDescription(titles[i]);navigation[i]=button;LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);params.setMargins(dp(3),0,dp(3),0);nav.addView(button,params);button.setOnClickListener(v->renderTab(tab));}root.addView(nav);setContentView(root);
        getContentResolver().registerContentObserver(android.net.Uri.parse("content://"+GlassConfig.AUTHORITY+"/status"),false,observer);renderTab(state==null?0:state.getInt("tab",0));
    }
    private void renderTab(int tab){
        selectedTab=Math.max(0,Math.min(2,tab));body.removeAllViews();status=restartResult=connectionTitle=connectionDetail=loadedVersion=null;restartButton=null;iconsSwitch=blurSwitch=null;
        for(int i=0;i<3;i++){navigation[i].setSelected(i==selectedTab);ui.navigation(navigation[i],i==selectedTab);}
        if(selectedTab==0)overview();else if(selectedTab==1)appearance();else diagnostics();updateStatus();scroll.scrollTo(0,0);
    }
    private void connectionCard(){
        LinearLayout card=ui.card(body);ui.text(card,"LAUNCHER CONNECTION",11,ControllerUi.DIM,true);connectionTitle=ui.text(card,"Checking connection…",21,ControllerUi.INK,true);connectionTitle.setTag("connection:title");connectionDetail=ui.text(card,"",14,ControllerUi.MUTED,false);ui.divider(card);LinearLayout metrics=ui.row();ui.metric(metrics,"INSTALLED APP",BuildConfig.VERSION_NAME);loadedVersion=ui.metric(metrics,"LOADED HOOK","Unconfirmed");loadedVersion.setTag("connection:loaded");card.addView(metrics);
        Button restart=ui.action(card,"Restart launcher",true);restartButton=restart;restart.setTag("action:restart");restart.setOnClickListener(v->restartLauncher(restart));Button refresh=ui.action(card,"Refresh connection",false);refresh.setOnClickListener(v->refreshConnection());restartResult=ui.text(card,prefs.getString("restart_status",""),12,ControllerUi.MUTED,false);
    }
    private void overview(){
        LinearLayout hero=ui.row();LinearLayout copy=ui.column();ui.text(copy,"YOUR LAUNCHER",11,ControllerUi.DIM,true);ui.text(copy,"A clearer\npoint of view.",32,ControllerUi.INK,true);ui.text(copy,"Glass controls, all in one place.",14,ControllerUi.MUTED,false);hero.addView(copy,new LinearLayout.LayoutParams(0,-2,1));hero.addView(ui.emblem(),new LinearLayout.LayoutParams(dp(100),dp(120)));body.addView(hero);ui.space(body,12);connectionCard();
        if(!prefs.getBoolean("icons",false)){LinearLayout enable=ui.card(body);ui.text(enable,"Ready when you are",19,ControllerUi.INK,true);ui.text(enable,"Enable glass after selecting System launcher in LSPosed.",14,ControllerUi.MUTED,false);Button enableAction=ui.action(enable,"Enable glass",true);enableAction.setTag("action:enable");enableAction.setOnClickListener(v->enableGlass());}
        LinearLayout active=ui.card(body);ui.text(active,"Your setup",20,ControllerUi.INK,true);ui.text(active,"Selected settings · not a rendering verification",12,ControllerUi.DIM,false);setupLine(active,"Icons",prefs.getBoolean("icons",false)?("frosted".equals(prefs.getString("style","liquid"))?"Frosted":"Liquid Glass"):"Paused");setupLine(active,"Backdrop",prefs.getBoolean("screen_match",true)?"Live screen mode":"Static wallpaper lens");setupLine(active,"Widgets",prefs.getBoolean("widget_glass",true)?prefs.getBoolean("blur",false)?"Live screen mode":"Clear glass · blur off":"Native appearance");ui.action(active,"Customize appearance",false).setOnClickListener(v->renderTab(1));
        LinearLayout help=ui.card(body);ui.text(help,"First time here?",18,ControllerUi.INK,true);ui.text(help,"1  Enable this module in LSPosed.\n2  Select System launcher (com.miui.home).\n3  Restart, then refresh this connection.",14,ControllerUi.MUTED,false);ui.action(help,"Open Home",false).setOnClickListener(v->openHome());ui.text(body,"Designed by devarix · Experimental launcher add-on",12,ControllerUi.DIM,false);
    }
    private void setupLine(LinearLayout card,String name,String value){LinearLayout row=ui.row();TextView label=ui.label(name,14,ControllerUi.MUTED,false);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));TextView state=ui.label(value,13,ControllerUi.INK,true);state.setMaxWidth(dp(200));row.addView(state);row.setPadding(0,dp(9),0,dp(9));card.addView(row);}
    private void appearance(){
        ui.section(body,"Make it yours","Choose a material and the surfaces it touches.");LinearLayout icons=ui.card(body);ui.text(icons,"Icon material",19,ControllerUi.INK,true);LinearLayout choices=ui.row();String[] names={"Liquid Glass","Frosted"};for(int i=0;i<2;i++){final String style=i==0?"liquid":"frosted";Button pick=ui.button(names[i],style.equals(prefs.getString("style","liquid")));LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);params.setMargins(0,0,dp(6),dp(8));choices.addView(pick,params);pick.setOnClickListener(v->{prefs.edit().putString("style",style).commit();changed();renderTab(1);});}icons.addView(choices);
        iconsSwitch=toggle(icons,"Enable glass","Master switch for icons, surfaces and widgets.","icons");toggle(icons,"White symbols","Keep the foreground bright and readable.","white_symbols");toggle(icons,"Automatic icons","Generate a silver symbol for apps outside the pack.","automatic");ui.action(icons,"Artwork preview",false).setOnClickListener(v->startActivity(new Intent(this,PreviewActivity.class)));ui.text(icons,"The preview illustrates artwork. It does not verify launcher blur.",12,ControllerUi.DIM,false);
        LinearLayout surfaces=ui.card(body);ui.text(surfaces,"Launcher surfaces",19,ControllerUi.INK,true);toggle(surfaces,"Dock","The shared glass material behind your dock.","dock_glass");toggle(surfaces,"Search & page pill","Native controls, with a Search-sized page pill.","search_glass");toggle(surfaces,"App drawer","Glass behind the grid; native scrolling and search.","drawer_glass");toggle(surfaces,"Clock & weather","Live backdrop requested; provider content retained.","widget_glass");
        LinearLayout backdrop=ui.card(body);ui.text(backdrop,"Backdrop",19,ControllerUi.INK,true);toggle(backdrop,"Live screen backdrop","For icons and launcher surfaces. Off uses a still wallpaper lens. Widgets request live mode independently.","screen_match");blurSwitch=toggle(backdrop,"Backdrop blur","Requires Xiaomi firmware support. An unavailable live backend leaves a clear rim.","blur");TextView value=ui.text(backdrop,"Blur strength · "+GlassConfig.radius(prefs.getInt("radius",24)),16,ControllerUi.INK,true);SeekBar strength=new SeekBar(this);strength.setContentDescription("Blur strength");strength.setThumbTintList(android.content.res.ColorStateList.valueOf(ControllerUi.INK));strength.setProgressTintList(android.content.res.ColorStateList.valueOf(ControllerUi.INK));strength.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff424751));strength.setMax(52);strength.setProgress(GlassConfig.radius(prefs.getInt("radius",24))-8);backdrop.addView(strength);strength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int progress,boolean user){value.setText("Blur strength · "+(progress+8));}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){prefs.edit().putInt("radius",s.getProgress()+8).commit();changed();}});
        LinearLayout advanced=ui.card(body);Button expand=ui.action(advanced,"Advanced options",false);LinearLayout extra=ui.column();extra.setVisibility(View.GONE);advanced.addView(extra);toggle(extra,"Static wallpaper access via root","Optional fallback for the wallpaper lens. Live widgets do not use this wallpaper tile.","root_wallpaper");expand.setOnClickListener(v->{boolean show=extra.getVisibility()!=View.VISIBLE;extra.setVisibility(show?View.VISIBLE:View.GONE);expand.setText(show?"Hide advanced options":"Advanced options");});
    }
    private void diagnostics(){
        ui.section(body,"Let’s find the cause","Connection and focused reports for real launcher issues.");connectionCard();LinearLayout report=ui.card(body);ui.text(report,"Animation report",19,ControllerUi.INK,true);ui.text(report,"1  Keep diagnostic logging on.\n2  Open an app and return Home.\n3  Copy the report with its return-state events.",14,ControllerUi.MUTED,false);toggle(report,"Diagnostic logging","Up to 128 KiB locally, including device/build and sampled app package names. No wallpaper images or system logcat.","debug_logs");ui.action(report,"Copy animation report",true).setOnClickListener(v->copyReport(true));ui.action(report,"Open Home",false).setOnClickListener(v->openHome());
        LinearLayout detail=ui.card(body);ui.text(detail,"Live status details",18,ControllerUi.INK,true);status=ui.text(detail,"Waiting for launcher",12,ControllerUi.MUTED,false);status.setTypeface(android.graphics.Typeface.MONOSPACE);status.setTextIsSelectable(true);status.setTag("diagnostics:status");
        LinearLayout logs=ui.card(body);ui.text(logs,"All diagnostics",18,ControllerUi.INK,true);ui.action(logs,"Copy full report",false).setOnClickListener(v->copyReport(false));ui.action(logs,"Save report as file",false).setOnClickListener(v->exportReport());ui.action(logs,"Clear local logs",false).setOnClickListener(v->{try{DebugLog.clear(this);updateStatus();Toast.makeText(this,"Logs cleared",Toast.LENGTH_SHORT).show();}catch(Exception e){logFailure(e);}});ui.text(logs,"Reports are shared only through the action you choose. Native blur and final transition pixels need a phone check.",12,ControllerUi.DIM,false);
        ui.text(body,"devarix · QWEA0 / pandadog renderer (MIT)\nBase symbols: 一凡UI / Guofeng 26 Lite. Further attribution is in the repository.",12,ControllerUi.DIM,false);
    }
    private void enableGlass(){android.content.SharedPreferences.Editor edit=prefs.edit().putBoolean("icons",true);if(!prefs.contains("blur"))edit.putBoolean("blur",true);edit.commit();changed();renderTab(selectedTab);Toast.makeText(this,"Glass enabled. Restart launcher if the hook is not connected.",Toast.LENGTH_LONG).show();}
    private void openHome(){try{startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(GlassConfig.LAUNCHER));}catch(ActivityNotFoundException error){Toast.makeText(this,"Press Home to open System launcher.",Toast.LENGTH_LONG).show();}}
    private void refreshConnection(){checkStarted=System.currentTimeMillis();checking=true;changed();updateStatus();main.removeCallbacks(checkResult);main.postDelayed(checkResult,4000);}
    private void copyReport(boolean focused){new Thread(()->{try{String full=DebugLog.snapshot(this),report=focused?SupportReport.animation(full):full;main.post(()->{if(isDestroyed())return;((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("devarix Glass report",report));Toast.makeText(this,focused?"Animation report copied":"Full report copied",Toast.LENGTH_SHORT).show();});}catch(Exception error){logFailure(error);}},"glass-copy-report").start();}
    private void exportReport(){DebugLog.event(this,"EXPORT_REQUEST","User selected Save report");Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain").putExtra(Intent.EXTRA_TITLE,"devarix-glass-test-log.txt");try{startActivityForResult(save,71);}catch(ActivityNotFoundException error){Toast.makeText(this,"No document picker available. Use Copy full report.",Toast.LENGTH_LONG).show();}}
    @Override protected void onSaveInstanceState(Bundle state){state.putInt("tab",selectedTab);super.onSaveInstanceState(state);}
    private void changed(){DebugLog.event(this,"SETTINGS_CHANGED","style="+prefs.getString("style","liquid")+" icons="+prefs.getBoolean("icons",false)+" blur="+prefs.getBoolean("blur",false)+" root_wallpaper="+prefs.getBoolean("root_wallpaper",false));getContentResolver().notifyChange(GlassProvider.SETTINGS,null);}
    private void updateStatus(){
        updateRestartUi();
        ControllerStatus model=ControllerStatus.read(BuildConfig.VERSION_CODE,prefs.getInt("hook_code",0),prefs.getString("hook_version","Unreported"),prefs.getLong("connected_at",0),checkStarted,checking,prefs.getBoolean("icons",false));
        if(connectionTitle!=null){connectionTitle.setText(model.title);connectionTitle.setTextColor(model.connected?0xffc9ead7:ControllerUi.INK);}if(connectionDetail!=null)connectionDetail.setText(model.detail);if(loadedVersion!=null)loadedVersion.setText(model.loaded);
        if(status==null)return;
        long connected=prefs.getLong("connected_at",0);
        String connection=connected==0?"Launcher has not connected. Check LSPosed scope and restart launcher.":"Last launcher settings read: "+android.text.format.DateFormat.format("HH:mm:ss",connected);
        if(checkStarted>0&&connected<checkStarted)connection=checking?"Checking… Open the home screen now.":"No fresh launcher response. Check LSPosed scope, unlock the phone, and restart launcher.";
        String hook=HookVersion.status(BuildConfig.VERSION_CODE,BuildConfig.VERSION_NAME,prefs.getInt("hook_code",0),prefs.getString("hook_version","unreported"),connected,checkStarted);
        status.setText(hook+"\n"+connection+"\n"+prefs.getString("bindings","No icon binding counts received yet. Open the home screen or app drawer.")+"\n"+prefs.getString("surfaces","Waiting for dock/search surface bindings.")+"\n"+prefs.getString("widgets","Waiting for clock/weather widgets.")+"\nLogged glass animation hand-offs: "+prefs.getInt("animation_replacements",0)+"\n"+prefs.getString("last_animation","No native animation inputs logged yet.")+"\n"+prefs.getString("status","Waiting for launcher."));
    }
    private void updateRestartUi(){
        boolean pending=prefs.getBoolean("restart_ui_pending",false);long age=SystemClock.elapsedRealtime()-prefs.getLong("restart_ui_started",0);String token=prefs.getString("restart_ui_token",null);
        if(pending&&token!=null&&token.equals(prefs.getString("restart_ack",null))){prefs.edit().putBoolean("restart_ui_pending",false).putString("restart_status","Launcher accepted the restart request. Refresh the loaded hook to confirm.").apply();pending=false;}
        if(pending&&(age<0||age>=30000)){prefs.edit().putBoolean("restart_ui_pending",false).putString("restart_status","Restart is not confirmed. Refresh the connection or reboot before retrying.").apply();pending=false;}
        if(restartButton!=null){restartButton.setEnabled(!pending);restartButton.setText(pending?"Restart pending…":"Restart launcher");}
        if(restartResult!=null)restartResult.setText(prefs.getString("restart_status",""));main.removeCallbacks(restartExpiry);if(pending)main.postDelayed(restartExpiry,Math.max(100,30000-age));
    }
    private void restartLauncher(Button button){
        updateRestartUi();if(prefs.getBoolean("restart_ui_pending",false))return;
        prefs.edit().putBoolean("restart_ui_pending",true).putLong("restart_ui_started",SystemClock.elapsedRealtime()).remove("restart_ui_token").putString("restart_status","Restart requested; awaiting acknowledgement.").commit();updateRestartUi();
        if(HookVersion.canRestart(prefs.getString("hook_capabilities",""),prefs.getInt("hook_pid",0))){restartThroughHook(button);return;}
        restartWithRoot(button);
    }
    private void restartThroughHook(Button button){
        button.setEnabled(false);button.setText("Requesting launcher restart…");
        new Thread(()->{
            try{Bundle result=getContentResolver().call(GlassProvider.RESTART,"request_restart",null,null);
                String token=result==null?null:result.getString("token");if(token==null)throw new IllegalStateException("No restart token");
                prefs.edit().putString("restart_ui_token",token).apply();
                main.post(()->awaitRestart(button,token,SystemClock.elapsedRealtime()+3500));
            }catch(Exception e){DebugLog.event(this,"SELF_RESTART_REQUEST_ERROR",android.util.Log.getStackTraceString(e));main.post(()->finishRestart(button,"The launcher did not accept the restart request. Reboot the phone to load the updated hook."));}
        },"glass-hook-restart").start();
    }
    private void awaitRestart(Button button,String token,long deadline){
        if(isDestroyed())return;
        if(token.equals(prefs.getString("restart_ack",""))){
            finishRestart(button,"Launcher hook acknowledged restart. Opening home; return here to check the loaded version.");
            main.postDelayed(()->{if(isDestroyed())return;try{startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(GlassConfig.LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(ActivityNotFoundException e){Toast.makeText(this,"Press Home to start System launcher.",Toast.LENGTH_LONG).show();}},300);
        }else if(SystemClock.elapsedRealtime()>=deadline)finishRestart(button,"No launcher acknowledgement. Restart is unconfirmed. Reboot the phone to load the updated hook.");
        else main.postDelayed(()->awaitRestart(button,token,deadline),100);
    }
    private void finishRestart(Button button,String message){
        prefs.edit().putString("restart_status",message).putBoolean("restart_ui_pending",false).apply();getContentResolver().notifyChange(android.net.Uri.parse("content://"+GlassConfig.AUTHORITY+"/status"),null);if(isDestroyed())return;button.setEnabled(true);button.setText("Restart launcher");
        if(restartResult!=null)restartResult.setText(message);Toast.makeText(this,message,Toast.LENGTH_LONG).show();updateStatus();
    }
    private void restartWithRoot(Button button){
        DebugLog.event(this,"RESTART_REQUEST","Current user="+android.os.Process.myUid()/100000);
        button.setEnabled(false);button.setText("Waiting for root…");
        new Thread(()->{
            java.lang.Process process=null;String result;
            try{
                String command=LauncherRestart.command(android.os.Process.myUid()/100000);
                process=new ProcessBuilder("su","-c",command).redirectOutput(new java.io.File("/dev/null")).redirectError(new java.io.File("/dev/null")).start();
                if(!process.waitFor(25,java.util.concurrent.TimeUnit.SECONDS)){process.destroyForcibly();result="Root request timed out. Open the home screen. If needed, reboot to apply the module.";}
                else result=process.exitValue()==0?"Root restart command completed. Open home, then check Loaded hook to confirm the version.":"Restart failed or root denied. Allow root for devarix Glass and retry, or reboot the phone.";
            }catch(Exception e){DebugLog.event(this,"RESTART_ERROR",android.util.Log.getStackTraceString(e));result=e instanceof java.io.IOException&&String.valueOf(e.getMessage()).contains("Permission denied")?"Root execution is blocked for devarix Glass. The launcher was NOT restarted. Reboot the phone to load the updated hook.":"Could not restart launcher: "+e.getClass().getSimpleName()+". Reboot the phone instead.";}
            finally{if(process!=null&&process.isAlive())process.destroyForcibly();}
            DebugLog.event(this,"RESTART_RESULT",result);
            final String message=result;
            main.post(()->finishRestart(button,message));
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
    private Switch toggle(LinearLayout page,String title,String subtitle,String key){
        boolean fallback="debug_logs".equals(key)||"automatic".equals(key)||"dock_glass".equals(key)||"search_glass".equals(key)||"drawer_glass".equals(key)||"widget_glass".equals(key)||"white_symbols".equals(key)||"screen_match".equals(key);
        Switch control=ui.toggle(page,title,subtitle,prefs.getBoolean(key,fallback));control.setTag("control:"+key);control.setOnCheckedChangeListener((button,checked)->{android.content.SharedPreferences.Editor edit=prefs.edit().putBoolean(key,checked);if("icons".equals(key)&&checked&&!prefs.contains("blur"))edit.putBoolean("blur",true);edit.commit();if("icons".equals(key)&&blurSwitch!=null&&blurSwitch.isChecked()!=prefs.getBoolean("blur",false))blurSwitch.setChecked(prefs.getBoolean("blur",false));changed();updateStatus();});return control;
    }
    @Override protected void onResume(){super.onResume();checkStarted=System.currentTimeMillis();checking=true;main.removeCallbacks(checkResult);main.postDelayed(checkResult,4000);updateStatus();getContentResolver().notifyChange(GlassProvider.SETTINGS,null);}
    @Override protected void onDestroy(){main.removeCallbacksAndMessages(null);getContentResolver().unregisterContentObserver(observer);super.onDestroy();}
}
