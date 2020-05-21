package com.edalfons.common_code;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CarAlertItemAdapter extends RecyclerView.Adapter {
    private final ArrayList<CarAlertItem> data;
    private final Context mContext;

    public CarAlertItemAdapter(Context ctx, ArrayList<CarAlertItem> items) {
        this.mContext = ctx;
        this.data = items;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new CarAlertItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((CarAlertItemViewHolder) holder).bindData(data.get(position));
        ((CarAlertItemViewHolder) holder).mView.setOnClickListener(v ->
                Toast.makeText(mContext,
                        data.get(position).getHelper_text(),
                        Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(final int position) {
        return R.layout.item_car_alert;
    }
}

class CarAlertItemViewHolder extends RecyclerView.ViewHolder {
    final View mView;
    private final ImageView imgView;

    CarAlertItemViewHolder(final View itemView) {
        super(itemView);
        mView = itemView;
        imgView = itemView.findViewById(R.id.car_alerts_imgview_wear_id);
    }


    void bindData(final CarAlertItem item) {
        imgView.setImageResource(item.getDrawable_id());
    }
}