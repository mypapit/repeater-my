/*
 * 
 * This is the file for displaying Most Recently Repeater Search History 
 * This application requires (and has been compiled with) Google Play Service rev. 13
 *  
	MyRepeater Finder 
	Copyright 2014 Mohammad Hafiz bin Ismail <mypapit@gmail.com>
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

/**
 * 
 */
package net.mypapit.mobile.myrepeater;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

/**
 * @author mypapit
 * 
 */
public class RepeaterHistoryActivity extends Activity {
	ListView lv;
	TextView tvAddress;
	RepeaterAdapter adapter;
	StackHistory stackhistory;

	/**
	 * 
	 */
	public RepeaterHistoryActivity() {
		// TODO Auto-generated constructor stub
	}

	@SuppressWarnings("unchecked")
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.repeater_list);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		Intent intent = this.getIntent();
		ArrayList<Repeater> al = (ArrayList<Repeater>) intent.getExtras().get("historylist");

		double lat = intent.getExtras().getDouble("lat");
		double lon = intent.getExtras().getDouble("lon");

		if (al == null) {
			al = new ArrayList<Repeater>();
			Log.d("net.mypapit.mobile", "null Array List");
		}

		// RepeaterList rl = new RepeaterList();

		try {
			File infile = new File(Environment.getExternalStorageDirectory(), "history.txt");
			FileInputStream fis = new FileInputStream(infile);

			ObjectInputStream ois = new ObjectInputStream(fis);

			stackhistory = (StackHistory) ois.readObject();
			ois.close();
		} catch (IOException ioex) {
			stackhistory = new StackHistory();
			Log.d("net.mypapit.mobile", ioex.getMessage());
			ioex.printStackTrace(System.err);

		} catch (ClassNotFoundException cnfe) {
			stackhistory = new StackHistory();
			Log.d("net.mypapit.mobile", cnfe.getMessage());
			cnfe.printStackTrace(System.err);
		}
		RepeaterList rl = new RepeaterList(stackhistory);
		// rl.add(new Repeater("sux", "sux", "sux", "sux", "sux", 0, 0, 0, 0,
		// 0));
		Repeater repeater = new Repeater("unknown", lat, lon);
		repeater.calcDistanceAll(rl);
		// rl.sort();

		lv = (ListView) findViewById(R.id.repeaterListView);
		tvAddress = (TextView) findViewById(R.id.tvAddress);
		tvAddress.setText("Recently searched repeater");

		adapter = new RepeaterAdapter(this, rl);

		lv.setAdapter(adapter);

	}

	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case android.R.id.home:
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
				NavUtils.navigateUpFromSameTask(this);
			}

			return true;
		}
		return false;
	}

}
