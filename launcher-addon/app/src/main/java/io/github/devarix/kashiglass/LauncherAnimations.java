package io.github.devarix.kashiglass;

import android.graphics.Bitmap;
import android.view.View;
import java.lang.reflect.*;
import java.util.function.*;
import de.robv.android.xposed.*;

/** Replace explicit native animation inputs before adaptive/foreground layers are derived. */
final class LauncherAnimations {
    private final BooleanSupplier enabled;
    private final Function<Object,Bitmap> snapshots;
    private final Consumer<String> log;
    LauncherAnimations(BooleanSupplier enabled,Function<Object,Bitmap> snapshots,Consumer<String> log){this.enabled=enabled;this.snapshots=snapshots;this.log=log;}
    void install(ClassLoader loader){
        for(String name:AnimationPolicy.TARGETS){
            Class<?> type=XposedHelpers.findClassIfExists(name,loader);if(type==null){log.accept("Target absent: "+name);continue;}
            for(Method method:type.getDeclaredMethods())install(method,method.getParameterTypes());
            for(Constructor<?> constructor:type.getDeclaredConstructors())install(constructor,constructor.getParameterTypes());
        }
    }
    private void install(Member member,Class<?>[] parameters){
        String[] names=new String[parameters.length];for(int i=0;i<names.length;i++)names[i]=parameters[i].getName();
        int index=AnimationPolicy.drawableIndex(names);if(index<0)return;
        try{XposedBridge.hookMethod(member,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                if(!enabled.getAsBoolean())return;
                try{
                    Object target=p.args[index-1];Bitmap bitmap=snapshots.apply(target);
                    if(bitmap==null||bitmap.isRecycled()){log.accept("Animation snapshot missing: "+member.getDeclaringClass().getSimpleName()+"."+member.getName());return;}
                    p.args[index]=new OwnedTransitionDrawable(target instanceof View?((View)target).getResources():null,bitmap);
                    log.accept("Animation input replaced: "+member.getDeclaringClass().getSimpleName()+"."+member.getName()+" target="+(target==null?"null":target.getClass().getName()));
                }catch(Throwable e){log.accept("Animation input error: "+android.util.Log.getStackTraceString(e));}
            }
        });log.accept("Hooked native animation input: "+member.getDeclaringClass().getSimpleName()+"."+member.getName()+" drawableArg="+index);
        }catch(Throwable e){log.accept("Animation hook error: "+e);}
    }
}
