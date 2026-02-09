package com.example.uber3.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.R;
import com.example.uber3.network.model.history.AdminRideHistoryResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminRideHistoryAdapter extends RecyclerView.Adapter<AdminRideHistoryAdapter.RideViewHolder> {

    private List<AdminRideHistoryResponse> rides = new ArrayList<>();
    private final OnRideClickListener listener;

    public interface OnRideClickListener {
        void onRideClick(AdminRideHistoryResponse ride);
    }

    public AdminRideHistoryAdapter(OnRideClickListener listener) {
        this.listener = listener;
    }

    public void setRides(List<AdminRideHistoryResponse> rides) {
        this.rides = rides != null ? rides : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_ride_history, parent, false);
        return new RideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        holder.bind(rides.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    static class RideViewHolder extends RecyclerView.ViewHolder {

        private final CardView cardView;
        private final TextView tvRideDate;
        private final TextView tvStatusBadge;
        private final TextView tvPanicBadge;

        private final TextView tvStartAddress;
        private final TextView tvEndAddress;

        private final TextView tvPrice;
        private final TextView tvStartTime;
        private final TextView tvEndTime;

        RideViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardRide);
            tvRideDate = itemView.findViewById(R.id.tvRideDate);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvPanicBadge = itemView.findViewById(R.id.tvPanicBadge);

            tvStartAddress = itemView.findViewById(R.id.tvStartAddress);
            tvEndAddress = itemView.findViewById(R.id.tvEndAddress);

            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStartTime = itemView.findViewById(R.id.tvStartTime);
            tvEndTime = itemView.findViewById(R.id.tvEndTime);
        }

        void bind(AdminRideHistoryResponse ride, OnRideClickListener listener) {
            // date (from startTime)
            tvRideDate.setText(formatDate(ride.startTime));

            // status
            String status = ride.status != null ? ride.status : "";
            tvStatusBadge.setText(getStatusText(status));
            setStatusBadgeStyle(tvStatusBadge, status);

            // panic badge
            if (ride.panic) {
                tvPanicBadge.setVisibility(View.VISIBLE);
                tvPanicBadge.setText("PANIC");
                tvPanicBadge.setBackgroundColor(0xFFFEE2E2);
                tvPanicBadge.setTextColor(0xFF991B1B);
            } else {
                tvPanicBadge.setVisibility(View.GONE);
            }

            // addresses
            String startAddr = (ride.startLocation != null && ride.startLocation.address != null)
                    ? ride.startLocation.address : "N/A";
            String endAddr = (ride.endLocation != null && ride.endLocation.address != null)
                    ? ride.endLocation.address : "N/A";

            tvStartAddress.setText(startAddr);
            tvEndAddress.setText(endAddr);

            // price
            tvPrice.setText(String.format(Locale.getDefault(), "%.0f din", ride.price));

            // times (HH:mm)
            tvStartTime.setText(formatTime(ride.startTime));
            tvEndTime.setText(formatTime(ride.endTime));

            cardView.setOnClickListener(v -> {
                if (listener != null) listener.onRideClick(ride);
            });
        }

        private String formatDate(String isoDate) {
            if (isoDate == null) return "";
            try {
                SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat out = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                Date date = in.parse(isoDate);
                return date != null ? out.format(date) : "";
            } catch (Exception e) {
                return isoDate;
            }
        }

        private String formatTime(String isoDate) {
            if (isoDate == null) return "-";
            try {
                SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat out = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date date = in.parse(isoDate);
                return date != null ? out.format(date) : "-";
            } catch (Exception e) {
                return "-";
            }
        }

        private String getStatusText(String status) {
            switch (status) {
                case "COMPLETED":
                    return "Completed";
                case "CANCELLED_BY_DRIVER":
                case "CANCELLED":
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

            if ("COMPLETED".equals(status)) {
                backgroundColor = 0xFFD1FAE5;
                textColor = 0xFF065F46;
            } else if (status != null && (status.contains("CANCELLED") || status.equals("CANCELLED"))) {
                backgroundColor = 0xFFFEE2E2;
                textColor = 0xFF991B1B;
            } else if ("FINISHED_EARLY".equals(status)) {
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
