package com.example.uber3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.adapter.IncomingRideAdapter;
import com.example.uber3.network.model.tracking.IncomingRideResponse;
import com.example.uber3.network.service.IncomingRideService;

import java.util.ArrayList;
import java.util.List;

public class IncomingRideFragment extends Fragment implements IncomingRideAdapter.Listener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout tvEmptyState;

    private IncomingRideAdapter adapter;
    private IncomingRideService service;

    private List<IncomingRideResponse> rides = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_incoming_ride, container, false);

        recyclerView = v.findViewById(R.id.recyclerViewIncoming);
        progressBar = v.findViewById(R.id.progressBar);
        tvEmptyState = v.findViewById(R.id.tvEmptyState);

        adapter = new IncomingRideAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);

        service = new IncomingRideService(requireContext());

        loadIncoming();
        return v;
    }

    private void loadIncoming() {
        showLoading(true);

        service.getIncomingRides(new IncomingRideService.IncomingRidesCallback() {
            @Override
            public void onSuccess(List<IncomingRideResponse> res) {
                showLoading(false);
                rides = (res != null) ? res : new ArrayList<>();
                updateUI(rides);
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                rides = new ArrayList<>();
                updateUI(rides);
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void updateUI(List<IncomingRideResponse> list) {
        if (list == null || list.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            adapter.setRides(new ArrayList<>());
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.setRides(list);
        }
    }

    @Override
    public void onCancelClicked(IncomingRideResponse ride) {
        if (ride == null || ride.id == null) return;

        service.cancelRide(ride.id, new IncomingRideService.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), "Ride cancelled", Toast.LENGTH_SHORT).show();
                loadIncoming();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), "Cancel failed: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRideClicked(IncomingRideResponse ride) {
        Toast.makeText(requireContext(),
                "Ride #" + (ride != null ? ride.id : ""),
                Toast.LENGTH_SHORT).show();
    }
}
