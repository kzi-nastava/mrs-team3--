package com.example.uber3.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.R;
import com.example.uber3.network.model.driver.DriverRide;
import com.example.uber3.network.model.driver.Location;
import com.example.uber3.network.model.driver.PendingRide;

import java.util.ArrayList;
import java.util.List;

public class DriverRidesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DRIVER_RIDE = 1;
    private static final int TYPE_PENDING_RIDE = 2;

    private List<Object> rides = new ArrayList<>();
    private OnDriverRideClickListener driverRideListener;
    private OnPendingRideClickListener pendingRideListener;

    public interface OnDriverRideClickListener {
        void onRideClick(DriverRide ride);
    }

    public interface OnPendingRideClickListener {
        void onAcceptClick(PendingRide ride);
    }

    public DriverRidesAdapter(List<Object> rides,
                              OnDriverRideClickListener driverRideListener,
                              OnPendingRideClickListener pendingRideListener) {
        this.rides = rides != null ? rides : new ArrayList<>();
        this.driverRideListener = driverRideListener;
        this.pendingRideListener = pendingRideListener;
    }

    public void updateRides(List<DriverRide> newRides) {
        this.rides.clear();
        if (newRides != null) {
            this.rides.addAll(newRides);
        }
        notifyDataSetChanged();
    }

    public void updatePendingRides(List<PendingRide> newRides) {
        this.rides.clear();
        if (newRides != null) {
            this.rides.addAll(newRides);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = rides.get(position);
        if (item instanceof DriverRide) {
            return TYPE_DRIVER_RIDE;
        } else {
            return TYPE_PENDING_RIDE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_DRIVER_RIDE) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_driver_ride, parent, false);
            return new DriverRideViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pending_ride, parent, false);
            return new PendingRideViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = rides.get(position);
        if (holder instanceof DriverRideViewHolder) {
            ((DriverRideViewHolder) holder).bind((DriverRide) item, driverRideListener);
        } else if (holder instanceof PendingRideViewHolder) {
            ((PendingRideViewHolder) holder).bind((PendingRide) item, pendingRideListener);
        }
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    // ViewHolder for DriverRide
    static class DriverRideViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final TextView tvRideNumber;
        private final TextView tvStatus;
        private final TextView tvStartAddress;
        private final TextView tvEndAddress;
        private final TextView tvDistance;
        private final TextView tvPrice;
        private final LinearLayout layoutStops;

        public DriverRideViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardRide);
            tvRideNumber = itemView.findViewById(R.id.tvRideNumber);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvStartAddress = itemView.findViewById(R.id.tvStartAddress);
            tvEndAddress = itemView.findViewById(R.id.tvEndAddress);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            layoutStops = itemView.findViewById(R.id.layoutStops);
        }

        public void bind(DriverRide ride, OnDriverRideClickListener listener) {
            tvRideNumber.setText("Ride #" + ride.rideId);
            tvStatus.setText(ride.status);
            tvStartAddress.setText("🟢 " + ride.startLocation.address);
            tvEndAddress.setText("🔴 " + ride.endLocation.address);
            tvDistance.setText(String.format("%.1f km", ride.distance));
            tvPrice.setText(String.format("%.2f RSD", ride.calculatedPrice));

            // Set status badge styling
            setStatusBadgeStyle(tvStatus, ride.status);

            // Add stops
            layoutStops.removeAllViews();
            for (int i = 0; i < ride.stops.size(); i++) {
                Location stop = ride.stops.get(i);
                boolean reached = ride.stopStatuses != null &&
                        i < ride.stopStatuses.size() &&
                        ride.stopStatuses.get(i).reached;

                TextView tvStop = new TextView(itemView.getContext());
                tvStop.setText((reached ? "🟡" : "🔵") + " Stop " + (i + 1) + ": " + stop.address);
                tvStop.setPadding(0, 8, 0, 8);
                layoutStops.addView(tvStop);
            }

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRideClick(ride);
                }
            });
        }

        private void setStatusBadgeStyle(TextView badge, String status) {
            int backgroundColor;
            int textColor;

            switch (status) {
                case "PENDING":
                    backgroundColor = 0xFFFEF3C7;
                    textColor = 0xFF92400E;
                    break;
                case "ACCEPTED":
                    backgroundColor = 0xFFDBEAFE;
                    textColor = 0xFF1E40AF;
                    break;
                case "IN_PROGRESS":
                    backgroundColor = 0xFFD1FAE5;
                    textColor = 0xFF065F46;
                    break;
                case "COMPLETED":
                    backgroundColor = 0xFFE5E7EB;
                    textColor = 0xFF1F2937;
                    break;
                case "CANCELLED":
                    backgroundColor = 0xFFFEE2E2;
                    textColor = 0xFF991B1B;
                    break;
                default:
                    backgroundColor = 0xFFE5E7EB;
                    textColor = 0xFF374151;
            }

            badge.setBackgroundColor(backgroundColor);
            badge.setTextColor(textColor);
        }
    }

    // ViewHolder for PendingRide
    static class PendingRideViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final TextView tvRideNumber;
        private final TextView tvBadge;
        private final TextView tvStartAddress;
        private final TextView tvEndAddress;
        private final TextView tvDistance;
        private final TextView tvPrice;
        private final LinearLayout layoutStops;
        private final Button btnAccept;

        public PendingRideViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardRide);
            tvRideNumber = itemView.findViewById(R.id.tvRideNumber);
            tvBadge = itemView.findViewById(R.id.tvBadge);
            tvStartAddress = itemView.findViewById(R.id.tvStartAddress);
            tvEndAddress = itemView.findViewById(R.id.tvEndAddress);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            layoutStops = itemView.findViewById(R.id.layoutStops);
            btnAccept = itemView.findViewById(R.id.btnAccept);
        }

        public void bind(PendingRide ride, OnPendingRideClickListener listener) {
            tvRideNumber.setText("Ride #" + ride.rideId);
            tvBadge.setText("Available");
            tvStartAddress.setText("🟢 " + ride.startLocation.address);
            tvEndAddress.setText("🔴 " + ride.endLocation.address);
            tvDistance.setText(String.format("%.1f km", ride.distance));
            tvPrice.setText(String.format("%.2f RSD", ride.calculatedPrice));

            // Add stops
            layoutStops.removeAllViews();
            for (int i = 0; i < ride.stops.size(); i++) {
                Location stop = ride.stops.get(i);
                TextView tvStop = new TextView(itemView.getContext());
                tvStop.setText("🔵 Stop " + (i + 1) + ": " + stop.address);
                tvStop.setPadding(0, 8, 0, 8);
                layoutStops.addView(tvStop);
            }

            btnAccept.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAcceptClick(ride);
                }
            });
        }
    }
}