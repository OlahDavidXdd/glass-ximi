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
    private java.lang.ref.WeakReference<View> navigationScene=new java.lang.ref.WeakReference<>(null);
    ControllerUi(Context context){this.context=context;}
    int dp(float value){return Math.round(value*context.getResources().getDisplayMetrics().density);}
    TextView label(String value,int size,int color,boolean bold){TextView t=new TextView(context);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));t.setIncludeFontPadding(false);t.setLineSpacing(dp(3),1);return t;}
    TextView text(LinearLayout parent,String value,int size,int color,boolean bold){TextView t=label(value,size,color,bold);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);parent.addView(t,p);return t;}
    LinearLayout column(){LinearLayout l=new LinearLayout(context);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(context);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    LinearLayout card(LinearLayout parent){LinearLayout c=column();c.setPadding(dp(18),dp(18),dp(18),dp(18));c.setBackground(surface(22,0xff151515,0xff101010,0x12ffffff));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);parent.addView(c,p);return c;}
    void section(LinearLayout parent,String title,String hint){text(parent,title,22,INK,true);if(hint!=null)text(parent,hint,14,MUTED,false);}
    void space(LinearLayout parent,int height){parent.addView(new View(context),new LinearLayout.LayoutParams(1,dp(height)));}
    void divider(LinearLayout parent){View v=new View(context);v.setBackgroundColor(0xff292c34);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(1));p.topMargin=dp(8);p.bottomMargin=dp(8);parent.addView(v,p);}
    GradientDrawable surface(int radius,int top,int bottom,int border){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{top,bottom});d.setCornerRadius(dp(radius));d.setStroke(dp(1),border);return d;}
    private static final GlassBackdrop BLACK=new GlassBackdrop(){public boolean isAvailable(){return false;}public boolean draw(Canvas canvas,View view,Rect bounds,int margin){return false;}};
    // The same live-screen material/rim renderer as launcher icons, without a wallpaper tile.
    Drawable glass(View owner,int kind,boolean emphasized){
        float radius=kind==GlassSurface.SEARCH?1000:22;
        GradientDrawable body=surface((int)radius,emphasized?0xff303030:0xe61c1c1c,emphasized?0xff202020:0xe6121212,Color.TRANSPARENT);
        Drawable material=new LiquidGlassDrawable(owner,null,BLACK,false,24,message->{},kind,true);
        return new CapsuleMaterial(body,material);
    }
    private Drawable ripple(Drawable glass){return new RippleDrawable(ColorStateList.valueOf(0x20ffffff),glass,null);}
    Button button(String title,boolean primary){Button b=new Button(context);b.setText(title);b.setTextSize(14);b.setAllCaps(false);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setTextColor(INK);b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setPadding(dp(14),dp(10),dp(14),dp(10));b.setBackground(ripple(glass(b,GlassSurface.SEARCH,primary)));return b;}
    Button action(LinearLayout parent,String title,boolean primary){Button b=button(title,primary);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);parent.addView(b,p);return b;}
    void navigation(Button button,boolean selected){button.setTextColor(selected?INK:MUTED);button.setAlpha(1);button.setBackground(ripple(selected&&navigationScene.get()!=null?new KyantNavigationDrawable(button,navigationScene.get()):new ColorDrawable(Color.TRANSPARENT)));}
    void navigationSurface(LinearLayout navigation,View scene){navigationScene=new java.lang.ref.WeakReference<>(scene);navigation.setBackground(new KyantNavigationDrawable(navigation,scene));}
    void navigationIcon(Button button,int index){button.setCompoundDrawablesWithIntrinsicBounds(null,new NavigationGlyph(index,dp(20)),null,null);button.setCompoundDrawablePadding(dp(4));}
    private Drawable switchPart(View owner,boolean thumb){
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_checked},sizedGlass(owner,true,thumb));
        states.addState(new int[]{},sizedGlass(owner,false,thumb));
        return states;
    }
    private Drawable sizedGlass(View owner,boolean checked,boolean thumb){
        Drawable drawable=glass(owner,GlassSurface.SEARCH,checked);
        if(thumb)drawable=new LayerDrawable(new Drawable[]{drawable,surface(1000,checked?0xfffafafa:0xff929292,checked?0xffdedede:0xff777777,0x80ffffff)});
        return new SizedDrawable(drawable,dp(thumb?26:52),dp(thumb?26:30));
    }
    void slider(SeekBar slider){
        slider.setThumbTintList(null);slider.setProgressTintList(null);slider.setProgressBackgroundTintList(null);
        slider.setThumb(new SizedDrawable(glass(slider,GlassSurface.SEARCH,true),dp(24),dp(24)));
        LayerDrawable track=new LayerDrawable(new Drawable[]{glass(slider,GlassSurface.SEARCH,false),new ClipDrawable(glass(slider,GlassSurface.SEARCH,true),Gravity.LEFT,ClipDrawable.HORIZONTAL)});
        track.setId(0,android.R.id.background);track.setId(1,android.R.id.progress);for(int i=0;i<2;i++){track.setLayerHeight(i,dp(6));track.setLayerGravity(i,Gravity.CENTER_VERTICAL|Gravity.FILL_HORIZONTAL);}slider.setProgressDrawable(track);slider.setMinimumHeight(dp(48));
    }
    Switch toggle(LinearLayout parent,String title,String subtitle,boolean checked){
        LinearLayout row=row();row.setMinimumHeight(dp(64));row.setPadding(0,dp(10),0,dp(8));row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x12ffffff),new ColorDrawable(Color.TRANSPARENT),null));LinearLayout copy=column();TextView name=text(copy,title,16,INK,true);text(copy,subtitle,13,MUTED,false);row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        Switch control=new Switch(context);control.setId(View.generateViewId());control.setChecked(checked);control.setContentDescription(title);name.setLabelFor(control.getId());control.setThumbTintList(null);control.setTrackTintList(null);control.setSplitTrack(false);control.setThumbDrawable(switchPart(control,true));control.setTrackDrawable(switchPart(control,false));control.setMinimumWidth(dp(52));control.setMinimumHeight(dp(48));row.addView(control,new LinearLayout.LayoutParams(-2,-2));row.setOnClickListener(v->control.performClick());LinearLayout.LayoutParams option=new LinearLayout.LayoutParams(-1,-2);option.topMargin=dp(2);parent.addView(row,option);return control;
    }
    TextView metric(LinearLayout row,String title,String value){LinearLayout box=column();text(box,title,11,DIM,true);TextView number=text(box,value,15,INK,true);number.setMaxLines(2);row.addView(box,new LinearLayout.LayoutParams(0,-2,1));return number;}
    private static final class CapsuleMaterial extends LayerDrawable {
        private final Path clip=new Path();
        CapsuleMaterial(Drawable body,Drawable material){super(new Drawable[]{body,material});}
        @Override public void draw(Canvas canvas){Rect bounds=getBounds();if(bounds.isEmpty())return;float radius=Math.min(bounds.width(),bounds.height())*.5f;clip.reset();clip.addRoundRect(new RectF(bounds),radius,radius,Path.Direction.CW);int saved=canvas.saveLayer(new RectF(bounds),null);canvas.clipPath(clip);try{super.draw(canvas);}finally{canvas.restoreToCount(saved);}}
    }
    private static final class SizedDrawable extends LayerDrawable {
        private final int width,height;
        SizedDrawable(Drawable drawable,int width,int height){super(new Drawable[]{drawable});this.width=width;this.height=height;}
        @Override public int getIntrinsicWidth(){return width;}
        @Override public int getIntrinsicHeight(){return height;}
    }
    private static final class NavigationGlyph extends Drawable {
        private final int kind,size;private final Paint paint=new Paint(3);
        NavigationGlyph(int kind,int size){this.kind=kind;this.size=size;}
        @Override public int getIntrinsicWidth(){return size;}@Override public int getIntrinsicHeight(){return size;}
        @Override public void draw(Canvas c){int saved=c.save();Rect b=getBounds();c.translate(b.left,b.top);c.scale(b.width()/24f,b.height()/24f);paint.setColor(INK);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
            if(kind==0){Path home=new Path();home.moveTo(3,11);home.lineTo(12,3);home.lineTo(21,11);home.moveTo(5,10);home.lineTo(5,21);home.lineTo(10,21);home.lineTo(10,15);home.lineTo(14,15);home.lineTo(14,21);home.lineTo(19,21);home.lineTo(19,10);c.drawPath(home,paint);}
            else if(kind==1){for(int i=0;i<3;i++){float x=5+i*7,y=i==1?15:8;c.drawLine(x,3,x,y-3,paint);c.drawLine(x,y+3,x,21,paint);c.drawCircle(x,y,2.8f,paint);}}
            else{Path pulse=new Path();pulse.moveTo(2,12);pulse.lineTo(7,12);pulse.lineTo(10,5);pulse.lineTo(14,19);pulse.lineTo(17,12);pulse.lineTo(22,12);c.drawPath(pulse,paint);}c.restoreToCount(saved);}
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}@Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    View emblem(){return new View(context){final Paint p=new Paint(3);@Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();p.setShader(new RadialGradient(w*.48f,h*.28f,h,new int[]{0x32ffffff,0x05ffffff,0x00000000},null,Shader.TileMode.CLAMP));c.drawOval(0,0,w,h,p);float side=Math.min(w,h)*.68f;RectF r=new RectF((w-side)/2,(h-side)/2,(w+side)/2,(h+side)/2);p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{0x48ffffff,0x06494952,0x238a8d98},null,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);c.drawRoundRect(r,side*.27f,side*.27f,p);p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{0xb0ffffff,0x06ffffff,0x70ffffff},null,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));c.drawRoundRect(r,side*.27f,side*.27f,p);p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(0xfff4f4f6);float cx=w/2,cy=h/2;Path star=new Path();star.moveTo(cx,cy-side*.27f);star.quadTo(cx+side*.05f,cy-side*.04f,cx+side*.27f,cy);star.quadTo(cx+side*.05f,cy+side*.05f,cx,cy+side*.27f);star.quadTo(cx-side*.05f,cy+side*.05f,cx-side*.27f,cy);star.quadTo(cx-side*.05f,cy-side*.05f,cx,cy-side*.27f);c.drawPath(star,p);}};}
}
