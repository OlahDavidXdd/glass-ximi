package io.github.devarix.kashiglass;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.lang.ref.WeakReference;

/** Match the native Search sibling's preferred box and anchor without mutating shared params. */
final class PagePillLayout {
    private WeakReference<View> target=new WeakReference<>(null);
    private ViewGroup.LayoutParams original;
    private FrameLayout.LayoutParams ours;
    boolean match(View page,View search){
        if(page==null||search==null||page==search||page.getParent()==null||page.getParent()!=search.getParent()||!(search.getLayoutParams() instanceof FrameLayout.LayoutParams))return false;
        FrameLayout.LayoutParams reference=(FrameLayout.LayoutParams)search.getLayoutParams();
        if(search.getMeasuredWidth()<=0||search.getMeasuredHeight()<=0||search.isLayoutRequested()){
            int width=reference.width>0?View.MeasureSpec.makeMeasureSpec(reference.width,View.MeasureSpec.EXACTLY):View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
            int height=reference.height>0?View.MeasureSpec.makeMeasureSpec(reference.height,View.MeasureSpec.EXACTLY):View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
            search.measure(width,height);
        }
        int width=search.getMeasuredWidth(),height=search.getMeasuredHeight();if(width<=0||height<=0)return false;
        if(target.get()!=page){restore();target=new WeakReference<>(page);original=page.getLayoutParams();}
        else if(page.getLayoutParams()!=ours)original=page.getLayoutParams();
        if(ours!=null&&page.getLayoutParams()==ours&&ours.width==width&&ours.height==height&&sameAnchor(ours,reference))return true;
        ours=new FrameLayout.LayoutParams(reference);ours.width=width;ours.height=height;page.setLayoutParams(ours);return true;
    }
    private static boolean sameAnchor(FrameLayout.LayoutParams a,FrameLayout.LayoutParams b){return a.gravity==b.gravity&&a.leftMargin==b.leftMargin&&a.topMargin==b.topMargin&&a.rightMargin==b.rightMargin&&a.bottomMargin==b.bottomMargin&&a.getMarginStart()==b.getMarginStart()&&a.getMarginEnd()==b.getMarginEnd();}
    String description(){return ours==null?"pending":ours.width+"x"+ours.height+" gravity="+ours.gravity+" margins="+ours.leftMargin+","+ours.topMargin+","+ours.rightMargin+","+ours.bottomMargin;}
    void restore(){View view=target.get();if(view!=null&&ours!=null&&view.getLayoutParams()==ours&&original!=null)view.setLayoutParams(original);target.clear();original=null;ours=null;}
}
