package io.github.devarix.kashiglass;
import org.junit.Test;
import static org.junit.Assert.*;
public class HookVersionTest {
    @Test public void historicalMatchingVersionDoesNotPretendToBeAFreshConnection(){String stale=HookVersion.status(12,"0.11",12,"0.11",100,101);assertTrue(stale.contains("no fresh response"));assertFalse(stale.contains("matches app"));assertTrue(HookVersion.status(12,"0.11",12,"0.11",102,101).contains("matches app"));}
    @Test public void installedAppDoesNotProveItsHookIsLoaded(){assertFalse(HookVersion.matches(8,7));assertFalse(HookVersion.matches(8,0));assertTrue(HookVersion.matches(8,8));}
    @Test public void legacyConnectionRequiresAVersionReport(){String status=HookVersion.status(8,"0.7.0-test",0,"unreported",100);assertTrue(status.contains("older hook"));assertTrue(status.contains("Reboot"));}
    @Test public void mismatchNamesBothBuilds(){String status=HookVersion.status(8,"0.7.0-test",7,"0.6.0-test",100);assertTrue(status.contains("0.7.0-test"));assertTrue(status.contains("0.6.0-test"));assertTrue(status.contains("Versions differ"));}
    @Test public void capabilityMustBeAnExactTokenAndHaveAPid(){assertTrue(HookVersion.canRestart("other,self_restart_v1",123));assertFalse(HookVersion.canRestart("not_self_restart_v1",123));assertFalse(HookVersion.canRestart("self_restart_v1",0));assertFalse(HookVersion.canRestart(null,123));}
}
