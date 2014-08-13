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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;

public class RepeaterListActivity extends ListActivity {
	Repeater xlocation;
	RepeaterAdapter adapter;
	RepeaterList rl;
	ListView lv;
	

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
		
		lv=this.getListView();
		
		lv.setTextFilterEnabled(true);
		//Toast.makeText(this, "Please enable Location Services", Toast.LENGTH_LONG).show();
		SharedPreferences prefs = getSharedPreferences("Location", MODE_PRIVATE);
		
		//need to put token to avoid app from popping up annoying select manual location dialog
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM");
		Date date = new Date();
		
		if (!this.isLocationEnabled(this)&& !dateFormat.format(date).equalsIgnoreCase(prefs.getString("token", "28/10"))){
			
			
			
			

			
			//show dialog if Location Services is not enabled
			
			 AlertDialog.Builder builder = new AlertDialog.Builder(this);
		        builder.setTitle(R.string.gps_not_found_title);  // GPS not found
		        builder.setMessage(R.string.gps_not_found_message); // Want to enable?
		        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialogInterface, int i) {
		            	
		            	Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		            	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		            	
		                getApplicationContext().startActivity(intent);
		            }
		        });
		        
		        //if no - bring user to selecting Static Location Activity
		        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent();
						intent.setClassName(getBaseContext(),
								"net.mypapit.mobile.myrepeater.StaticLocationActivity");
						startActivity(intent);

						
					}
		        	
		        	
		        });
		        builder.create().show();
		        
			
		
		}
		

		
		
		
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
		
		Repeater rpt = (Repeater) adapter.getRepeater(position);
		
		intent.putExtra("Repeater", rpt.toArrayString());
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
				SharedPreferences prefs = getSharedPreferences("Location", MODE_PRIVATE); 
				float lat = prefs.getFloat("DefaultLat", 3.0f);
				float lon = prefs.getFloat("DefaultLon", 101.0f);
				
				location.setLatitude(lat);
				location.setLongitude(lon);
				
				

			}

			xlocation = new Repeater("", location.getLatitude(),
					location.getLongitude());

			if (xlocation != null) {

			} else {

				SharedPreferences prefs = getSharedPreferences("Location", MODE_PRIVATE); 
				float lat = prefs.getFloat("DefaultLat", 3.0f);
				float lon = prefs.getFloat("DefaultLon", 101.0f);

				xlocation = new Repeater("", lat, lon);

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
		
		
		  SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
	        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
	        searchView.setIconifiedByDefault(false);
	        searchView.setQueryHint("part of repeater callsign");
	        
	        SearchView.OnQueryTextListener textChangeListener = new SearchView.OnQueryTextListener() {
				
				@Override
				public boolean onQueryTextSubmit(String searchText) {
					// TODO Auto-generated method stub
					adapter.getFilter().filter(searchText);
					Log.d("MYRepeater","search: "+ searchText);
					adapter.notifyDataSetChanged();
					return true;
				}
				
				@Override
				public boolean onQueryTextChange(String searchText) {
					// TODO Auto-generated method stub
					adapter.getFilter().filter(searchText);
					adapter.notifyDataSetChanged();
					return true;
				}
			};
	        	        
	        searchView.setOnQueryTextListener(textChangeListener);
	        
	        
	        
	        
		
		return super.onCreateOptionsMenu(menu);
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
		case R.id.manual_settings:
			if (!this.isLocationEnabled(this)) {
			intent = new Intent();
			intent.setClassName(getBaseContext(),
					"net.mypapit.mobile.myrepeater.StaticLocationActivity");
			startActivity(intent);
		  } else {
			  this.showAlertDialog();
		  }
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
	
	public void showAlertDialog() {
		AlertDialog alertDialog = new AlertDialog.Builder(
                this).create();

        // Setting Dialog Title
        alertDialog.setTitle("Manual Location");

        // Setting Dialog Message
        alertDialog.setMessage("This feature is only available when Location Service is disabled");

        alertDialog.show();

		
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
	
	public  boolean isLocationEnabled(Context context) {
		 LocationManager lm = (LocationManager)
	                getSystemService(Context.LOCATION_SERVICE);
	        String provider = lm.getBestProvider(new Criteria(), true);
	        return (!TextUtils.isEmpty(provider) &&
	                !LocationManager.PASSIVE_PROVIDER.equals(provider));


	} 

}
