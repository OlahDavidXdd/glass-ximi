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
        final WeakReference<View> host;final Drawable glass;SearchBarBlur underlay;volatile boolean drawn;int removed;final Set<String> described=new HashSet<>();
        Card(View host,Drawable glass){this.host=new WeakReference<>(host);this.glass=glass;}
    }
    private final Map<Object,Card> roots=Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<View,WeakReference<Object>> hosts=new WeakHashMap<>();
    private final Set<String> diagnostics=new HashSet<>();
    private final Set<String> renderMessages=new HashSet<>();
    private final Consumer<String> log,changed;
    private final SearchBarBlur.Calls underlayCalls;
    private volatile boolean enabled;
    private boolean blur;private int strength;private GlassBackdrop backdrop;
    MamlWidgetGlass(Consumer<String> log,Consumer<String> changed){this(log,changed,null);}
    MamlWidgetGlass(Consumer<String> log,Consumer<String> changed,SearchBarBlur.Calls calls){this.log=log;this.changed=changed;this.underlayCalls=calls;}
    void configure(boolean enabled,boolean blur,int strength,GlassBackdrop backdrop){
        renderMessages.clear();this.enabled=enabled;this.blur=blur;this.strength=strength;this.backdrop=backdrop;
        synchronized(roots){for(Card card:roots.values())if(card.underlay!=null)card.underlay.restore();roots.clear();}for(View host:new ArrayList<>(hosts.keySet()))apply(host);
        changed.accept(summary());
    }
    void apply(View host){
        if(!nativeHost(host))return;
        Object root=engineRoot(host);
        Object nested=field(host,"mAwesomeView");View owner=nested instanceof View?(View)nested:host;
        Card bound=roots.get(root);if(enabled&&bound!=null&&bound.host.get()==owner){hosts.put(host,new WeakReference<>(root));return;}
        Object info=call(host,"getItemInfo");
        if(info==null&&host.getParent() instanceof View){info=call(host.getParent(),"getItemInfo");if(info==null)info=((View)host.getParent()).getTag();}
        String descriptor=metadata(info);
        View ancestor=host;for(int depth=0;ancestor!=null&&depth<6;depth++){descriptor+=" "+ancestor.getClass().getName()+" "+metadata(call(ancestor,"getItemInfo"))+" "+metadata(ancestor.getTag());ancestor=ancestor.getParent() instanceof View?(View)ancestor.getParent():null;}
        if(descriptor.isEmpty())descriptor=metadata(host.getTag());
        String label=String.valueOf(field(info,"title"));
        Object previous=hosts.get(host)==null?null:hosts.get(host).get();
        if(previous!=root){if(previous!=null)removeOwned(previous,owner);hosts.put(host,new WeakReference<>(root));}
        boolean supported=WidgetSurfacePolicy.supported(descriptor,label)||MamlWidgetPolicy.supportedMetadata(descriptor);
        String diagnostic="MAML host="+host.getClass().getName()+" metadata="+descriptor+" title="+label+" root="+(root==null?"pending":root.getClass().getName())+" clock_weather="+supported;
        if(diagnostics.size()<40&&diagnostics.add(diagnostic))log.accept(diagnostic);
        if(!enabled||backdrop==null||root==null||!supported){if(root!=null)removeOwned(root,owner);return;}
        bind(owner,root);hosts.put(host,new WeakReference<>(root));
    }
    // Called only after identifying a native clock/weather host; also exercised by Android tests.
    void bind(View host,Object root){
        if(!enabled||backdrop==null||root==null)return;
        Object previous=hosts.get(host)==null?null:hosts.get(host).get();if(previous!=null&&previous!=root)removeOwned(previous,host);
        hosts.put(host,new WeakReference<>(root));
        Card current=roots.get(root);if(current!=null&&current.host.get()==host)return;
        if(current!=null&&current.underlay!=null)current.underlay.restore();
        Drawable glass=SharedGlassMaterial.create(host,backdrop,blur,strength,this::renderStatus,GlassSurface.WIDGET);
        Card card=new Card(host,glass);
        if(underlayCalls!=null){View body=host instanceof android.view.ViewGroup&&((android.view.ViewGroup)host).getChildCount()>0?((android.view.ViewGroup)host).getChildAt(0):host;card.underlay=new SearchBarBlur(body,host,underlayCalls,log,"WIDGET");card.underlay.useSharedMaterial(true);}
        roots.put(root,card);host.invalidate();changed.accept(summary());
    }
    void detach(View host){WeakReference<Object> ref=hosts.remove(host);if(ref!=null){Object root=ref.get();Card card=roots.get(root);Object child=field(host,"mAwesomeView");View owner=child instanceof View?(View)child:host;if(card!=null&&card.host.get()==owner)remove(root);}changed.accept(summary());}
    boolean drawBackground(Object group,Canvas canvas){
        if(!enabled||Looper.myLooper()!=Looper.getMainLooper())return false;
        Object root=call(group,"getRoot");Card card=roots.get(root);
        if(card==null||card.host.get()==null||field(root,"mInnerGroup")!=group)return false;
        float w=number(call(root,"getWidth")),h=number(call(root,"getHeight"));
        if(!Float.isFinite(w)||!Float.isFinite(h)||w<=0||h<=0||w>16384||h>16384)return false;
        int width=Math.round(w),height=Math.round(h);
        if(card.underlay!=null)card.underlay.useSharedMaterial(false);
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
    static Object engineRoot(View host){if(!nativeHost(host))return null;Object root=call(host,"getMaMlRoot");if(root==null)root=call(host,"getRoot");return root==null?field(host,"mRoot"):root;}
    private void removeOwned(Object root,View owner){Card card=roots.get(root);if(card!=null&&card.host.get()==owner)remove(root);}
    private void remove(Object root){Card removed=roots.remove(root);if(removed!=null&&removed.underlay!=null)removed.underlay.restore();}
    String summary(){int bound=0,rendered=0,removed=0;synchronized(roots){for(Card card:roots.values())if(card.host.get()!=null){bound++;if(card.drawn)rendered++;removed+=card.removed;}}return bound+" MAML clock/weather bound, "+rendered+" rendered, "+removed+" background layers replaced";}
    private static boolean nativeHost(View host){for(Class<?> type=host.getClass();type!=null;type=type.getSuperclass())if(type.getName().equals("com.miui.home.launcher.maml.MaMlHostView")||type.getName().equals("com.miui.maml.MiAdvancedView")||type.getName().equals("miui.maml.MiAdvancedView")||type.getName().equals("com.miui.maml.component.MamlView")||type.getName().equals("miui.maml.component.MamlView")||type.getName().equals("com.miui.home.launcher.gadget.AwesomeClock"))return true;return false;}
    private void renderStatus(String message){if(renderMessages.size()<32&&renderMessages.add(message))log.accept(message);}
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
