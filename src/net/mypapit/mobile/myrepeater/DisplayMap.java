/*
 * 
	Copyright 2013,2015,2016 Mohammad Hafiz bin Ismail <mypapit@gmail.com>
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.mypapit.mobile.myrepeater.mapinfo.CallsignMapInfo;
import net.mypapit.mobile.myrepeater.mapinfo.MapInfoObject;
import net.mypapit.mobile.myrepeater.mapinfo.RepeaterMapInfo;
import net.sf.jfuzzydate.FuzzyDateFormat;
import net.sf.jfuzzydate.FuzzyDateFormatter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class DisplayMap extends FragmentActivity implements OnInfoWindowClickListener {

	private GoogleMap map;
	HashMap<Marker, MapInfoObject> hashMap = new HashMap<Marker, MapInfoObject>();
	RepeaterList rl = new RepeaterList();
	// private static String URL ="http://192.168.1.40/rmy/getposition.php";
	private static String URL = "http://api.repeater.my/v1/getposition.php";
	
	public final String CACHE_PREFS = "cache-prefs";
	public final String CACHE_TIME = "cache-time-map-";
	public final String CACHE_JSON = "cache-json-map-";
	
	SharedPreferences cache;
	private JSONArray rakanradio = null;
	ArrayList<HashMap<String, String>> listrakanradio;

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

			//Log.e("Map NULL", "MAP NULL");
			Toast.makeText(this, "Unable to display Map", Toast.LENGTH_SHORT).show();

		} else {

			LatLng latlng = (LatLng) getIntent().getExtras().get("LatLong");

			Repeater xlocation = new Repeater("", new Double[] { latlng.latitude, latlng.longitude });

			rl = RepeaterListActivity.loadData(R.raw.repeaterdata5, this);
			xlocation.calcDistanceAll(rl);
			rl.sort();

			map.setMyLocationEnabled(true);
			map.setOnInfoWindowClickListener(this);
			map.getUiSettings().setZoomControlsEnabled(true);

			AdView mAdView = (AdView) findViewById(R.id.adViewMap);
			mAdView.loadAd(new AdRequest.Builder().build());

			// counter i, for mapping marker with integer
			int i = 0;
						
			int counter = rl.size();
			Repeater repeater; 
			for(int j=0;j<counter;j++) {
				repeater = rl.get(j);
				MarkerOptions marking = new MarkerOptions();
				marking.position(new LatLng(repeater.getLatitude(), repeater.getLongitude()));
				marking.title(repeater.getCallsign() + " - " + repeater.getDownlink() + "MHz (" + repeater.getClub()
						+ ")");

				marking.snippet("Tone: " + repeater.getTone() + " Shift: " + repeater.getShift());

				RepeaterMapInfo rmi = new RepeaterMapInfo(repeater);
				rmi.setIndex(i);

				hashMap.put(map.addMarker(marking), rmi);

				i++;

			}


			// Marker RKG = map.addMarker(new MarkerOptions().position(new
			// LatLng(6.1,100.3)).title("9M4RKG"));

			map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 10));
			map.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);
			
			
			cache = this.getSharedPreferences(CACHE_PREFS, 0);

			Date cachedate = new Date(cache.getLong(CACHE_TIME, new Date(20000).getTime()));

			long secs = (new Date().getTime() - cachedate.getTime()) / 1000;
			long hours = secs / 3600L;
			secs = secs % 3600L;
			long mins = secs / 60L;
			
			
			

			if (mins < 4) {
				String jsoncache = cache.getString(CACHE_JSON, "none");
				if (jsoncache.compareToIgnoreCase("none") == 0) {
					new GetUserInfo(latlng,this).execute();
					
				} else {

					loadfromCache(jsoncache);
					//Toast.makeText(this, "Loaded from cache: " + mins + " mins", Toast.LENGTH_SHORT).show();
				}

			} else {

				
				new GetUserInfo(latlng,this).execute();
				
				

			}
			
			

			map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

				@Override
				public View getInfoWindow(Marker marker) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public View getInfoContents(Marker marker) {
					Context context = getApplicationContext(); // or
																// getActivity(),
																// YourActivity.this,
																// etc.

					LinearLayout info = new LinearLayout(context);
					info.setOrientation(LinearLayout.VERTICAL);

					TextView title = new TextView(context);
					title.setTextColor(Color.BLACK);
					title.setGravity(Gravity.CENTER);
					title.setTypeface(null, Typeface.BOLD);
					title.setText(marker.getTitle());

					TextView snippet = new TextView(context);
					snippet.setTextColor(Color.GRAY);
					snippet.setText(marker.getSnippet());

					info.addView(title);
					info.addView(snippet);

					return info;
				}
			});

	
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.display_map, menu);i
		return true;
	}

	@Override
	public void onInfoWindowClick(Marker marker) {

		MapInfoObject mio = hashMap.get(marker);

		if (!mio.getIsRepeater()) {

			Intent intent = new Intent();
			intent.setClassName(getApplicationContext(), "net.mypapit.mobile.myrepeater.CallsignDetailsActivity");

			HashMap<String, String> inforakanradio = listrakanradio.get(mio.getIndex());
	
			

			intent.putExtra("callsign", inforakanradio.get("callsign"));
			intent.putExtra("name", inforakanradio.get("name"));
			intent.putExtra("qsx", inforakanradio.get("qsx"));
			intent.putExtra("status", inforakanradio.get("status"));
			intent.putExtra("distance", inforakanradio.get("distance"));
			intent.putExtra("time", inforakanradio.get("time"));
			intent.putExtra("lat", inforakanradio.get("lat"));
			intent.putExtra("lng", inforakanradio.get("lng"));
			intent.putExtra("valid", inforakanradio.get("valid"));
			intent.putExtra("deviceid", inforakanradio.get("deviceid"));
			intent.putExtra("phoneno", inforakanradio.get("phoneno"));
			intent.putExtra("client", inforakanradio.get("client"));
			intent.putExtra("locality", inforakanradio.get("locality"));

			startActivity(intent);

			return;
		}

		Repeater rpt = rl.get(mio.getIndex());

		Intent intent = new Intent();
		intent.setClassName(getBaseContext(), "net.mypapit.mobile.myrepeater.RepeaterDetailsActivity");
		intent.putExtra("Repeater", rpt.toArrayString());
		startActivity(intent);

	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:

			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
				// NavUtils.navigateUpFromSameTask(this);
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
	
	//load data from Preference cache rather from getuser info
	public void loadfromCache(String jsonCache) {
		
		listrakanradio = new ArrayList<HashMap<String, String>>(200);

		if (jsonCache != null) {
			try {
				// JSONObject jsonObj = new JSONObject(jsonStr);

				rakanradio = new JSONArray(jsonCache);
				int num_of_rakanradio = rakanradio.length();
				for (int i = 0; i < num_of_rakanradio; i++) {
					JSONObject jsinfo = rakanradio.getJSONObject(i);

					String callsign = jsinfo.getString("callsign");

				
					String valid = jsinfo.getString("valid");

					HashMap<String, String> inforakanradio = new HashMap<String, String>();

					int validity = Integer.parseInt(valid);

					if (validity == 0) {
						callsign = callsign + " (Unverified)";
					}

					inforakanradio.put("callsign", callsign);
					inforakanradio.put("name", jsinfo.getString("name"));
					inforakanradio.put("qsx", jsinfo.getString("qsx"));
					inforakanradio.put("status", jsinfo.getString("status"));
					inforakanradio.put("distance", jsinfo.getString("distance"));
					inforakanradio.put("time", jsinfo.getString("time"));
					inforakanradio.put("lat", jsinfo.getString("lat"));
					inforakanradio.put("lng", jsinfo.getString("lng"));
					inforakanradio.put("valid", valid);
					inforakanradio.put("deviceid", jsinfo.getString("deviceid"));
					inforakanradio.put("phoneno", jsinfo.getString("phoneno"));
					inforakanradio.put("client", jsinfo.getString("client"));
					inforakanradio.put("locality", jsinfo.getString("locality"));

					listrakanradio.add(inforakanradio);
					
					

				}
			} catch (JSONException jse) {
				jse.printStackTrace();
			}

		} else {
			Log.e("mypapit ServiceHandler-cache", "Couldn't get any data from map cache :(");
		}
		
		
		Iterator<HashMap<String, String>> iter = listrakanradio.iterator();
		
		HashMap<String, String> inforakanradio;
		
		

		int callsignIndex = 0;
		
		SharedPreferences repeater_prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		String m_deviceid = repeater_prefs.getString("deviceid", "defaultx");

		while (iter.hasNext()) {
			inforakanradio = new HashMap<String, String>();
			inforakanradio = iter.next();

			MarkerOptions marking = new MarkerOptions();

			// java.util.Date utilDate = new java.util.Date();
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			java.util.Date time = new java.util.Date();

			try {
				time = formatter.parse(inforakanradio.get("time"));
			} catch (ParseException e) {
				time = new java.util.Date();

				e.printStackTrace();
			}

			FuzzyDateFormatter format = FuzzyDateFormat.getInstance();

			marking.position(new LatLng(Double.parseDouble(inforakanradio.get("lat")), Double
					.parseDouble(inforakanradio.get("lng"))));
			marking.title(new StringBuilder(inforakanradio.get("callsign")).append(" - ")
					.append(format.formatDistance(time)).toString());
			marking.snippet(new StringBuilder("@").append(inforakanradio.get("name")).append("\n#:")
					.append(inforakanradio.get("status")).append("\n").append(inforakanradio.get("phoneno"))
					.append("\n").append(inforakanradio.get("client")).toString());
			
			CallsignMapInfo cmi = new CallsignMapInfo();


			String valid = inforakanradio.get("valid");
			String deviceId = inforakanradio.get("deviceid");

			

			if (Integer.parseInt(valid) == 1) {
				marking.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
			} else {
				marking.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
			}
			

			if (deviceId.equalsIgnoreCase(m_deviceid)) {
				marking.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));

			}

			cmi.setIndex(callsignIndex);

			hashMap.put(map.addMarker(marking), cmi);

			callsignIndex++;

		}
		
		
	}

	// private class AsyncTask for retrieving repeater-my user information

	private class GetUserInfo extends AsyncTask<Void, Void, Void> {

		String currentlat, currentlng;
		DisplayMap activity;
		String jsonstringcache;

		GetUserInfo(LatLng latlng,DisplayMap activity) {
			currentlat = latlng.latitude + "";
			currentlng = latlng.longitude + "";
			this.activity = activity;

		}

		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			Toast.makeText(activity, "Getting stations info from servers",Toast.LENGTH_SHORT).show();

		}

		@Override
		protected Void doInBackground(Void... params) {
			// Creating service handler class instance
			ServiceHandler sh = new ServiceHandler();

			// Making a request to url and getting response
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);

			nameValuePairs.add(new BasicNameValuePair("lat", currentlat));
			nameValuePairs.add(new BasicNameValuePair("lng", currentlng));

			String jsonStr = sh.makeServiceCall(URL, ServiceHandler.GET, nameValuePairs);

			// Log.d("mypapit Json Response: ", "> " + jsonStr);

			listrakanradio = new ArrayList<HashMap<String, String>>(200);

			if (jsonStr != null) {
				try {
					// JSONObject jsonObj = new JSONObject(jsonStr);

					rakanradio = new JSONArray(jsonStr);
					int num_of_rakanradio = rakanradio.length();
					for (int i = 0; i < num_of_rakanradio; i++) {
						JSONObject jsinfo = rakanradio.getJSONObject(i);

						String callsign = jsinfo.getString("callsign");

						String valid = jsinfo.getString("valid");

						HashMap<String, String> inforakanradio = new HashMap<String, String>();

						int validity = Integer.parseInt(valid);

						if (validity == 0) {
							callsign = callsign + " (Unverified)";
						}

						inforakanradio.put("callsign", callsign);
						inforakanradio.put("name", jsinfo.getString("name"));
						inforakanradio.put("qsx", jsinfo.getString("qsx"));
						inforakanradio.put("status", jsinfo.getString("status"));
						inforakanradio.put("distance", jsinfo.getString("distance"));
						inforakanradio.put("time", jsinfo.getString("time"));
						inforakanradio.put("lat", jsinfo.getString("lat"));
						inforakanradio.put("lng", jsinfo.getString("lng"));
						inforakanradio.put("valid", valid);
						inforakanradio.put("deviceid", jsinfo.getString("deviceid"));
						inforakanradio.put("phoneno", jsinfo.getString("phoneno"));
						inforakanradio.put("client", jsinfo.getString("client"));
						inforakanradio.put("locality", jsinfo.getString("locality"));

						listrakanradio.add(inforakanradio);
						
						jsonstringcache = jsonStr;

					}
				} catch (JSONException jse) {
					jse.printStackTrace();
				}

			} else {
				Log.e("mypapit ServiceHandler", "Couldn't get any data from api endpoint");
			}
			return null;

		}

		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			if ( (listrakanradio.size() >0) && (jsonstringcache != null) ){
				SharedPreferences.Editor editor = cache.edit();
				editor.putLong(activity.CACHE_TIME, new Date().getTime());
				editor.putString(activity.CACHE_JSON, jsonstringcache);

				editor.commit();
				
			}
			

			Iterator<HashMap<String, String>> iter = listrakanradio.iterator();
			HashMap<String, String> inforakanradio;
			
			SharedPreferences repeater_prefs = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());

			String m_deviceid = repeater_prefs.getString("deviceid", "defaultx");

			int callsignIndex = 0;

			while (iter.hasNext()) {
				inforakanradio = new HashMap<String, String>();
				inforakanradio = iter.next();

				MarkerOptions marking = new MarkerOptions();

				// java.util.Date utilDate = new java.util.Date();
				DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				java.util.Date time = new java.util.Date();

				try {
					time = formatter.parse(inforakanradio.get("time"));
				} catch (ParseException e) {
					time = new java.util.Date();

					e.printStackTrace();
				}

				FuzzyDateFormatter format = FuzzyDateFormat.getInstance();

				marking.position(new LatLng(Double.parseDouble(inforakanradio.get("lat")), Double
						.parseDouble(inforakanradio.get("lng"))));
				marking.title(new StringBuilder(inforakanradio.get("callsign")).append(" - ")
						.append(format.formatDistance(time)).toString());
				marking.snippet(new StringBuilder("@").append(inforakanradio.get("name")).append("\n#:")
						.append(inforakanradio.get("status")).append("\n").append(inforakanradio.get("phoneno"))
						.append("\n").append(inforakanradio.get("client")).toString());

				CallsignMapInfo cmi = new CallsignMapInfo();
		
				
				String valid = inforakanradio.get("valid");
				String deviceId = inforakanradio.get("deviceid");


				if (Integer.parseInt(valid) == 1) {
					marking.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
				} else {
					marking.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
				}
			

				if (deviceId.equalsIgnoreCase(m_deviceid)) {
					marking.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));

				}

				cmi.setIndex(callsignIndex);

				hashMap.put(map.addMarker(marking), cmi);

				callsignIndex++;

			}

		}

	} //GetUserInfo

} //Display Map Activity

class ServiceHandler {

	static String response = null;
	public final static int GET = 1;
	public final static int POST = 2;

	public ServiceHandler() {

	}

	/**
	 * Making service call
	 * 
	 * @url - url to make request
	 * @method - http request method
	 * */
	public String makeServiceCall(String url, int method) {
		return this.makeServiceCall(url, method, null);
	}

	/**
	 * Making service call
	 * 
	 * @url - url to make request
	 * @method - http request method
	 * @params - http request params
	 * */
	public String makeServiceCall(String url, int method, List<NameValuePair> params) {
		try {
			// http client
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpEntity httpEntity = null;
			HttpResponse httpResponse = null;

			// Checking http request method type
			if (method == POST) {
				HttpPost httpPost = new HttpPost(url);
				// adding post params
				if (params != null) {
					httpPost.setEntity(new UrlEncodedFormEntity(params));
				}

				httpResponse = httpClient.execute(httpPost);

			} else if (method == GET) {
				// appending params to url
				if (params != null) {
					String paramString = URLEncodedUtils.format(params, "utf-8");
					url += "?" + paramString;
				}
				HttpGet httpGet = new HttpGet(url);

				httpResponse = httpClient.execute(httpGet);

			}
			httpEntity = httpResponse.getEntity();
			response = EntityUtils.toString(httpEntity);

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return response;

	}
}
