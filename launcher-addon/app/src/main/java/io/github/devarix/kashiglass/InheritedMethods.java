package io.github.devarix.kashiglass;
import java.lang.reflect.Method;
final class InheritedMethods {
    private InheritedMethods(){}
    static Method noArg(Class<?> type,String name){for(Class<?> current=type;current!=null;current=current.getSuperclass())try{return current.getDeclaredMethod(name);}catch(NoSuchMethodException absent){}return null;}
}
