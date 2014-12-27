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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Parcelable;
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

public class RepeaterListActivity extends Activity implements OnItemClickListener {
	Repeater xlocation;
	RepeaterAdapter adapter;
	RepeaterList rl;
	ListView lv;
	TextView tvAddress;
	static int static_distance = 500;
	boolean excludeLink = false;
	float local_distance=200.0f;	
	
	boolean mrefresh=false;

	// StackHistory stackhistory;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.repeater_list);

		overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);

		lv = (ListView) findViewById(R.id.repeaterListView);
		tvAddress = (TextView) findViewById(R.id.tvAddress);

		// Repeater repeater = (Repeater)
		// this.getIntent().getExtras().getSerializable("RepeaterList");

		// String coordinates = (String)
		// this.getIntent().getExtras().getSerializable("RepeaterList");
		String coordinates = null;
		if (coordinates == null) {
			coordinates = new String("6.5,100.5");
		}
		String coord[] = coordinates.split(",");

		xlocation = new Repeater("", Double.parseDouble(coord[0]), Double.parseDouble(coord[1]));

		rl = RepeaterListActivity.loadData(R.raw.repeaterdata5, this);
		xlocation.calcDistanceAll(rl);
		rl.sort();
		
		

		adapter = new RepeaterAdapter(this, rl,local_distance,excludeLink);
		

		// lv=this.getListView();

		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(this);
		
		

		
		SharedPreferences prefs = getSharedPreferences("Location", MODE_PRIVATE);

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

		// restore Stack history
		// function disabled

		/*
		 * try { this.restoreStackHistory(); } catch (IOException ioe){
		 * Log.d("net.mypapit.mobile", "File history.txt : "+ioe.getMessage() );
		 * 
		 * ioe.printStackTrace(System.err);
		 * 
		 * stackhistory = new StackHistory();
		 * 
		 * } catch (ClassNotFoundException cnfe){ Log.d("net.mypapit.mobile",
		 * "File history.txt : "+cnfe.getMessage() ); stackhistory = new
		 * StackHistory(); cnfe.printStackTrace(System.err); }
		 * 
		 * if (stackhistory.size() > 10) { stackhistory.clear(); }
		 */

		GPSThread thread = new GPSThread(this);
		thread.start();

		lv.setAdapter(adapter);

	}
	
	public void onStart() {
		super.onStart();
		
		if (mrefresh){
			this.refreshList();
		}
		
	}
	
	public void refreshList() {
		SharedPreferences repeater_prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		excludeLink=repeater_prefs.getBoolean("excludeLinkRepeater", false);
		//local_distance=repeater_prefs.getString("repeater_local_distance", "100");
		local_distance= repeater_prefs.getInt("range", 150);

		adapter = new RepeaterAdapter(this, rl,local_distance,excludeLink);
		mrefresh=false;

		
		lv.setAdapter(adapter);
		
	}

	/*
	 * @Override protected void onListItemClick(ListView lv, View view, int
	 * position, long id) {
	 * 
	 * // Toast.makeText(getBaseContext(), rl.get(position).getCallsign() + //
	 * " clicked!", Toast.LENGTH_SHORT).show();
	 * 
	 * Intent intent = new Intent(); intent.setClassName(getBaseContext(),
	 * "net.mypapit.mobile.myrepeater.RepeaterDetailsActivity");
	 * 
	 * Repeater rpt = (Repeater) adapter.getRepeater(position);
	 * 
	 * intent.putExtra("Repeater", rpt.toArrayString()); startActivity(intent);
	 * 
	 * super.onListItemClick(lv, view, position, id);
	 * 
	 * }
	 */

	/*
	 * Disable stackhistory - memory leaks
	 * 
	 * private void restoreStackHistory() throws IOException,
	 * ClassNotFoundException { // implements restoring stackhistory File infile
	 * = new File(Environment.getExternalStorageDirectory(),"history.txt");
	 * FileInputStream fis = new FileInputStream(infile); ObjectInputStream ois
	 * = new ObjectInputStream(fis);
	 * 
	 * stackhistory = (StackHistory) ois.readObject(); ois.close();
	 * 
	 * }
	 */

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

				} else if (addressList.size() > 0) { // else if there are
														// addresses
														// retrieved
					int addressCounter = addressList.size();
					addressLocality = new String[addressCounter];
					addressLocality[0] = new String();

					// store each of the address list in a string;
					for (int i = 0; i < addressCounter; i++) {
						Address singleAddress = addressList.get(i);
						stringBuffer = new StringBuffer();
						Log.d("net.mypapit.mobile", "address: " + stringBuffer.toString());
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

			if (location == null) {
				location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			}

			if (location == null) {
				location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}

			if (location == null) {

				location = new Location("");
				SharedPreferences prefs = getSharedPreferences("Location", MODE_PRIVATE);
				float lat = prefs.getFloat("DefaultLat", 3.0f);
				float lon = prefs.getFloat("DefaultLon", 101.0f);

				location.setLatitude(lat);
				location.setLongitude(lon);

				m_address = this.geoCode(lat, lon);

				activity.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						activity.showToast("Unable to detect location, falling back on preset location");
					}

				});

				// Geocoder geocoder = new Geocoder(activity,
				// Locale.getDefault());

				/*
				 * try {
				 * 
				 * addressList = geocoder.getFromLocation(lat, lon, 3);
				 * Log.d("new.mypapit.mobile", "inside geocoder Lat: " + lat +
				 * "Lon: " + lon);
				 * 
				 * // if there are no address retrieved, put in Unknown Address
				 * if (addressList == null) { Log.d("net.mypapit.mobile",
				 * "Unknown Location...");
				 * 
				 * addressLocality[0] = new String("Unknown Location");
				 * m_address = addressLocality[0];
				 * 
				 * } else if (addressList.size() > 0) { // else if there are //
				 * addresses // retrieved int addressCounter =
				 * addressList.size(); addressLocality = new
				 * String[addressCounter]; addressLocality[0] = new String();
				 * 
				 * // store each of the address list in a string; for (int i =
				 * 0; i < addressCounter; i++) { Address singleAddress =
				 * addressList.get(i); stringBuffer = new StringBuffer();
				 * Log.d("net.mypapit.mobile", "address: " +
				 * stringBuffer.toString()); addressLocality[i] =
				 * singleAddress.getLocality();
				 * 
				 * } }
				 * 
				 * } catch (IllegalArgumentException iae) { //
				 * Toast.makeText(getActivity(), //
				 * "Invalid GPS coordinates, this is not supposed to happen", //
				 * Toast.LENGTH_LONG).show(); Log.d("net.mypapit.mobile",
				 * "Invalid GPS coordinates : " + iae.getMessage() +
				 * " caused by: " + iae.getCause()); addressLocality[0] = new
				 * String("Unknown Location");
				 * 
				 * } catch (IOException ioe) { Log.d("net.mypapit.mobile",
				 * "Geocoder IO exception " + ioe.toString());
				 * addressLocality[0] = new String("Unknown Location");
				 * 
				 * }
				 */
				// Log.e("net.mypapit.mobile", "geocode address: " +
				// addressLocality[0]);
				// m_address = addressLocality[0];

			}

			xlocation = new Repeater("", location.getLatitude(), location.getLongitude());
			m_address = this.geoCode(location.getLatitude(), location.getLongitude());
			if (xlocation != null) {

			} else {

				SharedPreferences prefs = getSharedPreferences("Location", MODE_PRIVATE);
				float lat = prefs.getFloat("DefaultLat", 3.0f);
				float lon = prefs.getFloat("DefaultLon", 101.0f);

				xlocation = new Repeater("", lat, lon);
				m_address = "Auto Location Services Disabled";

			}

			activity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					xlocation.calcDistanceAll(rl);
					rl.sort();
					adapter = new RepeaterAdapter(activity, rl,local_distance,excludeLink);
					activity.setAddress(m_address);

					activity.setListAdapter(adapter);
					Log.d("latitud papit", "Geo: " + xlocation.getLatitude());
					Log.d("latitud papit", "Address: " + m_address);

					activity.showToast("Location auto-detected, listing nearest repeater");
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

					Thread kthread = new Thread(new Runnable() {

						@Override
						public void run() {

							m_location2 = geoCode(lat, lon);

							activity.runOnUiThread(new Runnable() {

								@Override
								public void run() {
									// TODO Auto-generated method stub
									xlocation = new Repeater("", lat, lon);
									xlocation.calcDistanceAll(rl);
									rl.sort();
									adapter = new RepeaterAdapter(activity, rl,local_distance,excludeLink);

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
					// TODO Auto-generated method stub
					// tvAddress.setText("Auto Location Service Disabled");
					final String mProvider = provider;
					activity.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							activity.showToast("Location detection sensor: " + mProvider + " disabled");
						}

					});
				}

				@Override
				public void onProviderEnabled(String provider) {

					final String mProvider = provider;
					activity.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							activity.showToast("Location detection sensor: " + mProvider + " reenabled");
						}

					});

				}

				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {
					// TODO Auto-generated method stub

				}

			};

			locationManager.requestLocationUpdates(bestProvider, 5000, static_distance, myLocationListener);

			Looper.loop();

		}
	}

	public static RepeaterList loadData(int resource, Activity activity) {
		RepeaterList mlist = new RepeaterList(200);
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
			Log.e("Read CSV Error", "Some CSV Error: ", ioe.getCause());

		} catch (NumberFormatException nfe) {
			Log.e("Number error", "parse number error - line: " + line + "  " + nfe.getMessage(), nfe.getCause());
		} catch (Exception ex) {
			Log.e("Some Exception", "some exception at line :" + line + " \n " + ex.getCause());
			ex.printStackTrace(System.err);
		}

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
			// intent.putExtra("Repeaters",rl);

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

		/*	
			  Old pre-1.0.x implementation Repeater Suggest
			  
			  Intent emailIntent = new Intent(Intent.ACTION_SEND);
			  
			  
			  emailIntent.setType("text/plain");
			  emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {
			  "mypapit+new_repeater_suggest@gmail.com" });
			  
			  emailIntent.putExtra(Intent.EXTRA_SUBJECT,
			  "Repeater.MY - new Repeater"); emailIntent .putExtra(
			  Intent.EXTRA_TEXT,
			  "Please put the repeater details you want to suggest --\n\nRepeater Callsign : \nFreq: \nShift: \nTone: \nClosest Known Location or Coordinates: \nOwner or Club:\n"
			  );
			  
			  
			  startActivity(createEmailOnlyChooserIntent(emailIntent,
			  "Suggest new Repeater"));
			 */
			return true;

		case R.id.action_contrib:
			intent = new Intent();
			intent.setClassName(getBaseContext(), "net.mypapit.mobile.myrepeater.ContribActivity");
			startActivity(intent);

			return true;
			
		case R.id.action_settings:
			intent = new Intent(getBaseContext(),SettingsActivity.class);
			mrefresh=true;			
			startActivity(intent);
			
		return true;
		
		case R.id.search:
			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
				Toast.makeText(this, "Search is unavailable for Android Gingerbread (2.3.7) and below",
						Toast.LENGTH_LONG).show();
			}
			/*
			 * Disable stackhistory due to memory leak case R.id.action_history:
			 * //RepeaterList historylist = new RepeaterList(stackhistory);
			 * intent = new Intent(); intent.setClassName(getBaseContext(),
			 * "net.mypapit.mobile.myrepeater.RepeaterHistoryActivity");
			 * //intent.putExtra("historylist", stackhistory);
			 * intent.putExtra("lat", xlocation.getLatitude());
			 * intent.putExtra("lon", xlocation.getLongitude());
			 * 
			 * startActivity(intent);
			 */

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
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();

		// Setting Dialog Title
		alertDialog.setTitle("Manual Location");

		// Setting Dialog Message
		alertDialog.setMessage("Please disable GPS or Location Service to use this feature.");

		alertDialog.show();

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

		intent.putExtra("Repeater", rpt.toArrayString());
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
	/*
	 * Disable stackhistory due to memory leak
	 * 
	 * private void saveHistory() {
	 * 
	 * try{ File outfile = new
	 * File(Environment.getExternalStorageDirectory(),"history.txt"); if
	 * (!outfile.exists()){ outfile.createNewFile(); }
	 * 
	 * FileOutputStream fos = new FileOutputStream(outfile); ObjectOutputStream
	 * oos = new ObjectOutputStream(fos); oos.writeObject(stackhistory);
	 * Log.d("net.mypapit.mobile","number stackhistory :" +
	 * stackhistory.size()); oos.close();
	 * 
	 * } catch (IOException ioe){
	 * Log.e("net.mypapit.mobile","Masalah Exception file "+ ioe.getMessage());
	 * ioe.printStackTrace(System.err); }
	 * 
	 * }
	 */

}
