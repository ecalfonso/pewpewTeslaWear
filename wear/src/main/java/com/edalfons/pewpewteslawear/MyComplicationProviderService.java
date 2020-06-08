package com.edalfons.pewpewteslawear;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;

import com.edalfons.common_code.TeslaApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyComplicationProviderService extends ComplicationProviderService {
    private static final int VEHICLE_ASLEEP = 0;
    private static final int VEHICLE_AWAKE = 1;
    private static final int DATA_UPDATED = 2;
    private static final int DATA_NOT_UPDATED = 3;

    private Handler handler;

    private SharedPreferences sharedPref;
    private TeslaApi teslaApi;

    @SuppressLint("HandlerLeak")
    @Override
    public void onComplicationUpdate(int complicationId, int type, ComplicationManager manager) {
        sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);
        teslaApi = new TeslaApi(sharedPref.getString("access_token", ""),
                sharedPref.getString("default_car_id", ""));

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case VEHICLE_ASLEEP:
                        /* Do nothing */
                        break;
                    case VEHICLE_AWAKE:
                        UpdateComplicationDataThread();
                        break;
                    case DATA_UPDATED:
                        UpdateComplication(complicationId, type, manager);
                        break;
                    case DATA_NOT_UPDATED:
                        /* Do nothing */
                        break;
                }
            }
        };

        CheckVehicleWakeStatusThread();
    }

    private void CheckVehicleWakeStatusThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = VEHICLE_ASLEEP;

                try {
                    teslaApi.reset();
                    teslaApi.getVehicleSummary();

                    if (teslaApi.resp.getJSONObject("response")
                            .getString("state")
                            .matches("online")) {
                        msg.what = VEHICLE_AWAKE;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    handler.sendMessage(msg);
                }
            }
        };
        t.start();
    }

    private void UpdateComplicationDataThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = DATA_NOT_UPDATED;

                try {
                    teslaApi.reset();
                    teslaApi.getVehicleData();

                    if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = sharedPref.edit();

                        JSONObject data = teslaApi.resp.getJSONObject("response").getJSONObject("charge_state");

                        editor.putInt(getString(R.string.complication_battery_level), data.getInt("battery_level"));
                        editor.putLong(getString(R.string.complication_timestamp), data.getLong("timestamp"));
                        editor.apply();

                        msg.what = DATA_UPDATED;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    handler.sendMessage(msg);
                }
            }
        };
        t.start();
    }

    private void UpdateComplication(int complicationId, int type, ComplicationManager manager) {
        int battery_level = sharedPref.getInt(getString(R.string.complication_battery_level), -1);
        String complicationText = String.format(Locale.getDefault(), "%d%%", battery_level);

        long timestamp = sharedPref.getLong(getString(R.string.complication_timestamp), 0L);
        Date date = new java.util.Date(timestamp);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("h:mma");
        sdf.setTimeZone(java.util.TimeZone.getDefault());

        ComplicationData complicationData;

        Intent app_intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, app_intent, 0);

        if (type == ComplicationData.TYPE_SHORT_TEXT) {
            complicationData =
                    new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                            .setShortText(ComplicationText.plainText(sdf.format(date)))
                            .setShortTitle(ComplicationText.plainText(complicationText))
                            .setTapAction(pi)
                            .build();

            manager.updateComplicationData(complicationId, complicationData);
        } else {
            manager.noUpdateRequired(complicationId);
        }
    }
}
