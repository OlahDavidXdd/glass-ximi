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
@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,35},manifest=Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SwipeGlassRuntimeTest {
    private org.robolectric.android.controller.ActivityController<Activity> host;private LiquidNavigationBar bar;private final List<Integer> chosen=new ArrayList<>();private long time;
    @Before public void setup(){RuntimeEnvironment.getApplication().getApplicationInfo().flags|=android.content.pm.ApplicationInfo.FLAG_SUPPORTS_RTL;host=Robolectric.buildActivity(Activity.class).setup().visible();Activity activity=host.get();FrameLayout root=new FrameLayout(activity);View scene=new View(activity);root.addView(scene);bar=new LiquidNavigationBar(activity,scene,chosen::add);for(int i=0;i<3;i++){Button button=new Button(activity);button.setText("Tab "+i);bar.addTab(button);}root.addView(bar,new FrameLayout.LayoutParams(600,100));activity.setContentView(root);root.measure(View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));root.layout(0,0,600,800);bar.select(0,false);}
    @After public void cleanup(){host.pause().stop().destroy();}
    private void event(int action,float x,float y){MotionEvent event=MotionEvent.obtain(0,time+=20,action,x,y,0);try{bar.dispatchTouchEvent(event);}finally{event.recycle();}}
    @Test public void dragFollowsFingerAndCommitsOnlyOnRelease(){event(MotionEvent.ACTION_DOWN,70,50);event(MotionEvent.ACTION_MOVE,510,50);assertTrue("position="+bar.selectionPosition()+" bar="+bar.getWidth()+" tabs="+bar.getChildAt(1).getWidth(),bar.selectionPosition()>1.5f);assertTrue(chosen.isEmpty());event(MotionEvent.ACTION_UP,510,50);assertEquals(Arrays.asList(2),chosen);assertEquals(2,bar.selectedIndex());}
    @Test public void cancelledDragKeepsCurrentTab(){event(MotionEvent.ACTION_DOWN,70,50);event(MotionEvent.ACTION_MOVE,510,50);event(MotionEvent.ACTION_CANCEL,510,50);assertTrue(chosen.isEmpty());assertEquals(0,bar.selectedIndex());Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(1,java.util.concurrent.TimeUnit.SECONDS);assertEquals(0,bar.selectionPosition(),.01f);}
    @Test public void verticalMovementDoesNotSelectATab(){event(MotionEvent.ACTION_DOWN,70,50);event(MotionEvent.ACTION_MOVE,75,250);event(MotionEvent.ACTION_UP,75,250);assertTrue(chosen.isEmpty());assertEquals(0,bar.selectedIndex());}
    @Test public void rtlDragAndOvershootStayWithinValidTabs(){bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);bar.requestLayout();bar.layout(0,0,600,100);assertEquals(View.LAYOUT_DIRECTION_RTL,bar.getLayoutDirection());event(MotionEvent.ACTION_DOWN,510,50);event(MotionEvent.ACTION_MOVE,-500,50);assertEquals(2,bar.selectionPosition(),.01f);event(MotionEvent.ACTION_UP,-500,50);assertEquals(Arrays.asList(2),chosen);}
    @Test public void nativeButtonTapSelectsWithoutDragging(){View tabs=bar.getChildAt(1);assertTrue(((android.view.ViewGroup)tabs).getChildAt(1).performClick());assertEquals(Arrays.asList(1),chosen);assertEquals(1,bar.selectedIndex());}
    @Test public void motionSnapshotHasGlassBodyAndTransparentCorners(){Bitmap glyph=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);glyph.setPixel(96,96,Color.WHITE);Bitmap glass=TransitionGlassBody.add(glyph);assertTrue(Color.alpha(glass.getPixel(50,96))>=20);assertEquals(0,Color.alpha(glass.getPixel(0,0)));assertEquals(Color.WHITE,glass.getPixel(96,96));assertEquals(0,Color.alpha(glyph.getPixel(50,96)));}
    @Test public void transitionBodyRemainsNeutralAndCanFadeNormally(){Bitmap glyph=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888),glass=TransitionGlassBody.add(glyph);int pixel=glass.getPixel(50,96);assertEquals(Color.red(pixel),Color.blue(pixel));assertEquals(Color.red(pixel),Color.green(pixel));android.graphics.drawable.Drawable drawable=new OwnedTransitionDrawable(null,glass);drawable.setAlpha(0);Bitmap output=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);drawable.setBounds(0,0,192,192);drawable.draw(new Canvas(output));assertEquals(0,Color.alpha(output.getPixel(50,96)));}
    @Test public void unavailableForegroundIsNotRendered(){assertNull(TransitionGlassBody.add(null));Bitmap bitmap=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888);bitmap.recycle();assertSame(bitmap,TransitionGlassBody.add(bitmap));}
}
