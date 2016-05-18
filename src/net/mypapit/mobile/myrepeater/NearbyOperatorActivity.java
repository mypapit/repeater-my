package net.mypapit.mobile.myrepeater;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import net.mypapit.mobile.myrepeater.R;
import net.mypapit.mobile.myrepeater.mapinfo.CallsignMapInfo;
import net.sf.jfuzzydate.FuzzyDateFormat;
import net.sf.jfuzzydate.FuzzyDateFormatter;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class NearbyOperatorActivity extends Activity implements OnItemClickListener {

	private ListView listview;
	private TextView tvNearby;
	private HamOperatorList holist;
	private NearbyOperatorAdapter adapter;
	private static String URL = "http://api.repeater.my/v1/nearbyoperator.php";
	private double mlat, mlng;
	private String mlocation;




	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.nearby_operator_list);
		overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
		
		Bundle bundle = this.getIntent().getExtras();
		
		mlocation=bundle.getString("location");
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
		
		
		new GetUserInfo(this,new LatLng(mlat,mlng)).execute();
		
		adapter = new NearbyOperatorAdapter(this,holist);
		
		listview.setAdapter(adapter);
		
		listview.setOnItemClickListener(this);
		
		





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
		

		HamOperator hop =  adapter.getHamOperator(position);
		
		//phoneno,status,name,locality,client,callsign,time
		
		intent.putExtra("phoneno", hop.getPhoneno());
		intent.putExtra("status", hop.getStatus());
		intent.putExtra("locality", hop.getLocality());
		intent.putExtra("client", hop.getClient());
		intent.putExtra("callsign",hop.getCallsign());
		intent.putExtra("time",hop.getStrDate());
		intent.putExtra("name", hop.getHandle());
		
		
		
		startActivity(intent);
		
	}

	private class GetUserInfo extends AsyncTask<Void, Void, Void> {

		String currentlat, currentlng;
		Activity activity;

		GetUserInfo(Activity activity, LatLng latlng) {
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

			String jsonStr = sh.makeServiceCall(URL, ServiceHandler.GET, nameValuePairs);

			// Log.d("mypapit Json Response: ", "> " + jsonStr);

			//listrakanradio = new ArrayList<HashMap<String, String>>(200);
			JSONArray rakanradio = null;

			if (jsonStr != null) {
				try {
					// JSONObject jsonObj = new JSONObject(jsonStr);

					rakanradio = new JSONArray(jsonStr);
					int num_of_rakanradio = rakanradio.length();
					for (int i = 0; i < num_of_rakanradio; i++) {
						JSONObject jsinfo = rakanradio.getJSONObject(i);

						String callsign = jsinfo.getString("callsign");

						/*
						 * debug use only, now we shortcut String callsign =
						 * jsinfo.getString("callsign"); String name =
						 * jsinfo.getString("name"); String qsx =
						 * jsinfo.getString("qsx"); String status =
						 * jsinfo.getString("status");
						 * 
						 * String distance = jsinfo.getString("distance");
						 * String time = jsinfo.getString("time"); String lat =
						 * jsinfo.getString("lat"); String lng =
						 * jsinfo.getString("lng"); String valid =
						 * jsinfo.getString("valid"); String deviceid =
						 * jsinfo.getString("deviceid"); String phoneno =
						 * jsinfo.getString("phoneno"); String rmyclient =
						 * jsinfo.getString("client");
						 */

						String valid = jsinfo.getString("valid");

						HashMap<String, String> inforakanradio = new HashMap<String, String>();




						int validity = Integer.parseInt(valid);

					
						DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						java.util.Date time = new java.util.Date();

						try {
							time = formatter.parse(jsinfo.getString("time"));
						} catch (ParseException e) {
							time = new java.util.Date();

							e.printStackTrace();
						}

						double holat,holng;
						try {

							holat=Double.parseDouble(jsinfo.getString("lat"));
							holng=Double.parseDouble(jsinfo.getString("lng"));
						} catch (Exception ex){

							holat=37.0;
							holng=115.0;

						}
						/*
						(String callsign, String handle, String status, String phoneno, String locality, String client,
								String deviceid, String qsx, Date date, double lat, double lng, boolean valid
						 */

						HamOperator hamoperator = new HamOperator(
								callsign,
								jsinfo.getString("name"),
								jsinfo.getString("status"),
								jsinfo.getString("phoneno"),
								jsinfo.getString("locality"),
								jsinfo.getString("client"),
								jsinfo.getString("deviceid"),
								jsinfo.getString("qsx"),
								time,
								jsinfo.getString("time"),
								holat,
								holng,
								(validity < 1) ? false:true

								);

						try {
							hamoperator.setActiveNo(   Integer.parseInt(jsinfo.getString("total"))     );
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

			adapter = new NearbyOperatorAdapter(activity,holist);
			listview.setAdapter(adapter);
			adapter.notifyDataSetChanged();
			

		}

	}

}

