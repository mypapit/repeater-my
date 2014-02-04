/*
 * 
 * This is the start-up main file for the application
 * This application requires (and has been compiled with) Google Play Service rev. 13
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Stack;

import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;

public class RepeaterListActivity extends ListActivity {
	Repeater xlocation;
	RepeaterAdapter adapter;
	RepeaterList rl;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		// Repeater repeater = (Repeater)
		// this.getIntent().getExtras().getSerializable("RepeaterList");

		// String coordinates = (String)
		// this.getIntent().getExtras().getSerializable("RepeaterList");
		String coordinates = null;
		if (coordinates == null) {
			coordinates = new String("6.5,100.5");
		}
		String coord[] = coordinates.split(",");

		xlocation = new Repeater("", Double.parseDouble(coord[0]),
				Double.parseDouble(coord[1]));

		rl = RepeaterListActivity.loadData(R.raw.repeaterdata5, this);
		xlocation.calcDistanceAll(rl);
		rl.sort();

		adapter = new RepeaterAdapter(this, rl);

		GPSThread thread = new GPSThread(this);
		thread.start();

		this.setListAdapter(adapter);

	}

	@Override
	protected void onListItemClick(ListView lv, View view, int position, long id) {

		// Toast.makeText(getBaseContext(), rl.get(position).getCallsign() +
		// " clicked!", Toast.LENGTH_SHORT).show();

		Intent intent = new Intent();
		intent.setClassName(getBaseContext(),
				"net.mypapit.mobile.myrepeater.RepeaterDetailsActivity");
		intent.putExtra("Repeater", rl.get(position).toArrayString());
		startActivity(intent);

		super.onListItemClick(lv, view, position, id);

	}

	class GPSThread extends Thread {
		ListActivity activity;
		StringBuffer sb;
		String bestProvider;

		public GPSThread(ListActivity activity) {
			this.activity = activity;

		}

		@Override
		public void run() {

			LocationManager locationManager;
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			criteria.setPowerRequirement(Criteria.POWER_LOW);

			Looper.prepare();

			int distance = 250;

			bestProvider = locationManager.getBestProvider(criteria, false);

			Location location = locationManager
					.getLastKnownLocation(bestProvider);

			if (location == null) {
				location = new Location("");
				location.setLatitude(3.0);
				location.setLongitude(101.5);

			}

			xlocation = new Repeater("", location.getLatitude(),
					location.getLongitude());

			if (xlocation != null) {

			} else {

				xlocation = new Repeater("", 3.0, 101.5);

			}

			activity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					xlocation.calcDistanceAll(rl);
					rl.sort();
					adapter = new RepeaterAdapter(activity, rl);

					activity.setListAdapter(adapter);
					Log.d("latitud papit", "Geo: " + xlocation.getLatitude());
					adapter.notifyDataSetChanged();

				}

			}

			);
			LocationListener myLocationListener = new LocationListener() {

				@Override
				public void onLocationChanged(final Location location) {
					// TODO Auto-generated method stub
					activity.runOnUiThread(new Runnable() {
						final double lat = location.getLatitude();
						final double lon = location.getLongitude();

						@Override
						public void run() {
							// TODO Auto-generated method stub
							xlocation = new Repeater("", lat, lon);
							xlocation.calcDistanceAll(rl);
							rl.sort();
							adapter = new RepeaterAdapter(activity, rl);

							activity.setListAdapter(adapter);
							adapter.notifyDataSetChanged();

							Log.d("latitud papit",
									"Geo: " + xlocation.getLatitude());

						}

					}

					);

				}

				@Override
				public void onProviderDisabled(String provider) {
					// TODO Auto-generated method stub
				}

				@Override
				public void onProviderEnabled(String provider) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onStatusChanged(String provider, int status,
						Bundle extras) {
					// TODO Auto-generated method stub

				}

			};

			locationManager.requestLocationUpdates(bestProvider, 15000,
					distance, myLocationListener);

			Looper.loop();

		}
	}

	public static RepeaterList loadData(int resource, Activity activity) {
		RepeaterList mlist = new RepeaterList(200);
		int line = 0;
		try {

			InputStream stream = activity.getResources().openRawResource(
					resource);
			InputStreamReader is = new InputStreamReader(stream);
			BufferedReader in = new BufferedReader(is);
			CSVReader csv = new CSVReader(in, ';', '\"', 0);
			String data[];

			while ((data = csv.readNext()) != null) {
				line++;
				mlist.add(new Repeater("", data[1], data[2], data[3], data[9],
						Double.parseDouble(data[4]), Double
								.parseDouble(data[5]), Double
								.parseDouble(data[6]), Double
								.parseDouble(data[7]), Double
								.parseDouble(data[8])));

			}

			in.close();

		} catch (IOException ioe) {
			Log.e("Read CSV Error", "Some CSV Error: ", ioe.getCause());

		} catch (NumberFormatException nfe) {
			Log.e("Number error", "parse number error - line: " + line + "  "
					+ nfe.getMessage(), nfe.getCause());
		} catch (Exception ex) {
			Log.e("Some Exception", "some exception: ", ex.getCause());
		}

		return mlist;

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.display_map, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;
		switch (item.getItemId()) {
		case R.id.show_map:
			intent = new Intent();
			intent.setClassName(getBaseContext(),
					"net.mypapit.mobile.myrepeater.DisplayMap");
			intent.putExtra("LatLong", new LatLng(xlocation.getLatitude(),
					xlocation.getLongitude()));
			// intent.putExtra("Repeaters",rl);

			startActivity(intent);

			return true;
		case R.id.action_about:
			try {
				showDialog();
			} catch (NameNotFoundException ex) {
				Toast toast = Toast.makeText(this, ex.toString(),
						Toast.LENGTH_SHORT);
				toast.show();

			}

			return true;
		case R.id.action_suggest:
			// Intent emailIntent = new Intent(Intent.ACTION_SENDTO,
			// Uri.parse("mailto:mypapit+new_repeater@gmail.com"));
			Intent emailIntent = new Intent(Intent.ACTION_SEND);

			emailIntent.setType("text/plain");
			emailIntent.putExtra(Intent.EXTRA_EMAIL,
					new String[] { "mypapit+new_repeater_suggest@gmail.com" });

			/*
			 * emailIntent.putExtra(Intent.EXTRA_EMAIL,
			 * "mypapit+new_repeater_suggest@gmail.com");
			 */
			emailIntent.putExtra(Intent.EXTRA_SUBJECT,
					"Repeater.MY - new Repeater");
			emailIntent
					.putExtra(
							Intent.EXTRA_TEXT,
							"Please put the repeater details you want to suggest --\n\nRepeater Callsign : \nFreq: \nShift: \nTone: \nClosest Known Location or Coordinates: \nOwner or Club:\n");

			/*
			 * String uriText = "mailto:mypapit+new_repeater@gmail.com" +
			 * "?subject=" + Uri.encode("Repeater.MY: New Repeater Suggestion")
			 * + "&body=" + Uri.encode(
			 * "Please put the repeater details you want to suggest --\n\nRepeater Callsign : \nFreq: \nShift: \nTone: \nClosest Known Location or Coordinates: \nOwner or Club:\n"
			 * );
			 * 
			 * Uri uri = Uri.parse(uriText);
			 * 
			 * 
			 * emailIntent.setData(uri);
			 */
			// startActivity(Intent.createChooser(sendIntent, "Send email"));

			startActivity(createEmailOnlyChooserIntent(emailIntent,
					"Suggest new Repeater"));

			return true;

		case R.id.action_contrib:
			intent = new Intent();
			intent.setClassName(getBaseContext(),
					"net.mypapit.mobile.myrepeater.ContribActivity");
			startActivity(intent);

			return true;
		}

		return false;
	}

	public void showDialog() throws NameNotFoundException {
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.about_dialog);
		dialog.setTitle("About Repeater.MY "
				+ getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		dialog.setCancelable(true);

		// text
		TextView text = (TextView) dialog.findViewById(R.id.tvAbout);
		text.setText(R.string.txtLicense);

		// icon image
		ImageView img = (ImageView) dialog.findViewById(R.id.ivAbout);
		img.setImageResource(R.drawable.ic_launcher);

		dialog.show();

	}

	public Intent createEmailOnlyChooserIntent(Intent source,
			CharSequence chooserTitle) {
		Stack<Intent> intents = new Stack<Intent>();
		Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto",
				"mypapit+new_repeater_suggest@gmail.com", null));
		List<ResolveInfo> activities = getPackageManager()
				.queryIntentActivities(i, 0);

		for (ResolveInfo ri : activities) {
			Intent target = new Intent(source);
			target.setPackage(ri.activityInfo.packageName);
			intents.add(target);
		}

		if (!intents.isEmpty()) {
			Intent chooserIntent = Intent.createChooser(intents.remove(0),
					chooserTitle);
			chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
					intents.toArray(new Parcelable[intents.size()]));

			return chooserIntent;
		} else {
			return Intent.createChooser(source, chooserTitle);
		}
	}

}
