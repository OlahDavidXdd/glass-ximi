package io.github.devarix.kashiglass;
import org.junit.Test;
import static org.junit.Assert.*;
public class ControllerStatusTest {
    @Test public void savedMatchingVersionIsNotPresentedAsLive(){ControllerStatus status=ControllerStatus.read(20,20,"0.19",100,200,false,true);assertFalse(status.connected);assertEquals("Unconfirmed",status.loaded);assertEquals("No fresh launcher reply",status.title);}
    @Test public void freshMatchingReplyConnects(){ControllerStatus status=ControllerStatus.read(20,20,"0.19",300,200,false,true);assertTrue(status.connected);assertEquals("0.19",status.loaded);}
    @Test public void freshOldHookRequiresRestart(){ControllerStatus status=ControllerStatus.read(20,19,"0.18",300,200,false,true);assertFalse(status.connected);assertEquals("Restart to update",status.title);assertEquals("0.18",status.loaded);}
    @Test public void noConnectionDoesNotUseInstalledVersionAsLoaded(){ControllerStatus status=ControllerStatus.read(20,0,"saved",0,200,true,true);assertEquals("Not connected",status.loaded);assertFalse(status.connected);}
    @Test public void connectedPausedGlassIsDescribedAccurately(){ControllerStatus status=ControllerStatus.read(20,20,"0.19",300,200,false,false);assertTrue(status.connected);assertTrue(status.detail.contains("Enable glass"));}
}
