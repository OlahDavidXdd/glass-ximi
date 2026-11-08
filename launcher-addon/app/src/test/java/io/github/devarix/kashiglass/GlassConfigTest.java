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
    @Test public void materialCannotChooseArbitraryAssets(){
        assertTrue(GlassConfig.validStyle("liquid"));assertTrue(GlassConfig.validStyle("frosted"));assertFalse(GlassConfig.validStyle("../secret"));assertEquals("liquid",GlassConfig.style(null));assertEquals("glyphs",GlassConfig.assetDirectory("liquid"));assertEquals("icons",GlassConfig.assetDirectory("frosted"));
    }
    @Test public void rootWallpaperPathIsRestrictedToNumericCurrentUsers(){assertEquals("/data/system/users/0/wallpaper",GlassConfig.wallpaperPath(0));assertEquals("/data/system/users/10/wallpaper",GlassConfig.wallpaperPath(10));}
    @Test(expected=IllegalArgumentException.class) public void invalidUserCannotConstructRootPath(){GlassConfig.wallpaperPath(-1);}
    @Test public void activityIconsTakePriorityOverPackageFallback(){
        java.util.Set<String> names=java.util.Set.of("com.android.contacts","com.android.contacts.activities.TwelveKeyDialer");
        assertEquals("com.android.contacts.activities.TwelveKeyDialer",GlassConfig.mapping("com.android.contacts","com.android.contacts.activities.TwelveKeyDialer",names));
        assertEquals("com.android.contacts",GlassConfig.mapping("com.android.contacts","other.Activity",names));assertNull(GlassConfig.mapping("not.mapped",null,names));
    }
    @Test public void automaticArtworkDoesNotOverrideCuratedMappings(){
        var names=java.util.Set.of("com.example.known");
        assertEquals("com.example.known",GlassConfig.iconKey("com.example.known","com.example.known.Main",names,true));
        assertEquals("auto:com.example.newapp:com.example.newapp.Main",GlassConfig.iconKey("com.example.newapp","com.example.newapp.Main",names,true));
        assertNull(GlassConfig.iconKey("com.example.newapp",null,names,false));
    }
    @Test public void automaticKeysDoNotTurnInvalidPackagesIntoAssets(){
        assertNull(GlassConfig.iconKey("../private",null,java.util.Set.of(),true));
        assertTrue(GlassConfig.automaticKey("auto:com.example.app:"));
        assertFalse(GlassConfig.automaticKey("com.example.app"));
    }
    @Test public void radiusStaysWithinAUsefulBound() {
        assertEquals(8,GlassConfig.radius(-1)); assertEquals(60,GlassConfig.radius(200)); assertEquals(24,GlassConfig.radius(24));
    }
}
