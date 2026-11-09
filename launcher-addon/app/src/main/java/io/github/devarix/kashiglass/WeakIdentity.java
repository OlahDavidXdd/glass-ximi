package io.github.devarix.kashiglass;

import java.lang.ref.WeakReference;

/** Weak cache identity that distinguishes an original null from a collected non-null value. */
final class WeakIdentity {
    private WeakReference<Object> reference=new WeakReference<>(null);
    private boolean wasNull=true;
    void set(Object value){reference=new WeakReference<>(value);wasNull=value==null;}
    boolean matches(Object value){return value==null?wasNull:reference.get()==value;}
}
