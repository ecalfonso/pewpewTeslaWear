package com.edalfons.pewpewteslawear;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class VehicleOfflineActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Objects.requireNonNull(getSupportActionBar()).hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_offline);

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);


        final TextView vehicle_offline_string = findViewById(R.id.vehicle_offline_textview);

        vehicle_offline_string.setText(String.format(getString(R.string.vehicle_offline_text),
                sharedPref.getString(getString(R.string.default_car_name), "display_name")));
    }
}