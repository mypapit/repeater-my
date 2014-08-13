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

import java.util.Iterator;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.os.Bundle;


import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;





public class DisplayMap extends FragmentActivity implements OnMarkerClickListener {

	private GoogleMap map;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_map);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();


		if (map == null){

			Log.e("Map NULL","MAP NULL");


		} else {

			LatLng latlng = (LatLng) getIntent().getExtras().get("LatLong");

			Repeater xlocation = new Repeater("", new Double[]{latlng.latitude,latlng.longitude});

			RepeaterList rl = new RepeaterList();
			rl=RepeaterListActivity.loadData(R.raw.repeaterdata5, this);
			xlocation.calcDistanceAll(rl);
			rl.sort();

			map.setMyLocationEnabled(true);
			Iterator<Repeater> iter=rl.iterator();

			int i=0;
			Marker marker[] = new Marker[200];


			while (iter.hasNext()){
				Repeater repeater = iter.next();
				MarkerOptions marking= new MarkerOptions();
				marking.position(new LatLng(repeater.getLatitude(),repeater.getLongitude()));
				marking.title(repeater.getCallsign()+" - " + repeater.getDownlink() + "MHz ("+repeater.getClub()+")");

				marking.snippet("Tone: " + repeater.getTone()+" Shift: " + repeater.getShift() );



				marker[i] = map.addMarker(marking);

				i++;

			}

			//Marker RKG = map.addMarker(new MarkerOptions().position(new LatLng(6.1,100.3)).title("9M4RKG"));

			map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 10));
			map.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.display_map, menu);
		return true;
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		if (marker.equals(marker)) {


		}

		return false;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
	
		

		}
		return false;
		
	}
		
	

}
