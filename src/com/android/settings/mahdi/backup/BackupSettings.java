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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.database.Cursor;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.DocumentsContract;
import android.util.Log;

import com.android.settings.R;

import com.android.settings.mahdi.backup.BackupService;
import com.android.settings.mahdi.omni.NumberPickerPreference;

/**
 * Settings for OmniBackup.
 */
public class BackupSettings extends PreferenceActivity
        implements OnPreferenceChangeListener {

    private static final String TAG = "SecuritySettings";

    private static final int SELECT_BACKUP_FOLDER_REQUEST_CODE = 126;

    private static final String KEY_BACKUP_LOCATION = "backup_location";
    private static final String KEY_BACKUP_HISTORY = "backup_history";

    private Preference mBackupLocation;
    private NumberPickerPreference mBackupHistory;
    private BackupService mBackupService;

    private ServiceConnection mBackupServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            mBackupService = ((BackupService.BackupServiceBinder) service).getService();
            mBackupLocation.setSummary(getBackupLocationName());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBackupService = null;
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindService(new Intent(this, BackupService.class),
                mBackupServiceConnection, Context.BIND_AUTO_CREATE);

        if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
            finish();
        }

        addPreferencesFromResource(R.xml.backup_settings);

        mBackupHistory = (NumberPickerPreference) findPreference(KEY_BACKUP_HISTORY);
        mBackupHistory.setMinValue(1);
        mBackupHistory.setMaxValue(Integer.MAX_VALUE);
        mBackupHistory.setOnPreferenceChangeListener(this);
        mBackupLocation = findPreference(KEY_BACKUP_LOCATION);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mBackupServiceConnection);
    }

    /**
     * Returns the backup location in the format 'rootname: path'.
     */
    private String getBackupLocationName() {
        Uri folder = mBackupService.getBackupLocation();

        Uri rootsUri = DocumentsContract.buildRootsUri(folder.getAuthority());
        Cursor cursor = getContentResolver().query(rootsUri, null, null, null, null);
        while (cursor.moveToNext()) {
            // HACK: Extract the root ID from the folder Uri by dropping the path and
            // taking the last segment. This might fail if a provider has a different Uri format.
            // "%3A" is urlencode(':')
            String[] split = folder.toString().split("%3A", 2)[0].split("/");
            String rootId = split[split.length - 1];

            String currentId = cursor.getString(cursor.getColumnIndex(
                    DocumentsContract.Root.COLUMN_ROOT_ID));
            if (rootId.equals(currentId)) {
                return cursor.getString(
                        cursor.getColumnIndex(DocumentsContract.Root.COLUMN_TITLE)) +
                        ": " + folder.getPath().split(":")[1];
            }
        }
        return null;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (KEY_BACKUP_LOCATION.equals(preference.getKey())) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(DocumentsContract.Document.MIME_TYPE_DIR);
            startActivityForResult(intent, SELECT_BACKUP_FOLDER_REQUEST_CODE);
            return true;
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_BACKUP_FOLDER_REQUEST_CODE &&
                resultCode == Activity.RESULT_OK && data != null) {
            final Uri uriOld = mBackupService.getBackupLocation();
            final Uri uriNew = data.getData();

            Log.i(TAG, "Setting new backup location: " + uriNew.toString());
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString("backup_location", uriNew.toString())
                    .apply();

            mBackupLocation.setSummary(getBackupLocationName());
            new AlertDialog.Builder(this)
                    .setTitle(R.string.backup_move_new_location)
                    .setPositiveButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mBackupService.moveBackups(uriOld, uriNew);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mBackupHistory &&
                (Integer) value < ((NumberPickerPreference) preference).getValue()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.backup_history_trim)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i(TAG, "Trimming backup history for all packages after " +
                                    "preference change.");
                            mBackupService.listBackups(null,
                                    mBackupService.new TrimBackupHistory());
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
        return true;
    }

}
