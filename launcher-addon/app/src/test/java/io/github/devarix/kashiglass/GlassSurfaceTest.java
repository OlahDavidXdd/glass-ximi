package io.github.devarix.kashiglass;
import org.junit.Test;
import static org.junit.Assert.*;
public class GlassSurfaceTest {
    @Test public void targetWhitelistExcludesOtherLauncherControls(){
        assertEquals(GlassSurface.DOCK,GlassSurface.kind("com.miui.home.launcher.hotseats.HotSeats"));
        assertEquals(GlassSurface.SEARCH,GlassSurface.kind("com.miui.home.launcher.SearchBarDesktopLayout"));
        assertEquals(GlassSurface.SEARCH,GlassSurface.kind("com.miui.home.launcher.SearchBarDrawerLayout"));
        assertEquals(GlassSurface.NONE,GlassSurface.kind("com.miui.home.dockView.HotseatBackgroundPreview"));
        assertEquals(GlassSurface.NONE,GlassSurface.kind("com.other.SearchBar"));
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
