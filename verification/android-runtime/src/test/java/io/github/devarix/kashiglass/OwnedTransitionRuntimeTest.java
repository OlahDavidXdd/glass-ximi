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
public class OwnedTransitionRuntimeTest {
    private Bitmap template;
    @Before public void setup(){
        LiquidGlassDrawable.configure(null,null,true,true);
        template=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888);
        template.eraseColor(Color.TRANSPARENT);template.setPixel(12,12,0xffeeeeee);
    }
    private OwnedTransitionDrawable create(){return new OwnedTransitionDrawable(RuntimeEnvironment.getApplication().getResources(),template);}
    private Bitmap pixels(Drawable drawable){return drawable instanceof OwnedTransitionDrawable?((OwnedTransitionDrawable)drawable).getBitmap():((BitmapDrawable)drawable).getBitmap();}
    @Test public void nativeMutationDoesNotChangeTemplateOrSibling(){
        OwnedTransitionDrawable original=create();Drawable.ConstantState state=original.getConstantState();
        Drawable a=state.newDrawable(),b=state.newDrawable(RuntimeEnvironment.getApplication().getResources());
        assertNotSame(template,pixels(original));assertNotSame(pixels(a),pixels(b));assertTrue(pixels(a).isMutable());
        pixels(a).eraseColor(Color.RED);
        assertEquals(0xffeeeeee,template.getPixel(12,12));assertEquals(0xffeeeeee,pixels(b).getPixel(12,12));
    }
    @Test public void constantStateStillClonesAfterNativeBitmapWasRecycled(){
        OwnedTransitionDrawable original=create();Drawable.ConstantState state=original.getConstantState();
        original.getBitmap().recycle();Drawable clone=state.newDrawable();
        assertFalse(pixels(clone).isRecycled());assertEquals(0xffeeeeee,pixels(clone).getPixel(12,12));
        Bitmap result=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888);clone.setBounds(0,0,24,24);clone.draw(new Canvas(result));
        assertEquals(0xffeeeeee,result.getPixel(12,12));assertEquals(0,Color.alpha(result.getPixel(0,0)));
    }
    @Test public void frostedClonesAlsoOwnIndependentPixels(){
        LiquidGlassDrawable.configure(null,null,true,false);
        OwnedTransitionDrawable original=create();Drawable.ConstantState state=original.getConstantState();
        Drawable a=state.newDrawable(),b=state.newDrawable();assertNotSame(pixels(a),pixels(b));
        pixels(a).recycle();assertFalse(pixels(b).isRecycled());assertFalse(template.isRecycled());
        assertEquals(0xffeeeeee,pixels(state.newDrawable()).getPixel(12,12));
    }
    @Test public void transparentHolesAndFadeAlphaArePreservedOnCanvas(){
        OwnedTransitionDrawable drawable=create();drawable.setBounds(0,0,24,24);drawable.setAlpha(127);
        Bitmap result=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888);drawable.draw(new Canvas(result));
        assertEquals(0,Color.alpha(result.getPixel(0,0)));assertEquals(127,Color.alpha(result.getPixel(12,12)));
    }
    @Test public void invalidTemplatesAreRejected(){
        try{OwnedTransitionDrawable.copy(null);fail("null template accepted");}catch(IllegalArgumentException expected){}
        template.recycle();try{OwnedTransitionDrawable.copy(template);fail("recycled template accepted");}catch(IllegalArgumentException expected){}
    }
}
