package xposed.audiorouter;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

public class PackageSelectDialog extends DialogFragment {

    interface OnPackageSelectedListener {
        void onPackageSelected(ResolveInfo resolveInfo);
    }

    public static final String TAG = PackageSelectDialog.class.getSimpleName();

    private OnPackageSelectedListener mListener;
    private PackageAdapter mAdapter;

    public static PackageSelectDialog newInstance(OnPackageSelectedListener onPackageSelectedListener) {
        PackageSelectDialog dialog = new PackageSelectDialog();
        dialog.setOnPackageSelectedListener(onPackageSelectedListener);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new PackageAdapter(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
        new LoadPackagesTask(mAdapter).execute(getActivity().getPackageManager());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.dialog_package_select, null);
        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setEmptyView(view.findViewById(android.R.id.empty));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (null != mListener)
                    mListener.onPackageSelected(mAdapter.getItem(position));
            }
        });
        listView.setAdapter(mAdapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(true)
                .setTitle("Select application")
                .setView(view);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);
        return dialog;
    }

    private void setOnPackageSelectedListener(OnPackageSelectedListener onPackageSelectedListener) {
        mListener = onPackageSelectedListener;
    }

    class LoadPackagesTask extends AsyncTask<PackageManager, Void, List<ResolveInfo>> {

        private final WeakReference<PackageAdapter> mRef;

        public LoadPackagesTask(PackageAdapter packageAdapter) {
            mRef = new WeakReference<>(packageAdapter);
        }

        @Override
        protected List<ResolveInfo> doInBackground(PackageManager... params) {
            return loadPackages(params[0]);
        }

        @Override
        protected void onPostExecute(List<ResolveInfo> resolveInfos) {
            PackageAdapter adapter = mRef.get();
            if (null != adapter)
                adapter.setPackages(resolveInfos);
        }

        private List<ResolveInfo> loadPackages(PackageManager packageManager) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> packages = packageManager.queryIntentActivities(intent, 0);
            Collections.sort(packages, new ResolveInfo.DisplayNameComparator(packageManager));
            return packages;
        }
    }

    private class PackageAdapter extends BaseAdapter {

        private final Context context;
        private final PackageManager packageManager;
        private List<ResolveInfo> packages;

        public PackageAdapter(Context context) {
            this.context = context;
            this.packageManager = context.getPackageManager();
        }

        @Override
        public int getCount() {
            return null != packages ? packages.size() : 0;
        }

        @Override
        public ResolveInfo getItem(int position) {
            return packages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder h;
            if (null == convertView) {
                convertView = LayoutInflater.from(context).inflate(R.layout.package_list_item, parent, false);
                h = new Holder(convertView);
            } else {
                h = (Holder) convertView.getTag();
            }
            ResolveInfo info = getItem(position);
            h.icon.setImageDrawable(info.loadIcon(packageManager));
            h.text1.setText(info.loadLabel(packageManager));
            h.text2.setText(info.activityInfo.packageName);
            return convertView;
        }

        public void setPackages(List<ResolveInfo> packages) {
            this.packages = packages;
            notifyDataSetChanged();
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
