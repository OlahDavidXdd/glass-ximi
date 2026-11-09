package io.github.devarix.kashiglass;

import android.appwidget.*;
import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
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
public class WidgetRuntimeTest {
    static class Host extends AppWidgetHostView {AppWidgetProviderInfo info;Host(Context c){super(c);}@Override public AppWidgetProviderInfo getAppWidgetInfo(){return info;}}
    private final java.util.List<String> nativeCalls=new java.util.ArrayList<>();
    private Activity activity;private Host host;private FrameLayout card;private ImageView background;private Drawable image,original;private LauncherWidgets widgets;
    @Before public void setup(){
        activity=Robolectric.buildActivity(Activity.class).setup().get();Context context=activity;host=new Host(context);host.info=new AppWidgetProviderInfo();host.info.provider=new ComponentName("com.miui.weather2","WeatherWidget");ActivityInfo metadata=new ActivityInfo();metadata.nonLocalizedLabel="Weather";metadata.applicationInfo=context.getApplicationInfo();org.robolectric.util.ReflectionHelpers.setField(host.info,"providerInfo",metadata);
        card=new FrameLayout(context);original=new ColorDrawable(Color.BLUE);card.setBackground(original);background=new ImageView(context);background.setId(android.R.id.background);image=new ColorDrawable(Color.CYAN);background.setImageDrawable(image);card.addView(background,new FrameLayout.LayoutParams(300,300));host.addView(card,new FrameLayout.LayoutParams(300,300));host.layout(0,0,300,300);card.layout(0,0,300,300);background.layout(0,0,300,300);
        LiquidGlassDrawable.configure(null,null,true,true);widgets=new LauncherWidgets(s->{},s->{},(v,m,args)->{nativeCalls.add(m);return m.startsWith("get")?0:null;});widgets.configure(true,false,24,new GlassBackdrop(){public boolean isAvailable(){return false;}public boolean draw(Canvas c,View v,Rect r,int margin){return false;}});widgets.apply(host);
        activity.setContentView(host);widgets.track(host);assertTrue(card.getBackground() instanceof LiquidGlassDrawable);assertNull(background.getDrawable());
    }
    @After public void teardown(){activity.finish();}
    @Test public void attachmentActivatesPendingUnderlayWithoutRepeatingRequests(){
        assertTrue(card.isAttachedToWindow());nativeCalls.clear();widgets.apply(host);assertTrue(nativeCalls.contains("setMiViewBlurMode"));
        int count=nativeCalls.size();widgets.apply(host);assertEquals(count,nativeCalls.size());
    }
    @Test public void asynchronousProviderBackgroundChangeIsReappliedBeforeDraw(){Drawable updated=new ColorDrawable(Color.GREEN);card.setBackground(updated);host.getViewTreeObserver().dispatchOnPreDraw();assertTrue(card.getBackground() instanceof LiquidGlassDrawable);widgets.configure(false,false,24,null);assertSame(updated,card.getBackground());}
    @Test public void asynchronousResizeRestoresDecorationBeforeDraw(){background.layout(0,0,40,40);host.getViewTreeObserver().dispatchOnPreDraw();assertSame(image,background.getDrawable());}
    @Test public void shrinkRestoresDecorationThatNoLongerCoversCard(){background.layout(0,0,40,40);widgets.apply(host);assertSame(image,background.getDrawable());}
    @Test public void newlyInteractiveImageIsRestoredAndKeepsItsClickState(){background.setClickable(true);widgets.apply(host);assertSame(image,background.getDrawable());assertTrue(background.isClickable());}
    @Test public void missingProviderRestoresPreviousCard(){host.info=null;widgets.apply(host);assertSame(original,card.getBackground());assertSame(image,background.getDrawable());}
    @Test public void replacingRootWithZeroSizeDoesNotRetainOldCardDecoration(){host.removeAllViews();host.addView(new FrameLayout(host.getContext()));widgets.apply(host);assertSame(original,card.getBackground());assertSame(image,background.getDrawable());}
    @Test public void disablingRestoresTheOriginalArt(){widgets.configure(false,false,24,null);assertSame(original,card.getBackground());assertSame(image,background.getDrawable());}
}
