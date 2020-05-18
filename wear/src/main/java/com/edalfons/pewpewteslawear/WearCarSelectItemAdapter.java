package com.edalfons.pewpewteslawear;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableRecyclerView;

import com.edalfons.common_code.CarSelectItem;

import java.util.ArrayList;

public class WearCarSelectItemAdapter extends WearableRecyclerView.Adapter {
    private ArrayList<CarSelectItem> data;
    private Context mContext;

    WearCarSelectItemAdapter(Context ctx, ArrayList<CarSelectItem> vehicles) {
        this.mContext = ctx;
        this.data = vehicles;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new WearCarSelectItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        ((WearCarSelectItemViewHolder) holder).bindData(data.get(position));
        ((WearCarSelectItemViewHolder) holder).mView.setOnClickListener(v -> {
            SharedPreferences sharedPref = mContext.getSharedPreferences(
                    mContext.getString(com.edalfons.common_code.R.string.shared_pref_file_key),
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();

            /* Save current access/refresh token and clear out previous data
             *  For cases when we switch between cars
             */
            String aToken = sharedPref.getString(mContext.getString(com.edalfons.common_code.R.string.access_token), "");
            String rToken = sharedPref.getString(mContext.getString(com.edalfons.common_code.R.string.refresh_token), "");
            editor.clear();

            /* Replace tokens and set name and id_s */
            editor.putString(mContext.getString(com.edalfons.common_code.R.string.access_token), aToken);
            editor.putString(mContext.getString(com.edalfons.common_code.R.string.refresh_token), rToken);
            editor.putString(mContext.getString(com.edalfons.common_code.R.string.default_car_id),
                    data.get(position).getId_s());
            editor.putString(mContext.getString(R.string.default_car_name),
                    data.get(position).getDisplay_name());
            editor.apply();

            Intent home_activity = new Intent(mContext, HomeActivity.class);
            home_activity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(home_activity);
            ((CarSelectActivity)mContext).finish();
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(final int position) {
        return R.layout.item_car_select_mobile;
    }
}

class WearCarSelectItemViewHolder extends WearableRecyclerView.ViewHolder {
    private TextView Title;
    View mView;

    WearCarSelectItemViewHolder(final View itemView) {
        super(itemView);
        Title = itemView.findViewById(R.id.car_select_text_wear_id);
        mView = itemView;
    }

    void bindData(final CarSelectItem tv) {
        Title.setText(tv.getDisplay_name());
    }
}