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

import androidx.annotation.NonNull;

import com.edalfons.common_code.TeslaApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyComplicationProviderService extends ComplicationProviderService {
    private static final int DATA_UPDATED = 0;
    private static final int DATA_NOT_UPDATED = 1;

    private static final String WEAR_COMPLICATION_UPDATE_MODULO = "WEAR_COMPLICATION_UPDATE_MODULO";
    private static final int default_modulo = 2;

    private Handler handler;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    private TeslaApi teslaApi;

    @SuppressLint({"HandlerLeak", "CommitPrefEdits"})
    @Override
    public void onComplicationUpdate(int complicationId, int type, ComplicationManager manager) {
        sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        teslaApi = new TeslaApi(sharedPref.getString("access_token", ""),
                sharedPref.getString("default_car_id", ""));

        handler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case DATA_NOT_UPDATED:
                        /* Do nothing */
                        break;
                    case DATA_UPDATED:
                        UpdateComplication(complicationId, type, manager);
                        break;
                }
            }
        };

        updateComplicationDataThread();
    }

    private void updateComplicationDataThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = DATA_NOT_UPDATED;

                try {
                    int modulo = sharedPref.getInt(WEAR_COMPLICATION_UPDATE_MODULO, default_modulo);

                    JSONObject data = new JSONObject(sharedPref.getString(getString(R.string.default_car_vehicle_data), ""));

                    JSONObject charge_state = data.getJSONObject("charge_state");
                    JSONObject vehicle_state = data.getJSONObject("vehicle_state");
                    JSONObject drive_state = data.getJSONObject("drive_state");

                    /*
                     * If sentry_mode, charging, or not parked, update every 10 minutes
                     * else update every 30 minutes if the car happens to be awake
                     */
                    if (vehicle_state.getBoolean("sentry_mode") ||
                            charge_state.getString("charging_state").matches("Charging") ||
                            !drive_state.isNull("shift_state") ||
                            (modulo % default_modulo) == 0) {
                        modulo = 0; /* Reset modulo */

                        teslaApi.getVehicleSummary();

                        if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                            if (teslaApi.resp.getJSONObject("response").getString("state")
                                    .matches("online")) {
                                teslaApi.reset();
                                teslaApi.getVehicleData();

                                if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                                    editor.putString(getString(R.string.default_car_vehicle_data),
                                            teslaApi.resp.getJSONObject("response").toString());

                                    editor.apply();

                                    msg.what = DATA_UPDATED;
                                }
                            }
                        }
                    }

                    modulo += 1;
                    editor.putInt(WEAR_COMPLICATION_UPDATE_MODULO, modulo);
                    editor.apply();
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
        try {
            JSONObject data = new JSONObject(sharedPref.getString(getString(R.string.default_car_vehicle_data), ""));
            JSONObject charge_state = data.getJSONObject("charge_state");
            JSONObject vehicle_state = data.getJSONObject("vehicle_state");

            String complicationText = String.format(Locale.getDefault(), "%d%%",
                    charge_state.getInt("battery_level"));

            Date date = new java.util.Date(vehicle_state.getLong("timestamp"));
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("h:mma");
            sdf.setTimeZone(java.util.TimeZone.getDefault());

            ComplicationData complicationData;

            Intent app_intent = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(this, 0, app_intent, 0);

            if (type == ComplicationData.TYPE_RANGED_VALUE) {
                complicationData =
                        new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                                .setMinValue(0)
                                .setMaxValue(100)
                                .setValue(charge_state.getInt("battery_level"))
                                .setShortText(ComplicationText.plainText(sdf.format(date)))
                                .setShortTitle(ComplicationText.plainText(complicationText))
                                .setTapAction(pi)
                                .build();

                manager.updateComplicationData(complicationId, complicationData);
            } else {
                manager.noUpdateRequired(complicationId);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
