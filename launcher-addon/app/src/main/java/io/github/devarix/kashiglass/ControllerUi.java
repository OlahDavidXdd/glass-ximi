package io.github.devarix.kashiglass;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;

import android.view.*;
import android.widget.*;

/** Native controls, restrained neutral surfaces, and no claim of simulated blur verification. */
final class ControllerUi {
    static final int INK=0xfff4f4f6,MUTED=0xffa9abb5,DIM=0xff797d89,BG=0xff000000;
    final Context context;
    ControllerUi(Context context){this.context=context;}
    int dp(float value){return Math.round(value*context.getResources().getDisplayMetrics().density);}
    TextView label(String value,int size,int color,boolean bold){TextView t=new TextView(context);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));t.setIncludeFontPadding(false);t.setLineSpacing(dp(3),1);return t;}
    TextView text(LinearLayout parent,String value,int size,int color,boolean bold){TextView t=label(value,size,color,bold);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);parent.addView(t,p);return t;}
    LinearLayout column(){LinearLayout l=new LinearLayout(context);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(context);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    LinearLayout card(LinearLayout parent){LinearLayout c=column();c.setPadding(dp(18),dp(18),dp(18),dp(18));c.setBackground(glass(24,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);parent.addView(c,p);return c;}
    void section(LinearLayout parent,String title,String hint){text(parent,title,22,INK,true);if(hint!=null)text(parent,hint,14,MUTED,false);}
    void space(LinearLayout parent,int height){parent.addView(new View(context),new LinearLayout.LayoutParams(1,dp(height)));}
    void divider(LinearLayout parent){View v=new View(context);v.setBackgroundColor(0xff292c34);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(1));p.topMargin=dp(8);p.bottomMargin=dp(8);parent.addView(v,p);}
    GradientDrawable surface(int radius,int top,int bottom,int border){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{top,bottom});d.setCornerRadius(dp(radius));d.setStroke(dp(1),border);return d;}
    Drawable glass(int radius,boolean emphasized){return new OpticalGlass(dp(radius),emphasized,0,0);}
    private Drawable ripple(Drawable glass){return new RippleDrawable(ColorStateList.valueOf(0x20ffffff),glass,null);}
    Button button(String title,boolean primary){Button b=new Button(context);b.setText(title);b.setTextSize(14);b.setAllCaps(false);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setTextColor(INK);b.setMinHeight(dp(52));b.setMinimumHeight(dp(52));b.setPadding(dp(14),dp(12),dp(14),dp(12));b.setBackground(ripple(glass(18,primary)));return b;}
    Button action(LinearLayout parent,String title,boolean primary){Button b=button(title,primary);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);parent.addView(b,p);return b;}
    void navigation(Button button,boolean selected){button.setTextColor(selected?INK:MUTED);button.setAlpha(1);button.setBackground(ripple(glass(22,selected)));}
    void navigationSurface(LinearLayout navigation){navigation.setBackground(glass(28,false));}
    private Drawable switchPart(boolean thumb){
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_checked},new OpticalGlass(dp(20),true,dp(thumb?28:54),dp(thumb?28:32)));
        states.addState(new int[]{},new OpticalGlass(dp(20),false,dp(thumb?28:54),dp(thumb?28:32)));
        return states;
    }
    void slider(SeekBar slider){
        slider.setThumbTintList(null);slider.setProgressTintList(null);slider.setProgressBackgroundTintList(null);
        slider.setThumb(new OpticalGlass(dp(20),true,dp(24),dp(24)));
        android.graphics.drawable.LayerDrawable track=new android.graphics.drawable.LayerDrawable(new Drawable[]{glass(8,false),new ClipDrawable(glass(8,true),Gravity.LEFT,ClipDrawable.HORIZONTAL)});
        track.setId(0,android.R.id.background);track.setId(1,android.R.id.progress);slider.setProgressDrawable(track);slider.setMinimumHeight(dp(48));
    }
    Switch toggle(LinearLayout parent,String title,String subtitle,boolean checked){
        LinearLayout row=row();row.setMinimumHeight(dp(64));row.setPadding(dp(12),dp(12),dp(12),dp(8));row.setBackground(ripple(glass(18,false)));LinearLayout copy=column();TextView name=text(copy,title,16,INK,true);text(copy,subtitle,13,MUTED,false);row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        Switch control=new Switch(context);control.setId(View.generateViewId());control.setChecked(checked);control.setContentDescription(title);name.setLabelFor(control.getId());control.setThumbTintList(null);control.setTrackTintList(null);control.setSplitTrack(false);control.setThumbDrawable(switchPart(true));control.setTrackDrawable(switchPart(false));control.setMinimumWidth(dp(52));control.setMinimumHeight(dp(48));row.addView(control,new LinearLayout.LayoutParams(-2,-2));row.setOnClickListener(v->control.performClick());LinearLayout.LayoutParams option=new LinearLayout.LayoutParams(-1,-2);option.topMargin=dp(8);parent.addView(row,option);return control;
    }
    TextView metric(LinearLayout row,String title,String value){LinearLayout box=column();text(box,title,11,DIM,true);TextView number=text(box,value,15,INK,true);number.setMaxLines(2);row.addView(box,new LinearLayout.LayoutParams(0,-2,1));return number;}
    /** Optical highlights over translucent neutral glass. No wallpaper capture or recursive blur. */
    private static final class OpticalGlass extends Drawable {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final RectF frame=new RectF();
        private final float radius;private final boolean bright;private final int width,height;private int opacity=255;
        OpticalGlass(float radius,boolean bright,int width,int height){this.radius=radius;this.bright=bright;this.width=width;this.height=height;}
        @Override public int getIntrinsicWidth(){return width>0?width:-1;}
        @Override public int getIntrinsicHeight(){return height>0?height:-1;}
        @Override public boolean getPadding(Rect padding){padding.set(0,0,0,0);return false;}
        @Override public void draw(Canvas canvas){
            Rect b=getBounds();if(b.isEmpty())return;float inset=Math.max(.75f,radius/24f),r=Math.min(radius,Math.min(b.width(),b.height())/2f);
            frame.set(b.left+inset,b.top+inset,b.right-inset,b.bottom-inset);
            int save=canvas.saveLayerAlpha(b.left,b.top,b.right,b.bottom,opacity);
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);
            paint.setShader(new LinearGradient(frame.left,frame.top,frame.right,frame.bottom,new int[]{bright?0x48ffffff:0x25ffffff,bright?0x20ffffff:0x09ffffff,bright?0x13ffffff:0x04ffffff,bright?0x32ffffff:0x18ffffff},new float[]{0,.30f,.64f,1},Shader.TileMode.CLAMP));
            canvas.drawRoundRect(frame,r,r,paint);
            Path clip=new Path();clip.addRoundRect(frame,r,r,Path.Direction.CW);int clipped=canvas.save();canvas.clipPath(clip);
            paint.setShader(new RadialGradient(frame.left+frame.width()*.18f,frame.top-frame.height()*.15f,Math.max(1,frame.width()*.9f),new int[]{bright?0x45ffffff:0x24ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));canvas.drawOval(frame.left-frame.width()*.25f,frame.top-frame.height()*.7f,frame.right+frame.width()*.1f,frame.top+frame.height()*.48f,paint);
            paint.setShader(new RadialGradient(frame.right,frame.bottom,Math.max(1,Math.min(frame.width(),frame.height())*.85f),new int[]{0x25ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));canvas.drawRect(frame,paint);canvas.restoreToCount(clipped);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(inset);
            paint.setShader(new LinearGradient(frame.left,frame.top,frame.right,frame.bottom,new int[]{bright?0xb8ffffff:0x80ffffff,0x18ffffff,0x07ffffff,0x60ffffff},new float[]{0,.34f,.58f,1},Shader.TileMode.CLAMP));canvas.drawRoundRect(frame,r,r,paint);
            frame.inset(inset*1.5f,inset*1.5f);paint.setStrokeWidth(inset*.6f);paint.setShader(new LinearGradient(frame.left,frame.top,frame.left,frame.bottom,new int[]{0x22ffffff,0x00ffffff,0x12ffffff},null,Shader.TileMode.CLAMP));canvas.drawRoundRect(frame,Math.max(0,r-inset),Math.max(0,r-inset),paint);
            paint.setShader(null);paint.setStyle(Paint.Style.FILL);canvas.restoreToCount(save);
        }
        @Override public void setAlpha(int alpha){opacity=Math.max(0,Math.min(255,alpha));invalidateSelf();}
        @Override public int getAlpha(){return opacity;}
        @Override public void setColorFilter(ColorFilter filter){}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    View emblem(){return new View(context){final Paint p=new Paint(3);@Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();p.setShader(new RadialGradient(w*.48f,h*.28f,h,new int[]{0x32ffffff,0x05ffffff,0x00000000},null,Shader.TileMode.CLAMP));c.drawOval(0,0,w,h,p);float side=Math.min(w,h)*.68f;RectF r=new RectF((w-side)/2,(h-side)/2,(w+side)/2,(h+side)/2);p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{0x48ffffff,0x06494952,0x238a8d98},null,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);c.drawRoundRect(r,side*.27f,side*.27f,p);p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{0xb0ffffff,0x06ffffff,0x70ffffff},null,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));c.drawRoundRect(r,side*.27f,side*.27f,p);p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(0xfff4f4f6);float cx=w/2,cy=h/2;Path star=new Path();star.moveTo(cx,cy-side*.27f);star.quadTo(cx+side*.05f,cy-side*.04f,cx+side*.27f,cy);star.quadTo(cx+side*.05f,cy+side*.05f,cx,cy+side*.27f);star.quadTo(cx-side*.05f,cy+side*.05f,cx-side*.27f,cy);star.quadTo(cx-side*.05f,cy-side*.05f,cx,cy-side*.27f);c.drawPath(star,p);}};}
}
