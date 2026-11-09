package io.github.devarix.kashiglass;
import org.junit.Test;
import static org.junit.Assert.*;
public class InheritedMethodsTest {
    static class Model {public Object getIconDrawable(){return null;}}
    static class Shortcut extends Model {public Object getIconDrawable(String context,Object fallback){return fallback;}}
    static class Override extends Shortcut {public Object getIconDrawable(){return this;}}
    @Test public void inheritedModelGetterResolvesItsActualDeclaringClass(){assertEquals(Model.class,InheritedMethods.noArg(Shortcut.class,"getIconDrawable").getDeclaringClass());}
    @Test public void nativeOverrideIsPreferred(){assertEquals(Override.class,InheritedMethods.noArg(Override.class,"getIconDrawable").getDeclaringClass());}
    @Test public void missingTypesAndMethodsAreAbsent(){assertNull(InheritedMethods.noArg(null,"getIconDrawable"));assertNull(InheritedMethods.noArg(Model.class,"missing"));}
}
