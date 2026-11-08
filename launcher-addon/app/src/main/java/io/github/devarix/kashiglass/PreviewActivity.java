package io.github.devarix.kashiglass;

import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PreviewActivity extends Activity {
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        try(var shader=getAssets().open("shaders/qwea0_lens.agsl");var plate=getAssets().open("liquid_plate.png")){
            LiquidGlassDrawable.configure(new String(shader.readAllBytes(),StandardCharsets.UTF_8),BitmapFactory.decodeStream(plate),getSharedPreferences(GlassConfig.PREFS,0).getBoolean("white_symbols",true),getSharedPreferences(GlassConfig.PREFS,0).getBoolean("screen_match",true));
        }catch(Exception e){throw new IllegalStateException("Missing preview assets",e);}
        setContentView(new PreviewView());
    }
    private final class PreviewView extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        private final ArrayList<LiquidGlassDrawable> icons=new ArrayList<>();
        private final ArrayList<String> labels=new ArrayList<>();
        private Bitmap background;
        private LiquidGlassDrawable dock,search;
        private String status="Live shader preview in this app. Launcher wallpaper capture is a separate device test.";
        private int dragging=-1;private float lastX,lastY;
        private final GlassBackdrop backdrop=new GlassBackdrop(){
            public boolean isAvailable(){return background!=null;}
            public boolean draw(Canvas c,View owner,Rect bounds,int margin){if(background==null)return false;c.drawBitmap(background,margin-bounds.left,margin-bounds.top,paint);return true;}
        };
        PreviewView(){
            super(PreviewActivity.this);setLayerType(View.LAYER_TYPE_HARDWARE,null);
            String[] packages={"com.android.settings","com.android.camera","com.miui.weather2","com.google.android.apps.bard","com.instagram.android","com.android.vending"};
            String[] names={"Settings","Camera","Weather","Gemini","Instagram","Play Store"};
            for(int i=0;i<packages.length;i++)try(var stream=getAssets().open("glyphs/"+packages[i]+".png")){
                BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
                LiquidGlassDrawable drawable=new LiquidGlassDrawable(this,BitmapFactory.decodeStream(stream,null,options),backdrop,true,16,message->{if(message.contains("unavailable")&&!message.equals(status)){status=message;invalidate();}});
                drawable.setCallback(this);icons.add(drawable);labels.add(names[i]);
            }catch(Exception e){throw new IllegalStateException(e);}
            dock=new LiquidGlassDrawable(this,null,backdrop,true,16,message->{},GlassSurface.DOCK);dock.setCallback(this);
            search=new LiquidGlassDrawable(this,null,backdrop,true,16,message->{},GlassSurface.SEARCH);search.setCallback(this);
            setContentDescription("Liquid Glass preview. Drag an icon across the striped background to inspect refraction.");
        }
        private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
            if(w<1||h<1)return;background=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(background);
            Paint p=new Paint();float spacing=dp(18);p.setShader(new LinearGradient(0,0,spacing,0,new int[]{0xff151515,0xff555555,0xffaaaaaa,0xff303030,0xff151515},new float[]{0,.32f,.48f,.68f,1},Shader.TileMode.REPEAT));c.drawRect(0,0,w,h,p);
            int cell=w/3,size=Math.min(cell-dp(24),(h-dp(533))/2);size=Math.max(dp(48),size);
            int firstY=dp(153),searchY=firstY+size+dp(44),dockY=searchY+dp(54);
            search.setBounds(w/2-dp(65),searchY,w/2+dp(65),searchY+dp(36));
            dock.setBounds(dp(10),dockY,w-dp(10),dockY+size+dp(52));
            for(int i=0;i<icons.size();i++){int x=(i%3)*cell+(cell-size)/2,y=i<3?firstY:dockY+dp(12);icons.get(i).setBounds(x,y,x+size,y+size);}
        }
        @Override protected boolean verifyDrawable(Drawable who){return icons.contains(who)||who==dock||who==search||super.verifyDrawable(who);}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);if(background==null)return;canvas.drawBitmap(background,0,0,paint);
            paint.setColor(0xec181818);canvas.drawRoundRect(dp(12),dp(16),getWidth()-dp(12),dp(133),dp(18),dp(18),paint);
            paint.setColor(Color.WHITE);paint.setTextSize(dp(24));canvas.drawText("devarix · Liquid Glass",dp(26),dp(61),paint);paint.setTextSize(dp(12));canvas.drawText("QWEA0 AGSL lens · Drag an icon to see refraction",dp(26),dp(94),paint);
            dock.draw(canvas);search.draw(canvas);
            paint.setColor(Color.WHITE);paint.setTextSize(dp(13));paint.setTextAlign(Paint.Align.CENTER);canvas.drawText("⌕  Search",search.getBounds().exactCenterX(),search.getBounds().exactCenterY()+dp(5),paint);paint.setTextAlign(Paint.Align.LEFT);
            for(int i=0;i<icons.size();i++){LiquidGlassDrawable icon=icons.get(i);icon.draw(canvas);Rect b=icon.getBounds();paint.setColor(Color.WHITE);paint.setTextSize(dp(13));paint.setTextAlign(Paint.Align.CENTER);canvas.drawText(labels.get(i),b.exactCenterX(),b.bottom+dp(26),paint);paint.setTextAlign(Paint.Align.LEFT);}
            paint.setColor(0xed181818);canvas.drawRoundRect(dp(12),getHeight()-dp(110),getWidth()-dp(12),getHeight()-dp(12),dp(18),dp(18),paint);paint.setColor(Color.WHITE);paint.setTextSize(dp(12));canvas.drawText("Actual GPU preview; no root needed for this screen.",dp(24),getHeight()-dp(76),paint);
            String line=status.length()>77?status.substring(0,77)+"…":status;paint.setColor(0xffbbbbbb);paint.setTextSize(dp(10));canvas.drawText(line,dp(24),getHeight()-dp(46),paint);
        }
        @Override public boolean onTouchEvent(MotionEvent event){
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){for(int i=icons.size()-1;i>=0;i--)if(icons.get(i).getBounds().contains((int)event.getX(),(int)event.getY())){dragging=i;lastX=event.getX();lastY=event.getY();icons.get(i).setState(new int[]{android.R.attr.state_pressed});invalidate();return true;}}
            if(action==MotionEvent.ACTION_MOVE&&dragging>=0){Rect b=new Rect(icons.get(dragging).getBounds());b.offset(Math.round(event.getX()-lastX),Math.round(event.getY()-lastY));b.offsetTo(Math.max(dp(6),Math.min(getWidth()-b.width()-dp(6),b.left)),Math.max(dp(145),Math.min(getHeight()-dp(145)-b.height(),b.top)));icons.get(dragging).setBounds(b);lastX=event.getX();lastY=event.getY();invalidate();return true;}
            if((action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)&&dragging>=0){icons.get(dragging).setState(new int[0]);dragging=-1;performClick();invalidate();return true;}
            return super.onTouchEvent(event);
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
