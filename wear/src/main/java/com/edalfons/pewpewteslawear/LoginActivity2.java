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

import com.edalfons.common_code.TeslaApi;

import java.net.HttpURLConnection;

public class LoginActivity2 extends WearableActivity {
    /* UI Handler State Machine Macros */
    private static final int LOGIN_BAD = 0;
    private static final int LOGIN_GOOD = 1;

    /* Child listener to handle UI changes */
    private Handler uiHandler = null;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login2);

        uiHandler = new Handler() {
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case LOGIN_GOOD:
                        Intent car_select_activity = new Intent(getApplicationContext(),
                                CarSelectActivity.class);
                        car_select_activity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(car_select_activity);
                        finish();
                        break;
                    case LOGIN_BAD:
                        Toast.makeText(LoginActivity2.this,
                                "LOGIN FAILED",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        final TextView access_token_tv = findViewById(R.id.editText3);
        final TextView refresh_token_tv = findViewById(R.id.editText4);
        final Button login_button = findViewById(R.id.button4);
        login_button.setOnClickListener(v -> {
            /* Empty field check */
            if (access_token_tv.getText().toString().matches("")) {
                Toast.makeText(LoginActivity2.this,
                        "Empty access token!",
                        Toast.LENGTH_SHORT).show();
            } else if (access_token_tv.getText().toString().contains(" ") ||
                    refresh_token_tv.getText().toString().contains(" ")) {
                Toast.makeText(LoginActivity2.this,
                        "Invalid username or password!",
                        Toast.LENGTH_SHORT).show();
                access_token_tv.setText("");
                refresh_token_tv.setText("");
            } else {
                tryLoginThread(access_token_tv.getText().toString(),
                        refresh_token_tv.getText().toString());
            }
        });
    }

    private class LoginThread extends Thread {
        private final String aToken;
        private final String rToken;

        private LoginThread(String a, String r) {
            this.aToken = a;
            this.rToken = r;
        }

        public void run() {
            Message msg = new Message();
            msg.what = LOGIN_BAD;

            TeslaApi tApi = new TeslaApi(aToken);
            tApi.getVehicleList();

            if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                /* Save tokens */
                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                        getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                editor.putString(getString(R.string.access_token), aToken);
                editor.putString(getString(R.string.refresh_token), rToken);
                editor.apply();

                msg.what = LOGIN_GOOD;
            }

            uiHandler.sendMessage(msg);
        }
    }

    private void tryLoginThread(String aToken, String rToken) {
        LoginActivity2.LoginThread t = new LoginActivity2.LoginThread(aToken, rToken);
        t.start();
    }
}
