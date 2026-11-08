package io.github.devarix.kashiglass;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class AnimationTargetResolverTest {
    static class Target {final List<Object> links=new ArrayList<>();String snapshot;}
    private String find(Object root){return AnimationTargetResolver.find(root,node->((Target)node).snapshot,node->((Target)node).links);}
    @Test public void wrapperAndBuddyInfoResolveTheSameArtwork(){Target wrapper=new Target(),view=new Target(),model=new Target();wrapper.links.add(view);view.links.add(model);model.snapshot="glass";assertEquals("glass",find(wrapper));}
    @Test public void directArtworkTakesPriorityOverAnotherLinkedApp(){Target view=new Target(),model=new Target();view.snapshot="current";model.snapshot="other";view.links.add(model);assertEquals("current",find(view));}
    @Test public void cyclesAndNullLinksDoNotPreventAnotherValidTarget(){Target root=new Target(),other=new Target();root.links.add(root);root.links.add(null);root.links.add(other);other.links.add(root);other.snapshot="glass";assertEquals("glass",find(root));}
    @Test public void missingTargetsFallBackWithoutBlocking(){Target root=new Target();root.links.add(root);assertNull(find(root));assertNull(find(null));}
    @Test public void pathologicalGraphsAreBounded(){Target root=new Target(),current=root;for(int i=0;i<100;i++){Target next=new Target();current.links.add(next);current=next;}current.snapshot="too far";assertNull(find(root));}
    @Test public void EqualModelsAreVisitedByIdentity(){Target root=new Target(){public boolean equals(Object o){return true;}public int hashCode(){return 1;}};Target other=new Target(){public boolean equals(Object o){return true;}public int hashCode(){return 1;}};root.links.add(other);other.snapshot="glass";assertEquals("glass",find(root));}
}
