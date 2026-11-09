package io.github.devarix.kashiglass;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.view.View;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;

/** Native MAML cards render their own backgrounds, outside Android RemoteViews decoration. */
final class MamlWidgetGlass {
    private static final java.util.concurrent.ConcurrentMap<Class<?>,Map<String,Optional<Method>>> METHODS=new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentMap<Class<?>,Map<String,Optional<Field>>> FIELDS=new java.util.concurrent.ConcurrentHashMap<>();
    private static final class Card {
        final WeakReference<View> host;final Drawable glass;volatile boolean drawn;int removed;final Set<String> described=new HashSet<>();
        Card(View host,Drawable glass){this.host=new WeakReference<>(host);this.glass=glass;}
    }
    private final Map<Object,Card> roots=Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<View,WeakReference<Object>> hosts=new WeakHashMap<>();
    private final Set<String> diagnostics=new HashSet<>();
    private final Consumer<String> log,changed;
    private volatile boolean enabled;
    private boolean blur;private int strength;private GlassBackdrop backdrop;
    MamlWidgetGlass(Consumer<String> log,Consumer<String> changed){this.log=log;this.changed=changed;}
    void configure(boolean enabled,boolean blur,int strength,GlassBackdrop backdrop){
        this.enabled=enabled;this.blur=blur;this.strength=strength;this.backdrop=backdrop;
        roots.clear();for(View host:new ArrayList<>(hosts.keySet()))apply(host);
        changed.accept(summary());
    }
    void apply(View host){
        if(!nativeHost(host))return;
        Object root=call(host,"getMaMlRoot");
        Card bound=roots.get(root);if(enabled&&bound!=null&&bound.host.get()==host)return;
        Object info=call(host,"getItemInfo");
        if(info==null&&host.getParent() instanceof View){info=call(host.getParent(),"getItemInfo");if(info==null)info=((View)host.getParent()).getTag();}
        String descriptor=metadata(info);
        if(descriptor.isEmpty())descriptor=metadata(host.getTag());
        String label=String.valueOf(field(info,"title"));
        Object previous=hosts.get(host)==null?null:hosts.get(host).get();
        if(previous!=root){if(previous!=null)roots.remove(previous);hosts.put(host,new WeakReference<>(root));}
        boolean supported=WidgetSurfacePolicy.supported(descriptor,label)||MamlWidgetPolicy.supportedMetadata(descriptor);
        String diagnostic="MAML host="+host.getClass().getName()+" metadata="+descriptor+" title="+label+" root="+(root==null?"pending":root.getClass().getName())+" clock_weather="+supported;
        if(diagnostics.size()<40&&diagnostics.add(diagnostic))log.accept(diagnostic);
        if(!enabled||backdrop==null||root==null||!supported){if(root!=null)roots.remove(root);return;}
        bind(host,root);
    }
    // Called only after identifying a native clock/weather host; also exercised by Android tests.
    void bind(View host,Object root){
        if(!enabled||backdrop==null||root==null)return;
        Object previous=hosts.get(host)==null?null:hosts.get(host).get();if(previous!=null&&previous!=root)roots.remove(previous);
        hosts.put(host,new WeakReference<>(root));
        Card current=roots.get(root);if(current!=null&&current.host.get()==host)return;
        Drawable glass=new LiquidGlassDrawable(host,null,backdrop,blur,strength,log,GlassSurface.WIDGET);
        roots.put(root,new Card(host,glass));host.invalidate();changed.accept(summary());
    }
    void detach(View host){WeakReference<Object> ref=hosts.get(host);if(ref!=null)roots.remove(ref.get());changed.accept(summary());}
    boolean drawBackground(Object group,Canvas canvas){
        if(!enabled||Looper.myLooper()!=Looper.getMainLooper())return false;
        Object root=call(group,"getRoot");Card card=roots.get(root);
        if(card==null||card.host.get()==null||field(root,"mInnerGroup")!=group)return false;
        float w=number(call(root,"getWidth")),h=number(call(root,"getHeight"));
        if(!Float.isFinite(w)||!Float.isFinite(h)||w<=0||h<=0||w>16384||h>16384)return false;
        int width=Math.round(w),height=Math.round(h);
        int save=canvas.save();
        try{
            Object alpha=call(group,"getAlpha");
            card.glass.setAlpha(alpha instanceof Number?Math.max(0,Math.min(255,((Number)alpha).intValue())):255);
            card.glass.setBounds(0,0,width,height);card.glass.draw(canvas);
            if(!card.drawn){card.drawn=true;log.accept("MAML glass rendered: "+width+"x"+height);changed.accept(summary());}
            return true;
        }catch(Throwable error){card.drawn=false;log.accept("MAML glass draw failed: "+error.getClass().getSimpleName());return false;}
        finally{canvas.restoreToCount(save);}
    }
    boolean skipDecoration(Object element){
        if(!enabled||Looper.myLooper()!=Looper.getMainLooper())return false;
        String type=element.getClass().getSimpleName();
        if(!type.equals("ImageScreenElement")&&!type.equals("RectangleScreenElement"))return false;
        Object root=call(element,"getRoot");Card card=roots.get(root);
        if(card==null||!card.drawn||card.host.get()==null)return false;
        String name=string(call(element,"getName")),src=string(call(element,"getSrc"));
        boolean interactive=Boolean.TRUE.equals(field(element,"mTouchable"))||Boolean.TRUE.equals(field(element,"mInterceptTouch"));
        String description=string(call(element,"getContentDescription"));
        boolean skip=MamlWidgetPolicy.decoration(type,name,src,number(call(element,"getWidth")),number(call(element,"getHeight")),number(call(root,"getWidth")),number(call(root,"getHeight")),interactive,!description.isEmpty());
        String diagnostic=type+" name="+name+" src="+src+" removed="+skip;
        if(card.described.size()<32&&card.described.add(diagnostic)){if(skip)card.removed++;log.accept("MAML layer "+diagnostic);if(skip)changed.accept(summary());}
        return skip;
    }
    String summary(){int bound=0,rendered=0,removed=0;synchronized(roots){for(Card card:roots.values())if(card.host.get()!=null){bound++;if(card.drawn)rendered++;removed+=card.removed;}}return bound+" MAML clock/weather bound, "+rendered+" rendered, "+removed+" background layers replaced";}
    private static boolean nativeHost(View host){for(Class<?> type=host.getClass();type!=null;type=type.getSuperclass())if(type.getName().equals("com.miui.home.launcher.maml.MaMlHostView"))return true;return false;}
    private static String metadata(Object info){if(info==null)return "";StringBuilder out=new StringBuilder();for(String name:new String[]{"appPackage","resPath","maMlTag","maMlTagCategory","type","title"}){Object value=field(info,name);if(value!=null)out.append(name).append('=').append(value).append(' ');}Object path=call(info,"getResPath");if(path!=null)out.append(path);return out.toString();}
    private static String string(Object value){return value==null?"":String.valueOf(value);}
    private static float number(Object value){return value instanceof Number?((Number)value).floatValue():0;}
    private static Object call(Object target,String name){
        if(target==null)return null;
        Optional<Method> found=METHODS.computeIfAbsent(target.getClass(),type->new java.util.concurrent.ConcurrentHashMap<>()).computeIfAbsent(name,key->{
            for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{Method method=type.getDeclaredMethod(key);method.setAccessible(true);return Optional.of(method);}catch(NoSuchMethodException absent){}catch(Throwable error){break;}return Optional.empty();
        });
        try{return found.isPresent()?found.get().invoke(target):null;}catch(Throwable error){return null;}
    }
    private static Object field(Object target,String name){
        if(target==null)return null;
        Optional<Field> found=FIELDS.computeIfAbsent(target.getClass(),type->new java.util.concurrent.ConcurrentHashMap<>()).computeIfAbsent(name,key->{
            for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{Field field=type.getDeclaredField(key);field.setAccessible(true);return Optional.of(field);}catch(NoSuchFieldException absent){}catch(Throwable error){break;}return Optional.empty();
        });
        try{return found.isPresent()?found.get().get(target):null;}catch(Throwable error){return null;}
    }
}
