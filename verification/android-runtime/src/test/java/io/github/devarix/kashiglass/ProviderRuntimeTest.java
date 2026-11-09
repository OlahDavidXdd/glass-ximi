package io.github.devarix.kashiglass;

import android.content.*;
import android.content.pm.*;
import android.os.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.*;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,35},manifest=Config.NONE)
public class ProviderRuntimeTest {
    private GlassProvider provider;private SharedPreferences prefs;private int own,launcher;
    @Before public void setup(){
        Context context=RuntimeEnvironment.getApplication();own=android.os.Process.myUid();launcher=own+720;
        PackageInfo pkg=new PackageInfo();pkg.packageName=GlassConfig.LAUNCHER;pkg.applicationInfo=new ApplicationInfo();pkg.applicationInfo.packageName=pkg.packageName;pkg.applicationInfo.uid=launcher;Shadows.shadowOf(context.getPackageManager()).installPackage(pkg);
        assertTrue(java.util.Arrays.asList(context.getPackageManager().getPackagesForUid(launcher)).contains(GlassConfig.LAUNCHER));
        ShadowBinder.setCallingUid(own);ShadowBinder.setCallingPid(android.os.Process.myPid());ReflectionHelpers.setStaticField(DebugLog.class,"log",null);
        provider=Robolectric.buildContentProvider(GlassProvider.class).create().get();prefs=context.getSharedPreferences(GlassConfig.PREFS,0);prefs.edit().clear().putBoolean("debug_logs",false).commit();
    }
    @Test public void untrustedUidCannotReadOrMutateProvider(){ShadowBinder.setCallingUid(own+9999);try{provider.call("settings",null,null);fail("untrusted caller accepted");}catch(SecurityException expected){}}
    @Test public void reportedPidComesFromBinderNotUntrustedExtras(){ShadowBinder.setCallingUid(launcher);ShadowBinder.setCallingPid(4567);Bundle identity=new Bundle();identity.putInt("hook_pid",9999);identity.putInt("hook_code",12);identity.putString("hook_version","test");provider.call("settings",null,identity);assertEquals(4567,prefs.getInt("hook_pid",0));}
    @Test public void uiReadDoesNotPretendLauncherConnected(){prefs.edit().putLong("connected_at",7).commit();provider.call("settings",null,null);assertEquals(7,prefs.getLong("connected_at",0));}
    private String request(){ShadowBinder.setCallingUid(own);prefs.edit().putInt("hook_pid",2222).putString("hook_capabilities",HookVersion.RESTART_CAPABILITY).commit();return provider.call("request_restart",null,null).getString("token");}
    @Test public void spoofedPidCannotConsumeAnotherProcessesRestart(){request();ShadowBinder.setCallingUid(launcher);ShadowBinder.setCallingPid(3333);Bundle spoof=new Bundle();spoof.putInt("pid",2222);assertNull(provider.call("consume_restart",null,spoof).getString("token"));assertNotNull(prefs.getString("restart_token",null));}
    @Test public void actualConnectedPidCanConsumeOnlyOnce(){String token=request();ShadowBinder.setCallingUid(launcher);ShadowBinder.setCallingPid(2222);Bundle ignored=new Bundle();ignored.putInt("pid",3333);assertEquals(token,provider.call("consume_restart",null,ignored).getString("token"));assertNull(provider.call("consume_restart",null,ignored).getString("token"));}
}
