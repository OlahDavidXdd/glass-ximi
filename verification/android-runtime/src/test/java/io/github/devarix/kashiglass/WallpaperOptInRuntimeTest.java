package io.github.devarix.kashiglass;
import java.io.IOException;
import java.lang.reflect.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,35},manifest=Config.NONE)
public class WallpaperOptInRuntimeTest {
    private void denied(boolean active) throws Exception {
        WallpaperBackdrop source=new WallpaperBackdrop(RuntimeEnvironment.getApplication(),()->{},s->{});
        ReflectionHelpers.setField(source,"active",active);ReflectionHelpers.setField(source,"rootAllowed",false);
        Method read=WallpaperBackdrop.class.getDeclaredMethod("readRootWallpaper");read.setAccessible(true);
        try{read.invoke(source);fail("disabled capture accepted");}catch(InvocationTargetException failure){assertTrue(failure.getCause() instanceof IOException);assertTrue(failure.getCause().getMessage().contains("disabled"));}
    }
    @Test public void inactiveSourceDoesNotStartRootRead() throws Exception {denied(false);}
    @Test public void revokedOptInDoesNotStartRootRead() throws Exception {denied(true);}
}
