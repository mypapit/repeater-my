/*
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

import garin.artemiy.compassview.library.CompassView;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

public class RepeaterAdapter extends BaseAdapter implements Filterable,SectionIndexer {

	private static LayoutInflater inflater = null;
	private RepeaterList data, realdata;
	private int mLastPosition = -1;
	float local_distance;
	boolean excludeLink, excludeDirection;
	private Repeater userLocation;
	private static final int[] interval= new int[]{0,25,50,75,100,150,200,250,300,350,400,500,600,700,800,900,1000,1250,1500,1750};

	private Activity activity;

	public RepeaterAdapter(Activity activity, RepeaterList rl, Repeater userLocation, float local_distance,
			boolean excludeLink, boolean excludeDirection) {
		this.activity = activity;
		this.userLocation = userLocation;
		data = rl.filterLink(rl, excludeLink);
		realdata = rl.filterLink(rl, excludeLink);

		
		this.excludeDirection = excludeDirection;

		this.excludeLink = excludeLink;
		this.local_distance = local_distance;

		SharedPreferences repeater_prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		if (repeater_prefs == null) {
			Toast shit = Toast.makeText(activity, "repeater_prefs null", Toast.LENGTH_SHORT);
			shit.show();

		}

		this.excludeLink = repeater_prefs.getBoolean("excludeLinkRepeater", false);
		// local_distance=repeater_prefs.getString("repeater_local_distance",
		// "100");
		this.local_distance = repeater_prefs.getInt("range", 150);

		inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	}

	@Override
	public int getCount() {

		return data.size();

	}

	@Override
	public Object getItem(int position) {
		//
		return data.get(position);
	}

	public Repeater getRepeater(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {

		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.repeater_row, parent, false);
			holder = new ViewHolder();
			holder.tvCallsign = (TextView) convertView.findViewById(R.id.tvCallsign);
			holder.tvDistance = (TextView) convertView.findViewById(R.id.tvDistance);
			
			holder.tvFreq = (TextView) convertView.findViewById(R.id.tvFreq);
			holder.tvTone = (TextView) convertView.findViewById(R.id.tvTone);
			holder.tvClub = (TextView) convertView.findViewById(R.id.tvClub);
			holder.tvLocation = (TextView) convertView.findViewById(R.id.tvLocation);
			holder.tvLink = (TextView) convertView.findViewById(R.id.tvdLink);
			
			if (!excludeDirection) {
				holder.compassView= (CompassView) convertView.findViewById(R.id.compassView);
			}

			
			convertView.setTag(holder);
			
		} else {

			Animation animation = AnimationUtils.loadAnimation(activity,
					(position > mLastPosition) ? R.anim.up_from_bottom : R.anim.down_from_bottom);
			convertView.startAnimation(animation);
			mLastPosition = position;
			holder = (ViewHolder) convertView.getTag();

		}

		
		Repeater repeater = data.get(position);
		DecimalFormat nf = new DecimalFormat("#.00");

		/*
		 * if ( (repeater.getLink().length()>0) && excludeLink ){ return
		 * inflater.inflate(R.layout.repeater_row, null);
		 * 
		 * 
		 * }
		 */
		Log.d("mypapit.excludeLink", "mypapit.local_distance: " + local_distance);

		holder.tvCallsign.setText((repeater.getCallsign()));
		holder.tvFreq.setText(Double.toString(repeater.getDownlink()) + " MHz (" + repeater.getShift() + ")");
		holder.tvTone.setText(Double.toString(repeater.getTone()));
		holder.tvLocation.setText(repeater.getLocation());
		holder.tvClub.setText(repeater.getClub());
		if (repeater.getLink().length() > 0) {

			holder.tvLink.setText("*link");
		} else {
			holder.tvLink.setText("");
		}

		double distance = repeater.getDistance() / 1000.0;
		holder.tvDistance.setText(nf.format(distance) + " km");

		if (distance > local_distance) {
			holder.tvDistance.setTextColor(Color.rgb(200, 0, 0));

		} else {
			holder.tvDistance.setTextColor(Color.rgb(0, 250, 0));

		}
		
		

		if (!excludeDirection) {
			holder.compassView.initializeCompass(userLocation, repeater, R.drawable.mediumarrow);
		}

		return convertView;
	}

	@Override
	public Filter getFilter() {


		return new Filter() {

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				
				FilterResults results = new FilterResults();
				RepeaterList list = new RepeaterList();

				if (constraint != null && constraint.toString().length() > 2) {
					for (int index = 0; index < realdata.size(); index++) {
						Repeater repeater = realdata.get(index);
						if (repeater.getCallsign().contains(constraint.toString().toUpperCase())) {

							list.add(repeater);

						}

					}

					results.values = list;
					results.count = list.size();

				} else {
					results.values = realdata;
					results.count = realdata.size();

				}

				return results;
			}

			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				data = (RepeaterList) results.values;
				RepeaterAdapter.this.notifyDataSetChanged();

			}

		};

	}

	static class ViewHolder {

		TextView tvCallsign;
		TextView tvDistance;
		TextView tvFreq;
		TextView tvTone;
		TextView tvClub;
		TextView tvLocation;
		TextView tvLink;
		CompassView compassView;

	}

	@Override
	public Object[] getSections() {
		
		
		 
		int i=interval.length;
		String strSection[] = new String[i];
		
		for (int j=0; j<i;j++){
			strSection[j] = ""+interval[j]+" km";
			
		}
	
		return strSection;

		
	}

	@Override
	public int getPositionForSection(int section) {
		int numOfItems= this.getCount();
		
		for (int i=0;i<numOfItems;i++){
			double distance = this.getRepeater(i).getDistance()/1000.0; 
			
			if ( (distance+9.5) >= interval[section]){
				return i;
			}
		}
	
		return 0;
	}

	@Override
	public int getSectionForPosition(int position) {
	
		return 0;
	}
}
