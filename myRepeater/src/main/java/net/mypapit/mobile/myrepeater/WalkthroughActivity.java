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

//uses vierpagerindicator libary from Patrik Akerfeldt and Jake Wharton

import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.ViewPagerAdapter.ViewPagerAdapter;
import com.viewpagerindicator.CirclePageIndicator;

public class WalkthroughActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walkthrough);

        CirclePageIndicator indicator;
        ViewPager viewPager;
        PagerAdapter adapter;

        String title[] = {"What's New?", "Get your position on Map!", "No GPS? No Problem", "Incorrect Info?",
                "Start using Repeater.MY!"};
        String description[] = {
                "Repeater.MY 2.1.x introduces several exciting new features:\n\n* Map Position reporting\n* Repeater Direction (Compass)\n* Manual Location selection\n\nSwipe to continue... =)",
                "Now you can see other users on Map!\n\nTo use it, just fill in your info in \"Map Positioning Report\" settings and you are good to go!\n\np/s: You need to key in your APRS passcode in order to be verified though.\n\n Enjoy!",
                "Repeater.MY can also work without GPS.\n\nJust select \"Set Location...\" from menu and disable Location Services, then you are set to go!\n\n*It is still recommended that you enable Location Service/GPS to fully enjoy Repeater.MY capabilities ",
                "Repeater information can get out-of-date all the time! \n\nHowever, you can contribute to the community by suggesting new repeater information. \n\nFrequent contributors will be featured in the \'Contributors List\'",
                "That's it! \nHopefully, you would enjoy all the latest features... \n\nStart using Repeater.MY now!"};
        int icon[] = {R.drawable.ic_launcher, R.drawable.map_icon, R.drawable.arrowtw, R.drawable.mapmarkertw,
                R.drawable.qmarktw, R.drawable.ic_launcher};
        boolean button[] = {false, false, false, false, true, true};

        adapter = new ViewPagerAdapter(this, title, description, icon, button);
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(adapter);

        indicator = (CirclePageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(viewPager);

    }

}
