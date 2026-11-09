package io.github.devarix.kashiglass;
import android.graphics.drawable.Drawable;
import android.view.View;
import java.util.*;
import java.util.function.Consumer;
/** Clear only Android host backgrounds, never clock content or provider image layers. */
final class WidgetHostDecoration {
    private final Map<View,Drawable> saved=new WeakHashMap<>();private final Consumer<String> log;
    WidgetHostDecoration(Consumer<String> log){this.log=log;}
    void clear(View view){Drawable current=view.getBackground();if(current==null)return;saved.put(view,current);view.setBackground(null);log.accept("Clock/weather Android host background removed: "+view.getClass().getName());}
    void restore(){for(var entry:new ArrayList<>(saved.entrySet()))if(entry.getKey()!=null&&entry.getKey().getBackground()==null)entry.getKey().setBackground(entry.getValue());saved.clear();}
}
