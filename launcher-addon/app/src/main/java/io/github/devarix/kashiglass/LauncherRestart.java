package io.github.devarix.kashiglass;

/** Only restarts the fixed launcher package for a validated Android user. */
final class LauncherRestart {
    private LauncherRestart() {}
    static String command(int user) {
        if(user<0||user>99999)throw new IllegalArgumentException("Invalid user");
        return "am force-stop --user "+user+" com.miui.home && am start --user "+user+" -a android.intent.action.MAIN -c android.intent.category.HOME -p com.miui.home";
    }
}
