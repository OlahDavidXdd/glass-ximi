package io.github.devarix.kashiglass;

import java.util.*;
import java.util.function.Function;

/** Bounded, cycle-safe walk of verified native target/model links. No view-tree traversal. */
final class AnimationTargetResolver {
    private AnimationTargetResolver(){}
    static <T> T find(Object target,Function<Object,T> snapshot,Function<Object,List<Object>> links){
        if(target==null)return null;
        ArrayDeque<Object> pending=new ArrayDeque<>();Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        pending.add(target);int visited=0;
        while(!pending.isEmpty()&&visited++<24){
            Object current=pending.removeFirst();if(!seen.add(current))continue;
            T result=snapshot.apply(current);if(result!=null)return result;
            List<Object> children=links.apply(current);if(children==null)continue;
            for(Object child:children)if(child!=null&&!seen.contains(child)&&pending.size()<24)pending.addLast(child);
        }
        return null;
    }
}
