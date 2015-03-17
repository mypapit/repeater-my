package net.mypapit.mobile.myrepeater;

//uses vierpagerindicator libary from Patrik Akerfeldt and Jake Wharton

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

import com.ViewPagerAdapter.ViewPagerAdapter;
import com.viewpagerindicator.CirclePageIndicator;

import android.app.Activity;
import android.os.Bundle;


public class WalkthroughActivity extends Activity {
	String title[],description[];
	int icon[];
	boolean button[];
	CirclePageIndicator indicator;
	ViewPager viewPager;
	PagerAdapter adapter;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_walkthrough);
		
		String title[] = {"Welcome!","What's New?","Compass", "No GPS? No Problem", "Incorrect Info?","Start using Repeater.MY!" };
		String description[] = {"This is a walkthrough to introduce you to Repeater.MY new features.\n\nSwipe to continue", 
				"Repeater.MY 2.0.x introduces several exciting new features:\n\n* Repeater Direction (Compass)\n* Manual Location selection\n* Repeater Information Suggestion",
				"You can now determine Amateur radio repeater direction. This is useful for those who wants to tune their radio in order to get the clearest signal. Just remember that compass may not be available in the following situation: \n\n* Your device does not have built-in compass\n* You disabled Location Services or GPS\n* You use Manual Location selection\n\n To get accurate reading from compass, just lay your device flat on your palm or lay it on the table.",
				"Repeater.MY can still work even if you do not have GPS. Just select \"Set Location...\" from menu and disable Location Services, then you are set to go!\n\n*It is still recommended that you use Location Service/GPS in order to fully utilize Repeater.MY capabilities ",
				"Repeater information can get out-of-date all the time! \n\nHowever, you can still contribute to the community by suggesting the correct repeater information. \n\nFrequent contributors will be featured in the \'Contributors List\'",
				
				"That's it! \nHopefully, you would enjoy all the latest features... \n\nStart using Repeater.MY now!"};
		int icon[] = {R.drawable.ic_launcher,R.drawable.ic_launcher,R.drawable.arrowtw,R.drawable.mapmarkertw,R.drawable.qmarktw,R.drawable.ic_launcher};
		boolean button[] = {false,false,false,false,false,true};
		
		
		
		adapter = new ViewPagerAdapter(this, title,description,icon,button);
		viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setAdapter(adapter);
		
		indicator = (CirclePageIndicator) findViewById(R.id.indicator);
		indicator.setViewPager(viewPager);
		
		
		
		
	}
	
	

	
}
