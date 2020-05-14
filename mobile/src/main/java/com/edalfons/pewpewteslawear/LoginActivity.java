package com.edalfons.pewpewteslawear;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edalfons.teslaapi.TeslaApi;

import org.json.JSONException;

import java.net.HttpURLConnection;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    /* UI Handler State Machine Macros */
    private static final int LOGIN_GOOD = 0;
    private static final int LOGIN_BAD = 1;

    /* Child listener to handle UI changes */
    private Handler uiHandler = null;

    SharedPreferences sharedPref;

    private TextView username_text;
    private TextView password_text;
    private TextView access_token_text;
    private TextView refresh_token_text;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Objects.requireNonNull(getSupportActionBar()).hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);

        uiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Intent car_select_activity_intent = new Intent(getApplicationContext(),
                        CarSelectActivity.class);

                switch (msg.what) {
                    case LOGIN_GOOD:
                        /* Launch car select activity on good login */
                        startActivity(car_select_activity_intent);
                        finish();
                        break;
                    case LOGIN_BAD:
                        Toast.makeText(getApplicationContext(),
                                "LOGIN IS BAD",
                                Toast.LENGTH_LONG).show();
                        break;
                }
            }
        };

        username_text = findViewById(R.id.editText2);
        password_text = findViewById(R.id.editText);

        /* Set Autofill support for username/password TextViews for Android Oreo+ */
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            username_text.setAutofillHints(View.AUTOFILL_HINT_USERNAME);
            password_text.setAutofillHints(View.AUTOFILL_HINT_PASSWORD);
        }

        /* Login button */
        final Button login_button = findViewById(R.id.button);
        login_button.setOnClickListener(v -> {
            /* Empty field check */
            if (username_text.getText().toString().matches("") ||
                password_text.getText().toString().matches("")) {
                Toast.makeText(getApplicationContext(),
                        "Please enter a Username/Password",
                        Toast.LENGTH_SHORT).show();
            } else if (username_text.getText().toString().contains(" ") ||
                    password_text.getText().toString().contains(" ")) {
                Toast.makeText(getApplicationContext(),
                        "Invalid Username/Password format",
                        Toast.LENGTH_SHORT).show();
            } else {
                testLoginCredentials();
            }
        });

        access_token_text = findViewById(R.id.editText3);
        refresh_token_text = findViewById(R.id.editText4);

        /* Access token button*/
        final Button token_button = findViewById(R.id.button2);
        token_button.setOnClickListener(v -> {
            /* Empty field check */
            String aToken = access_token_text.getText().toString();
            String rToken = refresh_token_text.getText().toString();
            if (aToken.matches("")) {
                Toast.makeText(getApplicationContext(),
                        "Please enter an Access Token",
                        Toast.LENGTH_SHORT).show();
            } else if (aToken.contains(" ") ||
                       rToken.contains(" ")) {
                Toast.makeText(getApplicationContext(),
                        "Invalid Token format",
                        Toast.LENGTH_SHORT).show();
            } else {
                testAccessToken();
            }
        });
    }

    /*
    Try to generate access_token with Tesla credentials
     */
    private void testLoginCredentials() {
        Thread t = new Thread() {
            @Override
            public void run () {
                Message msg = new Message();
                msg.what = LOGIN_BAD;

                SharedPreferences.Editor editor = sharedPref.edit();


                TextView username_textView = findViewById(R.id.editText2);
                String username = username_textView.getText().toString();

                TextView password_textView = findViewById(R.id.editText);
                String password = password_textView.getText().toString();

                TeslaApi tApi = new TeslaApi();
                tApi.login(username, password);

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = LOGIN_GOOD;

                    /* Save new tokens */
                    try {
                        String aToken = tApi.resp.getString("access_token");
                        String rToken = tApi.resp.getString("refresh_token");

                        editor.putString(getString(R.string.access_token), aToken);
                        editor.putString(getString(R.string.refresh_token), rToken);
                        editor.apply();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    /*
    Using User input Access Token, test Tesla API access
     */
    private void testAccessToken() {
        Thread t  = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = LOGIN_BAD;

                SharedPreferences.Editor editor = sharedPref.edit();

                TextView access_token_textView = findViewById(R.id.editText3);
                String aToken = access_token_textView.getText().toString();

                TextView refresh_token_textView = findViewById(R.id.editText4);
                String rToken = refresh_token_textView.getText().toString();

                TeslaApi tApi = new TeslaApi(aToken);
                tApi.getVehicleList();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = LOGIN_GOOD;

                    /* Save tokens to sharedPref */
                    editor.putString(getString(R.string.access_token), aToken);
                    if (!rToken.matches("")) {
                        editor.putString(getString(R.string.refresh_token), rToken);
                    }
                    editor.apply();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }
}
