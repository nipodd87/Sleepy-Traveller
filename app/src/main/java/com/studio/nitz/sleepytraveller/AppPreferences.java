package com.studio.nitz.sleepytraveller;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by nitinpoddar on 5/1/16.
 */
public class AppPreferences extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
