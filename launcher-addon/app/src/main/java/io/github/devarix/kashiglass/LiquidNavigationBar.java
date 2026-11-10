package io.github.devarix.kashiglass;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.function.IntConsumer;

/** View-based drag selection and damped spring, backed by the Kyant lens adapter. */
final class LiquidNavigationBar extends FrameLayout {
    private final LinearLayout tabs;private final View pill;private final IntConsumer selected;
    private final int slop;private int index,pointer=-1;private float position,target,velocity,downX,downY,origin;
    private boolean dragging,vertical,running;private long lastFrame;
    LiquidNavigationBar(Context context,View scene,IntConsumer selected){
        super(context);this.selected=selected;slop=ViewConfiguration.get(context).getScaledTouchSlop();
        setPadding(dp(6),dp(6),dp(6),dp(6));setMinimumHeight(dp(80));setClipChildren(false);setClickable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setBackground(new KyantNavigationDrawable(this,scene));
        pill=new View(context);pill.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);pill.setBackground(new KyantNavigationDrawable(pill,scene));addView(pill,new FrameLayout.LayoutParams(0,0));
        tabs=new LinearLayout(context);tabs.setOrientation(LinearLayout.HORIZONTAL);tabs.setGravity(Gravity.CENTER_VERTICAL);addView(tabs,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER_VERTICAL));
    }
    void addTab(Button button){final int value=tabs.getChildCount();tabs.addView(button,new LinearLayout.LayoutParams(0,-2,1));button.setOnClickListener(v->choose(value));}
    void select(int value,boolean animate){index=Math.max(0,Math.min(tabs.getChildCount()-1,value));target=index;if(!animate||!isAttachedToWindow()||!ValueAnimator.areAnimatorsEnabled()){stop();position=target;velocity=0;place();}else spring();}
    int selectedIndex(){return index;}float selectionPosition(){return position;}
    void refreshBackdrop(){invalidate();pill.invalidate();}
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private float cell(){return tabs.getChildCount()==0?0:(float)tabs.getWidth()/tabs.getChildCount();}
    private void place(){float step=cell();if(step<=0)return;float physical=getLayoutDirection()==LAYOUT_DIRECTION_RTL?tabs.getChildCount()-1-position:position;int left=Math.round(tabs.getLeft()+physical*step+dp(2));pill.layout(left,tabs.getTop(),Math.round(left+step-dp(4)),tabs.getBottom());pill.invalidate();}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){super.onLayout(changed,l,t,r,b);place();}
    private void choose(int value){int next=Math.max(0,Math.min(tabs.getChildCount()-1,value));boolean changed=next!=index;select(next,true);if(changed)selected.accept(next);}
    private void press(boolean down){pill.animate().scaleX(down?1.06f:1f).scaleY(down?1.08f:1f).setDuration(ValueAnimator.areAnimatorsEnabled()?140:0).start();}
    private void cancelDrag(){dragging=false;pointer=-1;vertical=false;press(false);target=index;spring();}
    private void begin(android.view.MotionEvent event){pointer=event.getPointerId(0);downX=event.getX();downY=event.getY();origin=position;dragging=vertical=false;}
    private void detectDrag(android.view.MotionEvent event){
        if(vertical||dragging)return;int p=event.findPointerIndex(pointer);if(p<0){cancelDrag();return;}
        float dx=event.getX(p)-downX,dy=event.getY(p)-downY;
        if(Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx)){vertical=true;return;}
        if(Math.abs(dx)>slop&&Math.abs(dx)>Math.abs(dy)){dragging=true;stop();press(true);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);}
    }
    @Override public boolean onInterceptTouchEvent(android.view.MotionEvent event){
        int action=event.getActionMasked();if(action==android.view.MotionEvent.ACTION_DOWN)begin(event);
        else if(action==android.view.MotionEvent.ACTION_MOVE){detectDrag(event);if(dragging)movePill(event);}
        else if(action==android.view.MotionEvent.ACTION_CANCEL){cancelDrag();return false;}
        return dragging;
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent event){
        int action=event.getActionMasked();if(action==android.view.MotionEvent.ACTION_DOWN){begin(event);return true;}if(action==android.view.MotionEvent.ACTION_MOVE)detectDrag(event);if(action==android.view.MotionEvent.ACTION_CANCEL){cancelDrag();return true;}
        if(action==android.view.MotionEvent.ACTION_POINTER_UP&&event.getPointerId(event.getActionIndex())==pointer){cancelDrag();return true;}
        if(!dragging){if(action==android.view.MotionEvent.ACTION_UP){pointer=-1;performClick();}return true;}
        int p=event.findPointerIndex(pointer);if(p<0){cancelDrag();return true;}
        if(action==android.view.MotionEvent.ACTION_MOVE){movePill(event);return true;}
        if(action==android.view.MotionEvent.ACTION_UP){dragging=false;pointer=-1;press(false);choose(Math.round(position));return true;}
        return true;
    }
    private void movePill(android.view.MotionEvent event){int p=event.findPointerIndex(pointer);float width=cell();if(p<0||width<=0)return;float next=origin+(event.getX(p)-downX)/width*(getLayoutDirection()==LAYOUT_DIRECTION_RTL?-1:1);position=Math.max(0,Math.min(tabs.getChildCount()-1,next));velocity=0;place();}
    @Override public boolean performClick(){return super.performClick();}
    private final Choreographer.FrameCallback frame=time->{tick(time);};
    private void spring(){if(!isAttachedToWindow()||!ValueAnimator.areAnimatorsEnabled()){stop();position=target;velocity=0;place();return;}if(!running){running=true;lastFrame=0;Choreographer.getInstance().postFrameCallback(frame);}}
    private void tick(long time){if(!running||!isAttachedToWindow()){stop();return;}float dt=lastFrame==0?1f/60:Math.max(1f/240,Math.min(1f/30,(time-lastFrame)/1_000_000_000f));lastFrame=time;velocity+=(-484*(position-target)-36*velocity)*dt;position+=velocity*dt;if(Math.abs(position-target)<.001f&&Math.abs(velocity)<.005f){position=target;velocity=0;running=false;}place();if(running)Choreographer.getInstance().postFrameCallback(frame);}
    private void stop(){running=false;lastFrame=0;Choreographer.getInstance().removeFrameCallback(frame);}
    @Override protected void onDetachedFromWindow(){stop();pill.animate().cancel();position=target;dragging=false;pointer=-1;super.onDetachedFromWindow();}
}
