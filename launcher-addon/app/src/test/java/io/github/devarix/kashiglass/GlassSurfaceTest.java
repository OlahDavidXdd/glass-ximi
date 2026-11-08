package io.github.devarix.kashiglass;
import org.junit.Test;
import static org.junit.Assert.*;
public class GlassSurfaceTest {
    @org.junit.Test public void widgetRadiusIsBoundedWithoutChangingIconGeometry(){org.junit.Assert.assertEquals(36f,GlassSurface.radius(400,400,GlassSurface.WIDGET,1),.01f);org.junit.Assert.assertEquals(27f,GlassSurface.radius(100,100,GlassSurface.ICON),.01f);}

    @Test public void targetWhitelistExcludesOtherLauncherControls(){
        assertEquals(GlassSurface.DOCK,GlassSurface.kind("com.miui.home.launcher.hotseats.HotSeats"));
        assertEquals(GlassSurface.SEARCH,GlassSurface.kind("com.miui.home.launcher.SearchBarDesktopLayout"));
        assertEquals(GlassSurface.SEARCH,GlassSurface.kind("com.miui.home.launcher.SearchBarDrawerLayout"));
        assertEquals(GlassSurface.NONE,GlassSurface.kind("com.miui.home.dockView.HotseatBackgroundPreview"));
        assertEquals(GlassSurface.NONE,GlassSurface.kind("com.other.SearchBar"));
    }
    @Test public void drawerOnlyClearsKnownStructuralBackdrops(){
        assertEquals(GlassSurface.DRAWER,GlassSurface.kind("com.miui.home.launcher.allapps.AllAppsContainerView"));
        assertTrue(GlassSurface.drawerLayer("com.miui.home.launcher.allapps.AllAppsRecyclerView"));
        assertFalse(GlassSurface.drawerLayer("com.miui.home.launcher.ShortcutIcon"));
        assertFalse(GlassSurface.drawerLayer("android.widget.EditText"));
        assertFalse(GlassSurface.drawerLayer("com.other.AllAppsRecyclerView"));
    }
    @Test public void fullScreenDrawerCornersStayBoundedOnLargeDisplays(){
        assertEquals(108f,GlassSurface.radius(1080,2400,GlassSurface.DRAWER,3),.001f);
        assertEquals(108f,GlassSurface.radius(2400,3400,GlassSurface.DRAWER,3),.001f);
        assertEquals(12f,GlassSurface.radius(100,500,GlassSurface.DRAWER,3),.001f);
    }
    @Test public void searchRadiusRemainsAPillAtDifferentWidths(){
        assertEquals(24f,GlassSurface.radius(120,48,GlassSurface.SEARCH),.001f);
        assertEquals(24f,GlassSurface.radius(600,48,GlassSurface.SEARCH),.001f);
        assertEquals(10f,GlassSurface.radius(20,48,GlassSurface.SEARCH),.001f);
    }
    @Test public void dockAndIconKeepTheirRoundedRectProfiles(){
        assertEquals(32f,GlassSurface.radius(600,100,GlassSurface.DOCK),.001f);
        assertEquals(27f,GlassSurface.radius(100,100,GlassSurface.ICON),.001f);
        assertEquals(0f,GlassSurface.radius(0,100,GlassSurface.DOCK),.001f);
    }
}
