package com.example.uber3.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.R;
import com.example.uber3.network.model.history.PassengerRideSummaryResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PassengerRideHistoryAdapter extends RecyclerView.Adapter<PassengerRideHistoryAdapter.VH> {

    private List<PassengerRideSummaryResponse> rides = new ArrayList<>();
    private final OnRideClickListener listener;

    public interface OnRideClickListener {
        void onRideClick(PassengerRideSummaryResponse ride);
    }

    public PassengerRideHistoryAdapter(OnRideClickListener listener) {
        this.listener = listener;
    }

    public void setRides(List<PassengerRideSummaryResponse> rides) {
        this.rides = rides != null ? rides : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_passenger_ride_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(rides.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvRoute, tvStart, tvEnd, tvFav;

        VH(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardRide);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvStart = itemView.findViewById(R.id.tvStartDate);
            tvEnd = itemView.findViewById(R.id.tvEndDate);
            tvFav = itemView.findViewById(R.id.tvFavoriteBadge);
        }

        void bind(PassengerRideSummaryResponse r, OnRideClickListener listener) {
            String s = (r.startLocation != null && r.startLocation.address != null) ? r.startLocation.address : "N/A";
            String e = (r.endLocation != null && r.endLocation.address != null) ? r.endLocation.address : "N/A";
            tvRoute.setText(s + " → " + e);

            tvStart.setText("Start: " + formatDateTime(r.startTime));
            tvEnd.setText("End: " + (r.endTime != null ? formatDateTime(r.endTime) : "-"));

            if (r.favorite) {
                tvFav.setVisibility(View.VISIBLE);
                tvFav.setText("★ FAVORITE");
            } else {
                tvFav.setVisibility(View.GONE);
            }

            card.setOnClickListener(v -> { if (listener != null) listener.onRideClick(r); });
        }

        private String formatDateTime(String iso) {
            Date d = parseIso(iso);
            if (d == null) return iso != null ? iso : "-";
            return new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(d);
        }

        private Date parseIso(String iso) {
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
}
