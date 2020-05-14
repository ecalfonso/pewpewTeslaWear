package com.edalfons.pewpewteslawear;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.wear.widget.drawer.WearableNavigationDrawerView;

import com.edalfons.teslaapi.TeslaApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HomeActivity extends WearableActivity {
    /* UI Handler State Machine Macros */
    private static final int VEHICLE_ASLEEP = 0;
    private static final int VEHICLE_AWAKE = 1;
    private static final int VEHICLE_WAKE_FAIL = 2;
    private static final int DATA_NOT_UPDATED = 3;
    private static final int DATA_UPDATED = 4;
    private static final int COMMAND_FAIL = 5;
    private static final int COMMAND_PASS = 6;

    /* Child listener to handle UI changes */
    private Handler uiHandler = null;

    /* Tesla API variables */
    private String aToken;
    private String id_s;
    private SharedPreferences sharedPref;

    /* Cmd Macros */
    private static final int CMD_LOCK = 1;
    private static final int CMD_UNLOCK = 2;
    private static final int CMD_CLIMATE_ON = 3;
    private static final int CMD_CLIMATE_OFF = 4;
    //private static final int CMD_FRUNK = 5;
    //private static final int CMD_TRUNK = 6;
    private static final int CMD_VENT_WINDOWS = 7;
    private static final int CMD_CLOSE_WINDOWS = 8;
    private static final int CMD_SENTRY_MODE_ON = 9;
    private static final int CMD_SENTRY_MODE_OFF = 10;
    private static final int CMD_START_CHARGE = 11;
    private static final int CMD_STOP_CHARGE = 12;
    private static final int CMD_OPEN_CHARGE_PORT = 13;
    private static final int CMD_CLOSE_CHARGE_PORT = 14;
    //private static final int CMD_SET_CHARGE_LIMIT = 15;
    //private static final int CMD_HOMELINK = 16;

    /* Nav drawer */
    private WearableNavigationDrawerView navDrawer;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Enables Always-on
        setAmbientEnabled();

        sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);
        aToken = sharedPref.getString(getString(R.string.access_token), "");
        id_s = sharedPref.getString(getString(R.string.default_car_id), "");

        uiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case VEHICLE_ASLEEP:
                        showRefreshGraphic(true);
                        wakeVehicleThread();
                        break;
                    case VEHICLE_AWAKE:
                    case COMMAND_PASS:
                        /* Update Vehicle Data */
                        updateVehicleDataThread();
                        break;
                    case VEHICLE_WAKE_FAIL:
                        Toast.makeText(HomeActivity.this,
                                "Unable to wake up vehicle!",
                                Toast.LENGTH_SHORT).show();
                        showRefreshGraphic(false);
                        break;
                    case DATA_NOT_UPDATED:
                        Toast.makeText(HomeActivity.this,
                                "Data NOT updated",
                                Toast.LENGTH_SHORT).show();
                        showRefreshGraphic(false);
                        break;
                    case DATA_UPDATED:
                        showRefreshGraphic(false);
                        updateHomeScreen();
                        break;
                    case COMMAND_FAIL:
                        Toast.makeText(HomeActivity.this,
                                "Command failed!",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        /* Nav Drawer */
        NavItem[] mNavItems = new NavItem[]{
                new NavItem("Refresh", getDrawable(R.drawable.refresh)),
                new NavItem("Car select", getDrawable(R.drawable.cars)),
                new NavItem("Sign out", getDrawable(R.drawable.logout))
        };

        navDrawer = findViewById(R.id.navigation_drawer);

        navDrawer.setAdapter(new NavItemAdapter(mNavItems));
        navDrawer.addOnItemSelectedListener(HomeActivity.this::onNavItemSelected);

        /* Set onClickListeners for the buttons */
        setCmdButtonOnClickListeners();

        /* Update home screen during onCreate with saved data */
        updateHomeScreen();
    }

    @Override
    public void onResume() {
        super.onResume();

        /* Scroll to top if not already there */
        final ScrollView scrollView = findViewById(R.id.home_screen_scrollview);
        if (scrollView.getScrollY() > 0) {
            scrollView.smoothScrollTo(0, 0);
        }

        /* requestFocus for Rotary input */
        scrollView.requestFocus();

        /* Start Data access */
        checkVehicleWakeStatusThread();
    }

    private void onNavItemSelected(int i) {
        switch (i) {
            case 0: // Refresh
                showRefreshGraphic(true);
                wakeVehicleThread();
                break;
            case 1: // Car Select
                Intent car_sel_intent = new Intent(HomeActivity.this, CarSelectActivity.class);
                startActivity(car_sel_intent);
                finish();
                break;
            case 2: // Logout
                DialogInterface.OnClickListener dialogClickListener =
                        (dialog, which) -> {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    /* Wipe shared preferences */
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.clear();
                                    editor.apply();

                                    /* Load login intent */
                                    Intent login_intent = new Intent(HomeActivity.this,
                                            LoginSelectActivity.class);
                                    startActivity(login_intent);
                                    finish();

                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    break;
                            }
                        };

                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);

                builder.setTitle(getString(R.string.nav_drawer_logout_title))
                        .setPositiveButton(getString(R.string.nav_drawer_logout), dialogClickListener)
                        .setNegativeButton(getString(R.string.generic_cancel), dialogClickListener)
                        .show();
                break;
        }
        navDrawer.getController().peekDrawer();
    }

    @SuppressLint("StringFormatInvalid")
    public void updateHomeScreen() {
        /*
        LinearLayout 1
        Upper part of the view
         */
        final TextView car_name_tv = findViewById(R.id.car_name_tv);
        car_name_tv.setText(sharedPref.getString(getString(R.string.default_car_name), ""));

        final TextView battery_charge_tv = findViewById(R.id.car_battery_level_tv);
        battery_charge_tv.setText(String.format(getString(R.string.battery_level),
                sharedPref.getInt(getString(R.string.default_car_battery_level), -1)));

        final TextView battery_range_tv = findViewById(R.id.car_battery_range_tv);
        battery_range_tv.setText(String.format(getString(R.string.battery_range),
                sharedPref.getFloat(getString(R.string.default_car_battery_range), -1),
                sharedPref.getString(getString(R.string.default_car_range_units), "unit")));

        final TextView indoor_temp_tv = findViewById(R.id.indoor_temp_tv);
        final TextView outdoor_temp_tv = findViewById(R.id.outdoor_temp_tv);
        String temp_unit = sharedPref.getString(getString(R.string.default_car_temperature_units), "C");
        float indoor_temp = sharedPref.getFloat(getString(R.string.default_car_indoor_temp), 0);
        float outdoor_temp = sharedPref.getFloat(getString(R.string.default_car_outdoor_temp), 0);
        assert temp_unit != null;
        if (temp_unit.matches("C")) {
            indoor_temp_tv.setText(String.format(getString(R.string.indoor_temp_c), indoor_temp));
            outdoor_temp_tv.setText(String.format(getString(R.string.outdoor_temp_c), outdoor_temp));
        } else {
            indoor_temp_tv.setText(String.format(getString(R.string.indoor_temp_f),
                    (indoor_temp * 9/5) + 32));
            outdoor_temp_tv.setText(String.format(getString(R.string.outdoor_temp_f),
                    (outdoor_temp * 9/5) + 32));
        }

        final TextView charger_status_tv = findViewById(R.id.charger_tv);
        String charge_state = sharedPref.getString(getString(R.string.default_car_charge_state), "Disconnected");
        /* Car doesn't have charger plugged in */
        assert charge_state != null;
        if (charge_state.matches("Disconnected")) {
            if (charger_status_tv.getVisibility() != View.GONE) {
                charger_status_tv.setVisibility(View.GONE);
            }
        }
        /* Car plugged but not charging */
        else if (charge_state.matches("Stopped")) {
            if (sharedPref.getBoolean(getString(R.string.default_car_scheduled_charge_pending), false)) {
                Date date =
                        new java.util.Date(sharedPref
                                .getLong(getString(R.string.default_car_scheduled_charge_start_time), 0L) * 1000);
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new java.text.SimpleDateFormat("h:mma");
                charger_status_tv.setText(String.format(getString(R.string.scheduled_charge), sdf.format(date)));
                if (charger_status_tv.getVisibility() != View.VISIBLE) {
                    charger_status_tv.setVisibility(View.VISIBLE);
                }
            }
        }
        /* Car is charging */
        else if (charge_state.matches("Charging")) {
            int minutes_rem = sharedPref.getInt(getString(R.string.default_car_minutes_til_charge_complete), 0);
            int hours = minutes_rem / 60;
            int mins = minutes_rem % 60;
            if (hours > 0) {
                charger_status_tv.setText(String.format(getString(R.string.charge_eta_hrs_mins),
                        hours, mins,
                        sharedPref.getInt(getString(R.string.default_car_max_charge_level), 0)));
            } else {
                charger_status_tv.setText(String.format(getString(R.string.charge_eta_mins),
                        mins,
                        sharedPref.getInt(getString(R.string.default_car_max_charge_level), 0)));
            }
            if (charger_status_tv.getVisibility() != View.VISIBLE) {
                charger_status_tv.setVisibility(View.VISIBLE);
            }
        }

        final TextView last_update_tv = findViewById(R.id.last_updated_tv);
        Date date = new java.util.Date(sharedPref.getLong(getString(R.string.default_car_timestamp), 0L));
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new java.text.SimpleDateFormat("h:mm a E MMM d");
        sdf.setTimeZone(java.util.TimeZone.getDefault());
        last_update_tv.setText(String.format(getString(R.string.last_updated), sdf.format(date)));

        /*
        LinearLayout 2
        Dynamic car alerts
         */
        int imgview_count = 0;
        boolean locked = sharedPref.getBoolean(getString(R.string.default_car_locked), false);
        final ImageView unlocked_imgview = findViewById(R.id.unlocked_imgview);
        if (!locked) {
            unlocked_imgview.setVisibility(View.VISIBLE);
            imgview_count++;
        } else {
            if (unlocked_imgview.getVisibility() != View.GONE) {
                unlocked_imgview.setVisibility(View.GONE);
            }
        }

        boolean doors_closed = sharedPref.getBoolean(getString(R.string.default_car_door_closed), false);
        final ImageView door_opened_imgview = findViewById(R.id.doors_open_imgview);
        if (!doors_closed) {
            door_opened_imgview.setVisibility(View.VISIBLE);
            imgview_count++;
        } else {
            if (door_opened_imgview.getVisibility() != View.GONE) {
                door_opened_imgview.setVisibility(View.GONE);
            }
        }

        boolean frunk_closed = sharedPref.getBoolean(getString(R.string.default_car_front_trunk_closed), false);
        final ImageView frunk_open_imgview = findViewById(R.id.frunk_open_imgview);
        if (!frunk_closed) {
            frunk_open_imgview.setVisibility(View.VISIBLE);
            imgview_count++;
        } else {
            if (frunk_open_imgview.getVisibility() != View.GONE) {
                frunk_open_imgview.setVisibility(View.GONE);
            }
        }

        boolean trunk_closed = sharedPref.getBoolean(getString(R.string.default_car_rear_trunk_closed), false);
        final ImageView trunk_open_imgview = findViewById(R.id.trunk_open_imgview);
        if (!trunk_closed) {
            trunk_open_imgview.setVisibility(View.VISIBLE);
            imgview_count++;
        } else {
            if (trunk_open_imgview.getVisibility() != View.GONE) {
                trunk_open_imgview.setVisibility(View.GONE);
            }
        }

        boolean windows_closed = sharedPref.getBoolean(getString(R.string.default_car_window_closed), false);
        final ImageView windows_open_imgview = findViewById(R.id.windows_open_imgview);
        if (!windows_closed) {
            windows_open_imgview.setVisibility(View.VISIBLE);
            imgview_count++;
        } else {
            if (windows_open_imgview.getVisibility() != View.GONE) {
                windows_open_imgview.setVisibility(View.GONE);
            }
        }

        boolean sw_update = sharedPref.getBoolean(getString(R.string.default_car_sw_update_available), true);
        final ImageView sw_update_imgview = findViewById(R.id.sw_update_imgview);
        if (sw_update) {
            sw_update_imgview.setVisibility(View.VISIBLE);
            imgview_count++;
        } else {
            if (sw_update_imgview.getVisibility() != View.GONE) {
                sw_update_imgview.setVisibility(View.GONE);
            }
        }

        boolean sentry_mode = sharedPref.getBoolean(getString(R.string.default_car_sentry_mode), true);
        final ImageView sentry_mode_imgview = findViewById(R.id.sentry_mode_imgview);
        if (sentry_mode) {
            sentry_mode_imgview.setVisibility(View.VISIBLE);
            imgview_count++;
        } else {
            if (sentry_mode_imgview.getVisibility() != View.GONE) {
                sentry_mode_imgview.setVisibility(View.GONE);
            }
        }

        boolean climate_off = sharedPref.getBoolean(getString(R.string.default_car_climate_off), false);
        final ImageView climate_on_imgview = findViewById(R.id.climate_on_imgview);
        if (!climate_off) {
            climate_on_imgview.setVisibility(View.VISIBLE);
            imgview_count++;
        } else {
            if (climate_on_imgview.getVisibility() != View.GONE) {
                climate_on_imgview.setVisibility(View.GONE);
            }
        }

        final TextView car_status_tv = findViewById(R.id.car_status_tv);
        final View divider = findViewById(R.id.divider3);
        if (imgview_count > 0) {
            car_status_tv.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
        } else {
            if (car_status_tv.getVisibility() != View.GONE) {
                car_status_tv.setVisibility(View.GONE);
            }
            if (divider.getVisibility() != View.GONE) {
                divider.setVisibility(View.GONE);
            }
        }
    }

    public void wakeVehicleThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = VEHICLE_WAKE_FAIL;

                TeslaApi tApi = new TeslaApi(aToken);
                tApi.waitUntilVehicleAwake(id_s);

                tApi.reset();
                if (tApi.getVehicleWakeStatusFromVehicleList(id_s).matches("online")) {
                    msg.what = VEHICLE_AWAKE;
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    public void checkVehicleWakeStatusThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = VEHICLE_ASLEEP;

                TeslaApi tApi = new TeslaApi(aToken);
                if (tApi.getVehicleWakeStatusFromVehicleList(id_s).matches("online")) {
                    msg.what = VEHICLE_AWAKE;
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    public void updateVehicleDataThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = DATA_NOT_UPDATED;
                JSONObject data;

                SharedPreferences.Editor editor = sharedPref.edit();

                try {
                    Thread.sleep(500);

                    TeslaApi tApi = new TeslaApi(aToken);
                    tApi.getVehicleData(id_s);

                    if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                        data = tApi.resp.getJSONObject("response");
                        JSONObject charge_state = data.getJSONObject("charge_state");
                        JSONObject gui_settings = data.getJSONObject("gui_settings");
                        JSONObject vehicle_state = data.getJSONObject("vehicle_state");
                        JSONObject climate_state = data.getJSONObject("climate_state");

                        /* Save data to sharedPref */
                        editor.putString(getString(R.string.default_car_name),
                                data.getString("display_name"));
                        editor.putInt(getString(R.string.default_car_battery_level),
                                charge_state.getInt("battery_level"));
                        editor.putFloat(getString(R.string.default_car_battery_range),
                                charge_state.getInt("battery_range"));
                        editor.putString(getString(R.string.default_car_range_units),
                                gui_settings.getString("gui_distance_units").matches("mi/hr") ?
                                "mi" : "km");

                        editor.putFloat(getString(R.string.default_car_indoor_temp),
                                climate_state.getInt("inside_temp"));
                        editor.putFloat(getString(R.string.default_car_outdoor_temp),
                                climate_state.getInt("outside_temp"));
                        editor.putString(getString(R.string.default_car_temperature_units),
                                gui_settings.getString("gui_temperature_units"));

                        editor.putString(getString(R.string.default_car_charge_state),
                                charge_state.getString("charging_state"));
                        editor.putString(getString(R.string.default_car_charge_units),
                                gui_settings.getString("gui_charge_rate_units"));
                        editor.putInt(getString(R.string.default_car_max_charge_level),
                                charge_state.getInt("charge_limit_soc"));
                        editor.putInt(getString(R.string.default_car_charge_rate),
                                charge_state.getInt("charge_rate"));
                        editor.putInt(getString(R.string.default_car_charge_power),
                                charge_state.getInt("charger_power"));
                        editor.putInt(getString(R.string.default_car_minutes_til_charge_complete),
                                charge_state.getInt("minutes_to_full_charge"));
                        boolean charge_pending = charge_state.getBoolean("scheduled_charging_pending");
                        editor.putBoolean(getString(R.string.default_car_scheduled_charge_pending),
                                charge_pending);
                        if (charge_pending) {
                            /* Because this can be NULL */
                            editor.putLong(getString(R.string.default_car_scheduled_charge_start_time),
                                    charge_state.getLong("scheduled_charging_start_time"));
                        }

                        editor.putBoolean(getString(R.string.default_car_locked),
                                vehicle_state.getBoolean("locked"));

                        int door_sum = vehicle_state.getInt("df") +
                                vehicle_state.getInt("pf") +
                                vehicle_state.getInt("dr") +
                                vehicle_state.getInt("pr");
                        editor.putBoolean(getString(R.string.default_car_door_closed),
                                door_sum == 0);

                        editor.putBoolean(getString(R.string.default_car_front_trunk_closed),
                                vehicle_state.getInt("ft") == 0);

                        editor.putBoolean(getString(R.string.default_car_rear_trunk_closed),
                                vehicle_state.getInt("rt") == 0);

                        int window_sum = vehicle_state.getInt("fd_window") +
                                vehicle_state.getInt("rd_window") +
                                vehicle_state.getInt("fp_window") +
                                vehicle_state.getInt("rp_window");
                        editor.putBoolean(getString(R.string.default_car_window_closed),
                                window_sum == 0);

                        editor.putBoolean(getString(R.string.default_car_climate_off),
                                !climate_state.getBoolean("is_auto_conditioning_on"));

                        editor.putBoolean(getString(R.string.default_car_sentry_mode),
                                vehicle_state.getBoolean("sentry_mode"));

                        editor.putBoolean(getString(R.string.default_car_sw_update_available),
                                !vehicle_state.getJSONObject("software_update")
                                        .getString("status").matches(""));

                        editor.putLong(getString(R.string.default_car_timestamp),
                                vehicle_state.getLong("timestamp"));

                        editor.apply();

                        msg.what = DATA_UPDATED;
                    }
                } catch (JSONException | InterruptedException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    public void showRefreshGraphic(boolean show) {
        /* Start refresh graphic */
        final View upper_blank_space = findViewById(R.id.upper_blank_space);
        final ProgressBar refresh_graphic = findViewById(R.id.refresh_view);

        if (show) {
            upper_blank_space.setVisibility(View.GONE);
            refresh_graphic.setVisibility(View.VISIBLE);
        } else {
            refresh_graphic.setVisibility(View.GONE);
            upper_blank_space.setVisibility(View.VISIBLE);
        }
    }

    public void setCmdButtonOnClickListeners() {
        final TextView lock = findViewById(R.id.lock_tv);
        lock.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Locking");
            startActivityForResult(i, CMD_LOCK);
        });

        final TextView unlock = findViewById(R.id.unlock_tv);
        unlock.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Unlocking");
            startActivityForResult(i, CMD_UNLOCK);
        });

        final TextView climate_on = findViewById(R.id.climate_on_tv);
        climate_on.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Turning climate on");
            startActivityForResult(i, CMD_CLIMATE_ON);
        });

        final TextView climate_off = findViewById(R.id.climate_off_tv);
        climate_off.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Turning climate off");
            startActivityForResult(i, CMD_CLIMATE_OFF);
        });

        final TextView actuate_frunk = findViewById(R.id.frunk_tv);
        actuate_frunk.setOnClickListener(v -> {
            DialogInterface.OnClickListener dialogClickListener =
                    (dialog, which) -> {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                /* Show success animation */
                                Intent i = new Intent(getApplicationContext(),
                                        ConfirmationActivity.class);
                                i.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                                        ConfirmationActivity.SUCCESS_ANIMATION);
                                startActivity(i);

                                /* Start Frunk open thread */
                                actuateFrunkThread();

                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    };

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);

            builder.setTitle(getString(R.string.frunk_open_title))
                    .setPositiveButton(getString(R.string.frunk_open_button), dialogClickListener)
                    .setNegativeButton(getString(R.string.generic_cancel), dialogClickListener)
                    .show();
        });

        final TextView actuate_trunk = findViewById(R.id.trunk_tv);
        actuate_trunk.setOnClickListener(v -> {
            DialogInterface.OnClickListener dialogClickListener =
                    (dialog, which) -> {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                /* Show success animation */
                                Intent i = new Intent(getApplicationContext(),
                                        ConfirmationActivity.class);
                                i.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                                        ConfirmationActivity.SUCCESS_ANIMATION);
                                startActivity(i);

                                /* Start Trunk open thread */
                                actuateTrunkThread();

                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    };

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);

            builder.setTitle(getString(R.string.trunk_open_title))
                    .setPositiveButton(getString(R.string.trunk_open_button), dialogClickListener)
                    .setNegativeButton(getString(R.string.generic_cancel), dialogClickListener)
                    .show();
        });

        final TextView vent_windows = findViewById(R.id.vent_window_tv);
        vent_windows.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Venting windows");
            startActivityForResult(i, CMD_VENT_WINDOWS);
        });

        final TextView close_windows = findViewById(R.id.close_window_tv);
        close_windows.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Closing windows");
            startActivityForResult(i, CMD_CLOSE_WINDOWS);
        });

        final TextView sentry_on = findViewById(R.id.sentry_mode_on_tv);
        sentry_on.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Turning Sentry mode on");
            startActivityForResult(i, CMD_SENTRY_MODE_ON);
        });

        final TextView sentry_off = findViewById(R.id.sentry_mode_off_tv);
        sentry_off.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Turning Sentry mode off");
            startActivityForResult(i, CMD_SENTRY_MODE_OFF);
        });

        final TextView start_charge = findViewById(R.id.start_charge_tv);
        start_charge.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Starting charge");
            startActivityForResult(i, CMD_START_CHARGE);
        });

        final TextView stop_charge = findViewById(R.id.stop_charge_tv);
        stop_charge.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Stopping charge");
            startActivityForResult(i, CMD_STOP_CHARGE);
        });

        final TextView open_charge_port = findViewById(R.id.open_charge_port_tv);
        open_charge_port.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Opening charge port");
            startActivityForResult(i, CMD_OPEN_CHARGE_PORT);
        });

        final TextView close_charge_port = findViewById(R.id.close_charge_port_tv);
        close_charge_port.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Closing charge port");
            startActivityForResult(i, CMD_CLOSE_CHARGE_PORT);
        });

        final TextView set_charge_limit = findViewById(R.id.set_charge_limit_tv);
        set_charge_limit.setOnClickListener(v -> {
            final NumberPicker numberPicker = new NumberPicker(HomeActivity.this);
            numberPicker.setMaxValue(100);
            numberPicker.setMinValue(50);
            numberPicker.setValue(sharedPref.getInt(getString(R.string.default_car_max_charge_level), 0));
            numberPicker.setWrapSelectorWheel(false);

            final DialogInterface.OnClickListener dialogClickListener =
                    (dialog, which) -> {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                /* Show success animation */
                                Intent i = new Intent(getApplicationContext(),
                                        ConfirmationActivity.class);
                                i.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                                        ConfirmationActivity.SUCCESS_ANIMATION);
                                startActivity(i);

                                /* Start set charge limit thread */
                                setChargeLimitThread(numberPicker.getValue());

                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    };

            final AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);

            builder.setTitle(getString(R.string.set_charge_limit_title))
                    .setView(numberPicker)
                    .setPositiveButton(getString(R.string.set_charge_limit_title), dialogClickListener)
                    .setNegativeButton(getString(R.string.generic_cancel), dialogClickListener)
                    .show();
        });

        final TextView homelink = findViewById(R.id.homelink_tv);
        homelink.setOnClickListener(v -> {
            DialogInterface.OnClickListener dialogClickListener =
                    (dialog, which) -> {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                /* Show success animation */
                                Intent i = new Intent(getApplicationContext(),
                                        ConfirmationActivity.class);
                                i.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                                        ConfirmationActivity.SUCCESS_ANIMATION);
                                startActivity(i);

                                /* Start Homelink thread */
                                activateHomelinkThread();

                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    };

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);

            builder.setTitle(getString(R.string.activate_homelink_title))
                    .setPositiveButton(getString(R.string.activate_homelink_button2), dialogClickListener)
                    .setNegativeButton(getString(R.string.generic_cancel), dialogClickListener)
                    .show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CMD_LOCK:          lockVehicleThread(); break;
                case CMD_UNLOCK:        unlockVehicleThread(); break;
                case CMD_CLIMATE_ON:    climateOnThread(); break;
                case CMD_CLIMATE_OFF:   climateOffThread(); break;
                case CMD_VENT_WINDOWS:  ventWindowsThread(); break;
                case CMD_CLOSE_WINDOWS: closeWindowsThread(); break;
                case CMD_SENTRY_MODE_ON: sentryModeOnThread(); break;
                case CMD_SENTRY_MODE_OFF: sentryModeOffThread(); break;
                case CMD_START_CHARGE:  startChargingThread(); break;
                case CMD_STOP_CHARGE:   stopChargingThread(); break;
                case CMD_OPEN_CHARGE_PORT: openChargePortThread(); break;
                case CMD_CLOSE_CHARGE_PORT: closeChargePortThread(); break;
                default:
                    Toast.makeText(this,
                            "Unknown requestCode!",
                            Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void lockVehicleThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAIL;

                try {
                    TeslaApi teslaApi = new TeslaApi(aToken);
                    teslaApi.lockVehicle(id_s);

                    if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (teslaApi.resp.getJSONObject("response").getBoolean("result")) {
                            msg.what = COMMAND_PASS;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void unlockVehicleThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAIL;

                try {
                    TeslaApi teslaApi = new TeslaApi(aToken);
                    teslaApi.unlockVehicle(id_s);

                    if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (teslaApi.resp.getJSONObject("response").getBoolean("result")) {
                            msg.what = COMMAND_PASS;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void climateOnThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAIL;

                try {
                    TeslaApi teslaApi = new TeslaApi(aToken);
                    teslaApi.startClimate(id_s);

                    if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (teslaApi.resp.getJSONObject("response").getBoolean("result")) {
                            msg.what = COMMAND_PASS;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void climateOffThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAIL;

                try {
                    TeslaApi teslaApi = new TeslaApi(aToken);
                    teslaApi.stopClimate(id_s);

                    if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (teslaApi.resp.getJSONObject("response").getBoolean("result")) {
                            msg.what = COMMAND_PASS;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void ventWindowsThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAIL;

                try {
                    TeslaApi teslaApi = new TeslaApi(aToken);
                    teslaApi.ventVehicleWindows(id_s);

                    if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (teslaApi.resp.getJSONObject("response").getBoolean("result")) {
                            msg.what = COMMAND_PASS;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void closeWindowsThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAIL;

                try {
                    TeslaApi teslaApi = new TeslaApi(aToken);
                    teslaApi.closeVehicleWindows(id_s);

                    if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (teslaApi.resp.getJSONObject("response").getBoolean("result")) {
                            msg.what = COMMAND_PASS;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void sentryModeOnThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAIL;

                try {
                    TeslaApi teslaApi = new TeslaApi(aToken);
                    teslaApi.sentryModeOn(id_s);

                    if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (teslaApi.resp.getJSONObject("response").getBoolean("result")) {
                            msg.what = COMMAND_PASS;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void sentryModeOffThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAIL;

                try {
                    TeslaApi teslaApi = new TeslaApi(aToken);
                    teslaApi.sentryModeOff(id_s);

                    if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (teslaApi.resp.getJSONObject("response").getBoolean("result")) {
                            msg.what = COMMAND_PASS;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void startChargingThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAIL;

                try {
                    TeslaApi teslaApi = new TeslaApi(aToken);
                    teslaApi.startCharging(id_s);

                    if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (teslaApi.resp.getJSONObject("response").getBoolean("result")) {
                            msg.what = COMMAND_PASS;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void stopChargingThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAIL;

                try {
                    TeslaApi teslaApi = new TeslaApi(aToken);
                    teslaApi.stopCharging(id_s);

                    if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (teslaApi.resp.getJSONObject("response").getBoolean("result")) {
                            msg.what = COMMAND_PASS;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void openChargePortThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAIL;

                try {
                    TeslaApi teslaApi = new TeslaApi(aToken);
                    teslaApi.openChargePort(id_s);

                    if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (teslaApi.resp.getJSONObject("response").getBoolean("result")) {
                            msg.what = COMMAND_PASS;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void closeChargePortThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAIL;

                try {
                    TeslaApi teslaApi = new TeslaApi(aToken);
                    teslaApi.closeChargePort(id_s);

                    if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (teslaApi.resp.getJSONObject("response").getBoolean("result")) {
                            msg.what = COMMAND_PASS;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void setChargeLimitThread(int input) {
        class ChargeLimitThread extends Thread {
            private int limit;
            private ChargeLimitThread(int in) {this.limit = in;}

            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAIL;

                TeslaApi tApi = new TeslaApi(aToken);
                tApi.setChargeLimit(id_s, limit);

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_PASS;
                }

                uiHandler.sendMessage(msg);
            }
        }

        ChargeLimitThread t = new ChargeLimitThread(input);
        t.start();
    }

    private void actuateFrunkThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAIL;

                try {
                    TeslaApi teslaApi = new TeslaApi(aToken);
                    teslaApi.actuateFrunk(id_s);

                    if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (teslaApi.resp.getJSONObject("response").getBoolean("result")) {
                            msg.what = COMMAND_PASS;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void actuateTrunkThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAIL;

                try {
                    TeslaApi teslaApi = new TeslaApi(aToken);
                    teslaApi.actuateTrunk(id_s);

                    if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (teslaApi.resp.getJSONObject("response").getBoolean("result")) {
                            msg.what = COMMAND_PASS;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void activateHomelinkThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAIL;

                try {
                    TeslaApi teslaApi = new TeslaApi(aToken);
                    teslaApi.triggerHomelink(id_s);

                    if (teslaApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (teslaApi.resp.getJSONObject("response").getBoolean("result")) {
                            msg.what = COMMAND_PASS;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }
}
