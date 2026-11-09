package io.github.devarix.kashiglass;

/** Native source-icon visibility must survive drawable replacement/rebinding. */
final class SourceVisibility {
    volatile boolean hidden;
    volatile int alpha=255;
    private String boundKey;
    boolean bind(String key){
        if(key==null)return false;
        boolean recycled=boundKey!=null&&!boundKey.equals(key);boundKey=key;
        if(recycled){hidden=false;alpha=255;}return recycled;
    }
    void observe(boolean isHidden,int opacity){hidden=isHidden;alpha=Math.max(0,Math.min(255,opacity));}
    void visibility(int visibility){hidden=visibility!=0;}
    static float normalized(float opacity){return Float.isFinite(opacity)?Math.max(0,Math.min(1,opacity)):Float.NaN;}
    void opacity(float opacity){float value=normalized(opacity);if(Float.isFinite(value))alpha=(int)(value*255);}
    void transparentPlaceholder(){hidden=true;}
}
