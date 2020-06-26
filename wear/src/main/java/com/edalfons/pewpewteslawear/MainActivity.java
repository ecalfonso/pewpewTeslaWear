package com.edalfons.pewpewteslawear;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.activity.WearableActivity;

import com.edalfons.common_code.TeslaApi;

import org.json.JSONException;

import java.net.HttpURLConnection;

public class MainActivity extends WearableActivity {
    /* UI Handler State Machine Macros */
    private static final int API_CHECK_PASS = 0;
    private static final int API_CHECK_FAIL = 1;
    private static final int CREDENTIAL_FAIL = 2;
    private static final int DEFAULT_CAR_ID_FAIL = 3;

    /* Child listener to handle UI changes */
    private Handler uiHandler = null;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enables Always-on
        setAmbientEnabled();

        uiHandler = new Handler() {
            @Override
            public void handleMessage(Message m) {
                Intent api_inaccessible_intent = new Intent(getApplicationContext(),
                        TeslaApiUnaccessible.class);
                Intent login_picker = new Intent(getApplicationContext(),
                        LoginSelectActivity.class);
                Intent home_activity = new Intent(getApplicationContext(),
                        HomeActivity.class);
                Intent car_select_intent = new Intent(getApplicationContext(),
                        CarSelectActivity.class);

                switch (m.what) {
                    case API_CHECK_FAIL:
                        startActivity(api_inaccessible_intent);
                        finish();
                        break;
                    case CREDENTIAL_FAIL:
                        startActivity(login_picker);
                        finish();
                        break;
                    case API_CHECK_PASS:
                        startActivity(home_activity);
                        finish();
                        break;
                    case DEFAULT_CAR_ID_FAIL:
                        startActivity(car_select_intent);
                        finish();
                        break;
                }
            }
        };

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
