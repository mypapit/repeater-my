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
public class StaticLocationAdapter extends BaseAdapter {

	/**
	 * 
	 */
	
	private ArrayList<StaticLocation> list;
	private Context activity;
	
	public StaticLocationAdapter(ArrayList<StaticLocation> list,Context context) {
		super();
		
		this.list=list;
		this.activity=context;
		
		
		
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		return list.size();
		
		
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return list.get(position);
	}
	
	public StaticLocation getStaticLocation(int position){
		return list.get(position);
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View vi = convertView;
		//LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LayoutInflater inflater = LayoutInflater.from(activity);
		
		
		if (convertView ==null) {
			vi = inflater.inflate(R.layout.static_location_row, null);
		}
		
		//Log.d("mypapit-static-location","Static Location List size: "+list.size());
		
		TextView tvsname= (TextView) vi.findViewById(R.id.tvsname);
		TextView tvsstate = (TextView) vi.findViewById(R.id.tvsstate);
		StaticLocation location = list.get(position);
		
		tvsname.setText(location.getName());
		tvsstate.setText(location.getStatename());
		

		
		return vi;
	}

	

}
