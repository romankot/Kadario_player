package com.kremor.karadio.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class MyPreferencesActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener{

    public static final String STATION_LIST_PATH = "stationListPath";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.StationListPath))) {
            Preference preference = findPreference(key);
            preference.setSummary(sharedPreferences.getString(key, ""));
        } else if (key.equals("IP")) {
            Preference preference = findPreference(key);
            preference.setSummary(sharedPreferences.getString(key, ""));
        }
    }


    public static class MyPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener
    {
        private static final int FILE_REQUEST_CODE = 1;

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            Preference filePicker = findPreference(getText(R.string.StationListPath));
            filePicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("text/*");
                    startActivityForResult(intent, FILE_REQUEST_CODE);
                    return false;
                }
            });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            String filepath = data.getDataString();
            PreferenceManager.getDefaultSharedPreferences(this.getActivity())
                .edit()
                .putString((String) getText(R.string.StationListPath), filepath)
                .apply();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(getString(R.string.StationListPath))) {
                Preference preference = findPreference(key);
                preference.setSummary(sharedPreferences.getString(key, ""));
            } else if (key.equals("IP")) {
                Preference preference = findPreference(key);
                preference.setSummary(sharedPreferences.getString(key, ""));
            }
        }
    }

}