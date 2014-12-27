package net.mypapit.mobile.myrepeater;

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

import java.util.List;
import java.util.Stack;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class RepeaterDetailsActivity extends Activity {
	private String[] repeater;
	private TextView tvCallsign, tvFreq, tvShift, tvTone, tvLocation, tvClub, tvDistance;
	Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext=this.getApplicationContext();

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		setContentView(R.layout.repeater_detail_layout);
		overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);

		repeater = (String[]) getIntent().getExtras().get("Repeater");
		if (repeater == null) {
			repeater = new String[] { "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A" };

		}

		tvCallsign = (TextView) findViewById(R.id.tvdCallsign);
		tvClub = (TextView) findViewById(R.id.tvdClub);
		tvFreq = (TextView) findViewById(R.id.tvdFreq);
		tvShift = (TextView) findViewById(R.id.tvdShift);
		tvLocation = (TextView) findViewById(R.id.tvdLocation);
		tvTone = (TextView) findViewById(R.id.tvdTone);
		tvDistance = (TextView) findViewById(R.id.tvdDistance);

		tvCallsign.setText(repeater[0]);
		tvClub.setText(repeater[1]);
		tvFreq.setText("" + repeater[2] + " MHz");
		tvShift.setText("" + repeater[3] + " MHz");
		tvLocation.setText(repeater[4]);
		tvTone.setText("" + repeater[5] + " Hz");
		tvDistance.setText("" + repeater[6] + " km");

		AdView mAdView = (AdView) findViewById(R.id.adView);

		mAdView.loadAd(new AdRequest.Builder().build());

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.repeater_details, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;
		switch (item.getItemId()) {
		case R.id.action_sharedetails:
			intent = new Intent(android.content.Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TITLE, repeater[0]);
			intent.putExtra(Intent.EXTRA_TEXT, "Repeater Callsign :" + repeater[0] + " (" + repeater[1] + ")"
					+ "\nLocation : " + repeater[4] + "\nFrequency : " + repeater[2] + " MHz ,Shift : " + repeater[3]
					+ " MHz, Tone : " + repeater[5]);

			startActivity(Intent.createChooser(intent, "Send via "));
			return true;

		case R.id.action_wrongInfo:
			showRepeaterCorrectionDialog();
			

			return true;
		case android.R.id.home:
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
				//NavUtils.navigateUpFromSameTask(this);
				finish();
			}

			return true;

		}

		return false;
	}

	/*
	 * I don't write this, but it is a handy function to focus on only sending Email - mypapit (Christmas day 2014)
	 */
	public Intent createEmailOnlyChooserIntent(Intent source, CharSequence chooserTitle) {
		Stack<Intent> intents = new Stack<Intent>();
		Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "mypapit+repeater_correction@gmail.com",
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

	protected void onPause() {
		super.onPause();

		overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);

	}
	
	public void showRepeaterCorrectionDialog(){
		CharSequence[] items={"Callsign","Frequency","Shift","Tone","Club","Location"};
		boolean [] states = {false,false,false,false,false,false};
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select details that needs correction");
		builder.setMultiChoiceItems(items,states,new DialogInterface.OnMultiChoiceClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				// TODO Auto-generated method stub
				
			}
		});
		
		builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
					SparseBooleanArray checked = ( (AlertDialog) dialog ).getListView().getCheckedItemPositions();
					
					/*
					 * replaced by additional CorrectActivity screen.
					submitToEmail(checked.get(0),checked.get(1),checked.get(2),
							checked.get(3),checked.get(4),checked.get(5));
																		
							
				    */
					
					boolean selected= checked.get(0)|checked.get(1)|checked.get(2)|checked.get(3)|checked.get(4)|checked.get(5);
					
					if (selected) {
											
							submitToCorrect(checked,repeater);
					} else {
						Toast x = Toast.makeText(mContext, "Please select at least one detail", Toast.LENGTH_SHORT);
						x.show();
						
						
					}
					
					
					
				
				
			}
		});
		
		builder.create().show();
		
		
	}
	
	public void submitToEmail(boolean callsign, boolean freq, boolean shift, boolean tone, boolean club, boolean location){
		Intent emailIntent = new Intent(Intent.ACTION_SEND);

		emailIntent.setType("text/plain");
		emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "mypapit+wrong_info_repeater_suggest@gmail.com" });

		emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Repeater.MY - Info Repeater " + repeater[0]);
		emailIntent.putExtra(Intent.EXTRA_TEXT,
				"Please put the repeater details you want to suggest --\n\nRepeater Callsign : " + repeater[0] +(callsign ? "*":"")
						+ "\nFreq: " + repeater[2] + (freq ? "*":"")+ "\nShift: " + repeater[3] +(shift ? "*":"")+ "\nTone: " + repeater[5] + (tone ? "*":"")
						+ "\nClosest Known Location or Coordinates: " + repeater[4] +(location ? "*":"")+ "\nOwner or Club: "
						+ repeater[1] + (club ? "*":"")+ "\n");
		//startActivity(emailIntent);
		
		startActivity(createEmailOnlyChooserIntent(emailIntent, "Suggest Correction"));
		
	}
	
	public void submitToCorrect(SparseBooleanArray checked, String[] repeater){
		 boolean check[] = {checked.get(0),checked.get(1),checked.get(2),checked.get(3),checked.get(4),checked.get(5)};
		Intent intent = new Intent(this.getApplicationContext(),CorrectActivity.class);
		intent.putExtra("repeater", repeater);
		intent.putExtra("options", check);
		
		startActivity(intent);
		 
		
	}

}
