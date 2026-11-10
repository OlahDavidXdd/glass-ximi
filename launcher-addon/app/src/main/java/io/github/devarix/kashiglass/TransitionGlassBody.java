package io.github.devarix.kashiglass;
import android.graphics.*;

/** Motion snapshots cannot retain a compositor blur node. Keep a neutral glass body instead. */
final class TransitionGlassBody {
    private TransitionGlassBody(){}
    static Bitmap add(Bitmap foreground){
        if(foreground==null||foreground.isRecycled())return foreground;
        Bitmap result=Bitmap.createBitmap(foreground.getWidth(),foreground.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(result);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        float w=result.getWidth(),h=result.getHeight(),small=Math.min(w,h),edge=small*.009f;
        RectF body=new RectF(edge,edge,w-edge,h-edge);float radius=small*.27f;
        paint.setShader(new LinearGradient(0,0,w,h,new int[]{0x58ffffff,0x24dadada,0x1c7c7c7c,0x42ffffff},new float[]{0,.30f,.72f,1},Shader.TileMode.CLAMP));canvas.drawRoundRect(body,radius,radius,paint);
        int saved=canvas.save();Path clip=new Path();clip.addRoundRect(body,radius,radius,Path.Direction.CW);canvas.clipPath(clip);
        paint.setShader(new LinearGradient(0,0,0,h*.5f,new int[]{0x32ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));canvas.drawOval(-w*.2f,-h*.6f,w*1.2f,h*.32f,paint);canvas.restoreToCount(saved);
        paint.setShader(null);canvas.drawBitmap(foreground,0,0,paint);return result;
    }
}
