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
    private Class<?> widgetTarget,folderTarget;
    LauncherAnimations(BooleanSupplier enabled,Function<Object,Bitmap> snapshots,Consumer<String> log){this.enabled=enabled;this.snapshots=snapshots;this.log=log;}
    void install(ClassLoader loader){
        widgetTarget=XposedHelpers.findClassIfExists("com.miui.home.launcher.anim.WidgetTypeAnimTarget",loader);
        folderTarget=XposedHelpers.findClassIfExists("com.miui.home.folder.api.IFolderPreviewIconView",loader);
        for(String name:AnimationPolicy.TARGETS){
            Class<?> type=XposedHelpers.findClassIfExists(name,loader);if(type==null){log.accept("Target absent: "+name);continue;}
            for(Method method:type.getDeclaredMethods()){install(method,method.getParameterTypes());preserveAlpha(method);refreshLayers(method);}
            for(Constructor<?> constructor:type.getDeclaredConstructors())install(constructor,constructor.getParameterTypes());
        }
        Class<?> helper=XposedHelpers.findClassIfExists("com.miui.home.recents.views.FloatingIconUtils$Companion",loader);
        if(helper!=null)for(Method method:helper.getDeclaredMethods())preserveAlpha(method);
    }
    private void refreshLayers(Method method){
        String type=method.getDeclaringClass().getSimpleName();
        boolean view="FloatingIconView2".equals(type)&&"setIcon".equals(method.getName())&&method.getParameterCount()==0;
        boolean layer="FloatingIconLayer2".equals(type)&&"createLayerSurface".equals(method.getName());
        if(!view&&!layer)return;
        try{XposedBridge.hookMethod(method,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){
            if(!enabled.getAsBoolean())return;
            try{
                Object ref=XposedHelpers.getObjectField(p.thisObject,"mAnimTargetRef");
                Object target=ref instanceof java.lang.ref.Reference?((java.lang.ref.Reference<?>)ref).get():null;
                if(layer&&p.args.length>1&&p.args[1]!=null)target=p.args[1];
                if(target==null||(widgetTarget!=null&&widgetTarget.isInstance(target))||(folderTarget!=null&&folderTarget.isInstance(target)))return;
                OwnedTransitionDrawable fresh=AnimationArtwork.fresh(target,target instanceof View?((View)target).getResources():null,snapshots);
                if(fresh!=null){XposedHelpers.setObjectField(p.thisObject,"mDrawable",fresh);log.accept("Animation layers refreshed: "+type+"."+method.getName());}
            }catch(Throwable e){log.accept("Animation layer refresh error: "+e);}
        }});}catch(Throwable e){log.accept("Animation layer hook error: "+e);}
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
                    Object target=p.args[index-1];
                    if((widgetTarget!=null&&widgetTarget.isInstance(target))||(folderTarget!=null&&folderTarget.isInstance(target)))return;
                    OwnedTransitionDrawable fresh=AnimationArtwork.fresh(target,target instanceof View?((View)target).getResources():null,snapshots);
                    if(fresh==null){log.accept("Animation snapshot missing: "+member.getDeclaringClass().getSimpleName()+"."+member.getName());return;}
                    p.args[index]=fresh;
                    log.accept("Animation input replaced: "+member.getDeclaringClass().getSimpleName()+"."+member.getName()+" target="+(target==null?"null":target.getClass().getName()));
                }catch(Throwable e){log.accept("Animation input error: "+android.util.Log.getStackTraceString(e));}
            }
        });log.accept("Hooked native animation input: "+member.getDeclaringClass().getSimpleName()+"."+member.getName()+" drawableArg="+index);
        }catch(Throwable e){log.accept("Animation hook error: "+e);}
    }
}
