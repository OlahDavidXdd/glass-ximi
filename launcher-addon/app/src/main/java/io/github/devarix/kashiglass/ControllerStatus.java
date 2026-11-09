package io.github.devarix.kashiglass;

/** Presentation uses fresh launcher reads, never just the installed APK or a saved hook version. */
final class ControllerStatus {
    final String title,detail,loaded;final boolean connected;
    private ControllerStatus(String title,String detail,String loaded,boolean connected){this.title=title;this.detail=detail;this.loaded=loaded;this.connected=connected;}
    static ControllerStatus read(int installed,int loadedCode,String loadedName,long connectedAt,long checkedAt,boolean checking,boolean enabled){
        if(connectedAt==0)return new ControllerStatus("Connect your launcher","Enable devarix Glass in LSPosed for System launcher, then restart it.","Not connected",false);
        if(checkedAt>0&&connectedAt<checkedAt)return new ControllerStatus(checking?"Checking connection…":"No fresh launcher reply",checking?"Requesting the loaded hook version. Open Home if the launcher is paused.":"The saved hook version is not a live connection. Open Home and refresh.","Unconfirmed",false);
        if(loadedCode<=0)return new ControllerStatus("Hook version unknown","An older hook may be running. Restart the launcher or reboot.","Unreported",false);
        if(installed!=loadedCode)return new ControllerStatus("Restart to update","The installed app and loaded hook differ. Restart the launcher; reboot if needed.",loadedName,false);
        return new ControllerStatus("Launcher connected",enabled?"Glass settings are enabled. Check effects in your launcher.":"The hook is connected. Enable glass icons in Appearance to get started.",loadedName,true);
    }
}
