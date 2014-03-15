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

import android.app.ListActivity;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.android.settings.mahdi.backup.BackupService;

/**
* Activity that shows a list of all apps and backups, which allows backup,
* delete and restore operations.
*/
public class ManageBackupsActivity extends ListActivity {

    private static final String TAG = "ManageBackupsActivity";

    private BackupService mBackupService;

    private BackupsAdapter mBackupsAdapter = new BackupsAdapter(this);

    private ServiceConnection mBackupServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            mBackupService = ((BackupService.BackupServiceBinder) service).getService();
            mBackupService.listBackups(null, mBackupsAdapter);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBackupService = null;
        }
    };

    class CreateBackupObserver implements BackupService.CreateBackupObserver {
        @Override
        public void onCreateBackupCompleted() {
            mBackupService.listBackups(null, mBackupsAdapter);
            uncheckAll();
        }
    }

    class RestoreBackupObserver implements BackupService.RestoreBackupObserver {
        @Override
        public void onRestoreBackupCompleted() {
            uncheckAll();
        }
    }

    class DeleteBackupObserver implements BackupService.DeleteBackupObserver {
        @Override
        public void onDeleteBackupCompleted() {
            mBackupService.listBackups(null, mBackupsAdapter);
            uncheckAll();
        }
    }

    /**
    * Exits if the current user is not the device owner.
    */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
            Log.w(TAG, "Finishing activity as user is not device owner.");
            finish();
            return;
        }
        bindService(new Intent(this, BackupService.class),
                mBackupServiceConnection, Context.BIND_AUTO_CREATE);
        getListView().setAdapter(mBackupsAdapter);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mBackupServiceConnection);
    }

    /**
    * Returns a list containing the package names of all apps that have their
    * checkbox checked.
    */
    private ArrayList<String> getCheckedPackageNames() {
        ArrayList<String> packageNames = new ArrayList<String>();
        for (int i = 0; i < mBackupsAdapter.getCount(); i++) {
            if (getListView().isItemChecked(i) &&
                    mBackupsAdapter.getItemViewType(i) == BackupsAdapter.VIEW_TYPE_APP) {
                BackupsAdapter.App item = (BackupsAdapter.App) mBackupsAdapter.getItem(i);
                packageNames.add(item.packageName);
            }
        }
        return packageNames;
    }

    /**
    * Returns a list containing the backups that have their checkbox checked.
    *
    * @param multi If true, multiple backups for a single app may be returned.
    *              Otherwise, only the newest backup is returned for each app.
    */
    private ArrayList<Backup> getCheckedBackups(boolean multi) {
        ArrayList<Backup> backups = new ArrayList<Backup>();
        for (int i = 0; i < mBackupsAdapter.getCount(); i++) {
            if (getListView().isItemChecked(i) &&
                    mBackupsAdapter.getItemViewType(i) == BackupsAdapter.VIEW_TYPE_BACKUP) {
                Backup item = (Backup) mBackupsAdapter.getItem(i);
                if (!multi) {
                    for (Backup b : backups) {
                        if (b.packageName.equals(item.packageName)) {
                            continue;
                        }
                    }
                }
                backups.add(item);
            }
        }
        return backups;
    }

    /**
    * Unchecks all items in the ListView.
    */
    private void uncheckAll() {
        for (int i = 0; i < mBackupsAdapter.getCount(); i++) {
            getListView().setItemChecked(i, false);
        }
    }

    /**
    * Inflates options menu from manage_backups_menu.xml.
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manage_backups_menu, menu);
        return true;
    }

    /**
    * Performs the corresponding action for each menu item, eg backup app
    * or restore/delete backup.
    */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
        case R.id.backup:
            ArrayList<String> packageNames = getCheckedPackageNames();
            if (packageNames.size() == 0) {
                Toast.makeText(this, R.string.no_apps_selected, Toast.LENGTH_SHORT).show();
                return true;
            }
            mBackupService.createBackup(packageNames.get(0), new CreateBackupObserver());
            return true;
        case R.id.restore:
            ArrayList<Backup> backups = getCheckedBackups(false);
            if (backups.size() == 0) {
                Toast.makeText(this, R.string.no_backups_selected, Toast.LENGTH_SHORT).show();
                return true;
            }
            mBackupService.restoreBackup(backups.get(0), null);
            return true;
        case R.id.delete:
            backups = getCheckedBackups(true);
            if (backups.size() == 0) {
                Toast.makeText(this, R.string.no_backups_selected, Toast.LENGTH_SHORT).show();
                return true;
            }
            mBackupService.deleteBackup(backups.get(0), new DeleteBackupObserver());
            return true;
        default:
            return false;
        }
    }

}
