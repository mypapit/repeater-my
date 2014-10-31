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
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class RepeaterAdapter extends BaseAdapter implements Filterable {

	private static LayoutInflater inflater = null;
	private RepeaterList data, realdata;
	private int mLastPosition = -1;

	// private Activity activity;

	public RepeaterAdapter(Activity activity, RepeaterList rl) {
		// this.activity = activity;
		data = rl;
		realdata = rl;

		inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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

	public Repeater getRepeater(int position) {
		return data.get(position);
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
		// LayoutInflater inflater = (LayoutInflater)
		// activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		if (convertView == null) {
			vi = inflater.inflate(R.layout.repeater_row, null);
		} else {

			TranslateAnimation animation = null;
			if (position > mLastPosition) {
				animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
						Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f);

				animation.setDuration(600);
				convertView.startAnimation(animation);
				mLastPosition = position;
			}

		}

		TextView tvCallsign = (TextView) vi.findViewById(R.id.tvCallsign);
		TextView tvDistance = (TextView) vi.findViewById(R.id.tvDistance);
		TextView tvFreq = (TextView) vi.findViewById(R.id.tvFreq);
		TextView tvTone = (TextView) vi.findViewById(R.id.tvTone);
		TextView tvClub = (TextView) vi.findViewById(R.id.tvClub);
		TextView tvLocation = (TextView) vi.findViewById(R.id.tvLocation);
		TextView tvLink = (TextView) vi.findViewById(R.id.tvdLink);

		Repeater repeater = data.get(position);
		DecimalFormat nf = new DecimalFormat("#.00");

		tvCallsign.setText((repeater.getCallsign()));
		tvFreq.setText(Double.toString(repeater.getDownlink()) + " MHz (" + repeater.getShift() + ")");
		tvTone.setText(Double.toString(repeater.getTone()));
		tvLocation.setText(repeater.getLocation());
		tvClub.setText(repeater.getClub());
		if (repeater.getLink().length() > 0) {

			tvLink.setText("*link");
		} else {
			tvLink.setText("");
		}

		double distance = repeater.getDistance() / 1000.0;
		tvDistance.setText(nf.format(distance) + " km");

		if (distance > 99.5) {
			tvDistance.setTextColor(Color.rgb(200, 0, 0));

		} else {
			tvDistance.setTextColor(Color.rgb(0, 250, 0));

		}

		return vi;
	}

	@Override
	public Filter getFilter() {
		// TODO Auto-generated method stub

		return new Filter() {

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				// TODO Auto-generated method stub
				FilterResults results = new FilterResults();
				RepeaterList i = new RepeaterList();

				if (constraint != null && constraint.toString().length() > 2) {
					for (int index = 0; index < realdata.size(); index++) {
						Repeater repeater = realdata.get(index);
						if (repeater.getCallsign().contains(constraint.toString().toUpperCase())) {

							i.add(repeater);
						}

					}

					results.values = i;
					results.count = i.size();

				} else {
					results.values = realdata;
					results.count = realdata.size();

				}

				return results;
			}

			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				// TODO Auto-generated method stub
				data = (RepeaterList) results.values;

				RepeaterAdapter.this.notifyDataSetChanged();

			}

		};

	}

}
