package io.github.devarix.kashiglass;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,35},manifest=Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PageIndicatorRuntimeTest {
    private Activity activity;private FrameLayout host;private LinearLayout dots;private ImageView dot;private Drawable original,image;private PageIndicatorGlass glass;
    private final java.util.Map<View,java.util.Map<String,Integer>> nativeModes=new java.util.IdentityHashMap<>();private int calls;
    private GlassBackdrop backdrop(){return new GlassBackdrop(){public boolean isAvailable(){return false;}public boolean draw(Canvas c,View v,Rect r,int margin){return false;}};}
    @Before public void setup(){
        activity=Robolectric.buildActivity(Activity.class).setup().get();host=new FrameLayout(activity);dots=new LinearLayout(activity);dot=new ImageView(activity);image=new ColorDrawable(Color.WHITE);dot.setImageDrawable(image);dot.setSelected(true);dot.setContentDescription("Page 1 of 3");dot.setClickable(true);dots.addView(dot,new LinearLayout.LayoutParams(12,12));host.addView(dots,new FrameLayout.LayoutParams(100,40));activity.setContentView(host);host.layout(0,0,200,80);dots.layout(0,0,100,40);dot.layout(0,0,12,12);original=new ColorDrawable(Color.BLACK);dots.setBackground(original);dots.setPaddingRelative(7,3,9,5);
        nativeModes.put(dots,new java.util.HashMap<>(java.util.Map.of("MiBackgroundBlurMode",2,"MiViewBlurMode",3)));
        glass=new PageIndicatorGlass((view,method,args)->{calls++;java.util.Map<String,Integer> map=nativeModes.computeIfAbsent(view,v->new java.util.HashMap<>());if(method.startsWith("get"))return map.getOrDefault(method.substring(3),0);if(method.startsWith("set"))map.put(method.substring(3),((Number)args[0]).intValue());return null;},s->{});
        LiquidGlassDrawable.configure(null,null,true,true);glass.configure(true,true,24,backdrop());
    }
    @After public void cleanup(){activity.finish();}
    @Test public void nativeDotsSelectionAndActionsSurviveSharedPill(){glass.bind(host,dots,false);assertTrue(dots.getBackground() instanceof LiquidGlassDrawable);assertSame(image,dot.getDrawable());assertTrue(dot.isSelected());assertTrue(dot.isClickable());assertEquals("Page 1 of 3",dot.getContentDescription());assertEquals(1,dots.getChildCount());assertEquals(0,(int)nativeModes.get(dots).get("MiViewBlurMode"));}
    @Test public void changingNativeTargetRestoresOldCardAndDoesNotForceVisibility(){glass.bind(host,dots,false);LinearLayout next=new LinearLayout(activity);host.addView(next);next.layout(0,0,90,40);next.setVisibility(View.GONE);glass.bind(host,next,false);assertSame(original,dots.getBackground());assertEquals(View.GONE,next.getVisibility());assertEquals(0,glass.visibleCount());}
    @Test public void disableRestoresBackgroundTintModesAndPadding(){ColorStateList tint=ColorStateList.valueOf(Color.BLUE);dots.setBackgroundTintList(tint);glass.bind(host,dots,false);glass.configure(false,true,24,backdrop());assertSame(original,dots.getBackground());assertEquals(tint,dots.getBackgroundTintList());assertEquals(2,(int)nativeModes.get(dots).get("MiBackgroundBlurMode"));assertEquals(3,(int)nativeModes.get(dots).get("MiViewBlurMode"));assertEquals(7,dots.getPaddingStart());assertEquals(9,dots.getPaddingEnd());}
    @Test public void nativeStyleRefreshReplacesReinsertedBackdropAndReassertsUnderlay(){glass.bind(host,dots,false);Drawable update=new ColorDrawable(Color.RED);dots.setBackground(update);nativeModes.get(dots).put("MiViewBlurMode",3);glass.bind(host,dots,true);assertTrue(dots.getBackground() instanceof LiquidGlassDrawable);assertEquals(0,(int)nativeModes.get(dots).get("MiViewBlurMode"));glass.configure(false,false,24,backdrop());assertSame(update,dots.getBackground());}
    @Test public void rtlPaddingAndNativeMeasuredGeometryStayUnchanged(){dots.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);int width=dots.getWidth(),height=dots.getHeight();glass.bind(host,dots,false);assertEquals(width,dots.getWidth());assertEquals(height,dots.getHeight());assertEquals(7,dots.getPaddingStart());assertEquals(9,dots.getPaddingEnd());}
    @Test public void pendingAttachmentAndUnchangedFramesDoNotLoseOrRepeatReset(){host.removeView(dots);glass.bind(host,dots,false);assertEquals(0,calls);host.addView(dots,new FrameLayout.LayoutParams(100,40));dots.layout(0,0,100,40);glass.bind(host,dots,false);assertTrue(calls>0);int count=calls;glass.bind(host,dots,false);assertEquals(count,calls);}
    @Test public void detachRestoresAndRebindingInstallsLiveMaterialAgain(){glass.bind(host,dots,false);glass.detach(host);assertSame(original,dots.getBackground());glass.bind(host,dots,false);assertTrue(dots.getBackground() instanceof LiquidGlassDrawable);}
}
