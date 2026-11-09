package io.github.devarix.kashiglass;

import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.*;
import android.widget.ImageView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,35},manifest=Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LiveIconBindingRuntimeTest {
    private final LiveIconBinding binding=new LiveIconBinding();
    private ImageView image;
    private Bitmap glyph;
    private LiveIconBinding.Slot slot;
    @Before public void setup(){
        image=new ImageView(RuntimeEnvironment.getApplication());glyph=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888);glyph.eraseColor(Color.WHITE);
        slot=new LiveIconBinding.Slot(){public Drawable get(){return image.getDrawable();}public void set(Drawable d){image.setImageDrawable(d);}};
    }
    private Drawable drawable(Bitmap source){return new BitmapDrawable(image.getResources(),OwnedTransitionDrawable.copy(source));}
    private boolean repair(){return binding.repair("liquid:app",glyph,slot,this::drawable);}
    @Test public void fiveHundredNativeSwapsNeverImportStockPixels(){
        assertTrue(repair());
        for(int i=0;i<500;i++){
            image.setImageDrawable(new android.graphics.drawable.ColorDrawable(Color.RED));
            assertFalse(binding.owns(image.getDrawable()));assertSame(glyph,binding.trusted("liquid:app"));
            // Simulate a cache miss: repair must use retained, trusted raw artwork.
            assertTrue(binding.repair("liquid:app",null,slot,this::drawable));
            assertEquals(Color.WHITE,((BitmapDrawable)image.getDrawable()).getBitmap().getPixel(12,12));
            assertFalse(repair());
        }
        assertEquals(Color.WHITE,glyph.getPixel(12,12));
    }
    @Test public void actualLiquidDrawableSurvivesRepeatedStockReplacementWithoutChangingSourceAlpha(){
        LiquidGlassDrawable.configure(null,null,true,true);
        java.util.function.Function<Bitmap,Drawable> liquid=source->{Drawable d=new LiquidGlassDrawable(image,source,null,false,24,m->{});d.setAlpha(137);return d;};
        for(int i=0;i<500;i++){
            image.setImageDrawable(new ColorDrawable(Color.RED));
            assertTrue(binding.repair("liquid:app",i==0?glyph:null,slot,liquid));
            assertTrue(image.getDrawable() instanceof LiquidGlassDrawable);
            assertSame(glyph,((LiquidGlassDrawable)image.getDrawable()).glyph());assertEquals(137,image.getDrawable().getAlpha());
            assertFalse(binding.repair("liquid:app",null,slot,liquid));
        }
    }
    @Test public void pixelMutationAndRecyclingAreRepairedWithoutPoisoningTemplate(){
        repair();((BitmapDrawable)image.getDrawable()).getBitmap().eraseColor(Color.RED);assertTrue(repair());
        ((BitmapDrawable)image.getDrawable()).getBitmap().recycle();assertTrue(repair());
        assertEquals(Color.WHITE,glyph.getPixel(12,12));assertTrue(binding.owns(image.getDrawable()));
    }
    @Test public void recycledViewAndStyleSwitchCannotReusePreviousArtwork(){
        repair();assertNull(binding.trusted("frosted:app"));assertNull(binding.trusted("liquid:other"));
        Bitmap other=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888);other.eraseColor(Color.GREEN);
        assertTrue(binding.repair("liquid:other",other,slot,this::drawable));assertSame(other,binding.trusted("liquid:other"));
        binding.clear();assertNull(binding.trusted("liquid:other"));assertFalse(binding.owns(image.getDrawable()));
    }
    @Test public void nativeNullTransparentMaskAndHiddenStateRemainAuthoritative(){
        repair();Drawable owned=image.getDrawable(),stock=new ColorDrawable(Color.RED),mask=new ColorDrawable(Color.TRANSPARENT);
        assertNull(LiveIconBinding.drawingSlot(owned,null,mask,false));
        assertSame(mask,LiveIconBinding.drawingSlot(owned,mask,mask,false));
        assertSame(mask,LiveIconBinding.drawingSlot(owned,stock,mask,true));
        assertSame(owned,LiveIconBinding.drawingSlot(owned,stock,mask,false));
        Drawable transparent=new ColorDrawable(Color.TRANSPARENT);assertSame(transparent,LiveIconBinding.drawingSlot(owned,transparent,mask,false));
        Drawable faded=drawable(glyph);faded.setAlpha(0);
        assertSame("A faded old icon is not a clear placeholder",owned,LiveIconBinding.drawingSlot(owned,faded,mask,false));
    }
    @Test public void preDrawRepairsAttachedViewAndStopsAfterDetach(){
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();activity.setContentView(image);
        final int[] calls={0};new IconIntegrityWatcher(image,v->{calls[0]++;repair();});
        image.getViewTreeObserver().dispatchOnPreDraw();assertTrue(binding.owns(image.getDrawable()));
        image.setImageDrawable(new ColorDrawable(Color.RED));image.getViewTreeObserver().dispatchOnPreDraw();assertTrue(binding.owns(image.getDrawable()));
        int attached=calls[0];activity.setContentView(new ImageView(activity));image.getViewTreeObserver().dispatchOnPreDraw();assertEquals(attached,calls[0]);
        activity.setContentView(image);image.setImageDrawable(new ColorDrawable(Color.RED));image.getViewTreeObserver().dispatchOnPreDraw();assertTrue(binding.owns(image.getDrawable()));
    }
    @Test public void fiveHundredTransitionHandoffsSurviveRecyclingAndTargetChanges(){
        Bitmap other=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888);other.eraseColor(Color.GREEN);
        OwnedTransitionDrawable previous=null;
        for(int i=0;i<500;i++){
            if(previous!=null){previous.getBitmap().eraseColor(Color.RED);previous.getBitmap().recycle();}
            Object target=i%2==0?glyph:other;
            OwnedTransitionDrawable next=AnimationArtwork.fresh(target,image.getResources(),key->(Bitmap)key);
            assertNotSame(previous,next);assertEquals(i%2==0?Color.WHITE:Color.GREEN,next.getBitmap().getPixel(12,12));previous=next;
        }
        assertEquals(Color.WHITE,glyph.getPixel(12,12));assertEquals(Color.GREEN,other.getPixel(12,12));
    }
}
