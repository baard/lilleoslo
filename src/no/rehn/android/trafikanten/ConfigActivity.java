package no.rehn.android.trafikanten;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.text.method.DigitsKeyListener;

//TODO implement
public class ConfigActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferences);
		EditTextPreference proxyPort = (EditTextPreference) findPreference("proxy_port");
		proxyPort.getEditText().setKeyListener(new DigitsKeyListener());
		EditTextPreference myLat = (EditTextPreference) findPreference("my_lat");
		myLat.getEditText().setKeyListener(new DigitsKeyListener(false, true));
		EditTextPreference myLon = (EditTextPreference) findPreference("my_lon");
		myLon.getEditText().setKeyListener(new DigitsKeyListener(false, true));
		EditTextPreference time = (EditTextPreference) findPreference("time");
		time.getEditText().setKeyListener(new DigitsKeyListener());
		
	}
}
