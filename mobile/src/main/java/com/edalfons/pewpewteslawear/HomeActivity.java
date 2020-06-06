package com.edalfons.pewpewteslawear;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
import java.util.Objects;

public class HomeActivity extends AppCompatActivity {
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

    /* Car Alerts */
    private ArrayList<CarAlertItem> alerts;
    private CarAlertItemAdapter adapter;

    /* UI Handler State Machine Macros */
    private static final int VEHICLE_ASLEEP = 0;
    private static final int VEHICLE_AWAKE = 1;
    private static final int VEHICLE_WAKE_FAIL = 2;
    private static final int DATA_NOT_UPDATED = 3;
    private static final int DATA_UPDATED = 4;
    private static final int COMMAND_FAILED = 5;
    private static final int COMMAND_COMPLETED = 6;

    /* Child listener to handle UI changes */
    private Handler uiHandler = null;

    private SharedPreferences sharedPref;
    private TeslaApi tApi;

    /* Swipe refresh layout */
    private SwipeRefreshLayout swipeRefreshLayout;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        uiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case VEHICLE_ASLEEP:
                        wakeVehicleThread();
                        break;
                    case VEHICLE_AWAKE:
                    case COMMAND_COMPLETED:
                        updateVehicleDataThread();
                        break;
                    case VEHICLE_WAKE_FAIL:
                        Toast.makeText(HomeActivity.this,
                                "Unable to wake up vehicle!",
                                Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                        break;
                    case DATA_NOT_UPDATED:
                        Toast.makeText(getApplicationContext(),
                                "Data NOT updated!",
                                Toast.LENGTH_LONG).show();
                        swipeRefreshLayout.setRefreshing(false);
                        break;
                    case DATA_UPDATED:
                        swipeRefreshLayout.setRefreshing(false);
                        updateHomeScreen();
                        break;
                    case COMMAND_FAILED:
                        /* Do nothing */
                        Toast.makeText(getApplicationContext(),
                                "Command failed",
                                Toast.LENGTH_LONG).show();
                        break;
                }
            }
        };

        /* Set sharedPref once, get access_token and default car id_s */
        sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);
        String access_token = sharedPref.getString(getString(R.string.access_token), "");
        String id_s = sharedPref.getString(getString(R.string.default_car_id), "");

        tApi = new TeslaApi(access_token, id_s);

        /* Car Alerts */
        RecyclerView car_alerts_recyclerview = findViewById(R.id.car_alerts_recyclerview);
        car_alerts_recyclerview.setLayoutManager(new LinearLayoutManager(this,
                RecyclerView.HORIZONTAL, false));

        alerts = new ArrayList<>();
        adapter = new CarAlertItemAdapter(this, alerts);
        car_alerts_recyclerview.setAdapter(adapter);

        /* Set title bar to car name */
        String display_name = sharedPref.getString(getString(R.string.default_car_name), "display_name");
        Objects.requireNonNull(getSupportActionBar()).setTitle(display_name);

        /* Swipe refresh layout */
        swipeRefreshLayout = findViewById(R.id.home_screen_swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this::checkVehicleWakeStatusThread);

        setCardViewOnClickListeners();

        updateHomeScreen();
    }

    @Override
    public void onResume() {
        super.onResume();

        /* Start data access */
        swipeRefreshLayout.setRefreshing(true);
        checkVehicleWakeStatusThread();
    }

    /**
     * onCreateOptionsMenu creates the Menu
     * onOptionItemSelected is the logic for the menu items
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /*
            case R.id.home_menu_settings:
                Toast.makeText(getApplicationContext(),
                        "Does nothing!",
                        Toast.LENGTH_SHORT).show();
                return true;
             */
            case R.id.home_screen_refresh_button:
                swipeRefreshLayout.setRefreshing(true);
                checkVehicleWakeStatusThread();
                return true;
            case R.id.home_menu_car_info:
                // Load Info activity
                Intent info_activity_intent = new Intent(getApplicationContext(),
                        InfoActivity.class);
                startActivity(info_activity_intent);
                return true;
            case R.id.home_menu_car_select:
                // Load Car Select activity
                Intent car_select_activity_intent = new Intent(getApplicationContext(),
                        CarSelectActivity.class);
                startActivity(car_select_activity_intent);
                finish();
                return true;
            case R.id.home_menu_logout:
                // Clear sharedPrefs
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.clear();
                editor.apply();

                // Load Login activity
                Intent login_activity_intent = new Intent(getApplicationContext(),
                        LoginActivity.class);
                startActivity(login_activity_intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void wakeVehicleThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = VEHICLE_WAKE_FAIL;

                tApi.waitUntilVehicleAwake();

                tApi.reset();
                if (tApi.getVehicleWakeStatusFromVehicleList().matches("online")) {
                    msg.what = VEHICLE_AWAKE;
                }
                uiHandler.sendMessage(msg);
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

                if (tApi.getVehicleWakeStatusFromVehicleList().matches("online")) {
                    msg.what = VEHICLE_AWAKE;
                }
                uiHandler.sendMessage(msg);
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
                JSONObject data;

                SharedPreferences.Editor editor = sharedPref.edit();

                try {
                    Thread.sleep(1000);

                    tApi.getVehicleData();

                    if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                        data = tApi.resp.getJSONObject("response");
                        JSONObject charge_state = data.getJSONObject("charge_state");
                        JSONObject gui_settings = data.getJSONObject("gui_settings");
                        JSONObject vehicle_state = data.getJSONObject("vehicle_state");
                        JSONObject climate_state = data.getJSONObject("climate_state");
                        JSONObject drive_state = data.getJSONObject("drive_state");

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

                            /* scheduled_departure_time only exists if it is set */
                            boolean scheduled_departure = charge_state.has("scheduled_departure_time");
                            editor.putBoolean(getString(com.edalfons.common_code.R.string.default_car_scheduled_departure),
                                    scheduled_departure);
                            if (scheduled_departure) {
                                editor.putLong(getString(com.edalfons.common_code.R.string.default_car_scheduled_departure_time),
                                        charge_state.getLong("scheduled_departure_time"));
                            }
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

                        if (drive_state.isNull("shift_state")) {
                            editor.putString(getString(R.string.default_car_drive_state), "Parked");
                        } else if (drive_state.getString("shift_state").matches("D")) {
                            editor.putString(getString(R.string.default_car_drive_state), "Driving");
                        } else if (drive_state.getString("shift_state").matches("R")) {
                            editor.putString(getString(R.string.default_car_drive_state), "Reversing");
                        } else if (drive_state.getString("shift_state").matches("N")) {
                            editor.putString(getString(R.string.default_car_drive_state), "In Neutral");
                        }

                        editor.putInt(getString(R.string.default_car_odometer),
                                vehicle_state.getInt("odometer"));
                        editor.putString(getString(R.string.default_car_vin),
                                data.getString("vin"));
                        editor.putString(getString(R.string.default_car_sw_version),
                                vehicle_state.getString("car_version"));

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

    @SuppressLint({"SetTextI18n", "StringFormatInvalid", "SimpleDateFormat", "StringFormatMatches"})
    private void updateHomeScreen() {
        SimpleDateFormat sdf;

        /* Battery stats */
        final TextView battery = findViewById(R.id.main_card_car_battery);
        battery.setText(String.format(getString(R.string.home_screen_main_card_battery),
                sharedPref.getInt(getString(R.string.default_car_battery_level), 0),
                sharedPref.getFloat(getString(R.string.default_car_battery_range), 0),
                sharedPref.getString(getString(R.string.default_car_range_units), "mi")));

        /* Temperature */
        final TextView temperature = findViewById(R.id.main_card_car_temperature);
        String temp_unit = sharedPref.getString(getString(R.string.default_car_temperature_units), "C");
        float indoor_temp = sharedPref.getFloat(getString(R.string.default_car_indoor_temp), 0);
        float outdoor_temp = sharedPref.getFloat(getString(R.string.default_car_outdoor_temp), 0);
        if (temp_unit.matches("C")) {
            temperature.setText(String.format(getString(R.string.home_screen_main_card_temperature),
                    indoor_temp, temp_unit, outdoor_temp, temp_unit));
        } else {
            temperature.setText(String.format(getString(R.string.home_screen_main_card_temperature),
                    (indoor_temp * 9/5) + 32, temp_unit,
                    (outdoor_temp * 9/5) + 32, temp_unit));
        }

        /* Charger status */
        final TextView drive_charge_tv = findViewById(R.id.main_card_drive_charge_status);

        String drive_state = sharedPref.getString(getString(com.edalfons.common_code.R.string.default_car_drive_state), "Parked");
        String charge_state = sharedPref.getString(getString(R.string.default_car_charge_state), "Disconnected");

        /* Check drive status */
        if (drive_state.matches("Parked")) {
            /* Car doesn't have charger plugged in */
            if (charge_state.matches("Disconnected")) {
                drive_charge_tv.setText(String.format(getString(R.string.home_screen_main_card_drive_state), drive_state));
            }
            /* Car is plugged in and charging is completed */
            else if (charge_state.matches("Complete")) {
                drive_charge_tv.setText(getString(R.string.home_screen_main_card_charging_complete));
            }
            /* Car plugged but not charging */
            else if (charge_state.matches("Stopped")) {
                if (sharedPref.getBoolean(getString(R.string.default_car_scheduled_charge_pending), false)) {
                    if (sharedPref.getBoolean(getString(com.edalfons.common_code.R.string.default_car_scheduled_departure), false)) {
                        Date start_date =
                                new java.util.Date(sharedPref
                                        .getLong(getString(R.string.default_car_scheduled_charge_start_time), 0L) * 1000);
                        Date depart_date =
                                new java.util.Date(sharedPref
                                        .getLong(getString(com.edalfons.common_code.R.string.default_car_scheduled_departure_time), 0L) * 1000);
                        sdf = new java.text.SimpleDateFormat("h:mma");
                        drive_charge_tv.setText(String.format(getString(R.string.home_screen_main_card_scheduled_depart),
                                sdf.format(start_date), sdf.format(depart_date)));
                    } else {
                        Date date =
                                new java.util.Date(sharedPref
                                        .getLong(getString(R.string.default_car_scheduled_charge_start_time), 0L) * 1000);
                        sdf = new java.text.SimpleDateFormat("h:mma");
                        drive_charge_tv.setText(String.format(getString(R.string.home_screen_main_card_scheduled_charge),
                                sdf.format(date)));
                    }
                } else {
                    drive_charge_tv.setText(R.string.home_screen_main_card_charging_stopped);
                }
            }
            /* Car is charging */
            else if (charge_state.matches("Charging")) {
                int minutes_rem = sharedPref.getInt(getString(R.string.default_car_minutes_til_charge_complete), 0);
                int hours = minutes_rem / 60;
                int mins = minutes_rem % 60;
                if (hours > 0) {
                    drive_charge_tv.setText(String.format(getString(R.string.home_screen_main_card_charge_eta_hrs_mins),
                            hours, mins,
                            sharedPref.getInt(getString(R.string.default_car_max_charge_level), 0)));
                } else {
                    drive_charge_tv.setText(String.format(getString(R.string.home_screen_main_card_charge_eta_mins),
                            mins,
                            sharedPref.getInt(getString(R.string.default_car_max_charge_level), 0)));
                }
            }
        } else {
            drive_charge_tv.setText(String.format(getString(R.string.home_screen_main_card_drive_state), drive_state));
        }

        /* Time stamp */
        final TextView last_update_tv = findViewById(R.id.main_card_last_updated);
        Date timestamp_date = new java.util.Date(sharedPref.getLong(getString(R.string.default_car_timestamp), 0L));
        sdf = new java.text.SimpleDateFormat("MMM d @ h:mma");
        sdf.setTimeZone(java.util.TimeZone.getDefault());
        last_update_tv.setText(String.format(getString(R.string.home_screen_main_card_last_updated), sdf.format(timestamp_date)));

        /*
         * Dynamic Car Alerts
         */
        alerts.clear();

        boolean locked = sharedPref.getBoolean(getString(R.string.default_car_locked), false);
        if (!locked) {
            alerts.add(alertsDict.get("unlocked"));
        } else {
            alerts.remove(alertsDict.get("unlocked"));
        }

        boolean doors_closed = sharedPref.getBoolean(getString(R.string.default_car_door_closed), false);
        if (!doors_closed) {
            alerts.add(alertsDict.get("doors_open"));
        } else {
            alerts.remove(alertsDict.get("doors_open"));
        }

        boolean frunk_closed = sharedPref.getBoolean(getString(R.string.default_car_front_trunk_closed), false);
        if (!frunk_closed) {
            alerts.add(alertsDict.get("frunk_open"));
        } else {
            alerts.remove(alertsDict.get("frunk_open"));
        }

        boolean trunk_closed = sharedPref.getBoolean(getString(R.string.default_car_rear_trunk_closed), false);
        if (!trunk_closed) {
            alerts.add(alertsDict.get("trunk_open"));
        } else {
            alerts.remove(alertsDict.get("trunk_open"));
        }

        boolean windows_closed = sharedPref.getBoolean(getString(R.string.default_car_window_closed), false);
        if (!windows_closed) {
            alerts.add(alertsDict.get("windows_open"));
        } else {
            alerts.remove(alertsDict.get("windows_open"));
        }

        boolean sw_update = sharedPref.getBoolean(getString(R.string.default_car_sw_update_available), true);
        if (sw_update) {
            alerts.add(alertsDict.get("sw_update"));
        } else {
            alerts.remove(alertsDict.get("sw_update"));
        }

        boolean sentry_mode = sharedPref.getBoolean(getString(R.string.default_car_sentry_mode), true);

        if (sentry_mode) {
            alerts.add(alertsDict.get("sentry_on"));
        } else {
            alerts.remove(alertsDict.get("sentry_on"));
        }

        boolean climate_off = sharedPref.getBoolean(getString(R.string.default_car_climate_off), false);

        if (!climate_off) {
            alerts.add(alertsDict.get("climate_on"));
        } else {
            alerts.remove(alertsDict.get("climate_on"));
        }

        adapter.notifyDataSetChanged();
    }

    private void setCardViewOnClickListeners() {
        /* Lock button */
        final CardView lock_button_cv = findViewById(R.id.lock_button_cv);
        lock_button_cv.setOnClickListener(v -> {
            lockVehicleThread();
            Toast.makeText(getApplicationContext(), "Locking", Toast.LENGTH_SHORT).show();
        });

        /* Unlock button */
        final CardView unlock_button_cv = findViewById(R.id.unlock_button_cv);
        unlock_button_cv.setOnClickListener(v -> {
            unlockVehicleThread();
            Toast.makeText(getApplicationContext(),
                    "Unlocking",
                    Toast.LENGTH_SHORT).show();
        });

        /* Close Windows button */
        final CardView close_window_button_cv = findViewById(R.id.close_window_cv);
        close_window_button_cv.setOnClickListener(v -> {
            closeWindowsThread();
            Toast.makeText(getApplicationContext(),
                    "Closing windows",
                    Toast.LENGTH_SHORT).show();
        });

        /* Vent Windows button */
        final CardView vent_window_button_cv = findViewById(R.id.vent_window_cv);
        vent_window_button_cv.setOnClickListener(v -> {
            ventWindowsThread();
            Toast.makeText(getApplicationContext(),
                    "Venting windows",
                    Toast.LENGTH_SHORT).show();
        });

        /* Open Frunk button */
        final CardView frunk_button_cv = findViewById(R.id.frunk_button_cv);
        frunk_button_cv.setOnClickListener(v -> {
            DialogInterface.OnClickListener diaglogClickListener =
                    (dialog, which) -> {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                actuateFrunkThread();
                                Toast.makeText(getApplicationContext(),
                                        "Opening Frunk",
                                        Toast.LENGTH_SHORT).show();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    };

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);

            builder.setMessage(R.string.home_screen_button_open_frunk_dialog_message)
                    .setPositiveButton(R.string.positive_open_button, diaglogClickListener)
                    .setNegativeButton(R.string.negative_cancel_button, diaglogClickListener)
                    .show();
        });

        /* Open Trunk button */
        final CardView trunk_button_cv = findViewById(R.id.trunk_button_cv);
        trunk_button_cv.setOnClickListener(v -> {
            DialogInterface.OnClickListener diaglogClickListener =
                    (dialog, which) -> {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                actuateTrunkThread();
                                Toast.makeText(getApplicationContext(),
                                        "Opening Trunk",
                                        Toast.LENGTH_SHORT).show();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    };

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);

            builder.setMessage(R.string.home_screen_button_open_trunk_dialog_message)
                    .setPositiveButton(R.string.positive_open_button, diaglogClickListener)
                    .setNegativeButton(R.string.negative_cancel_button, diaglogClickListener)
                    .show();
        });

        /* Set Charge Limit button */
        final CardView charge_limit_button_cv = findViewById(R.id.charge_limit_button_cv);
        charge_limit_button_cv.setOnClickListener(v -> {
            final EditText input = new EditText(HomeActivity.this);
            input.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
            input.setRawInputType(Configuration.KEYBOARD_12KEY);
            input.setText(String.valueOf(sharedPref.getInt(getString(R.string.default_car_max_charge_level), 0)));

            DialogInterface.OnClickListener diaglogClickListener =
                    (dialog, which) -> {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                try {
                                    int input_int = Integer.parseInt(input.getText().toString());

                                    if (input_int >= 50 && input_int <= 100) {
                                        setChargeLimitThread(input_int);
                                        Toast.makeText(getApplicationContext(),
                                                "Setting charge limit to "
                                                        .concat(input.getText().toString()),
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getApplicationContext(),
                                                "Incorrect limit entered: "
                                                        .concat(input.getText().toString()),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                } catch ( Exception e) {
                                    Toast.makeText(getApplicationContext(),
                                            "Incorrect input: "
                                                    .concat(input.getText().toString()),
                                            Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    };

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);



            builder.setMessage(R.string.home_screen_button_charge_limit_dialog_message)
                    .setView(input)
                    .setPositiveButton(R.string.positive_set_button, diaglogClickListener)
                    .setNegativeButton(R.string.negative_cancel_button, diaglogClickListener)
                    .show();
        });

        /* Trigger Homelink button */
        final CardView homelink_button_cv = findViewById(R.id.homelink_button_cv);
        homelink_button_cv.setOnClickListener(v -> {
            DialogInterface.OnClickListener diaglogClickListener =
                    (dialog, which) -> {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                triggerHomelinkThread();
                                Toast.makeText(getApplicationContext(),
                                        "Triggering Homelink",
                                        Toast.LENGTH_SHORT).show();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    };

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);

            builder.setMessage(R.string.home_screen_button_open_homelink_dialog_message)
                    .setPositiveButton(R.string.positive_homelink_button, diaglogClickListener)
                    .setNegativeButton(R.string.negative_cancel_button, diaglogClickListener)
                    .show();
        });

        /* Close charge port button */
        final CardView close_charge_port_button_cv = findViewById(R.id.charge_port_close_button_cv);
        close_charge_port_button_cv.setOnClickListener(v -> {
            closeChargePortThread();
            Toast.makeText(getApplicationContext(),
                    "Closing charge port",
                    Toast.LENGTH_SHORT).show();
        });

        /* Open charge port button */
        final CardView open_charge_port_button_cv = findViewById(R.id.charge_port_open_button_cv);
        open_charge_port_button_cv.setOnClickListener(v -> {
            openChargePortThread();
            Toast.makeText(getApplicationContext(),
                    "Opening charge port",
                    Toast.LENGTH_SHORT).show();
        });

        /* Start charge button */
        final CardView start_charge_button_cv = findViewById(R.id.start_charge_button_cv);
        start_charge_button_cv.setOnClickListener(v -> {
            startChargingThread();
            Toast.makeText(getApplicationContext(),
                    "Starting charge",
                    Toast.LENGTH_SHORT).show();
        });

        /* Stop charge button */
        final CardView stop_charge_button_cv = findViewById(R.id.stop_charge_button_cv);
        stop_charge_button_cv.setOnClickListener(v -> {
            stopChargingThread();
            Toast.makeText(getApplicationContext(),
                    "Ending charge",
                    Toast.LENGTH_SHORT).show();
        });

        /* Sentry mode on button */
        final CardView sentry_mode_on_button_cv = findViewById(R.id.sentry_mode_on_button_cv);
        sentry_mode_on_button_cv.setOnClickListener(v -> {
            sentryModeOnThread();
            Toast.makeText(getApplicationContext(),
                    "Turning on Sentry Mode",
                    Toast.LENGTH_SHORT).show();
        });

        /* Sentry mode off button */
        final CardView sentry_mode_off_button_cv = findViewById(R.id.sentry_mode_off_button_cv);
        sentry_mode_off_button_cv.setOnClickListener(v -> {
            sentryModeOffThread();
            Toast.makeText(getApplicationContext(),
                    "Turning off Sentry Mode",
                    Toast.LENGTH_SHORT).show();
        });

        /* Start climate button */
        final CardView start_climate_button_cv = findViewById(R.id.climate_on_button_cv);
        start_climate_button_cv.setOnClickListener(v -> {
            startClimateThread();
            Toast.makeText(getApplicationContext(),
                    "Turning on climate",
                    Toast.LENGTH_SHORT).show();
        });

        /* Stop climate button */
        final CardView stop_climate_button_cv = findViewById(R.id.climate_off_button_cv);
        stop_climate_button_cv.setOnClickListener(v -> {
            stopClimateThread();
            Toast.makeText(getApplicationContext(),
                    "Turning off climate",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void lockVehicleThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAILED;

                tApi.lockVehicle();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_COMPLETED;
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
                msg.what = COMMAND_FAILED;

                tApi.unlockVehicle();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_COMPLETED;
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
                msg.what = COMMAND_FAILED;

                tApi.closeVehicleWindows();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_COMPLETED;
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
                msg.what = COMMAND_FAILED;

                tApi.ventVehicleWindows();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_COMPLETED;
                }

                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void actuateFrunkThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAILED;

                tApi.actuateFrunk();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_COMPLETED;
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
                msg.what = COMMAND_FAILED;

                tApi.actuateTrunk();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_COMPLETED;
                }

                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void setChargeLimitThread(int input) {
        class ChargeLimitThread extends Thread {
            private final int limit;
            private ChargeLimitThread(int in) {this.limit = in;}

            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAILED;

                tApi.setChargeLimit(limit);

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_COMPLETED;
                }

                uiHandler.sendMessage(msg);
            }
        }

        ChargeLimitThread t = new ChargeLimitThread(input);
        t.start();
    }

    private void triggerHomelinkThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAILED;

                tApi.triggerHomelink();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_COMPLETED;
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
                msg.what = COMMAND_FAILED;

                tApi.closeChargePort();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_COMPLETED;
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
                msg.what = COMMAND_FAILED;

                tApi.openChargePort();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_COMPLETED;
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
                msg.what = COMMAND_FAILED;

                tApi.startCharging();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_COMPLETED;
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
                msg.what = COMMAND_FAILED;

                tApi.stopCharging();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_COMPLETED;
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
                msg.what = COMMAND_FAILED;

                tApi.sentryModeOn();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_COMPLETED;
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
                msg.what = COMMAND_FAILED;

                tApi.sentryModeOff();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_COMPLETED;
                }

                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void startClimateThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAILED;

                tApi.startClimate();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_COMPLETED;
                }

                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }

    private void stopClimateThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = COMMAND_FAILED;

                tApi.stopClimate();

                if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                    msg.what = COMMAND_COMPLETED;
                }

                uiHandler.sendMessage(msg);
            }
        };
        t.start();
    }
}
