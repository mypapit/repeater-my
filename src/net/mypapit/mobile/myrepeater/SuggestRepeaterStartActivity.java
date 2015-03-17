/*
 * 
	MyRepeater Finder 
	Copyright 2014 Mohammad Hafiz bin Ismail <mypapit@gmail.com>
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import android.widget.Toast;

public class SuggestRepeaterStartActivity extends Activity {
	String club[] = { "ASTRA", "AKRAB", "ARECTMJ", "MARTS", "MARES", "JASRA", "ARCS", "NESRAC", "PEMANCAR", "PERAMAH",
			"ARECS","SARES", "UNKNOWN", "OTHERS","SARC" };
	String state[] = { "JOHOR", "KEDAH", "KELANTAN", "LABUAN", "KUALA LUMPUR", "MELAKA", "NEGERI SEMBILAN", "PAHANG",
			"PULAU PINANG", "PENANG", "PERLIS", "SELANGOR", "SABAH", "SARAWAK", "TERENGGANU", "PERAK" };
	String callsign[] = { "9M4R", "9M2R", "9M2L", "9M4L", "9M4C", "9M2C", "9M6R", "9M6","9M4C","9M8","9M6" };
	String location[] = { "BUKIT ", "GUNUNG ", "KUALA ", "TELUK ", "HOTEL " };

	private ArrayAdapter<String> adapterClub, adapterState, adapterCallsign, adapterLocation;

	AutoCompleteTextView tvClub, tvState, tvSCallsign, tvSLocation;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.suggest_repeater_start_layout);
		overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);

		tvSCallsign = (AutoCompleteTextView) findViewById(R.id.tvSCallsign);
		tvSLocation = (AutoCompleteTextView) findViewById(R.id.tvSLocation);

		tvClub = (AutoCompleteTextView) findViewById(R.id.tvSClub);
		tvState = (AutoCompleteTextView) findViewById(R.id.tvSState);

		adapterClub = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, club);
		adapterState = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, state);
		adapterCallsign = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, callsign);
		adapterLocation = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, location);

		tvClub.setAdapter(adapterClub);
		tvState.setAdapter(adapterState);
		tvSCallsign.setAdapter(adapterCallsign);
		tvSLocation.setAdapter(adapterLocation);
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}


	}

	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_suggest, menu);

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent = new Intent();
		switch (item.getItemId()) {
		case R.id.action_suggest_done:
			if (tvSCallsign.getText().toString().length() < 5) {
				Toast.makeText(this, "Please fill in Repeater callsign", Toast.LENGTH_SHORT).show();
				return false;
			} else if (tvClub.getText().toString().length() < 3) {
				Toast.makeText(this, "Please fill in Club", Toast.LENGTH_SHORT).show();
				return false;
			} else if (tvState.getText().toString().length() < 3) {
				Toast.makeText(this, "Please fill in State", Toast.LENGTH_SHORT).show();
				return false;
			} else if (tvSLocation.getText().toString().length() < 3) {
				Toast.makeText(this, "Please fill in Location or GPS coordinate", Toast.LENGTH_SHORT).show();
				return false;
			}

			intent.setClassName(getBaseContext(), "net.mypapit.mobile.myrepeater.SuggestRepeaterSecondActivity");
			intent.putExtra("callsign", tvSCallsign.getText().toString());
			intent.putExtra("club", tvClub.getText().toString());
			intent.putExtra("location", tvSLocation.getText().toString());
			intent.putExtra("state", tvState.getText().toString());
			startActivity(intent);

			return true;
		case android.R.id.home:
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
				
				finish();
			}
			return true;
		}
		return false;
	}

	protected void onPause() {
		super.onPause();

		overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);

	}

}
