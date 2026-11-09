package io.github.devarix.kashiglass;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;

/** Background-only treatment: native dots, selection, accessibility and visibility stay intact. */
final class PageIndicatorGlass {
    private static final class Binding {
        WeakReference<View> target=new WeakReference<>(null);
        Drawable original;ColorStateList tint;PorterDuff.Mode mode;
        WeakReference<Drawable> ours=new WeakReference<>(null);SearchBarBlur underlay;
        final PagePillLayout layout=new PagePillLayout();WeakReference<View> reference=new WeakReference<>(null);
        String geometry="";int revision=-1;boolean applied;
    }
    private final Map<View,Binding> bindings=new WeakHashMap<>();
    private final SearchBarBlur.Calls calls;
    private final Consumer<String> log;
    private boolean enabled,blur;private int radius,revision;private GlassBackdrop backdrop;
    PageIndicatorGlass(SearchBarBlur.Calls calls,Consumer<String> log){this.calls=calls;this.log=log;}
    void configure(boolean enabled,boolean blur,int radius,GlassBackdrop backdrop){
        this.enabled=enabled;this.blur=blur;this.radius=radius;this.backdrop=backdrop;revision++;
        for(var entry:new ArrayList<>(bindings.entrySet()))bind(entry.getKey(),entry.getValue().target.get(),entry.getValue().reference.get(),false);
    }
    void bind(View host,View target,boolean nativeStyleChanged){bind(host,target,null,nativeStyleChanged);}
    void bind(View host,View target,View searchReference,boolean nativeStyleChanged){
        Binding binding=bindings.get(host);
        if(binding==null){if(!enabled||target==null)return;binding=new Binding();bindings.put(host,binding);}
        if(binding.target.get()!=target){restore(binding);binding.target=new WeakReference<>(target);}
        if(!enabled||backdrop==null||target==null){restore(binding);return;}
        binding.reference=new WeakReference<>(searchReference);if(searchReference!=null){binding.layout.match(target,searchReference);String geometry=binding.layout.description();if(!geometry.equals(binding.geometry)){binding.geometry=geometry;log.accept("PAGE_GEOMETRY matches Search: "+geometry);}}
        if(!binding.applied||binding.revision!=revision||target.getBackground()!=binding.ours.get()){
            if(!binding.applied||target.getBackground()!=binding.ours.get()){binding.original=target.getBackground();binding.tint=target.getBackgroundTintList();binding.mode=target.getBackgroundTintMode();}
            Drawable material=SharedGlassMaterial.create(target,backdrop,blur,radius,log,GlassSurface.SEARCH);
            binding.ours=new WeakReference<>(material);background(target,material);target.setBackgroundTintList(null);binding.applied=true;binding.revision=revision;
            log.accept("PAGE_MATERIAL shared search glass bound; native dots/actions retained: "+target.getClass().getName());
        }
        if(target.getBackgroundTintList()!=null){binding.tint=target.getBackgroundTintList();binding.mode=target.getBackgroundTintMode();target.setBackgroundTintList(null);}
        if(binding.underlay==null)binding.underlay=new SearchBarBlur(target,target,calls,log,"PAGE");
        binding.underlay.useSharedMaterial(nativeStyleChanged);
    }
    void detach(View host){Binding binding=bindings.get(host);if(binding!=null)restore(binding);}
    int visibleCount(){int count=0;for(Binding b:bindings.values()){View target=b.target.get();if(b.applied&&target!=null&&target.isShown())count++;}return count;}
    private void restore(Binding binding){
        binding.layout.restore();View target=binding.target.get();if(binding.underlay!=null){binding.underlay.restore();binding.underlay=null;}
        if(binding.applied&&target!=null&&target.getBackground()==binding.ours.get()){background(target,binding.original);target.setBackgroundTintList(binding.tint);target.setBackgroundTintMode(binding.mode);}
        binding.applied=false;binding.ours.clear();binding.original=null;binding.tint=null;
    }
    private static void background(View view,Drawable drawable){
        boolean relative=view.isPaddingRelative();int start=relative?view.getPaddingStart():view.getPaddingLeft(),end=relative?view.getPaddingEnd():view.getPaddingRight(),top=view.getPaddingTop(),bottom=view.getPaddingBottom();
        view.setBackground(drawable);if(relative)view.setPaddingRelative(start,top,end,bottom);else view.setPadding(start,top,end,bottom);
    }
}
