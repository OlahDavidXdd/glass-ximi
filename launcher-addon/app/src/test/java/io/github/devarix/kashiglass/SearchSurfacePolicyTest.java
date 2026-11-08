package io.github.devarix.kashiglass;
import org.junit.Test;
import static org.junit.Assert.*;
public class SearchSurfacePolicyTest {
    @Test public void newBuiltinHostIsExplicitlyRecognized(){assertTrue(SearchSurfacePolicy.quickHost("com.miui.home.launcher.search.QuickSearchBarWidgetView"));assertFalse(SearchSurfacePolicy.quickHost("com.other.search.QuickSearchBarWidgetView"));}
    @Test public void barSurfaceCanBeSmallerThanWidgetSlot(){assertTrue(SearchSurfacePolicy.coversBar(400,96,320,48));assertTrue(SearchSurfacePolicy.coversBar(400,96,400,96));}
    @Test public void iconsThinIndicatorsAndOffscreenContainersAreExcluded(){assertFalse(SearchSurfacePolicy.coversBar(400,96,32,48));assertFalse(SearchSurfacePolicy.coversBar(400,96,400,12));assertFalse(SearchSurfacePolicy.coversBar(400,96,900,96));assertFalse(SearchSurfacePolicy.coversBar(0,0,400,96));}
}
