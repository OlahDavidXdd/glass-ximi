package io.github.devarix.kashiglass;

import android.graphics.*;
import android.view.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,35},manifest=Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class MamlWidgetRuntimeTest {
    // Models the verified native getter/field boundary, not Xiaomi's MAML renderer or hooks.
    static class Root {Object mInnerGroup;public float getWidth(){return 300;}public float getHeight(){return 300;}}
    static class ElementGroup {Root root;int alpha=255;public int getAlpha(){return alpha;}ElementGroup(Root r){root=r;}public Object getRoot(){return root;}}
    static class ImageScreenElement {
        Root root;String name="widget_bg",src="weather/bg/day.png";float width=300,height=300;boolean mTouchable,mInterceptTouch;String description="";
        ImageScreenElement(Root r){root=r;}public Object getRoot(){return root;}public String getName(){return name;}public String getSrc(){return src;}
        public float getWidth(){return width;}public float getHeight(){return height;}public String getContentDescription(){return description;}
    }
    static class RectangleScreenElement extends ImageScreenElement {RectangleScreenElement(Root r){super(r);name="";src="";}}
    private MamlWidgetGlass glass;private Root root;private ElementGroup group;private View view;private Bitmap pixels;private Canvas canvas;
    @Before public void setup(){
        LiquidGlassDrawable.configure(null,null,true,true);view=new View(RuntimeEnvironment.getApplication());view.layout(0,0,300,300);
        glass=new MamlWidgetGlass(s->{},s->{});glass.configure(true,false,24,new GlassBackdrop(){public boolean isAvailable(){return false;}public boolean draw(Canvas c,View v,Rect r,int m){return false;}});
        root=new Root();group=new ElementGroup(root);root.mInnerGroup=group;glass.bind(view,root);pixels=Bitmap.createBitmap(300,300,Bitmap.Config.ARGB_8888);canvas=new Canvas(pixels);
    }
    static class Info {public String appPackage="com.miui.weather2",title="Vremea",resPath="/system/media/theme/weather_2x2";}
    @Test public void nativeHostDetectionFindsLocalizedWeatherMetadata(){
        MamlWidgetGlass detected=new MamlWidgetGlass(s->{},s->{});detected.configure(true,false,24,new GlassBackdrop(){public boolean isAvailable(){return false;}public boolean draw(Canvas c,View v,Rect r,int m){return false;}});
        com.miui.home.launcher.maml.MaMlHostView nativeHost=new com.miui.home.launcher.maml.MaMlHostView(view.getContext());nativeHost.root=root;nativeHost.info=new Info();detected.apply(nativeHost);
        assertTrue(detected.summary().contains("1 MAML clock/weather bound"));assertTrue(detected.drawBackground(group,canvas));
    }
    @Test public void pendingNativeRootCanBindAfterAsyncLoadAndUnknownCardsAreNotChanged(){
        MamlWidgetGlass detected=new MamlWidgetGlass(s->{},s->{});detected.configure(true,false,24,new GlassBackdrop(){public boolean isAvailable(){return false;}public boolean draw(Canvas c,View v,Rect r,int m){return false;}});
        com.miui.home.launcher.maml.MaMlHostView nativeHost=new com.miui.home.launcher.maml.MaMlHostView(view.getContext());nativeHost.info=new Info();detected.apply(nativeHost);assertTrue(detected.summary().startsWith("0 "));
        nativeHost.root=root;detected.apply(nativeHost);assertTrue(detected.drawBackground(group,canvas));
        Info unsupported=new Info();unsupported.appPackage="calendar";unsupported.resPath="/calendar";unsupported.title="Calendar";
        nativeHost.info=unsupported;Root other=new Root();nativeHost.root=other;detected.apply(nativeHost);assertTrue(detected.summary().startsWith("0 "));assertFalse(detected.skipDecoration(new ImageScreenElement(root)));
    }
    @Test public void widgetUsesTheApprovedSearchUnderlayResetAndRestoresOnDisable(){
        java.util.Map<String,Integer> modes=new java.util.HashMap<>();modes.put("MiBackgroundBlurMode",2);modes.put("MiViewBlurMode",3);final java.util.List<String> messages=new java.util.ArrayList<>();
        MamlWidgetGlass material=new MamlWidgetGlass(messages::add,s->{},(v,method,args)->{
            if(method.startsWith("get"))return modes.getOrDefault(method.substring(3),0);
            if(method.startsWith("set"))modes.put(method.substring(3),((Number)args[0]).intValue());return null;
        });
        org.robolectric.android.controller.ActivityController<android.app.Activity> controller=Robolectric.buildActivity(android.app.Activity.class).setup();controller.get().setContentView(view);
        material.configure(true,false,24,new GlassBackdrop(){public boolean isAvailable(){return false;}public boolean draw(Canvas c,View v,Rect r,int margin){return false;}});material.bind(view,root);
        assertEquals(0,(int)modes.get("MiBackgroundBlurMode"));assertEquals(0,(int)modes.get("MiViewBlurMode"));assertTrue(messages.stream().anyMatch(m->m.startsWith("WIDGET_MATERIAL")));
        material.configure(false,false,24,null);assertEquals(2,(int)modes.get("MiBackgroundBlurMode"));assertEquals(3,(int)modes.get("MiViewBlurMode"));controller.pause().stop().destroy();
    }
    static class ClockGadget extends android.widget.FrameLayout {ClockGadget(android.content.Context context){super(context);}}
    @Test public void legacyClockGadgetUsesItsVerifiedMiAdvancedRoot(){
        ClockGadget parent=new ClockGadget(view.getContext());com.miui.maml.MiAdvancedView legacy=new com.miui.maml.MiAdvancedView(view.getContext());legacy.root=root;parent.addView(legacy);
        MamlWidgetGlass detected=new MamlWidgetGlass(s->{},s->{});detected.configure(true,true,24,new GlassBackdrop(){public boolean isAvailable(){return false;}public boolean draw(Canvas c,View v,Rect r,int margin){return false;}});detected.apply(legacy);assertTrue(detected.summary().startsWith("1 "));assertTrue(detected.drawBackground(group,canvas));
    }
    @Test public void glassRendersInsideOnlyTheRegisteredRootBeforeBackgroundRemoval(){
        ImageScreenElement bg=new ImageScreenElement(root);assertFalse(glass.skipDecoration(bg));
        assertFalse(glass.drawBackground(new ElementGroup(root),canvas));assertTrue(glass.drawBackground(group,canvas));assertTrue(glass.skipDecoration(bg));
        assertTrue(glass.summary().contains("1 rendered"));assertTrue(glass.summary().contains("1 background layers replaced"));
        int visible=0;for(int y=0;y<300;y++)for(int x=0;x<300;x++)if(Color.alpha(pixels.getPixel(x,y))>0)visible++;assertTrue("actual material did not draw",visible>0);
    }
    @Test public void nativeRootFadeAndCanvasSaveStateArePreserved(){
        int saved=canvas.getSaveCount();group.alpha=0;assertTrue(glass.drawBackground(group,canvas));assertEquals(saved,canvas.getSaveCount());
        for(int y=0;y<300;y+=10)for(int x=0;x<300;x+=10)assertEquals(0,Color.alpha(pixels.getPixel(x,y)));
        group.alpha=255;assertTrue(glass.drawBackground(group,canvas));int visible=0;for(int y=0;y<300;y++)for(int x=0;x<300;x++)if(Color.alpha(pixels.getPixel(x,y))>0)visible++;assertTrue(visible>0);assertEquals(saved,canvas.getSaveCount());
    }
    @Test public void unknownOrFlattenedImagesAreRetained(){glass.drawBackground(group,canvas);ImageScreenElement e=new ImageScreenElement(root);e.name="weather_card";e.src="full_clock.png";assertFalse(glass.skipDecoration(e));}
    @Test public void clockHandsWeatherSymbolsTextAndInteractiveLayersStayNative(){
        glass.drawBackground(group,canvas);ImageScreenElement e=new ImageScreenElement(root);e.width=60;e.height=60;assertFalse(glass.skipDecoration(e));
        e.width=300;e.height=300;e.mTouchable=true;assertFalse(glass.skipDecoration(e));e.mTouchable=false;e.description="Weather forecast";assertFalse(glass.skipDecoration(e));
        assertFalse(glass.skipDecoration(new Object()));
    }
    @Test public void largeRectangleCanBeRemovedButSmallFaceMarkersCannot(){glass.drawBackground(group,canvas);RectangleScreenElement e=new RectangleScreenElement(root);assertTrue(glass.skipDecoration(e));e.width=10;assertFalse(glass.skipDecoration(e));}
    @Test public void disableImmediatelyStopsDrawingAndSkippingWithoutMutatingNativeElements(){
        ImageScreenElement bg=new ImageScreenElement(root);glass.drawBackground(group,canvas);assertTrue(glass.skipDecoration(bg));glass.configure(false,false,24,null);assertFalse(glass.drawBackground(group,canvas));assertFalse(glass.skipDecoration(bg));assertEquals("widget_bg",bg.name);assertEquals("weather/bg/day.png",bg.src);
    }
    @Test public void replacementRootsAndDetachedHostsLoseTheirOldRenderBinding(){
        glass.drawBackground(group,canvas);Root next=new Root();ElementGroup nextGroup=new ElementGroup(next);next.mInnerGroup=nextGroup;glass.bind(view,next);
        assertFalse(glass.skipDecoration(new ImageScreenElement(root)));assertFalse(glass.drawBackground(group,canvas));assertTrue(glass.drawBackground(nextGroup,canvas));glass.detach(view);assertFalse(glass.skipDecoration(new ImageScreenElement(next)));
    }
    @Test public void unrelatedMamlRootsAreNeverRestyled(){Root other=new Root();ElementGroup otherGroup=new ElementGroup(other);other.mInnerGroup=otherGroup;assertFalse(glass.drawBackground(otherGroup,canvas));assertFalse(glass.skipDecoration(new ImageScreenElement(other)));}
    @Test public void oversizedNonFiniteAndNonBackgroundImagesRemainNative(){glass.drawBackground(group,canvas);ImageScreenElement e=new ImageScreenElement(root);e.width=Float.NaN;assertFalse(glass.skipDecoration(e));e.width=600;assertFalse(glass.skipDecoration(e));assertTrue(MamlWidgetPolicy.supportedMetadata("/system/theme/clock_2x2"));assertFalse(MamlWidgetPolicy.supportedMetadata("calendar"));}
}
