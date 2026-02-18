package com.example.uber3.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.R;
import com.example.uber3.network.model.tracking.IncomingRideResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IncomingRideAdapter extends RecyclerView.Adapter<IncomingRideAdapter.VH> {

    public interface Listener {
        void onCancelClicked(IncomingRideResponse ride);
        void onRideClicked(IncomingRideResponse ride);
    }

    private final Listener listener;
    private final List<IncomingRideResponse> rides = new ArrayList<>();

    public IncomingRideAdapter(Listener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setRides(List<IncomingRideResponse> newRides) {
        rides.clear();
        if (newRides != null) rides.addAll(newRides);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_incoming_ride, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        IncomingRideResponse r = rides.get(position);

        String start = (r.startLocation != null && r.startLocation.address != null)
                ? r.startLocation.address
                : "N/A";

        String end = (r.endLocation != null && r.endLocation.address != null)
                ? r.endLocation.address
                : "N/A";

        h.tvStartAddress.setText(start);
        h.tvEndAddress.setText(end);

        h.tvStartTime.setText(formatDateTime(r.startTime));

        boolean canCancel = minutesToStart(r.startTime) >= 10;
        h.btnCancel.setEnabled(canCancel);
        h.btnCancel.setAlpha(canCancel ? 1f : 0.6f);
        h.btnCancel.setText(canCancel ? "Cancel ride" : "Too late to cancel");

        h.itemView.setOnClickListener(v -> listener.onRideClicked(r));
        h.btnCancel.setOnClickListener(v -> listener.onCancelClicked(r));
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView tvStartAddress;
        TextView tvEndAddress;
        TextView tvStartTime;
        Button btnCancel;

        VH(@NonNull View itemView) {
            super(itemView);

            tvStartAddress = itemView.findViewById(R.id.tvStartAddress);
            tvEndAddress = itemView.findViewById(R.id.tvEndAddress);
            tvStartTime = itemView.findViewById(R.id.tvStartTime);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }

    private static String formatDateTime(String iso) {
        if (iso == null) return "-";
        Date d = parseIsoToDate(iso);
        if (d == null) return iso;
        return new SimpleDateFormat("dd. MMMM yyyy, HH:mm", Locale.getDefault()).format(d);
    }

    private static long minutesToStart(String iso) {
        Date d = parseIsoToDate(iso);
        if (d == null) return Long.MIN_VALUE;
        long diffMs = d.getTime() - System.currentTimeMillis();
        return diffMs / 1000 / 60;
    }

    private static Date parseIsoToDate(String iso) {
        if (iso == null) return null;
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        };
        for (String p : patterns) {
            try {
                return new SimpleDateFormat(p, Locale.getDefault()).parse(iso);
            } catch (Exception ignored) {}
        }
        return null;
    }
}
