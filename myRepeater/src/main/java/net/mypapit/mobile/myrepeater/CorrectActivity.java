/*
 * 

	MyRepeater Finder 
	Copyright 2013, 2015, 2016 Mohammad Hafiz bin Ismail <mypapit@gmail.com>
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

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
//import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Stack;

public class CorrectActivity extends AppCompatActivity {

    private final String club[] = {"ASTRA", "AKRAB", "ARECTMJ", "MARTS", "MARES", "JASRA", "ARCS", "NESRAC",
            "PEMANCAR", "PERAMAH", "ARECS", "SARES", "UNKNOWN", "OTHERS"};
    private EditText etcFreq, etcShift, etcTone, etcLocation, etcNote;
    private AutoCompleteTextView etcClub;
    private TextView etcCallsign;
    private String[] repeater;
    private boolean[] checked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);

        setContentView(R.layout.correct_repeater);

        etcCallsign = (TextView) findViewById(R.id.etcCallsign);
        etcFreq = (EditText) findViewById(R.id.etcFreq);
        etcShift = (EditText) findViewById(R.id.etcShift);
        etcTone = (EditText) findViewById(R.id.etcTone);
        etcClub = (AutoCompleteTextView) findViewById(R.id.etcClub);
        etcLocation = (EditText) findViewById(R.id.etcLocation);
        etcNote = (EditText) findViewById(R.id.etcNote);

        Intent tempIntent = this.getIntent();
        repeater = (String[]) tempIntent.getExtras().get("repeater");
        checked = (boolean[]) tempIntent.getExtras().get("options");

        if (repeater == null) {
            repeater = new String[]{"N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A"};

        }

        etcCallsign.setText(repeater[0]);
        etcClub.setText(repeater[1]);
        etcFreq.setText(repeater[2]);
        etcShift.setText(repeater[3]);
        etcLocation.setText(repeater[4]);
        etcTone.setText(repeater[5]);

        // etcCallsign.setEnabled(!checked[0]);
        etcFreq.setEnabled(checked[1]);
        etcShift.setEnabled(checked[2]);
        etcTone.setEnabled(checked[3]);
        etcClub.setEnabled(checked[4]);
        etcLocation.setEnabled(checked[5]);

        ArrayAdapter<String> adapterClub = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, club);
        etcClub.setAdapter(adapterClub);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_suggest, menu);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_suggest_done:
                if (etcNote.getText().toString().length() < 3) {
                    Toast.makeText(this, "Please include note", Toast.LENGTH_SHORT).show();

                    return false;
                }

                this.submitToEmail(checked);
                return true;
            case android.R.id.home:


                finish();

                return true;

        }
        return false;
    }

    public void submitToEmail(boolean[] checked) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"repeaterbot+wrong_info_repeater_suggest@gmail.com"});

        String versionName;
        try {
            // workaround to identify Repeater.MY version in email reports
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;

        } catch (NameNotFoundException nnfe) {
            Toast.makeText(this, "Problem retrieving version name", Toast.LENGTH_SHORT).show();
            versionName = "Unknown";

        }

        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Repeater.MY - Info Repeater " + repeater[0]);
        emailIntent.putExtra(
                Intent.EXTRA_TEXT,
                "Please put the repeater details you want to correct --\n\nRepeater Callsign : "
                        + etcCallsign.getText() + (checked[0] ? "*" : "") + "\nFreq: " + etcFreq.getText().toString()
                        + (checked[1] ? "*" : "") + "\nShift: " + etcShift.getText().toString()
                        + (checked[2] ? "*" : "") + "\nTone: " + etcTone.getText().toString() + (checked[3] ? "*" : "")
                        + "\nLocation or Coordinates: " + etcLocation.getText().toString() + (checked[5] ? "*" : "")
                        + "\nOwner or Club: " + etcClub.getText().toString() + (checked[4] ? "*" : "") + "\n\n"
                        + Build.BRAND + "(" + Build.PRODUCT + "-" + Build.MODEL + ")\nRepeater.MY ver: " + versionName
                        + "\n\nNote:\n" + etcNote.getText().toString());

        startActivity(createEmailOnlyChooserIntent(emailIntent, "Suggest Correction"));

    }

    /*
     * I don't write this, but it is a handy function to focus on only sending
     * Email - mypapit (Christmas day 2014)
     */
    public Intent createEmailOnlyChooserIntent(Intent source, CharSequence chooserTitle) {
        Stack<Intent> intents = new Stack<Intent>();
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "repeaterbot+repeater_correction@gmail.com",
                null));
        List<ResolveInfo> activities = getPackageManager().queryIntentActivities(i, 0);

        for (ResolveInfo ri : activities) {
            Intent target = new Intent(source);
            target.setPackage(ri.activityInfo.packageName);

            intents.add(target);
        }

        if (!intents.isEmpty()) {
            Intent chooserIntent = Intent.createChooser(intents.remove(0), chooserTitle);
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new Parcelable[intents.size()]));

            return chooserIntent;
        } else {
            return Intent.createChooser(source, chooserTitle);
        }
    }

    protected void onPause() {
        super.onPause();

        overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);

    }

}
