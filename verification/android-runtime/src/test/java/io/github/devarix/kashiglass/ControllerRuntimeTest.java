package io.github.devarix.kashiglass;

import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import java.io.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,35},manifest=Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ControllerRuntimeTest {
    private ActivityController<SettingsActivity> controller;private SettingsActivity activity;
    @Before public void setup(){RuntimeEnvironment.setQualifiers("w360dp-h800dp-xhdpi");RuntimeEnvironment.getApplication().getSharedPreferences(GlassConfig.PREFS,0).edit().clear().commit();controller=Robolectric.buildActivity(SettingsActivity.class).setup().visible();activity=controller.get();layout(720,1600);}
    @After public void cleanup(){controller.pause().stop().destroy();}
    private View tag(String value){return activity.getWindow().getDecorView().findViewWithTag(value);}
    private void tab(int i){assertTrue(tag("tab:"+i).performClick());layout(720,1600);}
    private void layout(int w,int h){View root=activity.getWindow().getDecorView();root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));root.layout(0,0,w,h);}
    private void screenshot(String name) throws Exception {org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(700,java.util.concurrent.TimeUnit.MILLISECONDS);View root=activity.getWindow().getDecorView();Bitmap bitmap=Bitmap.createBitmap(720,1600,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));File dir=new File(System.getProperty("glass.results"),"controller");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name+"-api-"+Build.VERSION.SDK_INT+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}}
    @Test public void overviewSeparatesInstalledAndLoadedAndProducesNativeUiScreenshots() throws Exception {assertEquals("Not connected",((TextView)tag("connection:loaded")).getText().toString());screenshot("overview");tab(1);screenshot("appearance");tab(2);screenshot("diagnostics");}
    @Test public void appearanceControlsPersistWithoutResettingOtherPreferences(){tab(1);Switch widgets=(Switch)tag("control:widget_glass");assertTrue(widgets.isChecked());widgets.performClick();assertFalse(activity.getSharedPreferences(GlassConfig.PREFS,0).getBoolean("widget_glass",true));assertFalse(activity.getSharedPreferences(GlassConfig.PREFS,0).getBoolean("icons",false));assertTrue(((Switch)tag("control:screen_match")).isChecked());}
    @Test public void diagnosticsExposeSelectableStatusAndFocusedReportFlow(){tab(2);TextView status=(TextView)tag("diagnostics:status");assertTrue(status.isTextSelectable());assertTrue(status.getText().toString().contains("Loaded hook"));}
    @Test public void freshProviderStatusIsReflectedWithoutInventingALoadedVersion(){activity.getSharedPreferences(GlassConfig.PREFS,0).edit().putLong("connected_at",System.currentTimeMillis()+1000).putInt("hook_code",BuildConfig.VERSION_CODE).putString("hook_version",BuildConfig.VERSION_NAME).commit();activity.getContentResolver().notifyChange(android.net.Uri.parse("content://"+GlassConfig.AUTHORITY+"/status"),null);org.robolectric.shadows.ShadowLooper.idleMainLooper();assertEquals(BuildConfig.VERSION_NAME,((TextView)tag("connection:loaded")).getText().toString());assertEquals("Launcher connected",((TextView)tag("connection:title")).getText().toString());}
    @Test public void selectedTabSurvivesActivityRecreation(){tab(2);controller.recreate();activity=controller.get();layout(720,1600);assertNotNull(tag("diagnostics:status"));}
    @Test public void smallWindowKeepsNativeControlsReachableByScrolling(){tab(1);layout(640,1280);Switch widgets=(Switch)tag("control:widget_glass");assertNotNull(widgets);View parent=widgets;while(parent!=null&&!(parent instanceof ScrollView))parent=parent.getParent() instanceof View?(View)parent.getParent():null;assertTrue(parent instanceof ScrollView);assertTrue(((ScrollView)parent).canScrollVertically(1));}
    @Test public void pendingRestartStaysDisabledAcrossTabChangesAndRotation(){activity.getSharedPreferences(GlassConfig.PREFS,0).edit().putBoolean("restart_ui_pending",true).putLong("restart_ui_started",SystemClock.elapsedRealtime()).commit();tab(2);assertFalse(tag("action:restart").isEnabled());tab(0);assertFalse(tag("action:restart").isEnabled());controller.recreate();activity=controller.get();assertFalse(tag("action:restart").isEnabled());}
    @Test public void expiredRestartIsReportedAsUnconfirmedRatherThanSuccess(){activity.getSharedPreferences(GlassConfig.PREFS,0).edit().putBoolean("restart_ui_pending",true).putLong("restart_ui_started",SystemClock.elapsedRealtime()-31000).commit();tab(2);assertTrue(tag("action:restart").isEnabled());assertTrue(activity.getSharedPreferences(GlassConfig.PREFS,0).getString("restart_status","").contains("not confirmed"));}

    @Test public void enableActionKeepsChosenMaterialAndDefaultsBlurOnlyOnFirstUse(){activity.getSharedPreferences(GlassConfig.PREFS,0).edit().putString("style","frosted").remove("blur").commit();assertTrue(tag("action:enable").performClick());assertEquals("frosted",activity.getSharedPreferences(GlassConfig.PREFS,0).getString("style",""));assertTrue(activity.getSharedPreferences(GlassConfig.PREFS,0).getBoolean("blur",false));}
    @Test public void masterSwitchPreservesExplicitlyDisabledBlur(){activity.getSharedPreferences(GlassConfig.PREFS,0).edit().putBoolean("blur",false).commit();tab(1);((Switch)tag("control:icons")).performClick();assertFalse(activity.getSharedPreferences(GlassConfig.PREFS,0).getBoolean("blur",true));}

}
