/*
 * 
	MyRepeater Finder 
	Copyright 2013 Mohammad Hafiz bin Ismail <mypapit@gmail.com>
	http://blog.mypapit.net/
	http://repeater-my.googlecode.com/

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


import java.util.HashMap;
import java.util.Iterator;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.content.Intent;
import android.os.Bundle;

import android.support.v4.app.FragmentActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class DisplayMap extends FragmentActivity implements OnInfoWindowClickListener {

	private GoogleMap map;
	HashMap<Marker, Integer> hashMap = new HashMap<Marker, Integer>();
	RepeaterList rl = new RepeaterList();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_map);

		overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();

		if (map == null) {

			Log.e("Map NULL", "MAP NULL");

		} else {

			LatLng latlng = (LatLng) getIntent().getExtras().get("LatLong");

			Repeater xlocation = new Repeater("", new Double[] { latlng.latitude, latlng.longitude });

			rl = RepeaterListActivity.loadData(R.raw.repeaterdata5, this);
			xlocation.calcDistanceAll(rl);
			rl.sort();

			map.setMyLocationEnabled(true);
			map.setOnInfoWindowClickListener(this);

			// counter i, for mapping marker with integer
			int i;
			i = 0;

			Iterator<Repeater> iter = rl.iterator();
			while (iter.hasNext()) {
				Repeater repeater = iter.next();
				MarkerOptions marking = new MarkerOptions();
				marking.position(new LatLng(repeater.getLatitude(), repeater.getLongitude()));
				marking.title(repeater.getCallsign() + " - " + repeater.getDownlink() + "MHz (" + repeater.getClub()
						+ ")");

				marking.snippet("Tone: " + repeater.getTone() + " Shift: " + repeater.getShift());

				hashMap.put(map.addMarker(marking), Integer.valueOf(i));

				i++;

			}

			// Marker RKG = map.addMarker(new MarkerOptions().position(new
			// LatLng(6.1,100.3)).title("9M4RKG"));

			map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 10));
			map.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.display_map, menu);
		return true;
	}

	@Override
	public void onInfoWindowClick(Marker marker) {

		Integer integer = hashMap.get(marker);

		Repeater rpt = rl.get(integer.intValue());

		Intent intent = new Intent();
		intent.setClassName(getBaseContext(), "net.mypapit.mobile.myrepeater.RepeaterDetailsActivity");
		intent.putExtra("Repeater", rpt.toArrayString());
		startActivity(intent);

	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:

			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
				//NavUtils.navigateUpFromSameTask(this);
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
