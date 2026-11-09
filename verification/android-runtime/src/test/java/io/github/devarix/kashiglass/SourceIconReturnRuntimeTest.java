package io.github.devarix.kashiglass;

import android.graphics.*;
import android.graphics.drawable.*;
import android.widget.TextView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,35},manifest=Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SourceIconReturnRuntimeTest {
    private TextView source;private Drawable owned,stock,mask;
    @Before public void setup(){source=new TextView(RuntimeEnvironment.getApplication());owned=bitmap(Color.WHITE);stock=bitmap(Color.RED);mask=new ColorDrawable(Color.TRANSPARENT);}
    private Drawable bitmap(int color){Bitmap b=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888);b.eraseColor(color);Drawable d=new BitmapDrawable(RuntimeEnvironment.getApplication().getResources(),b);d.setBounds(0,0,32,32);return d;}
    @Test public void fiveHundredNativeExitRestoresAreReplacedBeforePainting(){
        for(int i=0;i<500;i++){
            Drawable incoming=SourceIconReturn.replacement(stock,owned,mask,false);source.setCompoundDrawables(null,incoming,null,null);
            assertSame(owned,source.getCompoundDrawables()[1]);Bitmap out=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888);source.getCompoundDrawables()[1].draw(new Canvas(out));assertEquals(Color.WHITE,out.getPixel(16,16));
        }
    }
    @Test public void lateNativeDrawingSlotMutationIsRepairedSynchronously(){owned.setBounds(0,0,0,0);source.setCompoundDrawables(null,stock,null,null);assertTrue(SourceIconReturn.repair(source,owned,mask,false));assertSame(owned,source.getCompoundDrawables()[1]);assertEquals(stock.getBounds(),owned.getBounds());assertFalse(SourceIconReturn.repair(source,owned,mask,false));}
    @Test public void nativeSourceHideIsNotReversed(){source.setCompoundDrawables(null,stock,null,null);assertTrue(SourceIconReturn.repair(source,owned,mask,true));assertSame(mask,source.getCompoundDrawables()[1]);assertSame(mask,SourceIconReturn.replacement(mask,owned,mask,false));}
    @Test public void explicitClearAndTransparentPlaceholdersSurviveHiddenAndVisibleStates(){
        for(boolean hidden:new boolean[]{true,false}){assertNull(SourceIconReturn.replacement(null,owned,mask,hidden));Drawable clear=new ColorDrawable(Color.TRANSPARENT);assertSame(clear,SourceIconReturn.replacement(clear,owned,mask,hidden));}
    }
    @Test public void fadedBitmapIsLiveArtworkAndNotAnExplicitClear(){stock.setAlpha(0);assertFalse(SourceIconReturn.isClear(stock));assertTrue(SourceIconReturn.isClear(null));assertTrue(SourceIconReturn.isClear(mask));assertTrue(SourceIconReturn.isClear(new BitmapDrawable(source.getResources(),(Bitmap)null)));}
    @Test public void missingArtworkKeepsTheNativeSource(){assertSame(stock,SourceIconReturn.replacement(stock,null,mask,false));}
    @Test public void repairsPreserveAlphaAndOtherDrawableSlots(){
        Drawable left=bitmap(Color.GREEN),right=bitmap(Color.BLUE),bottom=bitmap(Color.YELLOW);owned.setAlpha(0);source.setCompoundDrawables(left,stock,right,bottom);
        SourceIconReturn.repair(source,owned,mask,false);Drawable[] actual=source.getCompoundDrawables();assertSame(left,actual[0]);assertSame(right,actual[2]);assertSame(bottom,actual[3]);assertEquals(0,actual[1].getAlpha());
    }
    @Test public void backingFieldAndVisibleSlotRecoverTogetherAfterNativeRestore(){
        LiveIconBinding binding=new LiveIconBinding();Bitmap glyph=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888);glyph.eraseColor(Color.WHITE);final Drawable[] backing={stock};
        LiveIconBinding.Slot slot=new LiveIconBinding.Slot(){public Drawable get(){return backing[0];}public void set(Drawable d){backing[0]=d;}};
        for(int i=0;i<500;i++){
            backing[0]=stock;source.setCompoundDrawables(null,stock,null,null);
            assertTrue(binding.repair("liquid:app",i==0?glyph:null,slot,b->new BitmapDrawable(source.getResources(),OwnedTransitionDrawable.copy(b))));
            SourceIconReturn.repair(source,backing[0],mask,false);assertTrue(binding.owns(source.getCompoundDrawables()[1]));assertEquals(Color.WHITE,glyph.getPixel(16,16));
        }
    }
}
