package io.github.devarix.kashiglass;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class DiagnosticLogTest {
    @Rule public TemporaryFolder folder=new TemporaryFolder();
    @Test public void survivesReopenAndCanClear() throws Exception {
        DiagnosticLog log=new DiagnosticLog(folder.getRoot());log.append("START","connected");
        DiagnosticLog reopened=new DiagnosticLog(folder.getRoot());
        assertTrue(reopened.snapshot().contains("START connected"));reopened.clear();assertEquals("",log.snapshot());
    }
    @Test public void rotatesAndRetainsRecentEventsWithinBound() throws Exception {
        DiagnosticLog log=new DiagnosticLog(folder.getRoot());String payload="x".repeat(2000);
        for(int i=0;i<250;i++)log.append("ROW",i+" "+payload);
        String snapshot=log.snapshot();assertTrue(snapshot.contains("ROW 249 "));assertFalse(snapshot.contains("ROW 0 "));
        assertTrue(snapshot.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=2*DiagnosticLog.SEGMENT_BYTES);
    }
    @Test public void multilineInputCannotForgeAnotherLogEntry() throws Exception {
        DiagnosticLog log=new DiagnosticLog(folder.getRoot());log.append("EVENT\nFAKE","message\r\nFORGED\tvalue");
        assertEquals(1,log.snapshot().lines().count());assertTrue(log.snapshot().contains("EVENT FAKE message  FORGED value"));
    }
    @Test public void concurrentWritersKeepWholeEntries() throws Exception {
        DiagnosticLog log=new DiagnosticLog(folder.getRoot());
        java.util.concurrent.ExecutorService workers=java.util.concurrent.Executors.newFixedThreadPool(4);
        java.util.List<java.util.concurrent.Future<?>> futures=new java.util.ArrayList<>();
        for(int i=0;i<100;i++){final int value=i;futures.add(workers.submit(()->{try{log.append("ROW","id="+value);}catch(Exception e){throw new RuntimeException(e);}}));}
        for(var future:futures)future.get();workers.shutdown();assertEquals(100,log.snapshot().lines().count());
    }
}
