/*
 * 
	MyRepeater Finder 
	Copyright 2013 Mohammad Hafiz bin Ismail <mypapit@gmail.com>
	http://blog.mypapit.net/
	https://github.com/mypapit/repeater-my

	This file is part of MyRepeater Finder.

    MyRepeater Finder is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    MyRepeater Finder is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MyRepeater Finder.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.mypapit.mobile.myrepeater;

import java.util.List;
import java.util.Stack;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SuggestRepeaterSecondActivity extends Activity {

	Bundle extras;
	TextView labelCallsign;
	String callsign, club, location, state;
	EditText tvSShift, tvSFreq, tvSTone;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.suggest_repeater_second_layout);
		overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);

		extras = getIntent().getExtras();
		callsign = extras.getString("callsign").toUpperCase();
		club = extras.getString("club");
		location = extras.getString("location");
		state = extras.getString("state");

		tvSFreq = (EditText) findViewById(R.id.tvSFrequency);
		tvSShift = (EditText) findViewById(R.id.tvSShift);
		tvSTone = (EditText) findViewById(R.id.tvSTone);

		tvSShift.setText("-0.600");
		if (club.equalsIgnoreCase("ASTRA")) {
			tvSTone.setText("103.5");

		} else if (club.equalsIgnoreCase("MARTS")) {
			tvSTone.setText("203.5");

		}

		labelCallsign = (TextView) findViewById(R.id.tvLabelCallsign);

		labelCallsign.setText(callsign);

	}

	public boolean onOptionsItemSelected(MenuItem item) {

		// Intent intent = new Intent();
		switch (item.getItemId()) {
		case R.id.action_suggest_done:

			if (tvSFreq.getText().toString().length() < 4) {
				Toast.makeText(this, "Please fill in frequency", Toast.LENGTH_SHORT).show();
				return false;
			} else if (tvSShift.getText().toString().length() < 3) {
				Toast.makeText(this, "Please fill in repeater shift", Toast.LENGTH_SHORT).show();
				return false;
			} else if (tvSTone.getText().toString().length() < 3) {
				Toast.makeText(this, "Please fill in repeater tone", Toast.LENGTH_SHORT).show();
				return false;
			}

			Intent emailIntent = new Intent(Intent.ACTION_SEND);

			emailIntent.setType("text/plain");
			emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "mypapit+new_repeater_suggest@gmail.com" });

			emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Repeater.MY - new Repeater");

			String versionName = "Unknown";
			try {
				// workaround to identify Repeater.MY version in email reports
				versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;

			} catch (NameNotFoundException nnfe) {

			}
			emailIntent.putExtra(Intent.EXTRA_TEXT, "Please put the repeater details you want to suggest --"
					+ "\n\nRepeater Callsign : "
					+ callsign
					+ "\nFreq: "
					+ tvSFreq.getText().toString()
					+ "\nShift: "
					+ tvSShift.getText().toString()
					+ "\nTone: "
					+ tvSTone.getText().toString()
					+ "\nLocation: "
					+ location
					+ "\nOwner or Club: "
					+ club
					+ "\n"
					+ "State: "
					+ state
					+ "\n\n"
					+ "Sent from: "
					+ Build.MANUFACTURER
					+ " "
					+ Build.BRAND
					+ " "
					+ " "
					+ Build.PRODUCT
					+ " "
					+ Build.MODEL
					+ "\nRepeater.MY version: " + versionName);

			startActivity(createEmailOnlyChooserIntent(emailIntent, "Suggest new Repeater"));

		}
		return false;
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_suggest, menu);

		return true;
	}

	public Intent createEmailOnlyChooserIntent(Intent source, CharSequence chooserTitle) {
		Stack<Intent> intents = new Stack<Intent>();
		Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "mypapit+new_repeater_suggest@gmail.com",
				null));
		List<ResolveInfo> activities = getPackageManager().queryIntentActivities(i, 0);

		for (ResolveInfo ri : activities) {
			Intent target = new Intent(source);
			target.setPackage(ri.activityInfo.packageName);
			intents.add(target);
		}

		if (!intents.isEmpty()) {
			Intent chooserIntent = Intent.createChooser(intents.remove(0), chooserTitle);
			chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new Parcelable[intents.size()]));

			return chooserIntent;
		} else {
			return Intent.createChooser(source, chooserTitle);
		}
	}

	protected void onPause() {
		super.onPause();

		overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);

	}

}
