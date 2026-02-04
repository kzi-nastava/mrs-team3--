package com.example.uber3.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.R;
import com.example.uber3.network.model.history.DriverRideHistoryResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RideHistoryAdapter extends RecyclerView.Adapter<RideHistoryAdapter.RideViewHolder> {

    private List<DriverRideHistoryResponse> rides = new ArrayList<>();
    private OnRideClickListener listener;

    public interface OnRideClickListener {
        void onRideClick(DriverRideHistoryResponse ride);
    }

    public RideHistoryAdapter(OnRideClickListener listener) {
        this.listener = listener;
    }

    public void setRides(List<DriverRideHistoryResponse> rides) {
        this.rides = rides != null ? rides : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void clearRides() {
        this.rides.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ride_history, parent, false);
        return new RideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        DriverRideHistoryResponse ride = rides.get(position);
        holder.bind(ride, listener);
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    static class RideViewHolder extends RecyclerView.ViewHolder {

        private final CardView cardView;
        private final TextView tvRideDate;
        private final TextView tvStatusBadge;
        private final TextView tvStartAddress;
        private final TextView tvEndAddress;
        private final TextView tvPrice;
        private final TextView tvDistance;
        private final TextView tvDuration;

        public RideViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardRide);
            tvRideDate = itemView.findViewById(R.id.tvRideDate);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvStartAddress = itemView.findViewById(R.id.tvStartAddress);
            tvEndAddress = itemView.findViewById(R.id.tvEndAddress);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvDuration = itemView.findViewById(R.id.tvDuration);
        }

        public void bind(DriverRideHistoryResponse ride, OnRideClickListener listener) {
            tvRideDate.setText(formatDate(ride.startedAt));

            String status = ride.getFormattedStatus();
            tvStatusBadge.setText(getStatusText(status));
            setStatusBadgeStyle(tvStatusBadge, status);

            tvStartAddress.setText(ride.startAddress);
            tvEndAddress.setText(ride.endAddress);

            tvPrice.setText(String.format(Locale.getDefault(), "%.0f din", ride.price));
            tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", ride.distance));
            tvDuration.setText(String.format(Locale.getDefault(), "%d min", ride.getDurationMinutes()));

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRideClick(ride);
                }
            });
        }

        private String formatDate(String isoDate) {
            if (isoDate == null) return "";
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                Date date = inputFormat.parse(isoDate);
                return date != null ? outputFormat.format(date) : "";
            } catch (Exception e) {
                return isoDate;
            }
        }

        private String getStatusText(String status) {
            switch (status) {
                case "COMPLETED":
                    return "Completed";
                case "CANCELLED_BY_DRIVER":
                    return "Cancelled by Driver";
                case "CANCELLED_BY_PASSENGER":
                    return "Cancelled by Passenger";
                case "FINISHED_EARLY":
                    return "Finished Early";
                default:
                    return status;
            }
        }

        private void setStatusBadgeStyle(TextView badge, String status) {
            int backgroundColor;
            int textColor;

            if (status.equals("COMPLETED")) {
                backgroundColor = 0xFFD1FAE5;
                textColor = 0xFF065F46;
            } else if (status.contains("CANCELLED")) {
                backgroundColor = 0xFFFEE2E2;
                textColor = 0xFF991B1B;
            } else if (status.equals("FINISHED_EARLY")) {
                backgroundColor = 0xFFDBEAFE;
                textColor = 0xFF1E40AF;
            } else {
                backgroundColor = 0xFFE5E7EB;
                textColor = 0xFF374151;
            }

            badge.setBackgroundColor(backgroundColor);
            badge.setTextColor(textColor);
        }
    }
}