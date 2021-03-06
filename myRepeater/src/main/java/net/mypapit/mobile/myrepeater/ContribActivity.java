//uses vierpagerindicator libary from Patrik Akerfeldt and Jake Wharton
/*
 * 

 MyRepeater Finder 
 Copyright 2013,2015,2016 Mohammad Hafiz bin Ismail <mypapit@gmail.com>
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

import android.content.res.Resources;
import android.os.Bundle;
//import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.io.InputStream;

public class ContribActivity extends AppCompatActivity {
    private TextView tvContrib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contrib);

        overridePendingTransition(R.anim.activity_fadein, R.anim.activity_fadeout);

        tvContrib = (TextView) findViewById(R.id.txtcontriblist);

        try {
            Resources res = this.getResources();

            InputStream inputStream = res.openRawResource(R.raw.contributor);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);


            tvContrib.setText(new String(buffer));

            inputStream.close();

        } catch (Exception e) {

            tvContrib.setText("Sorry, can't show contrib list");

        }

    }

    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);
    }

}
