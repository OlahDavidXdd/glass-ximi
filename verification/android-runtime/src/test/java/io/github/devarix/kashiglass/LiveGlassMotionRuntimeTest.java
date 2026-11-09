package io.github.devarix.kashiglass;

import android.app.Activity;
import android.graphics.Matrix;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,35},manifest=Config.NONE)
public class LiveGlassMotionRuntimeTest {
    static class Icon extends ImageView {int invalidations;Icon(android.content.Context c){super(c);}@Override public void invalidate(){invalidations++;super.invalidate();}}
    private Activity activity;private FrameLayout host;private Icon icon;private boolean enabled=true;
    @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();host=new FrameLayout(activity);icon=new Icon(activity);icon.setImageBitmap(android.graphics.Bitmap.createBitmap(16,16,android.graphics.Bitmap.Config.ARGB_8888));icon.setScaleType(ImageView.ScaleType.MATRIX);host.addView(icon,new FrameLayout.LayoutParams(64,64));activity.setContentView(host);host.layout(0,0,300,500);icon.layout(0,0,64,64);new LiveGlassMotion(icon,v->enabled);icon.invalidations=0;}
    @After public void cleanup(){activity.finish();}
    private void frame(){host.getViewTreeObserver().dispatchOnPreDraw();}
    @Test public void firstPoseRefreshesOnceAndUnchangedFramesStayIdle(){frame();assertEquals(1,icon.invalidations);frame();frame();assertEquals(1,icon.invalidations);}
    @Test public void movementDuringDrawerScrollInvalidatesLiveDrawableWithoutChangingAlpha(){frame();icon.setTranslationY(40);icon.invalidations=0;frame();assertEquals(1,icon.invalidations);frame();assertEquals(1,icon.invalidations);assertEquals(1,icon.getAlpha(),0);}
    @Test public void scaleAndImageMatrixChangesRefreshLiveMaterial(){frame();icon.setScaleX(.8f);icon.invalidations=0;frame();assertEquals(1,icon.invalidations);Matrix matrix=new Matrix();matrix.setScale(.7f,.7f);icon.setImageMatrix(matrix);icon.invalidations=0;frame();assertEquals(1,icon.invalidations);}
    @Test public void disabledAndHiddenIconsDoNotRequestLiveFrames(){enabled=false;frame();assertEquals(0,icon.invalidations);enabled=true;icon.setAlpha(0f);icon.invalidations=0;frame();assertEquals(0,icon.invalidations);icon.setAlpha(1f);icon.invalidations=0;frame();assertEquals(1,icon.invalidations);}
    @Test public void detachStopsObservationAndReattachResetsThePose(){frame();host.removeView(icon);icon.invalidations=0;frame();assertEquals(0,icon.invalidations);host.addView(icon,new FrameLayout.LayoutParams(64,64));icon.layout(0,0,64,64);icon.invalidations=0;frame();assertEquals(1,icon.invalidations);}
}
