package net.mypapit.mobile.myrepeater;
import java.util.List;
import java.util.Map;

import net.mypapit.mobile.myrepeater.R;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class NearbyOperatorActivity extends Activity {

	private ListView listview;
	private TextView tvNearby;
	private HamOperatorList holist;
	

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.nearby_operator_list);
		overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);


		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		tvNearby = (TextView) findViewById(R.id.tvNearbyHamOp);

		listview = (ListView) findViewById(R.id.HamOperatorListView);
		
		holist = new HamOperatorList(120);
		




	}




}