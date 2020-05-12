package com.edalfons.pewpewteslawear;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TokenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token);

        final SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPref.edit();

        String access_token = sharedPref.getString(getString(R.string.access_token),
                "");
        String refresh_token = sharedPref.getString(getString(R.string.refresh_token),
                "");

        TextView aTokenTextView = findViewById(R.id.textView7);
        TextView rTokenTextView = findViewById(R.id.textView9);

        assert access_token != null;
        if (!access_token.matches("")) {
            aTokenTextView.setText(access_token);
        }
        assert refresh_token != null;
        if (!refresh_token.matches("")) {
            rTokenTextView.setText(refresh_token);
        }

		/* Debug utilities */
        final Button poison_atoken_button = findViewById(R.id.button3);
        poison_atoken_button.setOnClickListener(v -> {
            editor.putString(getString(R.string.access_token), "asdf");
            editor.apply();
        });

        final Button poison_default_id_s_button = findViewById(R.id.button4);
        poison_default_id_s_button.setOnClickListener(v -> {
            editor.putString(getString(R.string.default_car_id), "asdf");
            editor.apply();
        });
    }
}
