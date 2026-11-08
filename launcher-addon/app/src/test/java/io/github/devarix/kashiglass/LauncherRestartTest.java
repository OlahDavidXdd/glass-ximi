package io.github.devarix.kashiglass;
import org.junit.Test;
import static org.junit.Assert.*;
public class LauncherRestartTest {
    @Test public void restartTargetsOnlyLauncherAndCurrentUser(){
        String command=LauncherRestart.command(10);
        assertEquals("am force-stop --user 10 com.miui.home && am start --user 10 -a android.intent.action.MAIN -c android.intent.category.HOME -p com.miui.home",command);
        assertFalse(command.contains("clear"));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectInvalidUser(){LauncherRestart.command(-1);}
    @Test(expected=IllegalArgumentException.class) public void rejectOutOfRangeUser(){LauncherRestart.command(100000);}
}
