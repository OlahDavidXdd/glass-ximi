package io.github.devarix.kashiglass;

import android.view.View;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,35},manifest=Config.NONE)
public class WidgetLivePolicyRuntimeTest {
    @Test public void widgetsSelectLiveScreenEvenWhenIconsUseStaticWallpaper(){LiquidGlassDrawable.configure(null,null,true,false);View owner=new View(RuntimeEnvironment.getApplication());Object widget=SharedGlassMaterial.create(owner,null,true,24,s->{},GlassSurface.WIDGET);assertTrue((Boolean)ReflectionHelpers.getField(widget,"screenMatching"));assertFalse(LiquidGlassDrawable.isScreenMatching());}
    @Test public void legacyIconAndSearchModesRemainSelectedByTheUser(){LiquidGlassDrawable.configure(null,null,true,false);View owner=new View(RuntimeEnvironment.getApplication());Object icon=new LiquidGlassDrawable(owner,null,null,true,24,s->{}),search=SharedGlassMaterial.create(owner,null,true,24,s->{},GlassSurface.SEARCH);assertFalse((Boolean)ReflectionHelpers.getField(icon,"screenMatching"));assertFalse((Boolean)ReflectionHelpers.getField(search,"screenMatching"));}
}
