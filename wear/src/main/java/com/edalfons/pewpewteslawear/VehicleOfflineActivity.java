package com.edalfons.pewpewteslawear;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

public class VehicleOfflineActivity extends WearableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_offline);

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);

        final TextView vehicle_offline_tv = findViewById(R.id.vehicle_offline_textview);

        vehicle_offline_tv.setText(String.format(getString(R.string.vehicle_offline_text),
                sharedPref.getString(getString(R.string.default_car_name), "display_name")));
    }
}