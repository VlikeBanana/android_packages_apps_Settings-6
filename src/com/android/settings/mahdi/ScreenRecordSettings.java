/*
 * Copyright (C) 2014 The Dirty Unicorns Project
 * Modified for us by 2014 - The Schism Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.mahdi;

import android.app.ActivityManager;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SeekBarPreference;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class ScreenRecordSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String SREC_ENABLE_TOUCHES = "srec_enable_touches";
    private static final String SREC_ENABLE_MIC = "srec_enable_mic";

    private CheckBoxPreference mSrecEnableTouches;
    private CheckBoxPreference mSrecEnableMic;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.screen_record_settings);

        final ContentResolver resolver = getActivity().getContentResolver();

        mSrecEnableTouches = (CheckBoxPreference) findPreference(SREC_ENABLE_TOUCHES);
        mSrecEnableTouches.setChecked((Settings.System.getInt(resolver,
                Settings.System.SREC_ENABLE_TOUCHES, 0) == 1));

        mSrecEnableMic = (CheckBoxPreference) findPreference(SREC_ENABLE_MIC);
        mSrecEnableMic.setChecked((Settings.System.getInt(resolver,
                Settings.System.SREC_ENABLE_MIC, 0) == 1));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if  (preference == mSrecEnableTouches) {
            boolean checked = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SREC_ENABLE_TOUCHES, checked ? 1:0);
            return true;
        } else if  (preference == mSrecEnableMic) {
            boolean checked = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SREC_ENABLE_MIC, checked ? 1:0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
         return true;
    }
}