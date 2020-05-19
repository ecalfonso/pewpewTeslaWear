package com.edalfons.pewpewteslawear;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        final SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);

        final String access_token = sharedPref.getString(getString(R.string.access_token),
                "");
        final String refresh_token = sharedPref.getString(getString(R.string.refresh_token),
                "");
        int odometer = sharedPref.getInt(getString(R.string.default_car_odometer), 0);
        String range_unit = sharedPref.getString(getString(R.string.default_car_range_units), "mi");
        final String vin = sharedPref.getString(getString(R.string.default_car_vin),
                getString(R.string.info_screen_vin));
        String sw_version = sharedPref.getString(getString(R.string.default_car_sw_version),
                getString(R.string.info_screen_sw_version));

        final TextView access_token_tv = findViewById(R.id.info_screen_access_token_tv);
        final TextView refresh_token_tv = findViewById(R.id.info_screen_refresh_token_tv);
        final TextView odometer_tv = findViewById(R.id.info_screen_odometer_tv);
        final TextView vin_tv = findViewById(R.id.info_screen_vin_tv);
        final TextView sw_version_tv = findViewById(R.id.info_screen_sw_version_tv);

        assert access_token != null;
        if (!access_token.matches("")) {
            access_token_tv.setText(getString(R.string.info_screen_tap_to_reveal));
            access_token_tv.setOnClickListener(v -> access_token_tv.setText(access_token));
        }

        assert refresh_token != null;
        if (!refresh_token.matches("")) {
            refresh_token_tv.setText(getString(R.string.info_screen_tap_to_reveal));
            refresh_token_tv.setOnClickListener(v -> refresh_token_tv.setText(refresh_token));
        }

        odometer_tv.setText(String.format(getString(R.string.info_screen_odometer), odometer, range_unit));

        assert vin != null;
        vin_tv.setText(vin.substring(0, vin.length() - 6).concat("XXXXXX"));
        vin_tv.setOnClickListener(v -> vin_tv.setText(vin));

        sw_version_tv.setText(sw_version);
    }
}
