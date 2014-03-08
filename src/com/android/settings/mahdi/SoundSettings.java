/*
 * Copyright (C) 2014 The Mahdi-Rom Project
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

import java.util.prefs.PreferenceChangeListener;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.media.AudioSystem;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.VolumePanel;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

public class SoundSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "SoundSettings";

    private static final String CATEGORY_VOLUME = "button_volume_keys";
    private static final String BUTTON_VOLUME_DEFAULT = "button_volume_default_screen";

    private ListPreference mVolumeDefault;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.mahdi_sound_settings);

        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        final Resources res = getResources();

        final PreferenceCategory volumeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_VOLUME);

        if (hasVolumeRocker()) {
            mVolumeDefault = (ListPreference) findPreference(BUTTON_VOLUME_DEFAULT);
            String currentDefault = Settings.System.getString(resolver, Settings.System.VOLUME_KEYS_DEFAULT);
            if (!Utils.isVoiceCapable(getActivity())) {
                removeListEntry(mVolumeDefault, String.valueOf(AudioSystem.STREAM_RING));
            }
            if (currentDefault == null) {
                currentDefault = mVolumeDefault.getEntryValues()[mVolumeDefault.getEntryValues().length - 1].toString();
            }
            mVolumeDefault.setValue(currentDefault);
            mVolumeDefault.setOnPreferenceChangeListener(this);
        } else {
            prefScreen.removePreference(volumeCategory);
        }
    }

    private boolean hasVolumeRocker() {
        return getActivity().getResources().getBoolean(R.bool.config_has_volume_rocker);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVolumeDefault) {
            String value = (String)newValue;
            Settings.System.putString(getActivity().getContentResolver(), Settings.System.VOLUME_KEYS_DEFAULT, value);
            return true;
        }
        return false;
    }

    public void removeListEntry(ListPreference list, String valuetoRemove) {
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();

        for (int i = 0; i < list.getEntryValues().length; i++) {
            if (list.getEntryValues()[i].toString().equals(valuetoRemove)) {
                continue;
            } else {
                entries.add(list.getEntries()[i]);
                values.add(list.getEntryValues()[i]);
            }
        }

        list.setEntries(entries.toArray(new CharSequence[entries.size()]));
        list.setEntryValues(values.toArray(new CharSequence[values.size()]));
    }
}
