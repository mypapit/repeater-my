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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
//import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import au.com.bytecode.opencsv.CSVReader;

public class StaticLocationActivity extends AppCompatActivity {

    private StaticLocationAdapter adapter;
    private Activity localActivity;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.static_location);
        overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        localActivity = this;

        ArrayList<StaticLocation> list = this.loadData(R.raw.staticlocation);

        ListView lv = (ListView) findViewById(R.id.lvStatic);
        Button btnEnable = (Button) findViewById(R.id.btnEnable);
        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.enable_bounce);
        btnEnable.setAnimation(animation);

        adapter = new StaticLocationAdapter(list, this);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                StaticLocation sl = adapter.getStaticLocation(position);

                SharedPreferences.Editor editor = getSharedPreferences("Location", MODE_PRIVATE).edit();
                editor.putFloat("DefaultLat", (float) sl.getLat());
                editor.putFloat("DefaultLon", (float) sl.getLon());

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM");

                // need to put token to avoid app from popping up annoying
                // select manual location dialog
                // token = today's date in dd/MM format
                editor.putString("token", dateFormat.format(new Date()));
                editor.apply();

                NavUtils.navigateUpFromSameTask(localActivity);

            }

        });

        btnEnable.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // Open up Location Services settings

                Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                getApplicationContext().startActivity(intent);
            }

        });

        lv.setFastScrollEnabled(true);
        lv.setVerticalScrollBarEnabled(true);

    }

    public ArrayList<StaticLocation> loadData(int resource) {
        ArrayList<StaticLocation> locationList = new ArrayList<StaticLocation>();
        int line = 0;
        try {

            InputStreamReader is = new InputStreamReader(this.getResources().openRawResource(resource));
            BufferedReader in = new BufferedReader(is);
            CSVReader csv = new CSVReader(in, ';', '\"', 0);
            String data[];
            while ((data = csv.readNext()) != null) {
                line++;
                locationList.add(new StaticLocation(data[0], data[1], Double.parseDouble(data[2]), Double
                        .parseDouble(data[3])));

            }
            in.close();

        } catch (IOException ioe) {
            Toast.makeText(this, "IOException: Couldn't read static location file", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException nfe) {
            Toast.makeText(this, "NumberFormatException: Couldn't read long/lat at line: " + line, Toast.LENGTH_SHORT)
                    .show();
        } catch (Exception ex) {
            Toast.makeText(this, ex.toString() + ": at line -" + line, Toast.LENGTH_SHORT).show();
            Log.e("mypapit-static-location", ": at line -" + line);
            ex.printStackTrace(System.err);

        }

        class LocationNameComparator implements Comparator<StaticLocation> {

            @Override
            public int compare(StaticLocation lhs, StaticLocation rhs) {
                return lhs.getStatename().compareTo(rhs.getStatename());

            }

        }

        Collections.sort(locationList, new LocationNameComparator());

        return locationList;

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:

                NavUtils.navigateUpFromSameTask(this);

                return true;

        }
        return false;

    }

    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);
    }

}
