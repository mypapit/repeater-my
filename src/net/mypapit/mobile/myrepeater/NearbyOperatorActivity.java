package net.mypapit.mobile.myrepeater;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

public class NearbyOperatorActivity extends Activity implements OnItemClickListener {

	private ListView listview;
	private TextView tvNearby;
	private HamOperatorList holist;
	private NearbyOperatorAdapter adapter;
	private static String URL = "http://api.repeater.my/v1/nearbyoperator.php";
	private double mlat, mlng;
	private String mlocation;
	public final String CACHE_PREFS = "cache-prefs";
	public final String CACHE_TIME = "cache-time-";
	public final String CACHE_JSON = "cache-json-";
	SharedPreferences cache;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.nearby_operator_list);
		overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);

		Bundle bundle = this.getIntent().getExtras();

		mlocation = bundle.getString("location");
		mlat = bundle.getDouble("lat");
		mlng = bundle.getDouble("lng");

		overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		tvNearby = (TextView) findViewById(R.id.tvNearbyRepeater);

		tvNearby.setText("Active stations near " + toTitleCase(mlocation.toLowerCase()));

		listview = (ListView) findViewById(R.id.HamOperatorListView);

		holist = new HamOperatorList(120);

		adapter = new NearbyOperatorAdapter(this, holist);

		listview.setAdapter(adapter);
		cache = this.getSharedPreferences(CACHE_PREFS, 0);

		Date cachedate = new Date(cache.getLong(CACHE_TIME + mlocation, new Date(20000).getTime()));

		long secs = (new Date().getTime() - cachedate.getTime()) / 1000;
		long hours = secs / 3600L;
		secs = secs % 3600L;
		long mins = secs / 60L;

		if (hours < 6) {
			String jsoncache = cache.getString(CACHE_JSON + mlocation, "none");
			if (jsoncache.compareToIgnoreCase("none") == 0) {
				new GetUserInfo(this, new LatLng(mlat, mlng)).execute();
			} else {

				loadfromCache(jsoncache);
			}

		} else {

			new GetUserInfo(this, new LatLng(mlat, mlng)).execute();

		}

		listview.setOnItemClickListener(this);

	}

	public void loadfromCache(String jsonCache) {
		JSONArray rakanradio = null;
		try {
			// JSONObject jsonObj = new JSONObject(jsonStr);

			rakanradio = new JSONArray(jsonCache);
			int num_of_rakanradio = rakanradio.length();
			for (int i = 0; i < num_of_rakanradio; i++) {
				JSONObject jsinfo = rakanradio.getJSONObject(i);

				String callsign = jsinfo.getString("callsign");

				String valid = jsinfo.getString("valid");

				int validity = Integer.parseInt(valid);

				DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				java.util.Date time = new java.util.Date();

				try {
					time = formatter.parse(jsinfo.getString("time"));
				} catch (ParseException e) {
					time = new java.util.Date();

					e.printStackTrace();
				}

				double holat, holng;
				try {

					holat = Double.parseDouble(jsinfo.getString("lat"));
					holng = Double.parseDouble(jsinfo.getString("lng"));
				} catch (Exception ex) {

					holat = 37.0;
					holng = 115.0;

				}
				/*
				 * (String callsign, String handle, String status, String
				 * phoneno, String locality, String client, String deviceid,
				 * String qsx, Date date, double lat, double lng, boolean valid
				 */

				HamOperator hamoperator = new HamOperator(callsign, jsinfo.getString("name"),
						jsinfo.getString("status"), jsinfo.getString("phoneno"), jsinfo.getString("locality"),
						jsinfo.getString("client"), jsinfo.getString("deviceid"), jsinfo.getString("qsx"), time,
						jsinfo.getString("time"), holat, holng, (validity < 1) ? false : true

						);

				try {
					hamoperator.setActiveNo(Integer.parseInt(jsinfo.getString("total")));
				} catch (Exception ex) {

					hamoperator.setActiveNo(1);
					ex.printStackTrace();

				}

				holist.add(hamoperator);

			}
		} catch (JSONException jse) {
			jse.printStackTrace();
		}

	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
		case android.R.id.home:

			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
				// NavUtils.navigateUpFromSameTask(this);
				finish();
			}

		}
		return false;

	}

	public static String toTitleCase(String input) {
		StringBuilder titleCase = new StringBuilder();
		boolean nextTitleCase = true;

		for (char c : input.toCharArray()) {
			if (Character.isSpaceChar(c)) {
				nextTitleCase = true;
			} else if (nextTitleCase) {
				c = Character.toTitleCase(c);
				nextTitleCase = false;
			}

			titleCase.append(c);
		}

		return titleCase.toString();
	}

	protected void onPause() {
		super.onPause();

		overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);

	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent intent = new Intent(this.getApplicationContext(), CallsignDetailsActivity.class);

		HamOperator hop = adapter.getHamOperator(position);

		// phoneno,status,name,locality,client,callsign,time

		intent.putExtra("phoneno", hop.getPhoneno());
		intent.putExtra("status", hop.getStatus());
		intent.putExtra("locality", hop.getLocality());
		intent.putExtra("client", hop.getClient());
		intent.putExtra("callsign", hop.getCallsign());
		intent.putExtra("time", hop.getStrDate());
		intent.putExtra("name", hop.getHandle());

		startActivity(intent);

	}

	private class GetUserInfo extends AsyncTask<Void, Void, Void> {

		String currentlat, currentlng;
		NearbyOperatorActivity activity;
		String jsonStr;

		GetUserInfo(NearbyOperatorActivity activity, LatLng latlng) {
			this.activity = activity;
			currentlat = latlng.latitude + "";
			currentlng = latlng.longitude + "";

		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			// put a dialog or whatsoever

		}

		@Override
		protected Void doInBackground(Void... params) {
			// Creating service handler class instance
			ServiceHandler sh = new ServiceHandler();

			// Making a request to url and getting response
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);

			nameValuePairs.add(new BasicNameValuePair("lat", currentlat));
			nameValuePairs.add(new BasicNameValuePair("lng", currentlng));

			jsonStr = sh.makeServiceCall(URL, ServiceHandler.GET, nameValuePairs);

			// Log.d("mypapit Json Response: ", "> " + jsonStr);

			// listrakanradio = new ArrayList<HashMap<String, String>>(200);
			JSONArray rakanradio = null;

			if (jsonStr != null) {
				try {
					// JSONObject jsonObj = new JSONObject(jsonStr);

					rakanradio = new JSONArray(jsonStr);
					int num_of_rakanradio = rakanradio.length();
					for (int i = 0; i < num_of_rakanradio; i++) {
						JSONObject jsinfo = rakanradio.getJSONObject(i);

						String callsign = jsinfo.getString("callsign");

						String valid = jsinfo.getString("valid");

						

						int validity = Integer.parseInt(valid);

						DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						java.util.Date time = new java.util.Date();

						try {
							time = formatter.parse(jsinfo.getString("time"));
						} catch (ParseException e) {
							time = new java.util.Date();

							e.printStackTrace();
						}

						double holat, holng;
						try {

							holat = Double.parseDouble(jsinfo.getString("lat"));
							holng = Double.parseDouble(jsinfo.getString("lng"));
						} catch (Exception ex) {

							holat = 37.0;
							holng = 115.0;

						}
						/*
						 * (String callsign, String handle, String status,
						 * String phoneno, String locality, String client,
						 * String deviceid, String qsx, Date date, double lat,
						 * double lng, boolean valid
						 */

						HamOperator hamoperator = new HamOperator(callsign, jsinfo.getString("name"),
								jsinfo.getString("status"), jsinfo.getString("phoneno"), jsinfo.getString("locality"),
								jsinfo.getString("client"), jsinfo.getString("deviceid"), jsinfo.getString("qsx"),
								time, jsinfo.getString("time"), holat, holng, (validity < 1) ? false : true

								);

						try {
							hamoperator.setActiveNo(Integer.parseInt(jsinfo.getString("total")));
						} catch (Exception ex) {

							hamoperator.setActiveNo(1);
							ex.printStackTrace();

						}

						holist.add(hamoperator);

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

			adapter = new NearbyOperatorAdapter(activity, holist);
			listview.setAdapter(adapter);
			adapter.notifyDataSetChanged();

			if ((holist.size() > 0) && (jsonStr != null)) {
				SharedPreferences.Editor editor = cache.edit();
				editor.putLong(activity.CACHE_TIME + mlocation, new Date().getTime());
				editor.putString(activity.CACHE_JSON + mlocation, jsonStr);

				editor.commit();

			}

		}

	}

}
