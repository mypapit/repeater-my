package net.mypapit.mobile.myrepeater;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
	
	public void onCreate(Bundle bundle){
		super.onCreate(bundle);
		addPreferencesFromResource(R.xml.repeater_settings);
		
		overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
		
	}
	
	protected void onPause() {
		super.onPause();
		// this.saveHistory();

		overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);

	}
	
	
}