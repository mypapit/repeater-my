package net.mypapit.mobile.myrepeater;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

public class SettingsActivity extends PreferenceActivity {

    private static final String WALK_CALLSIGN_SETTINGS_PREFS = "callsign_settings_k220";

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.repeater_settings);

        overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);

        SharedPreferences prefs = getSharedPreferences("Location", MODE_PRIVATE);
        int count = prefs.getInt(WALK_CALLSIGN_SETTINGS_PREFS, RepeaterListActivity.WALK_VERSION_CODE);
        // count=count-4;

        if (count < (RepeaterListActivity.WALK_VERSION_CODE + 2)) {
            showOverlay();
            count++;
            SharedPreferences.Editor prefEditor = prefs.edit();
            prefEditor.putInt(WALK_CALLSIGN_SETTINGS_PREFS, count);
            prefEditor.apply();
        }

    }

    protected void onPause() {
        super.onPause();
        // this.saveHistory();

        overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);

    }

    private void showOverlay() {

        final Dialog dialog = new Dialog(this, R.style.cust_dialog);

        dialog.setContentView(R.layout.overlay_settings);
        LinearLayout layout = (LinearLayout) dialog.findViewById(R.id.overlayLayoutSettings);
        layout.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {

                dialog.dismiss();

            }

        });

        dialog.show();

    }

    protected void onResume() {
        super.onResume();

    }

}