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

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;

import android.widget.TextView;

/**
 * 
 */

/**
 * @author mypapit
 * 
 */
public class StaticLocationAdapter extends BaseAdapter implements SectionIndexer {

	/**
	 * 
	 */

	private ArrayList<StaticLocation> list;
	private Context activity;
	private final String[] states = new String[] { "#", "Johor", "Kedah", "Kelantan", "Kuala Lumpur", "Melaka",
			"N. Sembilan", "Pahang", "Penang", "Perlis", "Putrajaya", "Sabah", "Sarawak", "Selangor", "Terengganu" };

	public StaticLocationAdapter(ArrayList<StaticLocation> list, Context context) {
		super();

		this.list = list;
		this.activity = context;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		return list.size();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public Object getItem(int position) {

		return list.get(position);
	}

	public StaticLocation getStaticLocation(int position) {
		return list.get(position);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int position) {

		return position;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getView(int, android.view.View,
	 * android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		LayoutInflater inflater = LayoutInflater.from(activity);

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.static_location_row, parent, false);

		}

		// Log.d("mypapit-static-location","Static Location List size: "+list.size());

		TextView tvsname = (TextView) convertView.findViewById(R.id.tvsname);
		TextView tvsstate = (TextView) convertView.findViewById(R.id.tvsstate);
		StaticLocation location = list.get(position);

		tvsname.setText(location.getName());
		tvsstate.setText(location.getStatename());

		return convertView;
	}

	@Override
	public Object[] getSections() {

		return states;
	}

	@Override
	public int getPositionForSection(int section) {

		int numItems = this.getCount();

		for (int i = 0; i < numItems; i++) {

			if (this.list.get(i).getStatename().equalsIgnoreCase(states[section])) {
				return i;
			}

		}

		return 0;
	}

	@Override
	public int getSectionForPosition(int position) {
		/*
		 * String name=this.list.get(position).getStatename();
		 * 
		 * int len = states.length;
		 * 
		 * for (int i=0;i<len;i++){ if (states[i].equalsIgnoreCase(name)) {
		 * return i; } }
		 */

		return 0;
	}

}
