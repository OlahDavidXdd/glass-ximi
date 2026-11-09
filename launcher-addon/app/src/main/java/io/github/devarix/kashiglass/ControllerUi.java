package io.github.devarix.kashiglass;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

/** Native controls, restrained neutral surfaces, and no claim of simulated blur verification. */
final class ControllerUi {
    static final int INK=0xfff4f4f6,MUTED=0xffa9abb5,DIM=0xff797d89,BG=0xff090a0d;
    final Context context;
    ControllerUi(Context context){this.context=context;}
    int dp(float value){return Math.round(value*context.getResources().getDisplayMetrics().density);}
    TextView label(String value,int size,int color,boolean bold){TextView t=new TextView(context);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));t.setIncludeFontPadding(false);t.setLineSpacing(dp(3),1);return t;}
    TextView text(LinearLayout parent,String value,int size,int color,boolean bold){TextView t=label(value,size,color,bold);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);parent.addView(t,p);return t;}
    LinearLayout column(){LinearLayout l=new LinearLayout(context);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(context);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    LinearLayout card(LinearLayout parent){LinearLayout c=column();c.setPadding(dp(18),dp(18),dp(18),dp(18));c.setBackground(surface(22,0xff1b1d23,0xff111318,0x25ffffff));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);parent.addView(c,p);return c;}
    void section(LinearLayout parent,String title,String hint){text(parent,title,22,INK,true);if(hint!=null)text(parent,hint,14,MUTED,false);}
    void space(LinearLayout parent,int height){parent.addView(new View(context),new LinearLayout.LayoutParams(1,dp(height)));}
    void divider(LinearLayout parent){View v=new View(context);v.setBackgroundColor(0xff292c34);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(1));p.topMargin=dp(8);p.bottomMargin=dp(8);parent.addView(v,p);}
    GradientDrawable surface(int radius,int top,int bottom,int border){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{top,bottom});d.setCornerRadius(dp(radius));d.setStroke(dp(1),border);return d;}
    Button button(String title,boolean primary){Button b=new Button(context);b.setText(title);b.setTextSize(14);b.setAllCaps(false);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setTextColor(primary?0xff14151a:INK);b.setMinHeight(dp(52));b.setMinimumHeight(dp(52));b.setPadding(dp(14),dp(12),dp(14),dp(12));GradientDrawable base=surface(15,primary?0xfff5f5f7:0xff252830,primary?0xffdcdde3:0xff1b1e25,primary?0xffffffff:0x20ffffff);b.setBackground(new RippleDrawable(ColorStateList.valueOf(primary?0x20000000:0x22ffffff),base,null));return b;}
    Button action(LinearLayout parent,String title,boolean primary){Button b=button(title,primary);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);parent.addView(b,p);return b;}
    void navigation(Button button,boolean selected){button.setTextColor(selected?INK:DIM);button.setAlpha(1);button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22ffffff),surface(15,selected?0xff343944:0xff171a20,selected?0xff252a34:0xff13161c,selected?0x38ffffff:0x10ffffff),null));}
    Switch toggle(LinearLayout parent,String title,String subtitle,boolean checked){
        LinearLayout row=row();row.setMinimumHeight(dp(64));row.setPadding(0,dp(10),0,dp(10));LinearLayout copy=column();TextView name=text(copy,title,16,INK,true);text(copy,subtitle,13,MUTED,false);row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        Switch control=new Switch(context);control.setId(View.generateViewId());control.setChecked(checked);control.setContentDescription(title);name.setLabelFor(control.getId());control.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{INK,0xff9296a2}));control.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xff737b8b,0xff383d48}));control.setMinimumWidth(dp(52));control.setMinimumHeight(dp(48));row.addView(control,new LinearLayout.LayoutParams(-2,-2));row.setOnClickListener(v->control.performClick());parent.addView(row,new LinearLayout.LayoutParams(-1,-2));return control;
    }
    TextView metric(LinearLayout row,String title,String value){LinearLayout box=column();text(box,title,11,DIM,true);TextView number=text(box,value,15,INK,true);number.setMaxLines(2);row.addView(box,new LinearLayout.LayoutParams(0,-2,1));return number;}
    View emblem(){return new View(context){final Paint p=new Paint(3);@Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();p.setShader(new RadialGradient(w*.48f,h*.28f,h,new int[]{0x32ffffff,0x05ffffff,0x00000000},null,Shader.TileMode.CLAMP));c.drawOval(0,0,w,h,p);float side=Math.min(w,h)*.68f;RectF r=new RectF((w-side)/2,(h-side)/2,(w+side)/2,(h+side)/2);p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{0x48ffffff,0x06494952,0x238a8d98},null,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);c.drawRoundRect(r,side*.27f,side*.27f,p);p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{0xb0ffffff,0x06ffffff,0x70ffffff},null,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));c.drawRoundRect(r,side*.27f,side*.27f,p);p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(0xfff4f4f6);float cx=w/2,cy=h/2;Path star=new Path();star.moveTo(cx,cy-side*.27f);star.quadTo(cx+side*.05f,cy-side*.04f,cx+side*.27f,cy);star.quadTo(cx+side*.05f,cy+side*.05f,cx,cy+side*.27f);star.quadTo(cx-side*.05f,cy+side*.05f,cx-side*.27f,cy);star.quadTo(cx-side*.05f,cy-side*.05f,cx,cy-side*.27f);c.drawPath(star,p);}};}
}
