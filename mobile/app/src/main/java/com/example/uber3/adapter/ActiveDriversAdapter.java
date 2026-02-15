package com.example.uber3.adapter;

import android.annotation.SuppressLint;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.R;
import com.example.uber3.network.model.user.admin.ActiveDriverDto;

import java.util.List;

public class ActiveDriversAdapter extends RecyclerView.Adapter<ActiveDriversAdapter.VH> {

    public interface OnTrackRideClickListener {
        void onTrackRide(ActiveDriverDto driver);
    }

    private final List<ActiveDriverDto> data;
    private final OnTrackRideClickListener trackRideListener;

    public ActiveDriversAdapter(List<ActiveDriverDto> data, OnTrackRideClickListener trackRideListener) {
        this.data = data;
        this.trackRideListener = trackRideListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_active_driver, parent, false);
        return new VH(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ActiveDriverDto d = data.get(position);
        h.tvName.setText(d.name + " " + d.surname);
        h.tvEmail.setText(d.email);

        if (d.blocked) {
            h.tvStatus.setText("BLOCKED");
            h.tvStatus.setBackgroundColor(0xFFFEE2E2);
            h.tvStatus.setTextColor(0xFF991B1B);
            h.cardDriver.setAlpha(0.6f);
            h.btnTrackRide.setEnabled(false);
            h.btnTrackRide.setAlpha(0.5f);
        } else {
            h.tvStatus.setText("ACTIVE");
            h.tvStatus.setBackgroundColor(0xFFD1FAE5);
            h.tvStatus.setTextColor(0xFF065F46);
            h.cardDriver.setAlpha(1.0f);
            h.btnTrackRide.setEnabled(true);
            h.btnTrackRide.setAlpha(1.0f);
        }

        h.btnTrackRide.setOnClickListener(v -> {
            if (trackRideListener != null) {
                trackRideListener.onTrackRide(d);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        CardView cardDriver;
        TextView tvName, tvEmail, tvStatus;
        Button btnTrackRide;

        VH(@NonNull View itemView) {
            super(itemView);
            cardDriver = itemView.findViewById(R.id.cardDriver);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnTrackRide = itemView.findViewById(R.id.btnTrackRide);
        }
    }

}