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
    @Test public void widgetsSelectLiveScreenEvenWhenIconsUseStaticWallpaper(){LiquidGlassDrawable.configure(null,null,true,false);View owner=new View(RuntimeEnvironment.getApplication());Object widget=SharedGlassMaterial.create(owner,null,true,24,s->{},GlassSurface.WIDGET);assertTrue((Boolean)ReflectionHelpers.getField(widget,"screenMatching"));assertFalse(LiquidGlassDrawable.isScreenMatching());java.util.List<String> messages=new java.util.ArrayList<>();android.graphics.drawable.Drawable drawn=SharedGlassMaterial.create(owner,null,true,24,messages::add,GlassSurface.WIDGET);drawn.setBounds(0,0,64,64);drawn.draw(new android.graphics.Canvas(android.graphics.Bitmap.createBitmap(64,64,android.graphics.Bitmap.Config.ARGB_8888)));assertTrue(messages.stream().anyMatch(m->m.contains("not a wallpaper texture")));}
    @Test public void legacyIconAndSearchModesRemainSelectedByTheUser(){LiquidGlassDrawable.configure(null,null,true,false);View owner=new View(RuntimeEnvironment.getApplication());Object icon=new LiquidGlassDrawable(owner,null,null,true,24,s->{}),search=SharedGlassMaterial.create(owner,null,true,24,s->{},GlassSurface.SEARCH);assertFalse((Boolean)ReflectionHelpers.getField(icon,"screenMatching"));assertFalse((Boolean)ReflectionHelpers.getField(search,"screenMatching"));}
}
