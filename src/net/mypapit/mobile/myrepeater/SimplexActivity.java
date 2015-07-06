/*
 * 
	MyRepeater Finder 
	Copyright 2014, 2015 Mohammad Hafiz bin Ismail <mypapit@gmail.com>
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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import au.com.bytecode.opencsv.CSVReader;

public class SimplexActivity extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simplex);
		overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}


		ListView lv = (ListView) findViewById(R.id.lvSimplex);

		List<Map<String, String>> simplexData = this.loadSimplexData(getResources().openRawResource(R.raw.simplex)); 

		SimpleAdapter adapter = new SimpleAdapter(this, simplexData,android.R.layout.simple_list_item_2,new String[]{"V","Freq"},new int[]{android.R.id.text1,android.R.id.text2});
		lv.setAdapter(adapter);


	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
				finish();
			}
			return true;

		}
		return false;

	}

	public List<Map<String, String>> loadSimplexData(InputStream stream)
	{
		List<Map<String, String>> datalist = new ArrayList<Map<String,String>>();
		int line=0;
		try {
			InputStreamReader is = new InputStreamReader(stream);
			BufferedReader in = new BufferedReader(is);
			CSVReader csv = new CSVReader(in, ',', '\"', 0);



			String data[];
			Map <String,String> simplexfreq;

			while ((data = csv.readNext()) != null) {
				line++;
				simplexfreq = new HashMap<String,String>();

				simplexfreq.put("Freq",data[0]+" MHz");
				simplexfreq.put("V",data[1]);

				Log.d("Simplex net.mypapit","data[0]: " + data[0]);
				datalist.add(simplexfreq);
			}



			in.close();

		} catch (IOException ioe) {
			Log.e("Read CSV Error mypapit", "Some CSV Error: ", ioe.getCause());

		} catch (NumberFormatException nfe) {
			Log.e("Number error", "parse number error - line: " + line + "  " + nfe.getMessage(), nfe.getCause());
		} catch (Exception ex) {
			Log.e("Some Exception", "some exception at line :" + line + " \n " + ex.getCause());
			ex.printStackTrace(System.err);
		}

		return datalist;

	}


}
