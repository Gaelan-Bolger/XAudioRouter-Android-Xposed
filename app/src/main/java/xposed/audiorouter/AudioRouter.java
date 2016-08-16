package xposed.audiorouter;

import android.app.Application;

public class AudioRouter extends Application {

    public static String getAppVersion() {
        return BuildConfig.VERSION_NAME;
    }

}
