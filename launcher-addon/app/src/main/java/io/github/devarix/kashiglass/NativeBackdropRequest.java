package io.github.devarix.kashiglass;

import android.view.View;

/** Some firmware methods accept a call but ignore it. Validate supported mode getters. */
final class NativeBackdropRequest {
    private NativeBackdropRequest(){}
    static void configure(NativeScreenBackdrop.Access access,View view) throws Throwable {
        require(access.call(view,"setMiBackgroundBlurMode",1));require(access.call(view,"setMiViewBlurMode",1));
        check(access,view,"getMiBackgroundBlurMode",1);check(access,view,"getMiViewBlurMode",1);
        try{access.call(view,"clearMiBackgroundBlendColor");}catch(Throwable ignored){}
    }
    private static void check(NativeScreenBackdrop.Access access,View view,String method,int expected) throws Throwable {
        Object value;try{value=access.call(view,method);}catch(Throwable unavailable){return;}
        if(value instanceof Number&&((Number)value).intValue()!=expected)throw new IllegalStateException(method+" returned "+value+" after requesting "+expected);
    }
    private static void require(Object value){if(Boolean.FALSE.equals(value))throw new IllegalStateException("Native backdrop mode rejected");}
}
