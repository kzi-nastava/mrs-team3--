package com.example.uber3;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Toast;
import com.example.uber3.network.model.AdminDriverProfileChangeRequestDto;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.uber3.network.service.AdminProfileChangeService;


public class DriverChangeRequestFragment extends Fragment {

    private Button btnAll, btnPending, btnApproved, btnRejected;
    private RecyclerView recycler;
    private ChangeRequestAdapter adapter;
    private AdminProfileChangeService service;

    private List<AdminDriverProfileChangeRequestDto> allRequests;


    public DriverChangeRequestFragment() {
        super(R.layout.fragment_driver_change_request);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        btnAll = view.findViewById(R.id.btnAll);
        btnPending = view.findViewById(R.id.btnPending);
        btnApproved = view.findViewById(R.id.btnApproved);
        btnRejected = view.findViewById(R.id.btnRejected);

        btnAll.setOnClickListener(v -> {
            selectFilter(btnAll);
            filter("ALL");
        });

        btnPending.setOnClickListener(v -> {
            selectFilter(btnPending);
            filter("PENDING");
        });

        btnApproved.setOnClickListener(v -> {
            selectFilter(btnApproved);
            filter("APPROVED");
        });

        btnRejected.setOnClickListener(v -> {
            selectFilter(btnRejected);
            filter("REJECTED");
        });

        recycler = view.findViewById(R.id.requestsRecycler);

        recycler.setLayoutManager(
                new LinearLayoutManager(getContext())
        );

        // ✅ FIXED SWIPE
        new androidx.recyclerview.widget.ItemTouchHelper(
                new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                        0,
                        androidx.recyclerview.widget.ItemTouchHelper.LEFT |
                                androidx.recyclerview.widget.ItemTouchHelper.RIGHT
                ) {

                    @Override
                    public boolean onMove(
                            RecyclerView r,
                            RecyclerView.ViewHolder v,
                            RecyclerView.ViewHolder t) {
                        return false;
                    }

                    @Override
                    public void onSwiped(
                            RecyclerView.ViewHolder vh,
                            int dir) {

                        int pos = vh.getAdapterPosition();

                        if (adapter == null) return;

                        // 👉 koristi adapter listu
                        var req = adapter.getItem(pos);

                        // 👉 dozvoli samo PENDING
                        if (!"PENDING".equalsIgnoreCase(req.status)) {

                            Toast.makeText(getContext(),
                                    "Only pending requests can be decided",
                                    Toast.LENGTH_SHORT).show();

                            adapter.notifyItemChanged(pos);
                            return;
                        }

                        if (dir ==
                                androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {

                            decide(req.requestId, true);
                        } else {
                            decide(req.requestId, false);
                        }
                    }
                }
        ).attachToRecyclerView(recycler);

        service = new AdminProfileChangeService(getContext());

        loadRequests();
        selectFilter(btnAll);
    }


    private void selectFilter(Button active) {

        btnAll.setAlpha(0.5f);
        btnPending.setAlpha(0.5f);
        btnApproved.setAlpha(0.5f);
        btnRejected.setAlpha(0.5f);

        active.setAlpha(1f);
    }

    private void loadRequests() {

        service.getAll(
                new Callback<List<AdminDriverProfileChangeRequestDto>>() {

                    @Override
                    public void onResponse(
                            Call<List<AdminDriverProfileChangeRequestDto>> call,
                            Response<List<AdminDriverProfileChangeRequestDto>> response
                    ) {

                        if (!response.isSuccessful()
                                || response.body() == null) return;

                        allRequests = response.body();
                        adapter = new ChangeRequestAdapter(
                                allRequests,
                                DriverChangeRequestFragment.this::openDetails
                        );

                        recycler.setAdapter(adapter);

                    }

                    @Override
                    public void onFailure(
                            Call<List<AdminDriverProfileChangeRequestDto>> call,
                            Throwable t
                    ) {
                        Toast.makeText(getContext(),
                                "Failed to load",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filter(String status) {

        if (allRequests == null) return;

        List<AdminDriverProfileChangeRequestDto> filtered =
                new ArrayList<>();

        for (var r : allRequests) {
            if (status.equals("ALL")
                    || r.status.equalsIgnoreCase(status)) {
                filtered.add(r);
            }
        }

        adapter = new ChangeRequestAdapter(
                filtered,
                DriverChangeRequestFragment.this::openDetails
        );
        recycler.setAdapter(adapter);
    }

    private void openDetails(
            AdminDriverProfileChangeRequestDto req
    ) {

        service.getDetails(
                req.requestId,
                new Callback<>() {

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onResponse(Call call, Response response) {

                        if (!response.isSuccessful()
                                || response.body() == null) return;

                        var d =
                                (com.example.uber3.network.model
                                        .AdminDriverProfileChangeRequestDetailsDto)
                                        response.body();

                        View v = getLayoutInflater()
                                .inflate(R.layout.dialog_request_details, null);

                        ((android.widget.TextView)v.findViewById(R.id.tvTitle))
                                .setText("Request #" + d.requestId);

                        ((android.widget.TextView)v.findViewById(R.id.tvStatus))
                                .setText("Status: " + d.status);

                        ((android.widget.TextView)v.findViewById(R.id.tvDriver))
                                .setText("Driver: "
                                        + d.driverFirstName + " "
                                        + d.driverLastName);

                        ((android.widget.TextView)v.findViewById(R.id.tvEmail))
                                .setText("Email: " + d.driverEmail);

                        ((android.widget.TextView)v.findViewById(R.id.tvRequested))
                                .setText("Requested: " + d.requestedAt);

                        ((android.widget.TextView)v.findViewById(R.id.tvNameChange))
                                .setText("Name: "
                                        + d.oldFirstName
                                        + " → "
                                        + d.newFirstName);

                        ((android.widget.TextView)v.findViewById(R.id.tvPhoneChange))
                                .setText("Phone: "
                                        + d.oldPhoneNumber
                                        + " → "
                                        + d.newPhoneNumber);

                        ((android.widget.TextView)v.findViewById(R.id.tvAddressChange))
                                .setText("Address: "
                                        + d.oldAddress
                                        + " → "
                                        + d.newAddress);

                        boolean pending =
                                "PENDING".equalsIgnoreCase(d.status);

                        androidx.appcompat.app.AlertDialog.Builder builder =
                                new androidx.appcompat.app.AlertDialog.Builder(getContext())
                                        .setView(v)
                                        .setNeutralButton("Close", null);

                        if (pending) {

                            builder.setPositiveButton("Approve", (di,w) -> {
                                decide(d.requestId, true);
                            });

                            builder.setNegativeButton("Reject", (di,w) -> {
                                decide(d.requestId, false);
                            });
                        }

                        builder.show();

                    }

                    @Override
                    public void onFailure(Call call, Throwable t) {
                        Toast.makeText(getContext(),
                                "Failed to load details",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void decide(Long id, boolean approve) {

        service.decide(
                id,
                new com.example.uber3.network.model
                        .AdminProfileChangeDecisionDto(
                        approve,
                        approve ? null : "Rejected by admin"
                ),
                new retrofit2.Callback<Void>() {

                    @Override
                    public void onResponse(
                            retrofit2.Call<Void> call,
                            retrofit2.Response<Void> response
                    ) {

                        Toast.makeText(getContext(),
                                approve ? "Approved!" : "Rejected!",
                                Toast.LENGTH_SHORT).show();

                        loadRequests(); // refresh list
                    }

                    @Override
                    public void onFailure(
                            retrofit2.Call<Void> call,
                            Throwable t
                    ) {
                        Toast.makeText(getContext(),
                                "Decision failed",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }



}
