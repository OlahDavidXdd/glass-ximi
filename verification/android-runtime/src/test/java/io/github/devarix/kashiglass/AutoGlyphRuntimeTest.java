package io.github.devarix.kashiglass;
import android.graphics.*;
import android.graphics.drawable.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,35},manifest=Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AutoGlyphRuntimeTest {
    @Test public void reflectiveUncloneableLayersKeepTheirOriginalCallbacksAndBounds(){
        Drawable child=new Drawable(){public void draw(Canvas canvas){Paint paint=new Paint();paint.setColor(Color.WHITE);canvas.drawCircle(64,64,32,paint);}public void setAlpha(int value){}public void setColorFilter(ColorFilter filter){}public int getOpacity(){return PixelFormat.TRANSLUCENT;}};
        Drawable.Callback owner=new Drawable.Callback(){public void invalidateDrawable(Drawable who){}public void scheduleDrawable(Drawable who,Runnable what,long when){}public void unscheduleDrawable(Drawable who,Runnable what){}};
        child.setCallback(owner);child.setBounds(11,22,77,88);Drawable wrapper=new com.miui.home.common.drawable.LayerAdaptiveIconDrawable(child);assertNotNull(AutoGlyph.capture(wrapper));assertSame(owner,child.getCallback());assertEquals(new Rect(11,22,77,88),child.getBounds());
    }
    @Test public void emptyBitmapDrawableDoesNotCrashAutomaticCapture(){assertNull(AutoGlyph.capture(new BitmapDrawable(RuntimeEnvironment.getApplication().getResources(),(Bitmap)null)));}
    @Test public void validCapturePreservesBoundsAndProducesCenteredTransparentGlyph(){Bitmap source=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(source);Paint paint=new Paint();paint.setColor(Color.WHITE);canvas.drawCircle(16,16,8,paint);Drawable drawable=new BitmapDrawable(RuntimeEnvironment.getApplication().getResources(),source);drawable.setBounds(7,8,44,56);Bitmap capture=AutoGlyph.capture(drawable);assertEquals(new Rect(7,8,44,56),drawable.getBounds());Bitmap result=AutoGlyph.generate(capture);assertEquals(256,result.getWidth());assertEquals(0,Color.alpha(result.getPixel(0,0)));assertTrue(Color.alpha(result.getPixel(128,128))>200);}
}
