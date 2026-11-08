package io.github.devarix.kashiglass;
import org.junit.Test;
import static org.junit.Assert.*;
public class AnimationPolicyTest {
    private static final String TARGET="com.miui.home.common.animate.LaunchAppAndBackHomeAnimTarget",DRAWABLE="android.graphics.drawable.Drawable";
    @Test public void nativeInitializerReplacesExplicitDrawableEvenWhenGettersAreBypassed(){
        assertEquals(2,AnimationPolicy.drawableIndex(new String[]{"com.miui.home.launcher.BaseLauncher",TARGET,DRAWABLE,"com.android.systemui.shared.recents.system.SurfaceControlCompat","boolean"}));
    }
    @Test public void nestedInitializerUsesItsOwnParameterPosition(){assertEquals(1,AnimationPolicy.drawableIndex(new String[]{TARGET,DRAWABLE}));}
    @Test public void unrelatedDrawableMethodsAreNeverPatched(){
        assertEquals(-1,AnimationPolicy.drawableIndex(new String[]{"android.content.Context",DRAWABLE}));
        assertEquals(-1,AnimationPolicy.drawableIndex(new String[]{TARGET,"boolean",DRAWABLE}));
        assertEquals(-1,AnimationPolicy.drawableIndex(new String[]{"com.example.LaunchAppAndBackHomeAnimTarget",DRAWABLE}));
    }
    @Test public void missingDrawableCannotProduceAnInvalidIndex(){assertEquals(-1,AnimationPolicy.drawableIndex(new String[]{}));assertEquals(-1,AnimationPolicy.drawableIndex(new String[]{TARGET}));}
}
