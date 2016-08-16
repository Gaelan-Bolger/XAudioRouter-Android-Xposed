package xposed.audiorouter;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import xposed.audiorouter.utils.PackageUtils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String LAUNCHER_ALIAS = "xposed.audiorouter.Launcher";
    private static final String PREF_HIDE_ICON = "hide_icon";

    private Gson mGson = new Gson();
    private SharedPreferences mPrefs;
    private MediaPlayer mMediaPlayer;
    private ApplicationRuleAdapter mAdapter;
    private List<Rule> mRulesList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = getSharedPreferences(Xposed.PREFERENCES, MODE_WORLD_READABLE);

        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setEmptyView(findViewById(android.R.id.empty));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Log.d(TAG, "onItemClick: ");
                showStreamSelectDialog(mAdapter.getItem(position));
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
                Log.d(TAG, "onItemLongClick: " + position);
                deleteRule(mAdapter.getItem(position));
                return true;
            }
        });
        listView.setAdapter(mAdapter = new ApplicationRuleAdapter(this));

        findViewById(R.id.fab).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRules();
    }

    @Override
    protected void onPause() {
        super.onPause();
        persistRules();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mMediaPlayer) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        menu.findItem(R.id.item_app_version)
                .setTitle(getString(R.string.app_version, AudioRouter.getAppVersion()));
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.item_play_test).setIcon(isMediaPlaying() ? R.drawable.ic_stop_white_24dp : R.drawable.ic_play_arrow_white_24dp);
        menu.findItem(R.id.item_hide_icon).setChecked(mPrefs.getBoolean(PREF_HIDE_ICON, false));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_play_test:
                playTestSound();
                return true;
            case R.id.item_hide_icon:
                boolean hideIcon = mPrefs.getBoolean(PREF_HIDE_ICON, false);
                setLauncherAliasEnabled(hideIcon);
                mPrefs.edit().putBoolean(PREF_HIDE_ICON, !hideIcon).apply();
                invalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab:
                showPackageSelectDialog();
                break;
        }
    }

    private void loadRules() {
        String json = mPrefs.getString("rules", "");
        mRulesList = mGson.fromJson(json, new TypeToken<List<Rule>>() {
        }.getType());
        mAdapter.notifyDataSetChanged();
    }

    private void persistRules() {
        String json = mGson.toJson(mRulesList);
        mPrefs.edit().putString("rules", json).apply();
    }

    private void addRule(Rule rule) {
        if (null == mRulesList)
            mRulesList = new ArrayList<>();
        mRulesList.add(rule);
        mAdapter.notifyDataSetChanged();
    }

    private void deleteRule(Rule rule) {
        mRulesList.remove(rule);
        mAdapter.notifyDataSetChanged();
    }

    private void playTestSound() {
        if (isMediaPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            invalidateOptionsMenu();
        } else {
            try {
                AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.test);
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.start();
                        invalidateOptionsMenu();
                    }
                });
                mMediaPlayer.prepareAsync();
            } catch (IOException e) {
                Log.e(TAG, "onCreate: Error preparing MediaPlayer, " + e.getMessage());
            }
        }
    }

    private boolean isMediaPlaying() {
        return null != mMediaPlayer && mMediaPlayer.isPlaying();
    }

    private void showPackageSelectDialog() {
        PackageSelectDialog.newInstance(new PackageSelectDialog.OnPackageSelectedListener() {
            @Override
            public void onPackageSelected(ResolveInfo resolveInfo) {
                addRule(new Rule(resolveInfo.activityInfo.packageName));
            }
        }).show(getSupportFragmentManager(), PackageSelectDialog.TAG);
    }

    private void showStreamSelectDialog(final Rule rule) {
        final int[] streamOptionValues = getResources().getIntArray(R.array.stream_option_values);
        int selection = 0;
        for (int i = 0; i < streamOptionValues.length; i++) {
            if (rule.getStream() == streamOptionValues[i]) {
                selection = i;
                break;
            }
        }
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(PackageUtils.getLabelForPackage(getPackageManager(), rule.getPackageName()))
                .setSingleChoiceItems(R.array.stream_options, selection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                        rule.setStream(streamOptionValues[which]);
                        persistRules();
                        mAdapter.notifyDataSetChanged();
                    }
                }).create().show();
    }

    private void setLauncherAliasEnabled(boolean enabled) {
        int mode = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        getPackageManager().setComponentEnabledSetting(new ComponentName(this, LAUNCHER_ALIAS),
                mode, PackageManager.DONT_KILL_APP);
    }

    private class ApplicationRuleAdapter extends BaseAdapter {

        private final Context context;
        private final PackageManager packageManager;
        private final String[] streamOptions;
        private final int[] streamOptionValues;

        public ApplicationRuleAdapter(Context context) {
            this.context = context;
            this.packageManager = context.getPackageManager();
            this.streamOptions = context.getResources().getStringArray(R.array.stream_options);
            this.streamOptionValues = context.getResources().getIntArray(R.array.stream_option_values);
        }

        @Override
        public int getCount() {
            return null != mRulesList ? mRulesList.size() : 0;
        }

        @Override
        public Rule getItem(int position) {
            return mRulesList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder h;
            if (null == convertView) {
                convertView = LayoutInflater.from(context).inflate(R.layout.application_rule_list_item, parent, false);
                h = new Holder(convertView);
            } else {
                h = (Holder) convertView.getTag();
            }
            Rule rule = getItem(position);
            final String packageName = rule.getPackageName();
            int stream = rule.getStream();
            try {
                ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
                h.icon.setImageDrawable(packageManager.getApplicationIcon(info));
                h.text1.setText(packageManager.getApplicationLabel(info));
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(TAG, "getView: Error loading application icon, " + e.getMessage());
                h.icon.setImageResource(R.drawable.ic_default_package);
                h.text1.setText(packageName);
            }
            h.icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        Intent intent = packageManager.getLaunchIntentForPackage(packageName);
                        context.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "getView: Error starting activity for package, " + e.getMessage());
                    }
                }
            });
            for (int i = 0; i < streamOptionValues.length; i++) {
                if (streamOptionValues[i] == stream) {
                    h.text2.setText(String.format("Stream: %s", streamOptions[i]));
                    break;
                }
            }
            return convertView;
        }

        private class Holder {

            private final ImageView icon;
            private final TextView text1;
            private final TextView text2;

            public Holder(View view) {
                view.setTag(this);
                icon = (ImageView) view.findViewById(android.R.id.icon);
                text1 = (TextView) view.findViewById(android.R.id.text1);
                text2 = (TextView) view.findViewById(android.R.id.text2);
            }
        }
    }
}
