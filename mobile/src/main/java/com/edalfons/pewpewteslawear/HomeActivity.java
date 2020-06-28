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
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case VEHICLE_ASLEEP:
                        wakeVehicleThread();
                        break;
                    case VEHICLE_AWAKE:
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
                }
            }
        };

        /* Set sharedPref once, get access_token and default car id_s */
        sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);
        String access_token = sharedPref.getString(getString(R.string.access_token), "");
        String id_s = sharedPref.getString(getString(R.string.default_car_id), "");

        tApi = new TeslaApi(this, access_token, id_s);

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
                DialogInterface.OnClickListener diaglogClickListener =
                        (dialog, which) -> {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    // Clear sharedPrefs
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.clear();
                                    editor.apply();

                                    // Load Login activity
                                    Intent login_activity_intent = new Intent(getApplicationContext(),
                                            LoginActivity.class);
                                    startActivity(login_activity_intent);
                                    finish();
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    break;
                            }
                        };

                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);

                builder.setMessage(R.string.home_screen_menu_item4_prompt)
                        .setPositiveButton(R.string.home_screen_menu_item4, diaglogClickListener)
                        .setNegativeButton(R.string.negative_cancel_button, diaglogClickListener)
                        .show();
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
                } finally {
                    uiHandler.sendMessage(msg);
                }
            }
        };
        t.start();
    }

    @SuppressLint({"SetTextI18n", "StringFormatInvalid", "SimpleDateFormat", "StringFormatMatches"})
    private void updateHomeScreen() {
        SimpleDateFormat sdf;

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

            /* Battery stats */
            final TextView battery = findViewById(R.id.main_card_car_battery);
            battery.setText(String.format(getString(R.string.home_screen_main_card_battery),
                    charge_state.getInt("battery_level"),
                    charge_state.getDouble("battery_range"),
                    gui_settings.getString("gui_distance_units").matches("mi/hr") ?
                            "mi" : "km"));

            /* Temperature */
            final TextView temperature = findViewById(R.id.main_card_car_temperature);
            String temp_unit = gui_settings.getString("gui_temperature_units");
            float indoor_temp = climate_state.getInt("inside_temp");
            float outdoor_temp = climate_state.getInt("outside_temp");
            if (temp_unit.matches("C")) {
                temperature.setText(String.format(getString(R.string.home_screen_main_card_temperature),
                        indoor_temp, temp_unit, outdoor_temp, temp_unit));
            } else {
                temperature.setText(String.format(getString(R.string.home_screen_main_card_temperature),
                        (indoor_temp * 9 / 5) + 32, temp_unit,
                        (outdoor_temp * 9 / 5) + 32, temp_unit));
            }

            /* Charger status */
            final TextView drive_charge_tv = findViewById(R.id.main_card_drive_charge_status);

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

            /* Check drive status */
            if (car_drive_state.matches("Parked")) {
                /* Car doesn't have charger plugged in */
                if (car_charge_state.matches("Disconnected")) {
                    drive_charge_tv.setText(String.format(getString(R.string.home_screen_main_card_drive_state), car_drive_state));
                }
                /* Car is plugged in and charging is completed */
                else if (car_charge_state.matches("Complete")) {
                    drive_charge_tv.setText(getString(R.string.home_screen_main_card_charging_complete));
                }
                /* Car plugged but not charging */
                else if (car_charge_state.matches("Stopped")) {
                    if (charge_state.getBoolean("scheduled_charging_pending")) {
                        if (charge_state.has("scheduled_departure_time")) {
                            Date start_date =
                                    new java.util.Date(charge_state.getLong("scheduled_charging_start_time") * 1000);
                            Date depart_date =
                                    new java.util.Date(charge_state.getLong("scheduled_departure_time") * 1000);
                            sdf = new java.text.SimpleDateFormat("h:mma");
                            drive_charge_tv.setText(String.format(getString(R.string.home_screen_main_card_scheduled_depart),
                                    sdf.format(start_date), sdf.format(depart_date)));
                        } else {
                            Date date =
                                    new java.util.Date(charge_state.getLong("scheduled_charging_start_time") * 1000);
                            sdf = new java.text.SimpleDateFormat("h:mma");
                            drive_charge_tv.setText(String.format(getString(R.string.home_screen_main_card_scheduled_charge),
                                    sdf.format(date)));
                        }
                    } else {
                        drive_charge_tv.setText(R.string.home_screen_main_card_charging_stopped);
                    }
                }
                /* Car is charging */
                else if (car_charge_state.matches("Charging")) {
                    int minutes_rem = charge_state.getInt("minutes_to_full_charge");
                    int hours = minutes_rem / 60;
                    int mins = minutes_rem % 60;
                    if (hours > 0) {
                        drive_charge_tv.setText(String.format(getString(R.string.home_screen_main_card_charge_eta_hrs_mins),
                                hours, mins, charge_state.getInt("charge_limit_soc")));
                    } else {
                        drive_charge_tv.setText(String.format(getString(R.string.home_screen_main_card_charge_eta_mins),
                                mins, charge_state.getInt("charge_limit_soc")));
                    }
                }
            } else {
                drive_charge_tv.setText(String.format(getString(R.string.home_screen_main_card_drive_state), car_drive_state));
            }

            /* Time stamp */
            final TextView last_update_tv = findViewById(R.id.main_card_last_updated);
            Date timestamp_date = new java.util.Date(vehicle_state.getLong("timestamp"));
            sdf = new java.text.SimpleDateFormat("MMM d @ h:mma");
            sdf.setTimeZone(java.util.TimeZone.getDefault());
            last_update_tv.setText(String.format(getString(R.string.home_screen_main_card_last_updated), sdf.format(timestamp_date)));

            /*
             * Dynamic Car Alerts
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
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("SetTextI18n")
    private void setCardViewOnClickListeners() {
        /* Lock button */
        final CardView lock_button_cv = findViewById(R.id.lock_button_cv);
        lock_button_cv.setOnClickListener(v -> {
            tApi.sendCmd(TeslaApi.CMD_LOCK, 0);
            Toast.makeText(getApplicationContext(), "Locking", Toast.LENGTH_SHORT).show();
        });

        /* Unlock button */
        final CardView unlock_button_cv = findViewById(R.id.unlock_button_cv);
        unlock_button_cv.setOnClickListener(v -> {
            tApi.sendCmd(TeslaApi.CMD_UNLOCK, 0);
            Toast.makeText(getApplicationContext(),
                    "Unlocking",
                    Toast.LENGTH_SHORT).show();
        });

        /* Close Windows button */
        final CardView close_window_button_cv = findViewById(R.id.close_window_cv);
        close_window_button_cv.setOnClickListener(v -> {
            tApi.sendCmd(TeslaApi.CMD_CLOSE_WINDOW, 0);
            Toast.makeText(getApplicationContext(),
                    "Closing windows",
                    Toast.LENGTH_SHORT).show();
        });

        /* Vent Windows button */
        final CardView vent_window_button_cv = findViewById(R.id.vent_window_cv);
        vent_window_button_cv.setOnClickListener(v -> {
            tApi.sendCmd(TeslaApi.CMD_VENT_WINDOW, 0);
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
                                tApi.sendCmd(TeslaApi.CMD_ACTUATE_FRUNK, 0);
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
                                tApi.sendCmd(TeslaApi.CMD_ACTUATE_TRUNK, 0);
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

            JSONObject charge_state;

            try {
                JSONObject data = new JSONObject(sharedPref.getString(getString(R.string.default_car_vehicle_data), ""));
                charge_state = data.getJSONObject("charge_state");
                input.setText(String.valueOf(charge_state.getInt("charge_limit_soc")));
            } catch (JSONException e) {
                e.printStackTrace();
                input.setText("80");
            }

            DialogInterface.OnClickListener diaglogClickListener =
                    (dialog, which) -> {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                try {
                                    int input_int = Integer.parseInt(input.getText().toString());

                                    if (input_int >= 50 && input_int <= 100) {
                                        tApi.sendCmd(TeslaApi.CMD_SET_CHARGE_LIMIT, input_int);
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

                        InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(input.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
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
                                tApi.sendCmd(TeslaApi.CMD_HOMELINK, 0);
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
            tApi.sendCmd(TeslaApi.CMD_CLOSE_CHARGE_PORT, 0);
            Toast.makeText(getApplicationContext(),
                    "Closing charge port",
                    Toast.LENGTH_SHORT).show();
        });

        /* Open charge port button */
        final CardView open_charge_port_button_cv = findViewById(R.id.charge_port_open_button_cv);
        open_charge_port_button_cv.setOnClickListener(v -> {
            tApi.sendCmd(TeslaApi.CMD_OPEN_CHARGE_PORT, 0);
            Toast.makeText(getApplicationContext(),
                    "Opening charge port",
                    Toast.LENGTH_SHORT).show();
        });

        /* Start charge button */
        final CardView start_charge_button_cv = findViewById(R.id.start_charge_button_cv);
        start_charge_button_cv.setOnClickListener(v -> {
            tApi.sendCmd(TeslaApi.CMD_START_CHARGE, 0);
            Toast.makeText(getApplicationContext(),
                    "Starting charge",
                    Toast.LENGTH_SHORT).show();
        });

        /* Stop charge button */
        final CardView stop_charge_button_cv = findViewById(R.id.stop_charge_button_cv);
        stop_charge_button_cv.setOnClickListener(v -> {
            tApi.sendCmd(TeslaApi.CMD_STOP_CHARGE, 0);
            Toast.makeText(getApplicationContext(),
                    "Ending charge",
                    Toast.LENGTH_SHORT).show();
        });

        /* Sentry mode on button */
        final CardView sentry_mode_on_button_cv = findViewById(R.id.sentry_mode_on_button_cv);
        sentry_mode_on_button_cv.setOnClickListener(v -> {
            tApi.sendCmd(TeslaApi.CMD_SENTRY_MODE_ON, 0);
            Toast.makeText(getApplicationContext(),
                    "Turning on Sentry Mode",
                    Toast.LENGTH_SHORT).show();
        });

        /* Sentry mode off button */
        final CardView sentry_mode_off_button_cv = findViewById(R.id.sentry_mode_off_button_cv);
        sentry_mode_off_button_cv.setOnClickListener(v -> {
            tApi.sendCmd(TeslaApi.CMD_SENTRY_MODE_OFF, 0);
            Toast.makeText(getApplicationContext(),
                    "Turning off Sentry Mode",
                    Toast.LENGTH_SHORT).show();
        });

        /* Start climate button */
        final CardView start_climate_button_cv = findViewById(R.id.climate_on_button_cv);
        start_climate_button_cv.setOnClickListener(v -> {
            tApi.sendCmd(TeslaApi.CMD_CLIMATE_ON, 0);
            Toast.makeText(getApplicationContext(),
                    "Turning on climate",
                    Toast.LENGTH_SHORT).show();
        });

        /* Stop climate button */
        final CardView stop_climate_button_cv = findViewById(R.id.climate_off_button_cv);
        stop_climate_button_cv.setOnClickListener(v -> {
            tApi.sendCmd(TeslaApi.CMD_CLIMATE_OFF, 0);
            Toast.makeText(getApplicationContext(),
                    "Turning off climate",
                    Toast.LENGTH_SHORT).show();
        });
    }
}
