package xposed.audiorouter;

import android.annotation.SuppressLint;
import android.app.AndroidAppHelper;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class Xposed implements IXposedHookZygoteInit {

    private static final String TAG = Xposed.class.getSimpleName();
    private static final String PACKAGE_NAME = Xposed.class.getPackage().getName();
    public static final String PREFERENCES = "preferences";

    private static XSharedPreferences mPrefs;

    private Gson mGson = new Gson();

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        loadPrefs();

        try {
            hookMediaPlayer();
        } catch (Exception e) {
            log("Error hooking MediaPlayer, " + e.getMessage());
        }
        try {
            hookAudioAttributesBuilder();
        } catch (Exception e) {
            log("Error hooking AudioAttributesBuilder, " + e.getMessage());
        }
    }

    private void hookMediaPlayer() {
        XC_MethodHook prepareHook = new XC_MethodHook() {
            @SuppressLint("DefaultLocale")
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String packageName = AndroidAppHelper.currentPackageName();
                Rule rule = getRuleForPackage(packageName);
                int stream;
                if (null != rule && (stream = rule.getStream()) != -1) {
                    log(String.format("MediaPlayer.prepare/prepareAsync: Rule found for package [%s]", packageName));
                    MediaPlayer mediaPlayer = (MediaPlayer) param.thisObject;
                    mediaPlayer.setAudioStreamType(stream);
                    log(String.format("MediaPlayer.prepare/prepareAsync: AudioStreamType.set to [%d]", stream));
                }
            }
        };
        findAndHookMethod(MediaPlayer.class, "prepare", prepareHook);
        findAndHookMethod(MediaPlayer.class, "prepareAsync", prepareHook);
    }

    private void hookAudioAttributesBuilder() {
        findAndHookMethod(AudioAttributes.Builder.class, "build", new XC_MethodHook() {
            @SuppressLint("DefaultLocale")
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String packageName = AndroidAppHelper.currentPackageName();
                Rule rule = getRuleForPackage(packageName);
                int stream;
                if (null != rule && (stream = rule.getStream()) != -1) {
                    log(String.format("AudioAttributes.Builder.build: Rule found for package [%s]", packageName));
                    AudioAttributes.Builder builder = (AudioAttributes.Builder) param.thisObject;
                    builder.setLegacyStreamType(stream);
                    log(String.format("AudioAttributes.Builder.build: LegacyStreamType set to [%d]", stream));
                }
            }
        });
    }

    private Rule getRuleForPackage(String packageName) {
        mPrefs.reload();
        String json = mPrefs.getString("rules", "");
        List<Rule> rules = mGson.fromJson(json, new TypeToken<List<Rule>>() {
        }.getType());
        if (null != rules)
            for (Rule rule : rules) {
                if (TextUtils.equals(packageName, rule.getPackageName()))
                    return rule;
            }
        return null;
    }


    public static void loadPrefs() {
        mPrefs = new XSharedPreferences(PACKAGE_NAME, PREFERENCES);
        mPrefs.makeWorldReadable();
    }
}
