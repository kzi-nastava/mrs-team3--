package com.example.uber3;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomeFragment extends Fragment {

    private static final String ARG_ROLE = "role";
    private static final long VEHICLE_POLL_INTERVAL_MS = 20_000L; // 20 seconds

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;

    public HomeFragment() {}

    public static HomeFragment newInstance(String role) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROLE, role);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        FloatingActionButton fab = view.findViewById(R.id.bookRideFab);

        fab.setOnClickListener(v -> {
            fab.hide();

            RideBookingFragment sheet = RideBookingFragment.newInstance();
            sheet.setOnDismissCallback(fab::show);
            MapFragment.setOnLocationSelectedListener(sheet::setPickupFromMap);

            sheet.show(getParentFragmentManager(), "RideBooking");
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getChildFragmentManager().executePendingTransactions();
        startVehiclePolling();
    }

    @Override
    public void onResume() {
        super.onResume();
        startVehiclePolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopVehiclePolling();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopVehiclePolling();
    }

    private void startVehiclePolling() {
        stopVehiclePolling();

        pollRunnable = new Runnable() {
            @Override
            public void run() {
                refreshVehiclesOnMap();
                pollHandler.postDelayed(this, VEHICLE_POLL_INTERVAL_MS);
            }
        };

        pollHandler.post(pollRunnable);
    }

    private void stopVehiclePolling() {
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
            pollRunnable = null;
        }
    }

    private void refreshVehiclesOnMap() {
        MapFragment mapFragment = (MapFragment) getChildFragmentManager()
                .findFragmentById(R.id.homeMapContainer);

        if (mapFragment != null) {
            mapFragment.loadAndShowActiveVehicles();
        }
    }
}