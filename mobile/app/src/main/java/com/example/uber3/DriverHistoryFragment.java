package com.example.uber3;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DriverHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private RideHistoryAdapter adapter;
    private List<RideHistory> rideList;
    private List<RideHistory> filteredList;

    private TextView tvFromDate, tvToDate;
    private MaterialButton btnViewReport;

    private Long startDateMillis = null;
    private Long endDateMillis = null;

    private String currentSortField = "id";
    private boolean isAscending = true;

    public static DriverHistoryFragment newInstance() {
        return new DriverHistoryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driver_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();
        loadMockData();
        setupDatePickers();
        setupSortHeaders(view);
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewHistory);
        tvFromDate = view.findViewById(R.id.tvFromDate);
        tvToDate = view.findViewById(R.id.tvToDate);
        btnViewReport = view.findViewById(R.id.btnViewReport);

        btnViewReport.setOnClickListener(v -> filterByDate());
    }

    private void setupRecyclerView() {
        rideList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new RideHistoryAdapter(filteredList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupDatePickers() {
        tvFromDate.setOnClickListener(v -> showDatePicker(true));
        tvToDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isStartDate) {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isStartDate ? "Select Start Date" : "Select End Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            String dateString = sdf.format(new Date(selection));

            if (isStartDate) {
                startDateMillis = selection;
                tvFromDate.setText(dateString);
            } else {
                endDateMillis = selection;
                tvToDate.setText(dateString);
            }
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void filterByDate() {
        if (startDateMillis == null || endDateMillis == null) {
            filteredList.clear();
            filteredList.addAll(rideList);
            adapter.notifyDataSetChanged();
            return;
        }

        filteredList.clear();
        for (RideHistory ride : rideList) {
            if (ride.getStartTimeMillis() >= startDateMillis && ride.getStartTimeMillis() <= endDateMillis) {
                filteredList.add(ride);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setupSortHeaders(View view) {
        LinearLayout headerID = view.findViewById(R.id.headerID);
        LinearLayout headerStart = view.findViewById(R.id.headerStart);
        LinearLayout headerEnd = view.findViewById(R.id.headerEnd);
        LinearLayout headerFrom = view.findViewById(R.id.headerFrom);
        LinearLayout headerTo = view.findViewById(R.id.headerTo);
        LinearLayout headerPrice = view.findViewById(R.id.headerPrice);
        LinearLayout headerCancelled = view.findViewById(R.id.headerCancelled);
        LinearLayout headerPanic = view.findViewById(R.id.headerPanic);

        headerID.setOnClickListener(v -> sortBy("id"));
        headerStart.setOnClickListener(v -> sortBy("start"));
        headerEnd.setOnClickListener(v -> sortBy("end"));
        headerFrom.setOnClickListener(v -> sortBy("from"));
        headerTo.setOnClickListener(v -> sortBy("to"));
        headerPrice.setOnClickListener(v -> sortBy("price"));
        headerCancelled.setOnClickListener(v -> sortBy("cancelled"));
        headerPanic.setOnClickListener(v -> sortBy("panic"));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void sortBy(String field) {
        if (currentSortField.equals(field)) {
            isAscending = !isAscending;
        } else {
            currentSortField = field;
            isAscending = true;
        }

        Comparator<RideHistory> comparator = null;

        switch (field) {
            case "id":
                comparator = Comparator.comparingLong(RideHistory::getId);
                break;
            case "start":
                comparator = Comparator.comparing(RideHistory::getStartTime);
                break;
            case "end":
                comparator = Comparator.comparing(RideHistory::getEndTime);
                break;
            case "from":
                comparator = Comparator.comparing(RideHistory::getFromAddress);
                break;
            case "to":
                comparator = Comparator.comparing(RideHistory::getToAddress);
                break;
            case "price":
                comparator = Comparator.comparingDouble(RideHistory::getPrice);
                break;
            case "cancelled":
                comparator = Comparator.comparing(RideHistory::isCancelled);
                break;
            case "panic":
                comparator = Comparator.comparing(RideHistory::isPanic);
                break;
        }

        if (comparator != null) {
            if (!isAscending) {
                comparator = comparator.reversed();
            }
            Collections.sort(filteredList, comparator);
            adapter.notifyDataSetChanged();
        }
    }

    private void loadMockData() {
        rideList.add(new RideHistory(1, "10:00", "10:25", "Main Street", "University",
                "prle, Andjela", 85.0, false, false, System.currentTimeMillis() - 86400000));
        rideList.add(new RideHistory(2, "12:30", "12:55", "Airport", "City Center",
                "Marko, Jovan, Ana", 150.0, true, false, System.currentTimeMillis() - 172800000));
        rideList.add(new RideHistory(3, "15:00", "15:30", "Downtown", "Suburban Area",
                "Stefan", 120.0, false, false, System.currentTimeMillis() - 259200000));
        rideList.add(new RideHistory(4, "08:00", "08:45", "Train Station", "Shopping Mall",
                "Milica, Nikola", 95.0, false, true, System.currentTimeMillis() - 345600000));

        filteredList.addAll(rideList);
        adapter.notifyDataSetChanged();
    }

    // Inner class for RideHistory data model
    public static class RideHistory {
        private long id;
        private String startTime;
        private String endTime;
        private String fromAddress;
        private String toAddress;
        private String passengers;
        private double price;
        private boolean cancelled;
        private boolean panic;
        private long startTimeMillis;

        public RideHistory(long id, String startTime, String endTime, String fromAddress,
                           String toAddress, String passengers, double price,
                           boolean cancelled, boolean panic, long startTimeMillis) {
            this.id = id;
            this.startTime = startTime;
            this.endTime = endTime;
            this.fromAddress = fromAddress;
            this.toAddress = toAddress;
            this.passengers = passengers;
            this.price = price;
            this.cancelled = cancelled;
            this.panic = panic;
            this.startTimeMillis = startTimeMillis;
        }

        public long getId() { return id; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public String getFromAddress() { return fromAddress; }
        public String getToAddress() { return toAddress; }
        public String getPassengers() { return passengers; }
        public double getPrice() { return price; }
        public boolean isCancelled() { return cancelled; }
        public boolean isPanic() { return panic; }
        public long getStartTimeMillis() { return startTimeMillis; }
        public String getCancelledDisplay() { return cancelled ? "Yes (Driver)" : "No"; }
        public String getPanicDisplay() { return panic ? "Yes" : "No"; }
    }

    // Adapter class
    private static class RideHistoryAdapter extends RecyclerView.Adapter<RideHistoryAdapter.ViewHolder> {

        private final List<RideHistory> rides;

        public RideHistoryAdapter(List<RideHistory> rides) {
            this.rides = rides;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ride_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RideHistory ride = rides.get(position);
            holder.bind(ride);
        }

        @Override
        public int getItemCount() {
            return rides.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvId, tvStart, tvEnd, tvFrom, tvTo, tvPassengers, tvPrice, tvCancelled, tvPanic;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvId = itemView.findViewById(R.id.tvId);
                tvStart = itemView.findViewById(R.id.tvStart);
                tvEnd = itemView.findViewById(R.id.tvEnd);
                tvFrom = itemView.findViewById(R.id.tvFrom);
                tvTo = itemView.findViewById(R.id.tvTo);
                tvPassengers = itemView.findViewById(R.id.tvPassengers);
                tvPrice = itemView.findViewById(R.id.tvPrice);
                tvCancelled = itemView.findViewById(R.id.tvCancelled);
                tvPanic = itemView.findViewById(R.id.tvPanic);
            }

            public void bind(RideHistory ride) {
                tvId.setText(String.valueOf(ride.getId()));
                tvStart.setText(ride.getStartTime());
                tvEnd.setText(ride.getEndTime());
                tvFrom.setText(ride.getFromAddress());
                tvTo.setText(ride.getToAddress());
                tvPassengers.setText(ride.getPassengers());
                tvPrice.setText(String.valueOf((int) ride.getPrice()));
                tvCancelled.setText(ride.getCancelledDisplay());
                tvPanic.setText(ride.getPanicDisplay());
            }
        }
    }
}