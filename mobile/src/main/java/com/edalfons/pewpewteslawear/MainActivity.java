package com.edalfons.pewpewteslawear;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.app.AppCompatActivity;

import com.edalfons.teslaapi.TeslaApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    /* UI Handler State Machine Macros */
    private static final int API_CHECK_PASS = 0;
    private static final int API_CHECK_FAIL = 1;
    private static final int CREDENTIAL_CHECK_PASS = 2;
    private static final int CREDENTIAL_CHECK_RENEW = 3;
    private static final int CREDENTIAL_CHECK_FAIL = 4;
    private static final int CREDENTIAL_RENEW_PASS = 5;
    private static final int CREDENTIAL_RENEW_FAIL = 6;
    private static final int DEFAULT_CAR_ID_PASS = 7;
    private static final int DEFAULT_CAR_ID_FAIL= 8;

    /* Child listener to handle UI changes */
    private Handler uiHandler = null;

    private SharedPreferences sharedPref;

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
                    case API_CHECK_PASS:
                        credentialsCheckThread();
                        break;
                    case API_CHECK_FAIL:
                        startActivity(api_inaccessible_intent);
                        finish(); // Close splash screen activity
                        break;
                    case CREDENTIAL_CHECK_FAIL:
                    case CREDENTIAL_RENEW_FAIL:
                        startActivity(login_activity_intent);
                        finish(); // Close splash screen activity
                        break;
                    case CREDENTIAL_CHECK_RENEW:
                        credentialRefreshThread();
                        break;
                    case CREDENTIAL_CHECK_PASS:
                    case CREDENTIAL_RENEW_PASS:
                        checkDefaultCarThread();
                        break;
                    case DEFAULT_CAR_ID_PASS:
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

        /* Set sharedPref once */
        sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);

        /* Background thread to check Tesla API status */
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
                msg.what = CREDENTIAL_CHECK_FAIL;

                String access_token = sharedPref.getString(getString(R.string.access_token), "");

                /* If access_token is not blank, try to access API */
                assert access_token != null;
                if (!access_token.equals("")) {
                    TeslaApi tApi = new TeslaApi(access_token);
                    tApi.getVehicleList();

                    if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                        msg.what = CREDENTIAL_CHECK_PASS;
                    } else if (tApi.respCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        msg.what = CREDENTIAL_CHECK_RENEW;
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
                msg.what = CREDENTIAL_RENEW_FAIL;

                SharedPreferences.Editor editor = sharedPref.edit();

                String refresh_token = sharedPref.getString(getString(R.string.refresh_token), "");

                /* If refresh_token is not blank, try to access API */
                assert refresh_token != null;
                if (!refresh_token.equals("")) {
                    TeslaApi tApi = new TeslaApi();
                    tApi.refreshToken(refresh_token);

                    if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                        msg.what = CREDENTIAL_RENEW_PASS;

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

    /*
    Read SharedPref if default_car_id is set, if not, take User to car select screen
     */
    private void checkDefaultCarThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = DEFAULT_CAR_ID_FAIL;

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
