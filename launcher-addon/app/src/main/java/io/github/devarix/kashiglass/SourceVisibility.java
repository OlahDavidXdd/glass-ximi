package io.github.devarix.kashiglass;

/** Native source-icon visibility must survive drawable replacement/rebinding. */
final class SourceVisibility {
    volatile boolean hidden;
    volatile int alpha=255;
    void visibility(int visibility){hidden=visibility!=0;}
    void opacity(float opacity){if(Float.isFinite(opacity))alpha=(int)(Math.max(0,Math.min(1,opacity))*255);}
    void transparentPlaceholder(){hidden=true;}
}
