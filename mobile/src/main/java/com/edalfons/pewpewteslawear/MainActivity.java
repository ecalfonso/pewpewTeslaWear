package com.edalfons.pewpewteslawear;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.app.AppCompatActivity;

import com.edalfons.common_code.TeslaApi;

import org.json.JSONException;

import java.net.HttpURLConnection;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    /* UI Handler State Machine Macros */
    private static final int API_CHECK_PASS = 0;
    private static final int API_CHECK_FAIL = 1;
    private static final int CREDENTIAL_FAIL = 2;
    private static final int DEFAULT_CAR_ID_FAIL= 3;

    /* Child listener to handle UI changes */
    private Handler uiHandler = null;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Objects.requireNonNull(getSupportActionBar()).hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uiHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg) {
                Intent api_inaccessible_intent = new Intent(getApplicationContext(),
                        TeslaApiUnaccessibleActivity.class);
                Intent login_activity_intent = new Intent(getApplicationContext(),
                        LoginActivity.class);
                Intent home_activity_intent = new Intent(getApplicationContext(),
                        HomeActivity.class);
                Intent car_select_activity_intent = new Intent(getApplicationContext(),
                        CarSelectActivity.class);

                switch (msg.what){
                    case API_CHECK_FAIL:
                        startActivity(api_inaccessible_intent);
                        finish(); // Close splash screen activity
                        break;
                    case CREDENTIAL_FAIL:
                        startActivity(login_activity_intent);
                        finish(); // Close splash screen activity
                        break;
                    case API_CHECK_PASS:
                        startActivity(home_activity_intent);
                        finish(); // Close splash screen activity
                        break;
                    case DEFAULT_CAR_ID_FAIL:
                        startActivity(car_select_activity_intent);
                        finish(); // Close splash screen activity
                        break;
                }
            }
        };

        /* Background thread to check Tesla API status */
        checkTeslaApiStatusThread();
    }

    /*
    Check Tesla API based on current saved data
    1. If we have access_token + id_s
        Check Tesla API is up
        Check Credentials are good
        Check that id_s exists in account
    2. If we only have access_token
        Check Tesla API is up
        Check Credentials are good
    3. If we don't have anything saved
        Check Tesla API is up
     */
    private void checkTeslaApiStatusThread() {
        Thread t = new Thread()
        {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = API_CHECK_FAIL;

                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                        getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                String access_token = sharedPref.getString(getString(R.string.access_token), "");
                String refresh_token = sharedPref.getString(getString(R.string.refresh_token), "");
                String id_s = sharedPref.getString(getString(R.string.default_car_id), "");

                TeslaApi tApi;

                /* If we have a saved access_token and default car id_s */
                if (!(access_token.matches("")) && !(id_s.matches(""))) {
                    tApi = new TeslaApi(access_token, id_s);
                    tApi.getVehicleSummary();

                    /* Token is good and id_s exists */
                    if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                        msg.what = API_CHECK_PASS;
                    }
                    /* HTTP 404 Not Found = API is good, id_s doesn't exist */
                    else if (tApi.respCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        msg.what = DEFAULT_CAR_ID_FAIL;
                    }
                    /* 401 Unauthorized - Try to refresh token */
                    else if (tApi.respCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        msg.what = CREDENTIAL_FAIL;
                        if (!refresh_token.equals("")) {
                            tApi = new TeslaApi();
                            tApi.refreshToken(refresh_token);

                            if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                                msg.what = API_CHECK_PASS;

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
                        }
                    }
                }
                /* If we have saved token, but no default car id_s */
                else if (!(access_token.matches("")) && (id_s.matches(""))) {
                    tApi = new TeslaApi(access_token);
                    tApi.getVehicleList();

                    /* Token is good so go to car select */
                    if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                        msg.what = DEFAULT_CAR_ID_FAIL;
                    }
                    /* 401 Unauthorized - Try to refresh token */
                    else if (tApi.respCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        msg.what = CREDENTIAL_FAIL;
                        if (!refresh_token.equals("")) {
                            tApi = new TeslaApi();
                            tApi.refreshToken(refresh_token);

                            if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                                msg.what = API_CHECK_PASS;

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
                        }
                    }
                }
                /* No access_token, check that API is up then send user to login page */
                else {
                    tApi = new TeslaApi();
                    tApi.getApiStatus();

                    if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                        msg.what = CREDENTIAL_FAIL;
                    }
                }

                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }
}
