package io.github.devarix.kashiglass;
import org.junit.Test;
import static org.junit.Assert.*;
public class WidgetSurfacePolicyTest {
    @Test public void clockAndWeatherProvidersAreRecognized(){assertTrue(WidgetSurfacePolicy.supported("com.miui.weather2/.WidgetProvider",null));assertTrue(WidgetSurfacePolicy.supported("com.google.android.deskclock/.AnalogAppWidgetProvider",null));assertTrue(WidgetSurfacePolicy.supported("org.example/.ClockWidget","Clock"));}
    @Test public void genericWidgetAppsNeedAnExplicitClockOrWeatherLabel(){assertTrue(WidgetSurfacePolicy.supported("org.widgets/.Provider","Analog Clock"));assertTrue(WidgetSurfacePolicy.supported("org.widgets/.Provider","Weather"));assertFalse(WidgetSurfacePolicy.supported("org.widgets/.Provider","Calendar"));assertFalse(WidgetSurfacePolicy.supported(null,null));}
    @Test public void backgroundsMustCoverMostOfTheCard(){assertTrue(WidgetSurfacePolicy.coversCard(300,300,280,280));assertFalse(WidgetSurfacePolicy.coversCard(300,300,64,64));assertFalse(WidgetSurfacePolicy.coversCard(300,300,700,300));assertFalse(WidgetSurfacePolicy.coversCard(0,0,300,300));}
    @Test public void onlyNamedDecorativeImagesAreRemovable(){assertTrue(WidgetSurfacePolicy.decorativeImage("weather_background",false,false));assertTrue(WidgetSurfacePolicy.decorativeImage("widget_bg",false,false));assertFalse(WidgetSurfacePolicy.decorativeImage("weather_icon",false,false));assertFalse(WidgetSurfacePolicy.decorativeImage("clock_face",false,false));assertFalse(WidgetSurfacePolicy.decorativeImage("bg_weather_icon",false,false));}
    @Test public void interactionsAndDescriptionsProtectImages(){assertFalse(WidgetSurfacePolicy.decorativeImage("background",true,false));assertFalse(WidgetSurfacePolicy.decorativeImage("background",false,true));assertFalse(WidgetSurfacePolicy.decorativeImage(null,false,false));}
}
