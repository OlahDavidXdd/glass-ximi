package io.github.devarix.kashiglass;
import android.view.View;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,35},manifest=Config.NONE)
public class NativeBackdropRequestRuntimeTest {
    static class Calls implements NativeScreenBackdrop.Access {
        int background,mode;boolean ignored,rejected,missing;
        public Object field(Object target,String name){return null;}
        public Object call(Object target,String name,Object...args){
            if(name.startsWith("get")){if(missing)throw new UnsupportedOperationException();return name.contains("Background")?background:mode;}
            if(rejected)return false;if(!ignored){if(name.equals("setMiBackgroundBlurMode"))background=1;if(name.equals("setMiViewBlurMode"))mode=1;}return null;
        }
    }
    private View view(){return new View(RuntimeEnvironment.getApplication());}
    @Test public void acceptedModesAreReadBack() throws Throwable {Calls calls=new Calls();NativeBackdropRequest.configure(calls,view());assertEquals(1,calls.background);assertEquals(1,calls.mode);}
    @Test public void silentlyIgnoredModeIsNotReportedAsConfigured() throws Throwable {Calls calls=new Calls();calls.ignored=true;try{NativeBackdropRequest.configure(calls,view());fail();}catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("returned 0"));}}
    @Test public void explicitRejectionIsPropagated() throws Throwable {Calls calls=new Calls();calls.rejected=true;try{NativeBackdropRequest.configure(calls,view());fail();}catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("rejected"));}}
    @Test public void unavailableOptionalGettersDoNotBreakOlderFirmware() throws Throwable {Calls calls=new Calls();calls.missing=true;NativeBackdropRequest.configure(calls,view());assertEquals(1,calls.mode);}
}
