package com.edalfons.pewpewteslawear;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.activity.WearableActivity;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.edalfons.teslaapi.TeslaApi;

import org.json.JSONException;

import java.net.HttpURLConnection;

public class LoginActivity extends WearableActivity {
    /* UI Handler State Machine Macros */
    private static final int LOGIN_BAD = 0;
    private static final int LOGIN_GOOD = 1;

    /* Child listener to handle UI changes */
    private Handler uiHandler = null;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Enables Always-on
        setAmbientEnabled();

        uiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case LOGIN_GOOD:
                        /* Load Car select activity */
                        Intent car_select_activity = new Intent(getApplicationContext(),
                                CarSelectActivity.class);
                        startActivity(car_select_activity);

                        finish();
                        break;
                    case LOGIN_BAD:
                        Toast.makeText(LoginActivity.this,
                                "LOGIN FAILED",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        final TextView username_tv = findViewById(R.id.editText);
        final TextView password_tv = findViewById(R.id.editText2);
        final Button login_button = findViewById(R.id.button3);
        login_button.setOnClickListener(v -> {
            /* Empty field check */
            if (username_tv.getText().toString().matches("") ||
                password_tv.getText().toString().matches("")) {
                Toast.makeText(LoginActivity.this,
                        "Empty username or password!",
                        Toast.LENGTH_SHORT).show();
            } else if (username_tv.getText().toString().contains(" ") ||
                       password_tv.getText().toString().contains(" ")) {
                Toast.makeText(LoginActivity.this,
                        "Invalid username or password!",
                        Toast.LENGTH_SHORT).show();
                username_tv.setText("");
                password_tv.setText("");
            } else {
                tryLoginThread(username_tv.getText().toString(),
                        password_tv.getText().toString());
            }
        });
    }

    private class LoginThread extends Thread {
        private String username;
        private String password;

        private LoginThread(String u, String p) {
            this.username = u;
            this.password = p;
        }

        public void run() {
            Message msg = new Message();
            msg.what = LOGIN_BAD;

            TeslaApi tApi = new TeslaApi();
            tApi.login(username, password);

            if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                /* Save tokens */
                try {
                    String aToken = tApi.resp.getString("access_token");
                    String rToken = tApi.resp.getString("refresh_token");

                    SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                            getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();

                    editor.putString(getString(R.string.access_token), aToken);
                    editor.putString(getString(R.string.refresh_token), rToken);
                    editor.apply();

                    msg.what = LOGIN_GOOD;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            uiHandler.sendMessage(msg);
        }
    }

    private void tryLoginThread(String username, String password) {
        LoginThread t = new LoginThread(username, password);
        t.start();
    }
}
