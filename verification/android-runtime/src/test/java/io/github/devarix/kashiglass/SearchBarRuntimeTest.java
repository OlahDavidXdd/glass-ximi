package io.github.devarix.kashiglass;

import android.app.Activity;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** Native firmware calls are recorded/rejected, not emulated: verifies routing and recovery only. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,35},manifest=Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SearchBarRuntimeTest {
    private Activity activity;private FrameLayout host;private ImageView body;private TextView label;
    private final List<String> messages=new ArrayList<>();
    private static final class NativeCalls implements SearchBarBlur.Calls {
        final Map<View,Map<String,Integer>> values=new IdentityHashMap<>();int count;boolean reject;
        final List<View> enabledTargets=new ArrayList<>();
        public Object call(View v,String method,Object... args){
            count++;Map<String,Integer> state=values.computeIfAbsent(v,k->new HashMap<>());
            if(method.startsWith("get"))return state.getOrDefault(method.substring(3),0);
            if(method.startsWith("set")){
                if(reject&&method.equals("setMiViewBlurMode"))return false;
                state.put(method.substring(3),((Number)args[0]).intValue());
                if(method.equals("setMiBackgroundBlurMode")&&((Number)args[0]).intValue()==1)enabledTargets.add(v);
            }
            return null;
        }
    }
    private NativeCalls calls;
    @Before public void setup(){
        activity=Robolectric.buildActivity(Activity.class).setup().get();host=new FrameLayout(activity);body=new ImageView(activity);label=new TextView(activity);label.setText("Search");label.setClickable(true);
        host.addView(body,new FrameLayout.LayoutParams(160,48));host.addView(label,new FrameLayout.LayoutParams(160,48));activity.setContentView(host);
        host.layout(0,0,200,60);body.layout(0,0,160,48);label.layout(0,0,160,48);calls=new NativeCalls();
        calls.values.put(body,new HashMap<>(Map.of("MiBackgroundBlurMode",2,"MiViewBlurMode",3,"MiBackgroundBlurRadius",12)));
        calls.values.put(host,new HashMap<>(Map.of("MiBackgroundBlurMode",4,"MiViewBlurMode",5)));
    }
    @After public void teardown(){activity.finish();}
    @Test public void blurTargetsTheAttachedDecorativeChildOnly(){
        assertTrue(body.isAttachedToWindow());new SearchBarBlur(body,host,calls,messages::add).update(true,72,true);
        assertEquals(List.of(body),calls.enabledTargets);assertEquals(72,(int)calls.values.get(body).get("MiBackgroundBlurRadius"));
        assertEquals("Search",label.getText().toString());assertTrue(label.isClickable());assertFalse(label.getClipToOutline());assertTrue(body.getClipToOutline());
    }
    @Test public void unchangedFramesDoNotRepeatNativeRequests(){
        SearchBarBlur blur=new SearchBarBlur(body,host,calls,messages::add);blur.update(true,72,true);int count=calls.count;blur.update(true,72,false);assertEquals(count,calls.count);
        blur.update(true,80,false);assertEquals(80,(int)calls.values.get(body).get("MiBackgroundBlurRadius"));
    }
    @Test public void disableRestoresModesRadiusAndOutline(){
        ViewOutlineProvider outline=body.getOutlineProvider();SearchBarBlur blur=new SearchBarBlur(body,host,calls,messages::add);blur.update(true,72,true);blur.update(false,72,false);
        assertEquals(2,(int)calls.values.get(body).get("MiBackgroundBlurMode"));assertEquals(3,(int)calls.values.get(body).get("MiViewBlurMode"));assertEquals(12,(int)calls.values.get(body).get("MiBackgroundBlurRadius"));
        assertEquals(4,(int)calls.values.get(host).get("MiBackgroundBlurMode"));assertSame(outline,body.getOutlineProvider());assertFalse(body.getClipToOutline());
    }
    @Test public void rejectedFirmwareRequestRollsBackAndDoesNotRetryEveryFrame(){
        calls.reject=true;SearchBarBlur blur=new SearchBarBlur(body,host,calls,messages::add);blur.update(true,72,true);int count=calls.count;blur.update(true,72,false);
        assertEquals(count,calls.count);assertFalse(body.getClipToOutline());assertTrue(messages.stream().anyMatch(s->s.contains("unavailable")));assertEquals("Search",label.getText().toString());
    }
    @Test public void unattachedBackgroundWaitsForAttachment(){
        host.removeView(body);SearchBarBlur blur=new SearchBarBlur(body,host,calls,messages::add);blur.update(true,72,true);assertEquals(0,calls.count);assertTrue(messages.get(0).contains("pending"));
    }
    @Test public void searchRimHasNoOpaqueFillEvenWhenIconsUseStaticWallpaper(){
        LiquidGlassDrawable.configure(null,null,true,false);SearchGlassDrawable rim=new SearchGlassDrawable(1);rim.setBounds(0,0,160,48);
        Bitmap result=Bitmap.createBitmap(160,48,Bitmap.Config.ARGB_8888);rim.draw(new Canvas(result));assertEquals(0,Color.alpha(result.getPixel(80,24)));assertTrue(Color.alpha(result.getPixel(80,0))>0);assertFalse(LiquidGlassDrawable.isScreenMatching());
    }
    @Test public void hiddenSearchDecorationDrawsNoPixels(){
        SearchGlassDrawable rim=new SearchGlassDrawable(1);rim.setBounds(0,0,160,48);rim.setAlpha(0);Bitmap result=Bitmap.createBitmap(160,48,Bitmap.Config.ARGB_8888);rim.draw(new Canvas(result));assertEquals(0,Color.alpha(result.getPixel(80,0)));
    }
    @Test public void sharedMaterialDisablesBothNativeUnderlaysWithoutTouchingText(){
        new SearchBarBlur(body,host,calls,messages::add).useSharedMaterial(true);
        for(View v:List.of(body,host)){assertEquals(0,(int)calls.values.get(v).get("MiBackgroundBlurMode"));assertEquals(0,(int)calls.values.get(v).get("MiViewBlurMode"));}
        assertTrue(calls.enabledTargets.isEmpty());assertEquals("Search",label.getText().toString());assertTrue(label.isClickable());assertFalse(label.getClipToOutline());
    }
    @Test public void sharedMaterialDeduplicatesAndReassertsAfterNativeStyleRefresh(){
        SearchBarBlur material=new SearchBarBlur(body,host,calls,messages::add);material.useSharedMaterial(true);int count=calls.count;material.useSharedMaterial(false);assertEquals(count,calls.count);
        calls.values.get(body).put("MiViewBlurMode",3);material.useSharedMaterial(true);assertEquals(0,(int)calls.values.get(body).get("MiViewBlurMode"));
    }
    @Test public void sharedMaterialRestoresOriginalNativeModesOnDisable(){
        ViewOutlineProvider outline=body.getOutlineProvider();SearchBarBlur material=new SearchBarBlur(body,host,calls,messages::add);material.useSharedMaterial(true);material.restore();
        assertEquals(2,(int)calls.values.get(body).get("MiBackgroundBlurMode"));assertEquals(3,(int)calls.values.get(body).get("MiViewBlurMode"));assertEquals(4,(int)calls.values.get(host).get("MiBackgroundBlurMode"));assertEquals(5,(int)calls.values.get(host).get("MiViewBlurMode"));assertSame(outline,body.getOutlineProvider());
    }
    @Test public void rejectedUnderlayResetIsReportedWithoutPerFrameRetries(){
        calls.reject=true;SearchBarBlur material=new SearchBarBlur(body,host,calls,messages::add);material.useSharedMaterial(true);int count=calls.count;material.useSharedMaterial(false);assertEquals(count,calls.count);assertTrue(messages.stream().anyMatch(s->s.contains("underlay reset unavailable")));assertTrue(calls.enabledTargets.isEmpty());
    }
    private GlassBackdrop noCapture(){return new GlassBackdrop(){public boolean isAvailable(){return false;}public boolean draw(Canvas c,View v,Rect r,int margin){return false;}};}
    @Test public void searchDockAndDrawerUseTheSameMaterialInBothModes(){
        for(boolean screenMatching:new boolean[]{true,false}){
            LiquidGlassDrawable.configure(null,null,true,screenMatching);Integer reference=null;
            for(int kind:new int[]{GlassSurface.DOCK,GlassSurface.SEARCH,GlassSurface.DRAWER,GlassSurface.WIDGET}){
                android.graphics.drawable.Drawable material=SharedGlassMaterial.create(body,noCapture(),false,24,messages::add,kind);assertTrue(material instanceof LiquidGlassDrawable);material.setBounds(0,0,160,48);
                Bitmap pixels=Bitmap.createBitmap(160,48,Bitmap.Config.ARGB_8888);material.draw(new Canvas(pixels));int center=pixels.getPixel(80,24);
                assertTrue("opaque search fill",Color.alpha(center)<100);if(kind==GlassSurface.WIDGET&&!screenMatching){assertEquals(0,Color.alpha(center));continue;}if(reference==null)reference=center;else assertEquals((int)reference,center);
            }
        }
    }
    @Test public void sharedHiddenMaterialDrawsNoPixels(){
        LiquidGlassDrawable.configure(null,null,true,true);android.graphics.drawable.Drawable material=SharedGlassMaterial.create(body,noCapture(),false,24,messages::add,GlassSurface.SEARCH);material.setBounds(0,0,160,48);material.setAlpha(0);
        Bitmap pixels=Bitmap.createBitmap(160,48,Bitmap.Config.ARGB_8888);material.draw(new Canvas(pixels));for(int y=0;y<48;y++)for(int x=0;x<160;x++)assertEquals(0,Color.alpha(pixels.getPixel(x,y)));
    }
    @Test public void sharedMaterialWaitsUntilTheRealBodyIsAttached(){
        host.removeView(body);SearchBarBlur material=new SearchBarBlur(body,host,calls,messages::add);material.useSharedMaterial(true);assertEquals(0,calls.count);host.addView(body,new FrameLayout.LayoutParams(160,48));body.layout(0,0,160,48);material.useSharedMaterial(false);assertEquals(0,(int)calls.values.get(body).get("MiViewBlurMode"));
    }

}
