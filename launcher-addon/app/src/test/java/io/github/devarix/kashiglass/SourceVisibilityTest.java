package io.github.devarix.kashiglass;
import org.junit.Test;
import static org.junit.Assert.*;
public class SourceVisibilityTest {
    @Test public void placeholderRemainsHiddenUntilNativeShow(){SourceVisibility state=new SourceVisibility();state.visibility(4);state.transparentPlaceholder();state.opacity(.5f);assertTrue(state.hidden);state.visibility(0);assertFalse(state.hidden);assertEquals(127,state.alpha);}
    @Test public void nativeFadeDoesNotChangeLayoutVisibility(){SourceVisibility state=new SourceVisibility();state.opacity(0);assertEquals(0,state.alpha);assertFalse(state.hidden);state.opacity(1);assertEquals(255,state.alpha);}
    @Test public void springOvershootCannotCreateInvalidAlpha(){SourceVisibility state=new SourceVisibility();state.opacity(-.2f);assertEquals(0,state.alpha);state.opacity(1.2f);assertEquals(255,state.alpha);}
    @Test public void malformedAlphaDoesNotResetAnOngoingFade(){SourceVisibility state=new SourceVisibility();state.opacity(.25f);state.opacity(Float.NaN);assertEquals(63,state.alpha);state.opacity(Float.POSITIVE_INFINITY);assertEquals(63,state.alpha);}
}
