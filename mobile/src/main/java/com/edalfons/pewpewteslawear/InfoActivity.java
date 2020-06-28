package com.edalfons.pewpewteslawear;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Objects.requireNonNull(getSupportActionBar()).hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        final SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);

        final String access_token = sharedPref.getString(getString(R.string.access_token),
                "");
        final String refresh_token = sharedPref.getString(getString(R.string.refresh_token),
                "");

        try {
            JSONObject data = new JSONObject(sharedPref.getString(getString(R.string.default_car_vehicle_data), ""));

            if (data.length() == 0) {
                return;
            }

            JSONObject gui_settings = data.getJSONObject("gui_settings");
            JSONObject vehicle_state = data.getJSONObject("vehicle_state");

            int odometer = vehicle_state.getInt("odometer");
            String range_unit = gui_settings.getString("gui_distance_units").matches("mi/hr") ?
                    "mi" : "km";
            final String vin = data.getString("vin");
            String sw_version = vehicle_state.getString("car_version");

            final TextView access_token_tv = findViewById(R.id.info_screen_access_token_tv);
            final TextView refresh_token_tv = findViewById(R.id.info_screen_refresh_token_tv);
            final TextView odometer_tv = findViewById(R.id.info_screen_odometer_tv);
            final TextView vin_tv = findViewById(R.id.info_screen_vin_tv);
            final TextView sw_version_tv = findViewById(R.id.info_screen_sw_version_tv);

            if (!access_token.matches("")) {
                access_token_tv.setText(getString(R.string.info_screen_tap_to_reveal));
                access_token_tv.setOnClickListener(v -> {
                    access_token_tv.setText(access_token);
                    access_token_tv.setTextIsSelectable(true);
                });
            }

            if (!refresh_token.matches("")) {
                refresh_token_tv.setText(getString(R.string.info_screen_tap_to_reveal));
                refresh_token_tv.setOnClickListener(v -> {
                    refresh_token_tv.setText(refresh_token);
                    refresh_token_tv.setTextIsSelectable(true);
                });
            }

            odometer_tv.setText(String.format(getString(R.string.info_screen_odometer), odometer, range_unit));

            vin_tv.setText(vin.substring(0, vin.length() - 6).concat("XXXXXX"));
            vin_tv.setOnClickListener(v -> {
                vin_tv.setText(vin);
                vin_tv.setTextIsSelectable(true);
            });

            sw_version_tv.setText(sw_version);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
