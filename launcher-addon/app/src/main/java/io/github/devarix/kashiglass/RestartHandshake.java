package io.github.devarix.kashiglass;

/** Validate a short-lived, explicit restart request for the already reported launcher PID. */
final class RestartHandshake {
    static final long MAX_AGE_MS=5000;
    private RestartHandshake(){}
    static boolean valid(String token,long created,long now,int requestedPid,int callerPid){
        return token!=null&&token.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")&&created>=0&&now>=created&&now-created<=MAX_AGE_MS&&requestedPid>0&&requestedPid==callerPid;
    }
}
