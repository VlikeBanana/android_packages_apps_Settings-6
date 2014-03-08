/*
 *  Copyright (C) 2013 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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

package com.android.settings.mahdi;

import java.util.prefs.PreferenceChangeListener;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

public class ButtonSettings extends SettingsPreferenceFragment {

    private static final String TAG = "ButtonSettings";

    private static final String KEY_HARDWARE_KEYS_CATEGORY = "hardware_keys_category";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";    

    private PreferenceScreen mHardwareKeys;    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.button_settings);

        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        final Resources res = getResources();		

        // Only show the hardware keys config on a device that does not have a navbar
        mHardwareKeys = (PreferenceScreen) findPreference(KEY_HARDWARE_KEYS);
        if (mHardwareKeys != null) {
            if (!res.getBoolean(R.bool.config_has_hardware_buttons)) {
                getPreferenceScreen().removePreference(mHardwareKeys);
                getPreferenceScreen().removePreference((PreferenceCategory) findPreference(KEY_HARDWARE_KEYS_CATEGORY));
            }        
        }
    }
}
