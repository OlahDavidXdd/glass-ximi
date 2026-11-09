package io.github.devarix.kashiglass;
import org.junit.Test;
import static org.junit.Assert.*;
public class SupportReportTest {
    @Test public void keepsBuildContextAndReturnEventsWithoutWidgetSpam(){String report=SupportReport.animation("Build: current\nLoaded hook: older\n\nT WIDGET frame\nT RETURN_STATE slot_owned=false\nT SOURCE_RETURN fixed\nT ANIMATION input");assertTrue(report.contains("Loaded hook: older"));assertTrue(report.contains("slot_owned=false"));assertTrue(report.contains("SOURCE_RETURN"));assertFalse(report.contains("WIDGET frame"));}
    @Test public void emptyEventReportGivesReproductionInstruction(){assertTrue(SupportReport.animation("Build: test\n\nT WIDGET render").contains("open and close an app"));}
    @Test public void reportWithoutEventSeparatorIsRetained(){assertTrue(SupportReport.animation("Build: missing logs").contains("Build: missing logs"));}
}
