package com.edalfons.pewpewteslawear;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.activity.WearableActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import com.edalfons.common.CarSelectItem;
import com.edalfons.common.TeslaApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;

public class CarSelectActivity extends WearableActivity {
    public static class CarSelectItemViewHolder extends WearableRecyclerView.ViewHolder {
        private TextView Title;
        private View mView;

        CarSelectItemViewHolder(final View itemView) {
            super(itemView);
            Title = itemView.findViewById(R.id.car_select_text_wear_id);
            mView = itemView;
        }

        void bindData(final CarSelectItem tv) {
            Title.setText(tv.getDisplay_name());
        }
    }

    public class CarSelectItemAdapter extends WearableRecyclerView.Adapter {
        private ArrayList<CarSelectItem> data;

        CarSelectItemAdapter(ArrayList<CarSelectItem> vehicles) {
            this.data = vehicles;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
            return new CarSelectItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            ((CarSelectItemViewHolder) holder).bindData(data.get(position));
            ((CarSelectItemViewHolder) holder).mView.setOnClickListener(v -> {
                SharedPreferences.Editor editor = sharedPref.edit();

                /* Save current access/refresh token and clear out previous data
                 *  For cases when we switch between cars
                 */
                String aToken = sharedPref.getString(getString(R.string.access_token), "");
                String rToken = sharedPref.getString(getString(R.string.refresh_token), "");
                editor.clear();

                /* Replace tokens and set name and id_s */
                editor.putString(getString(R.string.access_token), aToken);
                editor.putString(getString(R.string.refresh_token), rToken);
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
            return R.layout.item_car_select;
        }
    }

    /* UI Handler State Machine Macros */
    private static final int VEHICLE_LIST_OBTAINED = 0;
    private static final int VEHICLE_LIST_EMPTY = 1;
    private static final int VEHICLE_LIST_FAIL = 2;

    /* Child listener to handle UI changes */
    private Handler uiHandler = null;

    private ArrayList<CarSelectItem> vehicles;
    private CarSelectItemAdapter adapter;

    SharedPreferences sharedPref;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_select);

        // Enables Always-on
        setAmbientEnabled();

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
                                Toast.LENGTH_SHORT).show();

                        final TextView car_select_text = findViewById(R.id.car_select_textview);
                        car_select_text.setVisibility(View.GONE);

                        final Button car_select_logout_button = findViewById(R.id.car_select_logout_button);
                        car_select_logout_button.setVisibility(View.VISIBLE);
                        car_select_logout_button.setOnClickListener(v -> {
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.clear();
                            editor.apply();

                            Intent i = new Intent(getApplicationContext(), LoginSelectActivity.class);
                            startActivity(i);
                            finish();
                        });

                        break;
                    case VEHICLE_LIST_FAIL:
                        Toast.makeText(CarSelectActivity.this,
                                "Failed to get vehicles!",
                                Toast.LENGTH_LONG).show();
                        break;
                }
            }
        };

        /* Set up sharedPref once */
        sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.shared_pref_file_key), Context.MODE_PRIVATE);

        /* Setup RecyclerView */
        WearableRecyclerView view = findViewById(R.id.car_select_layout);
        view.setEdgeItemsCenteringEnabled(true);
        view.setLayoutManager(new WearableLinearLayoutManager(this));

        vehicles = new ArrayList<>();
        adapter = new CarSelectItemAdapter(vehicles);
        view.setAdapter(adapter);

        /* Get Vehicle list */
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
                                vehicles.add(new CarSelectItem(v.getString("display_name"),
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
