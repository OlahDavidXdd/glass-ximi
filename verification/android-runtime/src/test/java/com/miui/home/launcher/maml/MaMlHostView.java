package com.miui.home.launcher.maml;

/** Test-only reflection boundary with the names verified in Kashi V7.5. */
public class MaMlHostView extends android.widget.FrameLayout {
    public Object root,info;
    public MaMlHostView(android.content.Context context){super(context);}
    public Object getMaMlRoot(){return root;}
    public Object getItemInfo(){return info;}
}
