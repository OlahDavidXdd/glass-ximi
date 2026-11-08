package io.github.devarix.kashiglass;
import org.junit.Test;
import static org.junit.Assert.*;
public class GlassConfigTest {
    @Test public void providerRestrictsOtherApplications() {
        assertTrue(GlassConfig.allowedCaller(100,100,null));
        assertTrue(GlassConfig.allowedCaller(101,100,new String[]{"com.miui.home"}));
        assertFalse(GlassConfig.allowedCaller(102,100,new String[]{"com.example.random"}));
        assertFalse(GlassConfig.allowedCaller(102,100,null));
    }
    @Test public void assetNamesRejectTraversalAndAllowKnownPackages() {
        assertTrue(GlassConfig.validPackage("android"));
        assertTrue(GlassConfig.validPackage("io.github.seyud.weave"));
        assertTrue(GlassConfig.validPackage("com.zhiliaoapp.musically.go"));
        assertFalse(GlassConfig.validPackage("../../private"));
        assertFalse(GlassConfig.validPackage("com.example/icon"));
        assertFalse(GlassConfig.validPackage("com.example?secret"));
    }
    @Test public void radiusStaysWithinAUsefulBound() {
        assertEquals(8,GlassConfig.radius(-1)); assertEquals(60,GlassConfig.radius(200)); assertEquals(24,GlassConfig.radius(24));
    }
}
