package io.github.devarix.kashiglass;

final class HookVersion {
    static final String RESTART_CAPABILITY="self_restart_v1";
    private HookVersion(){}
    static boolean matches(int installed,int loaded){return loaded>0&&installed==loaded;}
    static boolean canRestart(String capabilities,int pid){
        if(capabilities==null||pid<=0)return false;
        for(String capability:capabilities.split(","))if(RESTART_CAPABILITY.equals(capability.trim()))return true;
        return false;
    }
    static String status(int installed,String appName,int loaded,String hookName,long connected){
        if(connected==0)return "Loaded hook: not connected. Enable the LSPosed scope and reboot the phone.";
        if(loaded<=0)return "Loaded hook: version not reported; an older hook may still be running. App "+appName+". Reboot the phone to load this build.";
        if(!matches(installed,loaded))return "Loaded hook: "+hookName+"; app: "+appName+". Versions differ. Restart launcher, or reboot if restart is blocked.";
        return "Loaded hook: "+hookName+" — matches app. Last connection must be refreshed after updating.";
    }
}
