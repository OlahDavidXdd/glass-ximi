package io.github.devarix.kashiglass;

import android.graphics.*;
import android.os.Build;
import org.robolectric.shadow.api.Shadow;
import android.view.View;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,35},manifest=Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LiquidSnapshotRuntimeTest {
    @Before public void setup(){LiquidGlassDrawable.configure(null,null,true,true);}
    @Test public void realGlyphSnapshotHasWhiteCenterAndTransparentBody() throws Exception {
        Bitmap glyph;try(InputStream stream=new FileInputStream(System.getProperty("glass.assets")+"/glyphs/com.google.android.gm.png")){glyph=BitmapFactory.decodeStream(stream);}
        assertNotNull(glyph);assertEquals("ShadowNativeBitmap",Shadow.extract(glyph).getClass().getSimpleName());Bitmap result=LiquidGlassDrawable.coldTransition(glyph);
        assertEquals(192,result.getWidth());assertEquals(0,Color.alpha(result.getPixel(0,0)));
        int white=0;for(int y=35;y<157;y++)for(int x=35;x<157;x++){int pixel=result.getPixel(x,y);if(Color.alpha(pixel)>100){assertEquals(255,Color.red(pixel));assertEquals(255,Color.green(pixel));assertEquals(255,Color.blue(pixel));white++;}}
        assertTrue("white glyph was not drawn",white>200);
        assertTrue("glass body was filled",Color.alpha(result.getPixel(96,175))<80);
        File directory=new File(System.getProperty("glass.results"),"canvas");directory.mkdirs();
        try(OutputStream output=new FileOutputStream(new File(directory,"gmail-snapshot-api-"+Build.VERSION.SDK_INT+".png"))){assertTrue(result.compress(Bitmap.CompressFormat.PNG,100,output));}
    }
    @Test public void hiddenLiveDrawableDrawsNoPixelsAndSnapshotIgnoresSourceFade(){
        Bitmap glyph=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);new Canvas(glyph).drawColor(Color.WHITE);
        View owner=new View(RuntimeEnvironment.getApplication());owner.layout(0,0,64,64);
        LiquidGlassDrawable live=new LiquidGlassDrawable(owner,glyph,null,false,24,m->{});live.setBounds(0,0,64,64);live.setAlpha(0);
        Bitmap result=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);live.draw(new Canvas(result));
        assertEquals(0,live.getAlpha());assertEquals(0,Color.alpha(result.getPixel(32,32)));
        Bitmap snapshot=live.transitionBitmap();assertEquals(255,Color.alpha(snapshot.getPixel(96,96)));
        assertEquals(0,live.getAlpha());
    }
    @Test public void actualCanvasFadeMatchesSourceState(){
        Bitmap glyph=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);glyph.eraseColor(Color.WHITE);
        View owner=new View(RuntimeEnvironment.getApplication());owner.layout(0,0,64,64);
        LiquidGlassDrawable live=new LiquidGlassDrawable(owner,glyph,null,false,24,m->{});live.setBounds(0,0,64,64);
        SourceVisibility state=new SourceVisibility();state.opacity(.5f);live.setAlpha(state.alpha);
        Bitmap result=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);live.draw(new Canvas(result));
        assertEquals(127,Color.alpha(result.getPixel(32,32)));
    }
}
