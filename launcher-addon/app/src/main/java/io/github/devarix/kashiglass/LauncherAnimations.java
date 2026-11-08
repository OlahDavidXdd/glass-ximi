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
            for(Method method:type.getDeclaredMethods()){install(method,method.getParameterTypes());preserveAlpha(method);}
            for(Constructor<?> constructor:type.getDeclaredConstructors())install(constructor,constructor.getParameterTypes());
        }
        Class<?> helper=XposedHelpers.findClassIfExists("com.miui.home.recents.views.FloatingIconUtils$Companion",loader);
        if(helper!=null)for(Method method:helper.getDeclaredMethods())preserveAlpha(method);
    }
    private void preserveAlpha(Method method){
        if(!"fillDrawable".equals(method.getName())||!android.graphics.drawable.Drawable.class.isAssignableFrom(method.getReturnType())||method.getParameterTypes().length==0||method.getParameterTypes()[0]!=android.graphics.drawable.Drawable.class)return;
        try{XposedBridge.hookMethod(method,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){
            if(enabled.getAsBoolean()&&p.args[0] instanceof OwnedTransitionDrawable&&((OwnedTransitionDrawable)p.args[0]).preserveTransparency){p.setResult(p.args[0]);log.accept("Preserved transparent glass animation pixels; skipped native opaque-fill conversion");}
        }});}catch(Throwable e){log.accept("Animation alpha hook error: "+e);}
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
