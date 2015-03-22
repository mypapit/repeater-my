/*
 * 
 * This is the start-up main file for the application
 * This application requires (and has been compiled with) Google Play Service rev. 13
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

import garin.artemiy.compassview.library.CompassSensorsActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;

public class RepeaterListActivity extends CompassSensorsActivity implements OnItemClickListener {
	Repeater xlocation;
	RepeaterAdapter adapter;
	RepeaterList rl;
	ListView lv;
	TextView tvAddress;
	static int static_distance = 500;
	private boolean excludeLink = false;
	private boolean excludeDirection = false;
	private float local_distance = 100.0f;

	boolean mrefresh = true;

	// StackHistory stackhistory;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.repeater_list);

		overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);

		

		lv = (ListView) findViewById(R.id.repeaterListView);
		tvAddress = (TextView) findViewById(R.id.tvAddress);

		String coordinates = null;
		if (coordinates == null) {
			coordinates = new String("37.5651,126.98955");
		}
		String coord[] = coordinates.split(",");

		xlocation = new Repeater("", Double.parseDouble(coord[0]), Double.parseDouble(coord[1]));

		rl = RepeaterListActivity.loadData(R.raw.repeaterdata5, this);
		xlocation.calcDistanceAll(rl);
		rl.sort();

		adapter = new RepeaterAdapter(this, rl, xlocation, local_distance, excludeLink, excludeDirection);


		
        lv.setFastScrollEnabled(true);
        lv.setVerticalFadingEdgeEnabled(false);
        lv.setVerticalScrollBarEnabled(true);
		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(this);

		SharedPreferences prefs = getSharedPreferences("Location", MODE_PRIVATE);
		
		
		if (prefs.getInt("walkthrough", 0)==0){
			Intent intent = new Intent();
			intent.setClassName(getBaseContext(), "net.mypapit.mobile.myrepeater.WalkthroughActivity");
			SharedPreferences.Editor prefEditor = prefs.edit();
			prefEditor.putInt("walkthrough", 1);
			prefEditor.commit();
			
			startActivity(intent);
		}


		// need to put token to avoid app from popping up annoying select manual
		// dialog will be triggered if location/gps is not enabled AND if the
		// date in dd/MM is not equal to 'token' saved in StaticLocationActivity
		// location dialog
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM");
		Date date = new Date();

		if (!this.isLocationEnabled(this)
				&& !dateFormat.format(date).equalsIgnoreCase(prefs.getString("token", "28/10"))) {

			// show dialog if Location Services is not enabled

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.gps_not_found_title); // GPS not found
			builder.setMessage(R.string.gps_not_found_message); // Want to
			// enable?

			// if yes - bring user to enable Location Service settings
			builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogInterface, int i) {

					Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

					getApplicationContext().startActivity(intent);
				}
			});

			// if no - bring user to selecting Static Location Activity
			builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent();
					intent.setClassName(getBaseContext(), "net.mypapit.mobile.myrepeater.StaticLocationActivity");
					startActivity(intent);

				}

			});
			builder.create().show();

		}

		GPSThread thread = new GPSThread(this);
		thread.start();

		lv.setAdapter(adapter);

	}

	public void onStart() {
		super.onStart();

		if (mrefresh) {
			this.refreshList();
		}

		excludeDirection = checkCompassSensor(xlocation);

	}

	public void refreshList() {
		SharedPreferences repeater_prefs = PreferenceManager.getDefaultSharedPreferences(this);

		excludeLink = repeater_prefs.getBoolean("excludeLinkRepeater", false);

		local_distance = repeater_prefs.getInt("range", 100);

		adapter = new RepeaterAdapter(this, rl, xlocation, local_distance, excludeLink, excludeDirection);
		mrefresh = false;

		lv.setAdapter(adapter);

	}

	class GPSThread extends Thread {
		RepeaterListActivity activity;
		StringBuffer sb;
		String bestProvider;
		String m_address;
		StringBuffer stringBuffer;
		List<Address> addressList = null;
		String[] addressLocality;

		// addressLocality = new AddressLocality[1];

		public GPSThread(RepeaterListActivity activity) {
			this.activity = activity;
			addressLocality = new String[10];

			m_address = new String("Unknown Address");
			stringBuffer = new StringBuffer("");

		}

		public String geoCode(double lat, double lon) {
			Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
			List<Address> addressList = null;
			StringBuffer stringBuffer = new StringBuffer("");
			String[] addressLocality;
			// addressLocality = new AddressLocality[1];
			addressLocality = new String[10];
			Log.d("net.mypapit.mobile", "Unknown Address");

			try {

				addressList = geocoder.getFromLocation(lat, lon, 3);
				Log.d("new.mypapit.mobile", "inside geocoder Lat: " + lat + "Lon: " + lon);

				// if there are no address retrieved, put in Unknown Address
				if (addressList == null) {
					Log.d("net.mypapit.mobile", "Unknown Address");

					addressLocality[0] = new String("Unknown Location");
					return addressLocality[0];

				} else if (addressList.size() > 0) { 
					// else if there are addresses retrieved
					int addressCounter = addressList.size();
					addressLocality = new String[addressCounter];
					addressLocality[0] = new String();

					// store each of the address list in a string;
					for (int i = 0; i < addressCounter; i++) {
						Address singleAddress = addressList.get(i);
						stringBuffer = new StringBuffer();
						Log.d("net.mypapit.mobile", "address: " + stringBuffer.toString());
						//However, only display locality info (like county, territory, area)
						addressLocality[i] = singleAddress.getLocality();

					}
				}

			} catch (IllegalArgumentException iae) {
				// Toast.makeText(getActivity(),
				// "Invalid GPS coordinates, this is not supposed to happen",
				// Toast.LENGTH_LONG).show();
				Log.d("net.mypapit.mobile",
						"Invalid GPS coordinates : " + iae.getMessage() + " caused by: " + iae.getCause());
				addressLocality[0] = new String("Unknown Location");

			} catch (IOException ioe) {
				Log.d("net.mypapit.mobile", "Geocoder IO exception " + ioe.toString());
				addressLocality[0] = new String("Unknown Location");

			}
			Log.e("net.mypapit.mobile", "geocode address: " + addressLocality[0]);
			return addressLocality[0];
		}

		@Override
		public void run() {

			LocationManager locationManager;
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			criteria.setPowerRequirement(Criteria.POWER_LOW);
			criteria.setSpeedRequired(false);
			criteria.setAltitudeRequired(false);
			criteria.setHorizontalAccuracy(Criteria.NO_REQUIREMENT);
			criteria.setCostAllowed(true);
			criteria.setVerticalAccuracy(Criteria.NO_REQUIREMENT);

			Looper.prepare();

			bestProvider = locationManager.getBestProvider(criteria, false);

			Location location = locationManager.getLastKnownLocation(bestProvider);

			// if couldn't get Location Provider (aka couldn't get coordinates,
			// try GPS

			if (location == null) {
				location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			}
			// if couldn't get Location Provider (aka couldn't get coordinates,
			// try Network
			if (location == null) {
				location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}

			if (location == null) {
				excludeDirection = true;
			}

			// if couldn't get Location Provider (aka couldn't get coordinates,
			// try set 37.56, 126.989 as coordinate and be done with it (Seoul,
			// Korea)
			if (location == null) {

				location = new Location("");
				SharedPreferences prefs = getSharedPreferences("Location", MODE_PRIVATE);
				float lat = prefs.getFloat("DefaultLat", 37.56f);
				float lon = prefs.getFloat("DefaultLon", 126.989f);
				excludeDirection = true;

				location.setLatitude(lat);
				location.setLongitude(lon);
				xlocation = new Repeater("Simulated", location.getLatitude(), location.getLongitude());

				m_address = "Simulated: " + this.geoCode(lat, lon);

				activity.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						activity.showToast("Unable to detect location, falling back on preset location");


					}

				});

			} else {
				xlocation = new Repeater(location.getProvider(), location.getLatitude(), location.getLongitude());
				m_address = this.geoCode(location.getLatitude(), location.getLongitude());

				excludeDirection = checkCompassSensor(xlocation);
				activity.showToast("Location auto-detected, listing nearest repeater");
			}

			if (xlocation != null) {

				// check again for compassSensor

			} else {
				// else if xlocation == null, get coordinate stored in Manual
				// Location setting
				SharedPreferences prefs = getSharedPreferences("Location", MODE_PRIVATE);
				float lat = prefs.getFloat("DefaultLat", 37.56f);
				float lon = prefs.getFloat("DefaultLon", 126.989f);
				excludeDirection = true;

				xlocation = new Repeater("Simulated", lat, lon);
				m_address = "Auto Location Services Disabled";

			}

			activity.runOnUiThread(new Runnable() {

				@Override
				public void run() {

					xlocation.calcDistanceAll(rl);
					rl.sort();
					if (xlocation.getProvider().equalsIgnoreCase("Simulated")) {
						excludeDirection = true;
					}
					adapter = new RepeaterAdapter(activity, rl, xlocation, local_distance, excludeLink,
							excludeDirection);
					activity.setAddress(m_address);

					activity.setListAdapter(adapter);
					Log.d("latitud papit", "Geo: " + xlocation.getLatitude());
					Log.d("latitud papit", "Address: " + m_address);

					adapter.notifyDataSetChanged();

				}

			}

					);
			LocationListener myLocationListener = new LocationListener() {
				String m_location2 = null;

				@Override
				public void onLocationChanged(final Location location) {

					final double lat = location.getLatitude();
					final double lon = location.getLongitude();
					if (location.getProvider().equalsIgnoreCase("Simulated")) {
						excludeDirection = true;
					} else {
						excludeDirection = checkCompassSensor(xlocation);
					}

					Thread kthread = new Thread(new Runnable() {

						@Override
						public void run() {

							m_location2 = geoCode(lat, lon);

							activity.runOnUiThread(new Runnable() {

								@Override
								public void run() {

									xlocation = new Repeater("", lat, lon);
									xlocation.calcDistanceAll(rl);
									rl.sort();

									adapter = new RepeaterAdapter(activity, rl, xlocation, local_distance, excludeLink,
											excludeDirection);

									if (m_location2 != null) {
										activity.setAddress(m_location2);
									} else {
										activity.setAddress("Couldn't detect address");

									}
									activity.setListAdapter(adapter);
									adapter.notifyDataSetChanged();

									Log.d("latitud papit", "Geo: " + xlocation.getLatitude());

								}

							}

									);
						}

					}, "on changed location thread");

					kthread.start();

				}

				@Override
				public void onProviderDisabled(String provider) {
					
					// tvAddress.setText("Auto Location Service Disabled");
					final String mProvider = provider;
					activity.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							activity.showToast("Location detection sensor: " + mProvider + " disabled");
							activity.excludeDirection = true;
						}

					});
				}

				@Override
				public void onProviderEnabled(String provider) {

					final String mProvider = provider;
					activity.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							activity.showToast("Location detection sensor: " + mProvider + " re-enabled");
							activity.excludeDirection = false;

						}

					});

				}

				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {
					

				}

			};

			locationManager.requestLocationUpdates(bestProvider, 5000, static_distance, myLocationListener);

			Looper.loop();

		}
	}

	public static RepeaterList loadData(int resource, Activity activity) {
		RepeaterList mlist = new RepeaterList(150);
		int line = 0;
		try {
			
			
			InputStream stream = activity.getResources().openRawResource(resource);
			InputStreamReader is = new InputStreamReader(stream);
			BufferedReader in = new BufferedReader(is);
			CSVReader csv = new CSVReader(in, ';', '\"', 0);
			String data[];

			while ((data = csv.readNext()) != null) {
				line++;
				mlist.add(new Repeater("", data[1], data[2], data[3], data[9], Double.parseDouble(data[4]), Double
						.parseDouble(data[5]), Double.parseDouble(data[6]), Double.parseDouble(data[7]), Double
						.parseDouble(data[8])));

			}

			in.close();

		} catch (IOException ioe) {
			Log.e("Read CSV Error mypapit", "Some CSV Error: ", ioe.getCause());

		} catch (NumberFormatException nfe) {
			Log.e("Number error", "parse number error - line: " + line + "  " + nfe.getMessage(), nfe.getCause());
		} catch (Exception ex) {
			Log.e("Some Exception", "some exception at line :" + line + " \n " + ex.getCause());
			ex.printStackTrace(System.err);
		}

		return mlist;

	}
	
	public static ArrayList<String> loadStringData(InputStream stream) {
		ArrayList<String> mlist = new ArrayList<String>(150);
		int line=0;
		try {

			//InputStream stream = activity.getResources().openRawResource(resource);
			InputStreamReader is = new InputStreamReader(stream);
			BufferedReader in = new BufferedReader(is);
			CSVReader csv = new CSVReader(in, ';', '\"', 0);
			String data[];

			while ((data = csv.readNext()) != null) {
				line++;
				mlist.add(data[1]);
				
				//Log.d("net.mypapit repeater data","stored data : " + data[1]);

			}

			in.close();

		} catch (IOException ioe) {
			Log.e("Read CSV Error mypapit", "Some CSV Error: ", ioe.getCause());

		} catch (NumberFormatException nfe) {
			Log.e("Number error", "parse number error - line: " + line + "  " + nfe.getMessage(), nfe.getCause());
		} catch (Exception ex) {
			Log.e("Some Exception", "some exception at line :" + line + " \n " + ex.getCause());
			ex.printStackTrace(System.err);
		}
		//Collections.sort(mlist);
				
		return mlist;
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.display_map, menu);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
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
					Log.d("MYRepeater", "search: " + searchText);
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
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;
		switch (item.getItemId()) {
		case R.id.show_map:
			intent = new Intent();
			intent.setClassName(getBaseContext(), "net.mypapit.mobile.myrepeater.DisplayMap");
			intent.putExtra("LatLong", new LatLng(xlocation.getLatitude(), xlocation.getLongitude()));

			startActivity(intent);

			return true;
		case R.id.manual_settings:
			if (!this.isLocationEnabled(this)) {
				intent = new Intent();
				intent.setClassName(getBaseContext(), "net.mypapit.mobile.myrepeater.StaticLocationActivity");
				startActivity(intent);
			} else {
				this.showAlertDialog();
			}
			return true;

		case R.id.action_about:
			try {
				showDialog();
			} catch (NameNotFoundException ex) {
				Toast toast = Toast.makeText(this, ex.toString(), Toast.LENGTH_SHORT);
				toast.show();

			}

			return true;
		case R.id.action_suggest:

			intent = new Intent();

			intent.setClassName(getBaseContext(), "net.mypapit.mobile.myrepeater.SuggestRepeaterStartActivity");
			this.startActivity(intent);

			return true;

		case R.id.action_contrib:
			intent = new Intent();
			intent.setClassName(getBaseContext(), "net.mypapit.mobile.myrepeater.ContribActivity");
			startActivity(intent);

			return true;

		case R.id.action_settings:
			intent = new Intent(getBaseContext(), SettingsActivity.class);
			mrefresh = true;
			startActivity(intent);

			return true;

		case R.id.search:
			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
				Toast.makeText(this, "Search is unavailable for Gingerbread (2.3.7) and below",
						Toast.LENGTH_LONG).show();
			}

			return true;
		}

		return false;
	}

	public void showDialog() throws NameNotFoundException {
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.about_dialog);
		dialog.setTitle("About Repeater.MY " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
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
		
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Manual Location"); 
		builder.setMessage("Note that this will not allow Repeater.MY to auto-detect location.\n\nDisable Location Service now?"); // Want to
		// enable?

		// if yes - bring user to enable Location Service settings
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int i) {

				Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				getApplicationContext().startActivity(intent);
			}
		});

		// if no - bring user to selecting Static Location Activity
		builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				
			}

		});
		builder.create().show();
		
		

		
	}

	public boolean isLocationEnabled(Context context) {
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		String provider = lm.getBestProvider(new Criteria(), true);
		return (!TextUtils.isEmpty(provider) && !LocationManager.PASSIVE_PROVIDER.equals(provider));

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent intent = new Intent();
		intent.setClassName(getBaseContext(), "net.mypapit.mobile.myrepeater.RepeaterDetailsActivity");

		Repeater rpt = (Repeater) adapter.getRepeater(position);

		// stackhistory.add(new FauxRepeater(rpt));
		String pass[] = rpt.toArrayString();
		pass[9] = xlocation.getLatitude() + "";
		pass[10] = xlocation.getLongitude() + "";

		intent.putExtra("Repeater", pass);
		intent.putExtra("noCompass", excludeDirection);
		startActivity(intent);

	}

	public void setListAdapter(RepeaterAdapter adapter) {
		lv.setAdapter(adapter);

	}

	public void setAddress(String address) {
		tvAddress.setText(address);

	}

	public void showToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	protected void onPause() {
		super.onPause();
		// this.saveHistory();

		overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);

	}

	// check for compass sensor
	public boolean checkCompassSensor(Location xlocation) {

		//if using Simulated provider, then automatically return true
		if (xlocation.getProvider().equalsIgnoreCase("Simulated")) {
			return true;
		}

		boolean sensor = getBaseContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);

		/*
		Toast toast;
		
		if (sensor) {
			toast = Toast.makeText(this, "Compass detected!", Toast.LENGTH_LONG);

		} else {
			toast = Toast.makeText(this, "Compass not detected!", Toast.LENGTH_LONG);
		}

		toast.show();
		*/

		return !sensor;

	}

}
