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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

public class MainActivity extends WearableActivity {
    /* UI Handler State Machine Macros */
    private static final int API_CHECK_PASS = 0;
    private static final int API_CHECK_FAIL = 1;
    private static final int CREDENTIAL_CHECK_PASS = 2;
    private static final int CREDENTIAL_CHECK_REFRESH = 3;
    private static final int CREDENTIAL_CHECK_PHONE= 4;
    private static final int CREDENTIAL_CHECK_FAIL = 5;
    private static final int DEFAULT_CAR_ID_PASS = 6;
    private static final int DEFAULT_CAR_ID_FAIL = 7;

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
                    case API_CHECK_PASS:
                        credentialsCheckThread();
                        break;
                    case API_CHECK_FAIL:
                        startActivity(api_inaccessible_intent);
                        finish();
                        break;
                    case CREDENTIAL_CHECK_PASS:
                        checkDefaultCarThread();
                        break;
                    case CREDENTIAL_CHECK_REFRESH:
                        credentialRefreshThread();
                        break;
                    case CREDENTIAL_CHECK_PHONE:
                        getCredentialFromPhoneThread();
                        break;
                    case CREDENTIAL_CHECK_FAIL:
                        startActivity(login_picker);
                        finish();
                        break;
                    case DEFAULT_CAR_ID_PASS:
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
    Check Tesla API status endpoint
    Can double for an internet connectivity check
     */
    private void checkTeslaApiStatusThread() {
        Thread t = new Thread()
        {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = API_CHECK_FAIL;

                TeslaApi tApi = new TeslaApi();
                tApi.getApiStatus();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = API_CHECK_PASS;
                }

                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    /*
    If we have a stored access token, try to access /api/1/vehicles endpoint
     */
    private void credentialsCheckThread() {
        Thread t = new Thread()
        {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = CREDENTIAL_CHECK_REFRESH;

                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                        getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);

                String access_token = sharedPref.getString(getString(R.string.access_token), "");

                /* If access_token is not blank, try to access API */
                assert access_token != null;
                if (!access_token.equals("")) {
                    TeslaApi tApi = new TeslaApi(access_token);
                    tApi.getVehicleList();

                    if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                        msg.what = CREDENTIAL_CHECK_PASS;
                    }
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    /*
    If access_token fails with 401 unauthorized, try refresh token if it exists
     */
    private void credentialRefreshThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = CREDENTIAL_CHECK_PHONE;

                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                        getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                String refresh_token = sharedPref.getString(getString(R.string.refresh_token), "");
                /* If refresh_token is not blank, try to access API */
                assert refresh_token != null;
                if (!refresh_token.equals("")) {
                    TeslaApi tApi = new TeslaApi();
                    tApi.refreshToken(refresh_token);

                    if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                        msg.what = CREDENTIAL_CHECK_PASS;

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
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void getCredentialFromPhoneThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = CREDENTIAL_CHECK_FAIL;

                /* TODO LATER */

                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    /*
    Read SharedPref if default_car_id is set, if not, take User to car select screen
     */
    private void checkDefaultCarThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = DEFAULT_CAR_ID_FAIL;

                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                        getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);
                String aToken = sharedPref.getString(getString(R.string.access_token), "");
                String default_car_id = sharedPref.getString(getString(R.string.default_car_id), "");

                /* Check default_car_id against User's vehicle list */
                assert default_car_id != null;
                if (!default_car_id.matches("")) {
                    TeslaApi tApi = new TeslaApi(aToken);
                    tApi.getVehicleList();

                    if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                        try {
                            int count = tApi.resp.getInt("count");
                            JSONArray vehicleListJSON = tApi.resp.getJSONArray("response");

                            for (int i = 0; i < count; i++) {
                                JSONObject v = vehicleListJSON.getJSONObject(i);
                                String curr_v_id_s = v.getString("id_s");

                                if (default_car_id.matches(curr_v_id_s)) {
                                    msg.what = DEFAULT_CAR_ID_PASS;
                                    break;
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }
}
