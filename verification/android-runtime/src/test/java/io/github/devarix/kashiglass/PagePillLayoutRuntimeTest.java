package io.github.devarix.kashiglass;

import android.app.Activity;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,35},manifest=Config.NONE)
public class PagePillLayoutRuntimeTest {
    private Activity activity;private FrameLayout host;private View page;private TextView search;private PagePillLayout geometry;
    @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();host=new FrameLayout(activity);page=new View(activity);search=new TextView(activity);search.setText("Search");search.setPadding(14,6,14,6);FrameLayout.LayoutParams searchParams=new FrameLayout.LayoutParams(-2,44,Gravity.CENTER);searchParams.bottomMargin=12;host.addView(page,new FrameLayout.LayoutParams(240,120,Gravity.BOTTOM|Gravity.LEFT));host.addView(search,searchParams);activity.setContentView(host);layout();geometry=new PagePillLayout();}
    private void layout(){host.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(200,View.MeasureSpec.EXACTLY));host.layout(0,0,400,200);}
    @After public void cleanup(){activity.finish();}
    @Test public void pageMatchesSearchBoxAndPhysicalAnchor(){assertTrue(geometry.match(page,search));layout();assertEquals(search.getWidth(),page.getWidth());assertEquals(search.getHeight(),page.getHeight());assertEquals(search.getLeft(),page.getLeft());assertEquals(search.getTop(),page.getTop());}
    @Test public void hiddenSearchCanBeMeasuredWithoutBeingShown(){search.setVisibility(View.GONE);search.forceLayout();assertTrue(geometry.match(page,search));assertEquals(View.GONE,search.getVisibility());assertEquals(44,page.getLayoutParams().height);assertTrue(page.getLayoutParams().width>0);}
    @Test public void repeatedMatchDoesNotRequestNewLayoutOrMutateSharedReferenceParams(){Object original=search.getLayoutParams();geometry.match(page,search);Object ours=page.getLayoutParams();geometry.match(page,search);assertSame(ours,page.getLayoutParams());assertSame(original,search.getLayoutParams());assertEquals(-2,search.getLayoutParams().width);}
    @Test public void disableRestoresOriginalNativeParams(){Object original=page.getLayoutParams();geometry.match(page,search);geometry.restore();assertSame(original,page.getLayoutParams());assertEquals(240,page.getLayoutParams().width);assertEquals(120,page.getLayoutParams().height);}
    @Test public void searchFontOrAnchorUpdatesAreFollowed(){geometry.match(page,search);search.setText("Find your apps");FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(-2,48,Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM);params.bottomMargin=20;search.setLayoutParams(params);geometry.match(page,search);layout();assertEquals(search.getWidth(),page.getWidth());assertEquals(search.getTop(),page.getTop());assertEquals(48,page.getHeight());}
}
