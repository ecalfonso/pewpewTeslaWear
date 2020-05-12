package com.edalfons.pewpewteslawear;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edalfons.teslaapi.TeslaApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;

public class CarSelectActivity extends AppCompatActivity {
    public static class TeslaVehicle {
        String display_name;
        String id_s;

        TeslaVehicle(String d_name, String i) {

            this.display_name = d_name;
            this.id_s = i;
        }

        String getDisplay_name() {
            return display_name;
        }

        String getId_s() {
            return id_s;
        }
    }

    public static class TeslaVehicleViewHolder extends RecyclerView.ViewHolder {
        private TextView Title;
        private View mView;

        TeslaVehicleViewHolder(final View itemView) {
            super(itemView);
            Title = itemView.findViewById(R.id.simple_text);
            mView = itemView;
        }

        void bindData(final TeslaVehicle tv) {
            Title.setText(tv.getDisplay_name());
        }
    }

    public class TeslaVehicleAdapter extends RecyclerView.Adapter {
        private ArrayList<TeslaVehicle> data;

        TeslaVehicleAdapter(ArrayList<TeslaVehicle> vehicles) {
            this.data = vehicles;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
            return new TeslaVehicleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            ((TeslaVehicleViewHolder) holder).bindData(data.get(position));
            ((TeslaVehicleViewHolder) holder).mView.setOnClickListener(v -> {
                /* Set default_car_id_s based on user input */
                SharedPreferences.Editor editor = sharedPref.edit();

                editor.putString(getString(R.string.default_car_id),
                        data.get(position).getId_s());
                editor.putString(getString(R.string.default_car_name),
                        data.get(position).getDisplay_name());
                editor.apply();

                Intent home_activity = new Intent(getApplicationContext(),
                        HomeActivity.class);
                startActivity(home_activity);
                finish();
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        @Override
        public int getItemViewType(final int position) {
            return R.layout.item_simple_itemview;
        }
    }

    /* UI Handler State Machine Macros */
    private static final int VEHICLE_LIST_OBTAINED = 0;
    private static final int VEHICLE_LIST_EMPTY = 1;
    private static final int VEHICLE_LIST_FAIL = 2;

    private ArrayList<TeslaVehicle> vehicles;
    private TeslaVehicleAdapter adapter;

    /* Child listener to handle UI changes */
    private Handler uiHandler = null;

    SharedPreferences sharedPref;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_select);

        uiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case VEHICLE_LIST_OBTAINED:
                        adapter.notifyDataSetChanged();
                        break;
                    case VEHICLE_LIST_EMPTY:
                        Toast.makeText(CarSelectActivity.this,
                                "No vehicles!",
                                Toast.LENGTH_LONG).show();
                        final Button logout = findViewById(R.id.car_select_logout_button);
                        logout.setOnClickListener(v -> {
                            /* Clear the login just used */
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.clear();
                            editor.apply();

                            /* Go back to main activity */
                            Intent i = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(i);

                            finish();
                        });
                        logout.setVisibility(View.VISIBLE);
                        break;
                    case VEHICLE_LIST_FAIL:
                        Toast.makeText(CarSelectActivity.this,
                                "Failed to get vehicles!",
                                Toast.LENGTH_LONG).show();
                        break;
                }
            }
        };

        /* Set sharedPref once */
        sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);

        /* Set up RecyclerView */
        RecyclerView view = findViewById(R.id.car_select_layout);
        view.setLayoutManager(new LinearLayoutManager(this));

        vehicles = new ArrayList<>();
        adapter = new TeslaVehicleAdapter(vehicles);

        view.setAdapter(adapter);

        /* Start thread to get vehicle list */
        getVehicleListThread();
    }

    private void getVehicleListThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                int i, count;
                JSONObject v;
                JSONArray list;
                Message msg = new Message();
                msg.what = VEHICLE_LIST_FAIL;

                String aToken = sharedPref.getString(getString(R.string.access_token), "");

                try {
                    TeslaApi tApi = new TeslaApi(aToken);
                    tApi.getVehicleList();

                    if (tApi.respCode == HttpURLConnection.HTTP_OK) {
                        count = tApi.resp.getInt("count");

                        if (count == 0) {
                            msg.what = VEHICLE_LIST_EMPTY;
                        } else {
                            list = tApi.resp.getJSONArray("response");
                            for (i = 0; i < count; i++) {
                                v = list.getJSONObject(i);
                                vehicles.add(new TeslaVehicle(v.getString("display_name"),
                                        v.getString("id_s")));
                            }
                            msg.what = VEHICLE_LIST_OBTAINED;
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