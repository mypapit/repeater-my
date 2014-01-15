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

package net.mypapit.mobile.myrepeater;
import java.text.DecimalFormat;


import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class RepeaterAdapter extends BaseAdapter {

	private static LayoutInflater inflater=null;
	private RepeaterList data;
	//private Activity activity;



	public RepeaterAdapter(Activity activity, RepeaterList rl){
		//this.activity = activity;
		data = rl;
		inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);


	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return data.size();

	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View vi = convertView;

		if (convertView ==null) {
			vi = inflater.inflate(R.layout.repeater_row, null);
		}

		TextView tvCallsign = (TextView) vi.findViewById(R.id.tvCallsign);
		TextView tvDistance = (TextView) vi.findViewById(R.id.tvDistance);
		TextView tvFreq = (TextView) vi.findViewById(R.id.tvFreq);
		TextView tvTone = (TextView) vi.findViewById(R.id.tvTone);
		TextView tvClub = (TextView) vi.findViewById(R.id.tvClub);
		TextView tvLocation = (TextView) vi.findViewById(R.id.tvLocation);


		Repeater repeater = data.get(position);




		DecimalFormat nf = new DecimalFormat("#.00");


		tvCallsign.setText( (repeater.getCallsign()));
		tvFreq.setText(Double.toString(repeater.getDownlink())+" MHz (" + repeater.getShift() +")");
		tvTone.setText(Double.toString(repeater.getTone()));
		tvLocation.setText(repeater.getLocation());
		tvClub.setText(repeater.getClub());

		double distance = repeater.getDistance()/1000.0; 
		tvDistance.setText(nf.format(distance)+" km");

		if (distance>99.5){
			tvDistance.setTextColor(Color.rgb(200, 0, 0));

		} else {
			tvDistance.setTextColor(Color.rgb(0, 250, 0));

		}





		return vi;
	}

}
