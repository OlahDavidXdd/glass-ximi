package io.github.devarix.kashiglass;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,35},manifest=Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NativeInputAndClockRuntimeTest {
    @Test public void setterCallbackReceivesIndependentGlassPixelsAndPreservesNativeAlpha(){Bitmap ready=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888);ready.eraseColor(Color.WHITE);Drawable stock=new ColorDrawable(Color.RED);stock.setAlpha(120);stock.setBounds(1,2,9,10);Drawable replacement=NativeIconInput.replace(stock,ready,null);assertTrue(replacement instanceof OwnedTransitionDrawable);assertEquals(stock.getAlpha(),replacement.getAlpha());assertEquals(stock.getBounds(),replacement.getBounds());((OwnedTransitionDrawable)replacement).getBitmap().eraseColor(Color.GREEN);assertEquals(Color.WHITE,ready.getPixel(0,0));}
    @Test public void setterLeavesNativeClearAndUnavailableArtworkAlone(){Drawable clear=new ColorDrawable(Color.TRANSPARENT),stock=new ColorDrawable(Color.RED);Bitmap ready=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888);assertSame(clear,NativeIconInput.replace(clear,ready,null));assertSame(stock,NativeIconInput.replace(stock,null,null));ready.recycle();assertSame(stock,NativeIconInput.replace(stock,ready,null));}
    @Test public void clockHostDecorationRestoresAndLeavesChildrenAndTimeContentAlone(){FrameLayout host=new FrameLayout(RuntimeEnvironment.getApplication());TextClock clock=new TextClock(host.getContext());host.addView(clock);Drawable background=new ColorDrawable(Color.BLUE);host.setBackground(background);WidgetHostDecoration state=new WidgetHostDecoration(m->{});state.clear(host);assertNull(host.getBackground());assertSame(clock,host.getChildAt(0));state.restore();assertSame(background,host.getBackground());assertSame(clock,host.getChildAt(0));}
    @Test public void providerBackgroundUpdatesAreRetainedWhenRestoring(){View view=new View(RuntimeEnvironment.getApplication());view.setBackgroundColor(Color.BLUE);WidgetHostDecoration state=new WidgetHostDecoration(m->{});state.clear(view);Drawable latest=new ColorDrawable(Color.RED);view.setBackground(latest);state.restore();assertSame(latest,view.getBackground());state.clear(view);assertNull(view.getBackground());state.restore();assertSame(latest,view.getBackground());}
    @Test public void navigationRejectsRecursiveCaptureAndAcceptsSiblingScene(){FrameLayout root=new FrameLayout(RuntimeEnvironment.getApplication());View source=new View(root.getContext()),nav=new View(root.getContext());root.addView(source);root.addView(nav);assertTrue(KyantNavigationDrawable.safeSource(nav,source));assertFalse(KyantNavigationDrawable.safeSource(nav,root));assertFalse(KyantNavigationDrawable.safeSource(nav,nav));}
    // Robolectric API 33 uses Android 12 native Skia, which predates child.eval AGSL.
    @Test @Config(sdk=35) public void kyantShaderCompilesWithTheAdapterUniforms() throws Exception {String code=java.nio.file.Files.readString(java.nio.file.Paths.get(System.getProperty("glass.assets"),"shaders/kyant_refraction.agsl"));RuntimeShader shader=new RuntimeShader(code);shader.setFloatUniform("size",320f,72f);shader.setFloatUniform("offset",-32f,-32f);shader.setFloatUniform("cornerRadii",36f,36f,36f,36f);shader.setFloatUniform("refractionHeight",24f);shader.setFloatUniform("refractionAmount",-24f);shader.setFloatUniform("depthEffect",0f);assertNotNull(RenderEffect.createRuntimeShaderEffect(shader,"content"));}
    @Test public void softwareNavigationFallbackNeverReentersSceneDrawing(){final int[] draws={0};View scene=new View(RuntimeEnvironment.getApplication()){@Override protected void onDraw(Canvas canvas){draws[0]++;}};View owner=new View(scene.getContext());KyantNavigationDrawable glass=new KyantNavigationDrawable(owner,scene);glass.setBounds(0,0,200,60);Bitmap bitmap=Bitmap.createBitmap(200,60,Bitmap.Config.ARGB_8888);glass.draw(new Canvas(bitmap));assertEquals(0,draws[0]);assertTrue(Color.alpha(bitmap.getPixel(100,30))>0);}
}
