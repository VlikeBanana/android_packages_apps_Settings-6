/*
*  Copyright (C) 2013 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
*/

package com.android.settings.mahdi.backup;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;

import com.android.settings.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.android.settings.mahdi.backup.Backup;
import com.android.settings.mahdi.backup.BackupService;

/**
* ListAdapter that provides apps and backups so that backups are grouped by app,
* and uninstalled apps are listed seperately.
*/
public class BackupsAdapter extends BaseAdapter
        implements ListAdapter, BackupService.ListBackupsObserver {

    private static final String TAG = "BackupsAdapter";

    public static final int VIEW_TYPE_APP = 0;

    public static final int VIEW_TYPE_BACKUP = 1;

    private int mDefaultTextColor;

    /**
    * Class representing an app and all its backups.
    *
    * Note: Access by id is O(n), as each element has to be checked for the number
    * of backups. As an improvement, a map from id to App could be used.
    */
    public class App implements Comparable<App> {

        public String label;

        public String packageName;

        public boolean isInstalled;

        public ArrayList<Backup> backups = new ArrayList<Backup>();

        @Override
        public int compareTo(App other) {
            if (other == this)
                return 0;
            else if (this.isInstalled && !other.isInstalled)
                return -1;
            else if (!this.isInstalled && other.isInstalled)
                return 1;
            else
                return this.label.compareTo(other.label);
        }

    }

    private Context mContext;

    private ArrayList<App> mItems = new ArrayList<App>();

    public BackupsAdapter(Context context) {
        mContext = context;
    }

    /**
    * Populates {@link mItems} with all installed apps and their backups, and, for
    * backups of uninstalled apps, these apps with their backups.
    */
    @Override
    public void onListBackupsCompleted(final Map<String, List<Backup>> backups) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                PackageManager pm = mContext.getPackageManager();
                List<PackageInfo> packages = pm.getInstalledPackages(0);
                mItems.clear();

                // Add installed apps and their backups.
                for (PackageInfo p : packages) {
                    App a = new App();
                    a.packageName = p.packageName;
                    a.label = (String) pm.getApplicationLabel(p.applicationInfo);
                    a.isInstalled = true;
                    if (backups.containsKey(p.packageName)) {
                        for (Backup b : backups.get(p.packageName)) {
                            a.backups.add(b);
                        }
                    }
                    mItems.add(a);
                    backups.remove(p.packageName);
                }

                // Add backups for uninstalled apps.
                for (Map.Entry<String, List<Backup>> entry: backups.entrySet()) {
                    App a = new App();
                    a.packageName = entry.getKey();
                    a.label = entry.getValue().get(0).label;
                    a.isInstalled = false;
                    for (Backup b : entry.getValue()) {
                        a.backups.add(b);
                    }
                    mItems.add(a);
                }

                Collections.sort(mItems);
                notifyDataSetChanged();
            }
        }).run();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    /**
    * Returns different values for apps and backups.
    */
    @Override
    public int getItemViewType(int position) {
        return (getItem(position) instanceof App)
                ? VIEW_TYPE_APP
                : VIEW_TYPE_BACKUP;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater)
                    mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            int layout = (getItemViewType(position) == VIEW_TYPE_APP)
                    ? R.layout.app_list_item
                    : R.layout.backup_list_item;
            convertView = inflater.inflate(layout, parent, false);
        }

        CheckedTextView text = (CheckedTextView) convertView;
        text.setTypeface(null, Typeface.NORMAL);
        // TODO: replace with android.R.drawable.btn_radio_holo_dark (gives compile error)
        text.setCheckMarkDrawable(android.R.drawable.btn_radio);
        if (getItemViewType(position) == VIEW_TYPE_APP) {
            App a = (App) getItem(position);
            text.setText(a.label);
            if (!a.isInstalled) {
                text.setTypeface(null, Typeface.ITALIC);
                text.setCheckMarkDrawable(0);
            }
        } else {
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
            Backup b = (Backup) getItem(position);
            text.setText(df.format(b.date) + " - " + b.versionName);
        }
        return convertView;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    /**
     * Returns true for all installed apps and all backups, false for uninstalled apps.
     */
    public boolean isEnabled(int position) {
        if (getItemViewType(position) == VIEW_TYPE_APP) {
            App item = (App) getItem(position);
            return item.isInstalled;
        }
        return true;
    }

    /**
    * Returns the number of apps and backups combined.
    */
    @Override
    public int getCount() {
        int count = 0;
        for (App a : mItems) {
            count += 1 + a.backups.size();
        }
        return count;
    }

    /**
    * Returns the {@link App}or {@link Backup} represented by the specified item.
    */
    @Override
    public Object getItem(int position) {
        int current = 0;
        for (App a : mItems) {
            if (current == position) {
                return a;
            }
            current += 1;
            if (position - current < a.backups.size()) {
                return a.backups.get(position - current);
            }
            current += a.backups.size();
        }
        Log.w(TAG, "No item for position " + Integer.toString(position) + " with size " +
                Integer.toString(getCount()));
        return null;
    }

    /**
    * Returns position again.
    */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
    * IDs are generated by position on the fly, so not stable.
    */
    @Override
    public boolean hasStableIds() {
        return false;
    }

}
