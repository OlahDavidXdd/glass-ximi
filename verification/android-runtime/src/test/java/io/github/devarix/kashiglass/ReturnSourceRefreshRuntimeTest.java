package io.github.devarix.kashiglass;

import android.app.Activity;
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
public class ReturnSourceRefreshRuntimeTest {
    static class Icon extends View {int invalidations;Icon(android.content.Context c,String key){super(c);setTag(key);}@Override public void invalidate(){invalidations++;super.invalidate();}}
    private Activity activity;private FrameLayout parent;private Icon source,home,other;
    @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();parent=new FrameLayout(activity);source=new Icon(activity,"app");home=new Icon(activity,"app");other=new Icon(activity,"other");parent.addView(source);parent.addView(home);parent.addView(other);activity.setContentView(parent);source.invalidations=home.invalidations=other.invalidations=0;}
    @After public void cleanup(){activity.finish();}
    @Test public void sourceAndDuplicateHomeRepresentationAreReconciledAndInvalidated(){List<View> repaired=new ArrayList<>();assertEquals(2,ReturnSourceRefresh.matching(source,List.of(source,home,other),v->(String)v.getTag(),repaired::add));assertEquals(List.of(source,home),repaired);assertTrue(source.invalidations>0);assertTrue(home.invalidations>0);assertEquals(0,other.invalidations);}
    @Test public void reconciliationDoesNotShowHiddenSourceOrResetFade(){source.setVisibility(View.INVISIBLE);source.setAlpha(.3f);ReturnSourceRefresh.refresh(source,v->{});assertEquals(View.INVISIBLE,source.getVisibility());assertEquals(.3f,source.getAlpha(),0);}
    @Test public void detachedAndUnknownRepresentationsAreNotRefreshed(){parent.removeView(home);List<View> repaired=new ArrayList<>();assertEquals(1,ReturnSourceRefresh.matching(source,List.of(home,other),v->(String)v.getTag(),repaired::add));source.setTag(null);assertEquals(1,ReturnSourceRefresh.matching(source,List.of(home,other),v->(String)v.getTag(),v->{}));}
}
