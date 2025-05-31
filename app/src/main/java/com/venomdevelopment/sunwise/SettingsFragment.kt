package com.venomdevelopment.sunwise;

import static com.venomdevelopment.sunwise.HomeFragment.myPref;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragmentCompat {
    EditTextPreference addressPreference;
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        EditTextPreference addressPreference = findPreference("address");
    }
    public String getPreferenceValue(String key)
    {
        SharedPreferences sp = getActivity().getSharedPreferences(myPref,0);
        String str = sp.getString(key,"");
        return str;
    }
    public void writeToPreference(String thePreference, String key)
    {
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(myPref,0).edit();
        editor.putString(key, thePreference);
        editor.commit();
    }

}