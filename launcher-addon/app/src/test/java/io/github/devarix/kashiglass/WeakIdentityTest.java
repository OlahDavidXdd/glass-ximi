package io.github.devarix.kashiglass;
import java.lang.ref.WeakReference;
import org.junit.Test;
import static org.junit.Assert.*;
public class WeakIdentityTest {
    @Test public void comparesByIdentityInsteadOfEquality(){WeakIdentity cached=new WeakIdentity();Object first=new String("same"),second=new String("same");cached.set(first);assertTrue(cached.matches(first));assertFalse(cached.matches(second));}
    @Test public void collectedOldTagCannotMatchANewNullTag() throws Exception {WeakIdentity cached=new WeakIdentity();cached.set(new Object());var field=WeakIdentity.class.getDeclaredField("reference");field.setAccessible(true);((WeakReference<?>)field.get(cached)).clear();assertFalse(cached.matches(null));}
    @Test public void explicitlyNullTagIsStillCacheable(){WeakIdentity cached=new WeakIdentity();cached.set(null);assertTrue(cached.matches(null));assertFalse(cached.matches(new Object()));}
}
