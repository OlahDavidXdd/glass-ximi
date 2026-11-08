package io.github.devarix.kashiglass;
import org.junit.Test;
import static org.junit.Assert.*;
public class AutoIconPixelsTest {
    @Test public void flatTileBackgroundIsRemovedButForegroundSurvives(){
        int[] source=new int[100];java.util.Arrays.fill(source,0xff003399);source[44]=source[45]=source[54]=source[55]=0xffffffff;
        int[] result=AutoIconPixels.silver(source,10);assertEquals(0,result[0]);assertEquals(255,result[44]>>>24);assertEquals(0xff003399,source[0]);
    }
    @Test public void transparentShapesKeepTheirAlphaAndBecomeGray(){
        int[] result=AutoIconPixels.silver(new int[]{0,0x8080ff00,0xff0000ff,0},2);
        assertEquals(0,result[0]);assertEquals(128,result[1]>>>24);
        for(int color:result)if((color>>>24)>0){assertEquals((color>>16)&255,(color>>8)&255);assertEquals((color>>8)&255,color&255);}
    }
    @Test public void uniformArtworkNeverDisappears(){
        int[] source=new int[100];java.util.Arrays.fill(source,0xffffffff);int[] result=AutoIconPixels.silver(source,10);
        for(int pixel:result)assertEquals(255,pixel>>>24);
    }
    @Test public void differentCornersPreservePhotographicTiles(){
        int[] source=new int[]{0xffff0000,0xff00ff00,0xff0000ff,0xffdddddd};
        for(int pixel:AutoIconPixels.silver(source,2))assertEquals(255,pixel>>>24);
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsInvalidDimensions(){AutoIconPixels.silver(new int[3],2);}
}
