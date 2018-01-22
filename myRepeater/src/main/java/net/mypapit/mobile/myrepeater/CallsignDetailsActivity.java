package net.mypapit.mobile.myrepeater;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
//import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import net.sf.jfuzzydate.FuzzyDateFormat;
import net.sf.jfuzzydate.FuzzyDateFormatter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class CallsignDetailsActivity extends AppCompatActivity {

    View dividerPhone, titlePhone;
    ImageButton btnCall;
    int jokecounter;
    private TextView tvName;
    private TextView tvClient;
    private TextView tvDistance;
    private TextView tvLocality;
    private TextView tvLastSeen;
    private TextView tvPhone;
    private TextView tvStatus;
    private String m_callsign;
    private String m_deviceid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.callsign_detail_layout);

        AdView mAdView = (AdView) findViewById(R.id.adViewcallsign);
        mAdView.loadAd(new AdRequest.Builder().setGender(AdRequest.GENDER_MALE).build());

        Bundle bundle = getIntent().getExtras();

        jokecounter = 0;

        tvName = (TextView) findViewById(R.id.tvcmiHandle);
        tvClient = (TextView) findViewById(R.id.tvcmiClient);

        tvLocality = (TextView) findViewById(R.id.tvcmiLocality);
        tvPhone = (TextView) findViewById(R.id.tvcmiPhone);
        tvLastSeen = (TextView) findViewById(R.id.tvcmiLastSeen);
        tvStatus = (TextView) findViewById(R.id.tvcmiStatus);
        dividerPhone = findViewById(R.id.DividercmiPhone);
        titlePhone = findViewById(R.id.TitlecmiPhone);
        btnCall = (ImageButton) findViewById(R.id.btnCall);


        String phoneNo = bundle.getString("phoneno");
        String status = bundle.getString("status");

        tvName.setText(bundle.getString("name"));
        // tvDistance.setText(bundle.getString("distance"));
        tvLocality.setText(bundle.getString("locality"));
        tvClient.setText(bundle.getString("client"));

        if ((phoneNo.length() < 3) || (phoneNo.equalsIgnoreCase("+60120000"))) {
            tvPhone.setVisibility(View.GONE);
            dividerPhone.setVisibility(View.GONE);
            btnCall.setVisibility(View.GONE);
            titlePhone.setVisibility(View.GONE);

        }

        tvStatus.setText(status);

        if (status.length() < 3) {
            tvStatus.setText("No Status");
        }
        tvPhone.setText(bundle.getString("phoneno"));

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        java.util.Date time;
        FuzzyDateFormatter format = FuzzyDateFormat.getInstance();

        try {
            time = formatter.parse(bundle.getString("time"));
        } catch (ParseException e) {
            time = new java.util.Date();

            e.printStackTrace();
        }

        tvLastSeen.setText(format.formatDistance(time));

        overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
        m_callsign = bundle.getString("callsign", "");
        m_deviceid = bundle.getString("deviceid", "");


        android.support.v7.app.ActionBar ab = this.getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        ab.setTitle(m_callsign);


        btnCall.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + tvPhone.getText().toString()));
                startActivity(intent);

            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_callsign_detailsx, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:


                finish();

                return true;
            case R.id.btnCall:
                intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + tvPhone.getText().toString()));
                startActivity(intent);
                return true;

            case R.id.action_callsign_stats:
                intent = new Intent(this, OperatorStatsActivity.class);
                intent.putExtra("callsign", m_callsign);
                intent.putExtra("deviceid", m_deviceid);
                startActivity(intent);
                return true;


        }
        return false;
    }

    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);

    }

}
