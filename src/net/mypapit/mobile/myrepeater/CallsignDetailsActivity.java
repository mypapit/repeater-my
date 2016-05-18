package net.mypapit.mobile.myrepeater;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import net.sf.jfuzzydate.FuzzyDateFormat;
import net.sf.jfuzzydate.FuzzyDateFormatter;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class CallsignDetailsActivity extends Activity {

	TextView tvName, tvClient, tvDistance, tvLocality, tvLastSeen, tvPhone, tvStatus;

	View dividerPhone, titlePhone;
	ImageButton btnCall, btnStats;
	
	int jokecounter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.callsign_detail_layout);

		AdView mAdView = (AdView) findViewById(R.id.adViewcallsign);
		mAdView.loadAd(new AdRequest.Builder().build());

		Bundle bundle = getIntent().getExtras();
		
		jokecounter=0;

		tvName = (TextView) findViewById(R.id.tvcmiHandle);
		tvClient = (TextView) findViewById(R.id.tvcmiClient);

		tvLocality = (TextView) findViewById(R.id.tvcmiLocality);
		tvPhone = (TextView) findViewById(R.id.tvcmiPhone);
		tvLastSeen = (TextView) findViewById(R.id.tvcmiLastSeen);
		tvStatus = (TextView) findViewById(R.id.tvcmiStatus);
		dividerPhone = findViewById(R.id.DividercmiPhone);
		titlePhone = findViewById(R.id.TitlecmiPhone);
		btnCall = (ImageButton) findViewById(R.id.btnCall);
		btnStats = (ImageButton) findViewById(R.id.btnStats);
		
		
		

		/*
		 * intent.putExtra("name", inforakanradio.get("name"));
		 * intent.putExtra("status", inforakanradio.get("status"));
		 * intent.putExtra("distance", inforakanradio.get("distance"));
		 * intent.putExtra("time", inforakanradio.get("time"));
		 * intent.putExtra("deviceid", inforakanradio.get("deviceid"));
		 * intent.putExtra("phoneno", inforakanradio.get("phoneno"));
		 * intent.putExtra("client", inforakanradio.get("client"));
		 * intent.putExtra("locality", inforakanradio.get("locality"));
		 */

		String phoneNo = new String(bundle.getString("phoneno"));
		String status = new String(bundle.getString("status"));

		tvName.setText(bundle.getString("name"));
		// tvDistance.setText(bundle.getString("distance"));
		tvLocality.setText(bundle.getString("locality"));
		tvClient.setText(bundle.getString("client"));

		if ((phoneNo.length() < 3) || (phoneNo.equalsIgnoreCase("+60120000"))) {
			tvPhone.setVisibility(View.GONE);
			dividerPhone.setVisibility(View.GONE);
			btnCall.setVisibility(View.GONE);
			titlePhone.setVisibility(View.GONE);

		}

		tvStatus.setText(status);

		if (status.length() < 3) {
			tvStatus.setText("No Status");
		}
		tvPhone.setText(bundle.getString("phoneno"));

		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		java.util.Date time = new java.util.Date();
		FuzzyDateFormatter format = FuzzyDateFormat.getInstance();

		try {
			time = formatter.parse(bundle.getString("time"));
		} catch (ParseException e) {
			time = new java.util.Date();

			e.printStackTrace();
		}

		tvLastSeen.setText(format.formatDistance(time));

		overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
		String callsign = bundle.getString("callsign", "9W2-error");

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			ActionBar ab = this.getActionBar();
			ab.setDisplayHomeAsUpEnabled(true);

			ab.setTitle(callsign);

		}

		if (Build.VERSION.SDK_INT >= 14) {
			// Version 4+
			int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
			TextView abTitle = (TextView) findViewById(titleId);
			if (abTitle != null) {
				abTitle.setText(callsign);

			}
		} else {
			// Other versions
			TextView abTitle = (TextView) getWindow().findViewById(android.R.id.title);
			if (abTitle != null) {
				abTitle.setText(callsign);
			}
		}

		btnCall.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {

				Intent intent = new Intent(Intent.ACTION_DIAL);
				intent.setData(Uri.parse("tel:" + tvPhone.getText().toString()));
				startActivity(intent);

			}
		});
		
		btnStats.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
					
				if (jokecounter < 4){
					Toast.makeText(getApplicationContext(), "Not implemented yet, sorry :(", Toast.LENGTH_SHORT).show();
					
				} else if (jokecounter <8){
					
					Toast.makeText(getApplicationContext(), "Hey, not implemented yet!!", Toast.LENGTH_SHORT).show();
				} else if (jokecounter <12) {
					
					Toast.makeText(getApplicationContext(), "OMGWTF!!! Get it through your skull, not implemented yet!", Toast.LENGTH_SHORT).show();
				} else {
					
					Toast.makeText(getApplicationContext(), "Wait for the next release - not implemented yet!", Toast.LENGTH_SHORT).show();
				}
				
				jokecounter++;
				
				
			}
		});

	
	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.callsign_details, menu);
		return true;
	}

	@Override
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
			return true;
		case R.id.btnCall:
			Intent intent = new Intent(Intent.ACTION_DIAL);
			intent.setData(Uri.parse("tel:" + tvPhone.getText().toString()));
			startActivity(intent);
			return true;

		}
		return false;
	}

	protected void onPause() {
		super.onPause();
		overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);

	}

}
