package xposed.audiorouter.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class PackageUtils {

    private static final String TAG = PackageUtils.class.getSimpleName();

    public static CharSequence getLabelForPackage(PackageManager packageManager, String packageName) {
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
            return packageManager.getApplicationLabel(info);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getLabelForPackage: Error resolving application info, " + e.getMessage());
            return packageName;
        }
    }
}
