package io.github.devarix.kashiglass;
import org.junit.Test;
import static org.junit.Assert.*;
public class RestartHandshakeTest {
    private static final String TOKEN="12345678-1234-1234-1234-123456789abc";
    @Test public void explicitRequestOnlyTargetsTheReportedProcess(){assertTrue(RestartHandshake.valid(TOKEN,100,100,101,101));assertFalse(RestartHandshake.valid(TOKEN,100,100,101,102));}
    @Test public void delayedRequestsExpire(){assertTrue(RestartHandshake.valid(TOKEN,100,5100,101,101));assertFalse(RestartHandshake.valid(TOKEN,100,5101,101,101));}
    @Test public void rebootedClockOrMissingTimestampRejectsRequest(){assertFalse(RestartHandshake.valid(TOKEN,100,99,101,101));assertFalse(RestartHandshake.valid(TOKEN,-1,100,101,101));}
    @Test public void consumedOrMalformedTokensCannotRestart(){assertFalse(RestartHandshake.valid(null,100,100,101,101));assertFalse(RestartHandshake.valid("------------------------------------",100,100,101,101));assertFalse(RestartHandshake.valid("arbitrary command",100,100,101,101));}
    @Test public void invalidProcessIdsAreNeverAccepted(){assertFalse(RestartHandshake.valid(TOKEN,100,100,0,0));assertFalse(RestartHandshake.valid(TOKEN,100,100,-1,-1));}
}
