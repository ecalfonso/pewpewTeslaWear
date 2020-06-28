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
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.drawer.WearableNavigationDrawerView;

import com.edalfons.common_code.CarAlertItem;
import com.edalfons.common_code.CarAlertItemAdapter;
import com.edalfons.common_code.TeslaApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends WearableActivity {
    /* Dictionary of Car Alerts */
    private static final Map<String, CarAlertItem> alertsDict = new HashMap<String, CarAlertItem>() {{
        put("unlocked",     new CarAlertItem(R.drawable.unlocked, "Car is unlocked!"));
        put("doors_open",   new CarAlertItem(R.drawable.door_open, "A door is open!"));
        put("frunk_open",   new CarAlertItem(R.drawable.frunk_open, "Frunk is open!"));
        put("trunk_open",   new CarAlertItem(R.drawable.trunk_open, "Trunk is open!"));
        put("windows_open", new CarAlertItem(R.drawable.windows_open2, "Windows are open!"));
        put("sw_update",    new CarAlertItem(R.drawable.sw_update, "Software update available!"));
        put("sentry_on",    new CarAlertItem(R.drawable.sentry_icon, "Sentry mode is on!"));
        put("climate_on",   new CarAlertItem(R.drawable.climate_on, "Climate is running!"));
    }};

    /* UI Handler State Machine Macros */
    private static final int VEHICLE_ASLEEP = 0;
    private static final int VEHICLE_AWAKE = 1;
    private static final int VEHICLE_WAKE_FAIL = 2;
    private static final int DATA_NOT_UPDATED = 3;
    private static final int DATA_UPDATED = 4;

    /* Child listener to handle UI changes */
    private Handler uiHandler = null;

    /* Tesla API variables */
    private SharedPreferences sharedPref;
    private TeslaApi tApi;

    /* Nav drawer */
    private WearableNavigationDrawerView navDrawer;

    private ScrollView scrollView;
    private boolean isRoundDisplay;

    private ArrayList<CarAlertItem> alerts;
    private CarAlertItemAdapter adapter;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);
        String aToken = sharedPref.getString(getString(R.string.access_token), "");
        String id_s = sharedPref.getString(getString(R.string.default_car_id), "");
        
        tApi = new TeslaApi(this, aToken, id_s);

        uiHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case VEHICLE_ASLEEP:
                        showRefreshGraphic(true);
                        wakeVehicleThread();
                        break;
                    case VEHICLE_AWAKE:
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
                }
            }
        };

        scrollView = findViewById(R.id.home_screen_scrollview);

        /* Custom inset */
        isRoundDisplay = getResources().getConfiguration().isScreenRound();
        if (isRoundDisplay) {
            scrollView.setPadding(50,0,50,0);
        } else {
            scrollView.setPadding(0,15,0,15);
            final Space upper_blank_space = findViewById(R.id.upper_space);
            final Space lower_blank_space = findViewById(R.id.lower_space);

            upper_blank_space.setVisibility(View.GONE);
            lower_blank_space.setVisibility(View.GONE);
        }

        /* Nav Drawer */
        NavItem[] mNavItems = new NavItem[]{
                new NavItem("Refresh", getDrawable(R.drawable.refresh)),
                new NavItem("Car select", getDrawable(R.drawable.cars)),
                new NavItem("Sign out", getDrawable(R.drawable.logout))
        };

        navDrawer = findViewById(R.id.navigation_drawer);

        navDrawer.setAdapter(new NavItemAdapter(mNavItems));
        navDrawer.addOnItemSelectedListener(HomeActivity.this::onNavItemSelected);

        /* Car Alerts */
        RecyclerView car_alerts_recyclerview = findViewById(R.id.car_alerts_recyclerview);
        car_alerts_recyclerview.setLayoutManager(new LinearLayoutManager(this,
                RecyclerView.HORIZONTAL, false));

        alerts = new ArrayList<>();
        adapter = new CarAlertItemAdapter(this, alerts);
        car_alerts_recyclerview.setAdapter(adapter);

        /* Set onClickListeners for the buttons */
        setCmdButtonOnClickListeners();

        /* Update home screen during onCreate with saved data */
        updateHomeScreen();
    }

    @Override
    public void onResume() {
        super.onResume();

        /* Scroll to top if not already there */
        scrollView = findViewById(R.id.home_screen_scrollview);
        if (scrollView.getScrollY() > 0) {
            scrollView.smoothScrollTo(0, 0);
        }

        /* requestFocus for Rotary input */
        scrollView.requestFocus();

        /* Start Data access */
        showRefreshGraphic(true);
        checkVehicleWakeStatusThread();
    }

    private void onNavItemSelected(int i) {
        switch (i) {
            case 0: // Refresh
                showRefreshGraphic(true);
                checkVehicleWakeStatusThread();
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

    @SuppressLint({"StringFormatInvalid", "SimpleDateFormat", "StringFormatMatches"})
    private void updateHomeScreen() {

        try {
            JSONObject data = new JSONObject(sharedPref.getString(getString(R.string.default_car_vehicle_data), ""));

            if (data.length() == 0) {
                return;
            }

            JSONObject charge_state = data.getJSONObject("charge_state");
            JSONObject gui_settings = data.getJSONObject("gui_settings");
            JSONObject vehicle_state = data.getJSONObject("vehicle_state");
            JSONObject climate_state = data.getJSONObject("climate_state");
            JSONObject drive_state = data.getJSONObject("drive_state");

            /*
             * car_info_linearlayout
             * Upper part of the view
             */
            final TextView car_name_tv = findViewById(R.id.car_name_tv);
            car_name_tv.setText(sharedPref.getString(getString(R.string.default_car_name), ""));

            final TextView battery_charge_tv = findViewById(R.id.car_battery_tv);
            battery_charge_tv.setText(String.format(getString(R.string.battery_level),
                    charge_state.getInt("battery_level"),
                    charge_state.getDouble("battery_range"),
                    gui_settings.getString("gui_distance_units").matches("mi/hr") ?
                            "mi" : "km"));

            final TextView drive_charge_tv = findViewById(R.id.drive_charge_tv);

            String car_drive_state = "";
            if (drive_state.isNull("shift_state")) {
                car_drive_state = "Parked";
            } else if (drive_state.getString("shift_state").matches("D")) {
                car_drive_state = "Driving";
            } else if (drive_state.getString("shift_state").matches("R")) {
                car_drive_state = "Reversing";
            } else if (drive_state.getString("shift_state").matches("N")) {
                car_drive_state = "In Neutral";
            }

            String car_charge_state = charge_state.getString("charging_state");

            /* Check drive_status */
            if (car_drive_state.matches("Parked")) {
                /* Car doesn't have charger plugged in */
                if (car_charge_state.matches("Disconnected")) {
                    drive_charge_tv.setText(String.format(getString(R.string.drive_state), car_drive_state));
                }
                /* Car is plugged in and charging is completed */
                else if (car_charge_state.matches("Complete")) {
                    drive_charge_tv.setText(getString(R.string.charging_complete));
                }
                /* Car plugged but not charging */
                else if (car_charge_state.matches("Stopped")) {
                    if (charge_state.getBoolean("scheduled_charging_pending")) {
                        if (charge_state.has("scheduled_departure_time")) {
                            Date start_date =
                                    new java.util.Date(charge_state.getLong("scheduled_charging_start_time") * 1000);
                            Date depart_date =
                                    new java.util.Date(charge_state.getLong("scheduled_departure_time") * 1000);
                            SimpleDateFormat sdf = new java.text.SimpleDateFormat("h:mma");
                            drive_charge_tv.setText(String.format(getString(R.string.scheduled_depart),
                                    sdf.format(start_date), sdf.format(depart_date)));
                        } else {
                            Date start_date =
                                    new java.util.Date(charge_state.getLong("scheduled_charging_start_time") * 1000);
                            SimpleDateFormat sdf = new java.text.SimpleDateFormat("h:mma");
                            drive_charge_tv.setText(String.format(getString(R.string.scheduled_charge),
                                    sdf.format(start_date)));
                        }
                    } else {
                        drive_charge_tv.setText(getString(R.string.charging_stopped));
                    }
                }
                /* Car is charging */
                else if (car_charge_state.matches("Charging")) {
                    int minutes_rem = charge_state.getInt("minutes_to_full_charge");
                    int hours = minutes_rem / 60;
                    int mins = minutes_rem % 60;
                    if (hours > 0) {
                        drive_charge_tv.setText(String.format(getString(R.string.charge_eta_hrs_mins),
                                hours, mins, charge_state.getInt("charge_limit_soc")));
                    } else {
                        drive_charge_tv.setText(String.format(getString(R.string.charge_eta_mins),
                                mins, charge_state.getInt("charge_limit_soc")));
                    }
                }
            } else {
                drive_charge_tv.setText(String.format(getString(R.string.drive_state), car_drive_state));
            }

            final TextView last_update_tv = findViewById(R.id.last_updated_tv);
            Date timestamp_date = new java.util.Date(vehicle_state.getLong("timestamp"));
            SimpleDateFormat timestamp_sdf = new java.text.SimpleDateFormat("MMM d @ h:mma");
            timestamp_sdf.setTimeZone(java.util.TimeZone.getDefault());
            last_update_tv.setText(String.format(getString(R.string.last_updated),
                    timestamp_sdf.format(timestamp_date)));

            /*
             * temperature_linearlayout
             */
            final TextView indoor_temp_tv = findViewById(R.id.indoor_temp_tv);
            final TextView outdoor_temp_tv = findViewById(R.id.outdoor_temp_tv);
            String temp_unit = gui_settings.getString("gui_temperature_units");
            float indoor_temp = climate_state.getInt("inside_temp");
            float outdoor_temp = climate_state.getInt("outside_temp");
            if (temp_unit.matches("C")) {
                indoor_temp_tv.setText(String.format(getString(R.string.indoor_temp),
                        indoor_temp, temp_unit));
                outdoor_temp_tv.setText(String.format(getString(R.string.outdoor_temp),
                        outdoor_temp, temp_unit));
            } else {
                indoor_temp_tv.setText(String.format(getString(R.string.indoor_temp),
                        (indoor_temp * 9 / 5) + 32, temp_unit));
                outdoor_temp_tv.setText(String.format(getString(R.string.outdoor_temp),
                        (outdoor_temp * 9 / 5) + 32, temp_unit));
            }

            /*
             * car_alerts_linearlayout
             * Dynamic car alerts
             */
            alerts.clear();

            boolean locked = vehicle_state.getBoolean("locked");
            if (!locked) {
                alerts.add(alertsDict.get("unlocked"));
            } else {
                alerts.remove(alertsDict.get("unlocked"));
            }

            int doors_open_sum = vehicle_state.getInt("df") +
                    vehicle_state.getInt("pf") +
                    vehicle_state.getInt("dr") +
                    vehicle_state.getInt("pr");
            if (doors_open_sum > 0) {
                alerts.add(alertsDict.get("doors_open"));
            } else {
                alerts.remove(alertsDict.get("doors_open"));
            }

            boolean frunk_closed = vehicle_state.getInt("ft") == 0;
            if (!frunk_closed) {
                alerts.add(alertsDict.get("frunk_open"));
            } else {
                alerts.remove(alertsDict.get("frunk_open"));
            }

            boolean trunk_closed = vehicle_state.getInt("rt") == 0;
            if (!trunk_closed) {
                alerts.add(alertsDict.get("trunk_open"));
            } else {
                alerts.remove(alertsDict.get("trunk_open"));
            }

            int window_sum = vehicle_state.getInt("fd_window") +
                    vehicle_state.getInt("rd_window") +
                    vehicle_state.getInt("fp_window") +
                    vehicle_state.getInt("rp_window");
            if (window_sum > 0) {
                alerts.add(alertsDict.get("windows_open"));
            } else {
                alerts.remove(alertsDict.get("windows_open"));
            }

            boolean sw_update = !vehicle_state.getJSONObject("software_update")
                    .getString("status").matches("");
            if (sw_update) {
                alerts.add(alertsDict.get("sw_update"));
            } else {
                alerts.remove(alertsDict.get("sw_update"));
            }

            boolean sentry_mode = vehicle_state.getBoolean("sentry_mode");
            if (sentry_mode) {
                alerts.add(alertsDict.get("sentry_on"));
            } else {
                alerts.remove(alertsDict.get("sentry_on"));
            }

            boolean climate_on = climate_state.getBoolean("is_auto_conditioning_on");
            if (climate_on) {
                alerts.add(alertsDict.get("climate_on"));
            } else {
                alerts.remove(alertsDict.get("climate_on"));
            }

            adapter.notifyDataSetChanged();

            final TextView car_status_tv = findViewById(R.id.car_status_tv);
            final View main_divider_2 = findViewById(R.id.main_divider_2);
            if (alerts.size() > 0) {
                car_status_tv.setVisibility(View.VISIBLE);
                main_divider_2.setVisibility(View.VISIBLE);
            } else {
                if (car_status_tv.getVisibility() != View.GONE) {
                    car_status_tv.setVisibility(View.GONE);
                }
                if (main_divider_2.getVisibility() != View.GONE) {
                    main_divider_2.setVisibility(View.GONE);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void wakeVehicleThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = VEHICLE_WAKE_FAIL;

                try {
                    tApi.waitUntilVehicleAwake();
                    tApi.reset();
                    tApi.getVehicleSummary();

                    if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (tApi.resp.getJSONObject("response")
                                .getString("state")
                                .matches("online")) {
                            msg.what = VEHICLE_AWAKE;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    uiHandler.sendMessage(msg);
                }
            }
        };
        t.start();
    }

    private void checkVehicleWakeStatusThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = VEHICLE_ASLEEP;

                try {
                    tApi.getVehicleSummary();

                    if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                        if (tApi.resp.getJSONObject("response")
                                .getString("state")
                                .matches("online")) {
                            msg.what = VEHICLE_AWAKE;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    uiHandler.sendMessage(msg);
                }
            }
        };
        t.start();
    }

    private void updateVehicleDataThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = DATA_NOT_UPDATED;

                SharedPreferences.Editor editor = sharedPref.edit();

                try {
                    tApi.getVehicleData();

                    if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                        editor.putString(getString(R.string.default_car_vehicle_data), tApi.resp.getJSONObject("response").toString());

                        editor.apply();

                        msg.what = DATA_UPDATED;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void showRefreshGraphic(boolean show) {
        /* Start refresh graphic */
        final View upper_blank_space = findViewById(R.id.upper_space);
        final ProgressBar refresh_graphic = findViewById(R.id.refresh_view);

        if (show) {
            if (isRoundDisplay) {
                upper_blank_space.setVisibility(View.GONE);
            }
            refresh_graphic.setVisibility(View.VISIBLE);
        } else {
            if (isRoundDisplay) {
                upper_blank_space.setVisibility(View.VISIBLE);
            }
            refresh_graphic.setVisibility(View.GONE);
        }
    }

    private void setCmdButtonOnClickListeners() {
        final TextView lock = findViewById(R.id.lock_tv);
        lock.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Locking");
            startActivityForResult(i, TeslaApi.CMD_LOCK);
        });

        final TextView unlock = findViewById(R.id.unlock_tv);
        unlock.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Unlocking");
            startActivityForResult(i, TeslaApi.CMD_UNLOCK);
        });

        final TextView climate_on = findViewById(R.id.climate_on_tv);
        climate_on.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Turning climate on");
            startActivityForResult(i, TeslaApi.CMD_CLIMATE_ON);
        });

        final TextView climate_off = findViewById(R.id.climate_off_tv);
        climate_off.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Turning climate off");
            startActivityForResult(i, TeslaApi.CMD_CLIMATE_OFF);
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
                                tApi.sendCmd(TeslaApi.CMD_ACTUATE_FRUNK, 0);

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
                                tApi.sendCmd(TeslaApi.CMD_ACTUATE_TRUNK, 0);

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
            startActivityForResult(i, TeslaApi.CMD_VENT_WINDOW);
        });

        final TextView close_windows = findViewById(R.id.close_window_tv);
        close_windows.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Closing windows");
            startActivityForResult(i, TeslaApi.CMD_CLOSE_WINDOW);
        });

        final TextView sentry_on = findViewById(R.id.sentry_mode_on_tv);
        sentry_on.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Turning Sentry mode on");
            startActivityForResult(i, TeslaApi.CMD_SENTRY_MODE_ON);
        });

        final TextView sentry_off = findViewById(R.id.sentry_mode_off_tv);
        sentry_off.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Turning Sentry mode off");
            startActivityForResult(i, TeslaApi.CMD_SENTRY_MODE_OFF);
        });

        final TextView start_charge = findViewById(R.id.start_charge_tv);
        start_charge.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Starting charge");
            startActivityForResult(i, TeslaApi.CMD_START_CHARGE);
        });

        final TextView stop_charge = findViewById(R.id.stop_charge_tv);
        stop_charge.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Stopping charge");
            startActivityForResult(i, TeslaApi.CMD_STOP_CHARGE);
        });

        final TextView open_charge_port = findViewById(R.id.open_charge_port_tv);
        open_charge_port.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Opening charge port");
            startActivityForResult(i, TeslaApi.CMD_OPEN_CHARGE_PORT);
        });

        final TextView close_charge_port = findViewById(R.id.close_charge_port_tv);
        close_charge_port.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MyConfirmationActivity.class);
            i.putExtra(getString(R.string.commands_key_str), "Closing charge port");
            startActivityForResult(i, TeslaApi.CMD_CLOSE_CHARGE_PORT);
        });

        final TextView set_charge_limit = findViewById(R.id.set_charge_limit_tv);
        set_charge_limit.setOnClickListener(v -> {
            final NumberPicker numberPicker = new NumberPicker(HomeActivity.this);
            numberPicker.setMaxValue(100);
            numberPicker.setMinValue(50);
            numberPicker.setWrapSelectorWheel(false);

            JSONObject charge_state;
            try {
                JSONObject data = new JSONObject(sharedPref.getString(getString(R.string.default_car_vehicle_data), ""));
                charge_state = data.getJSONObject("charge_state");
                numberPicker.setValue(charge_state.getInt("charge_limit_soc"));
            } catch (JSONException e) {
                e.printStackTrace();
                numberPicker.setValue(80);
            }

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
                                tApi.sendCmd(TeslaApi.CMD_SET_CHARGE_LIMIT, numberPicker.getValue());

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
                                tApi.sendCmd(TeslaApi.CMD_HOMELINK, 0);

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
                case TeslaApi.CMD_LOCK:             tApi.sendCmd(TeslaApi.CMD_LOCK, 0); break;
                case TeslaApi.CMD_UNLOCK:           tApi.sendCmd(TeslaApi.CMD_UNLOCK, 0); break;
                case TeslaApi.CMD_CLIMATE_ON:       tApi.sendCmd(TeslaApi.CMD_CLIMATE_ON, 0); break;
                case TeslaApi.CMD_CLIMATE_OFF:      tApi.sendCmd(TeslaApi.CMD_CLIMATE_OFF, 0); break;
                case TeslaApi.CMD_VENT_WINDOW:      tApi.sendCmd(TeslaApi.CMD_VENT_WINDOW, 0); break;
                case TeslaApi.CMD_CLOSE_WINDOW:     tApi.sendCmd(TeslaApi.CMD_CLOSE_WINDOW, 0); break;
                case TeslaApi.CMD_SENTRY_MODE_ON:   tApi.sendCmd(TeslaApi.CMD_SENTRY_MODE_ON, 0); break;
                case TeslaApi.CMD_SENTRY_MODE_OFF:  tApi.sendCmd(TeslaApi.CMD_SENTRY_MODE_OFF, 0); break;
                case TeslaApi.CMD_START_CHARGE:     tApi.sendCmd(TeslaApi.CMD_START_CHARGE, 0); break;
                case TeslaApi.CMD_STOP_CHARGE:      tApi.sendCmd(TeslaApi.CMD_STOP_CHARGE, 0); break;
                case TeslaApi.CMD_OPEN_CHARGE_PORT: tApi.sendCmd(TeslaApi.CMD_OPEN_CHARGE_PORT, 0); break;
                case TeslaApi.CMD_CLOSE_CHARGE_PORT: tApi.sendCmd(TeslaApi.CMD_CLOSE_CHARGE_PORT, 0); break;
                default:
                    Toast.makeText(this,
                            "Unknown requestCode!",
                            Toast.LENGTH_SHORT).show();
            }
        }
    }
}
