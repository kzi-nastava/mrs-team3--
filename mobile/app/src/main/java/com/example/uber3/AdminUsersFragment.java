package com.example.uber3;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.adapter.ActiveDriversAdapter;
import com.example.uber3.adapter.AdminUsersAdapter;
import com.example.uber3.network.api.*;
import com.example.uber3.network.model.user.admin.*;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.*;

public class AdminUsersFragment extends Fragment {

    private TabLayout tabLayout;
    private FrameLayout layoutUsersTab;
    private FrameLayout layoutDriversTab;
    private RecyclerView rvUsers, rvDrivers;
    private ProgressBar pbUsers, pbDrivers;
    private EditText etDriverSearch;
    private ImageButton btnClearSearch;
    private LinearLayout layoutDriversEmpty;
    private TextView tvDriversEmptyMessage;

    private AdminUsersAdapter usersAdapter;
    private ActiveDriversAdapter driversAdapter;

    private final List<AdminUserDetailsDto> users = new ArrayList<>();
    private final List<ActiveDriverDto> allDrivers = new ArrayList<>();
    private final List<ActiveDriverDto> filteredDrivers = new ArrayList<>();

    public static AdminUsersFragment newInstance() {
        return new AdminUsersFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initViews(view);
        setupAdapters();
        setupTabs();
        setupSearch();

        showUsersTab();
        loadUsers();
        loadDrivers();
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tabLayout);
        layoutUsersTab = view.findViewById(R.id.layoutUsersTab);
        layoutDriversTab = view.findViewById(R.id.layoutDriversTab);
        rvUsers = view.findViewById(R.id.rvUsers);
        rvDrivers = view.findViewById(R.id.rvDrivers);
        pbUsers = view.findViewById(R.id.pbUsers);
        pbDrivers = view.findViewById(R.id.pbDrivers);
        etDriverSearch = view.findViewById(R.id.etDriverSearch);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        layoutDriversEmpty = view.findViewById(R.id.layoutDriversEmpty);
        tvDriversEmptyMessage = view.findViewById(R.id.tvDriversEmptyMessage);
    }

    private void setupAdapters() {
        usersAdapter = new AdminUsersAdapter(users, user -> {
            UserDetailsDialog.show(requireContext(), user, () -> {
                loadUsers();
                loadDrivers();
            });
        });

        driversAdapter = new ActiveDriversAdapter(filteredDrivers, driver -> {
            // Navigate to AdminRideTrackingFragment
            navigateToRideTracking(driver);
        });

        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUsers.setAdapter(usersAdapter);

        rvDrivers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDrivers.setAdapter(driversAdapter);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All Users"));
        tabLayout.addTab(tabLayout.newTab().setText("Active Drivers"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showUsersTab();
                } else {
                    showDriversTab();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupSearch() {
        etDriverSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDrivers(s.toString());
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnClearSearch.setOnClickListener(v -> {
            etDriverSearch.setText("");
            filterDrivers("");
        });
    }

    private void filterDrivers(String query) {
        filteredDrivers.clear();

        if (query.trim().isEmpty()) {
            filteredDrivers.addAll(allDrivers);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (ActiveDriverDto driver : allDrivers) {
                String fullName = (driver.name + " " + driver.surname).toLowerCase();
                if (fullName.contains(lowerQuery)) {
                    filteredDrivers.add(driver);
                }
            }
        }

        driversAdapter.notifyDataSetChanged();
        updateDriversEmptyState(query);
    }

    private void updateDriversEmptyState(String query) {
        if (filteredDrivers.isEmpty()) {
            layoutDriversEmpty.setVisibility(View.VISIBLE);
            rvDrivers.setVisibility(View.GONE);

            if (!query.trim().isEmpty()) {
                tvDriversEmptyMessage.setText("No drivers found matching \"" + query + "\"");
            } else {
                tvDriversEmptyMessage.setText("No active drivers with rides in progress");
            }
        } else {
            layoutDriversEmpty.setVisibility(View.GONE);
            rvDrivers.setVisibility(View.VISIBLE);
        }
    }

    private void showUsersTab() {
        layoutUsersTab.setVisibility(View.VISIBLE);
        layoutDriversTab.setVisibility(View.GONE);
    }

    private void showDriversTab() {
        layoutDriversTab.setVisibility(View.VISIBLE);
        layoutUsersTab.setVisibility(View.GONE);
    }

    private ApiService api() {
        return ApiClient.getClient(requireContext()).create(ApiService.class);
    }

    private void loadUsers() {
        pbUsers.setVisibility(View.VISIBLE);

        api().getAdminUsersDetails().enqueue(new Callback<List<AdminUserDetailsDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<AdminUserDetailsDto>> call,
                                   @NonNull Response<List<AdminUserDetailsDto>> res) {
                pbUsers.setVisibility(View.GONE);
                if (!res.isSuccessful() || res.body() == null) return;

                users.clear();
                for (AdminUserDetailsDto u : res.body()) {
                    if (u.role != null && !"ADMIN".equals(u.role)) {
                        users.add(u);
                    }
                }
                usersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(@NonNull Call<List<AdminUserDetailsDto>> call, @NonNull Throwable t) {
                pbUsers.setVisibility(View.GONE);
            }
        });
    }

    private void loadDrivers() {
        pbDrivers.setVisibility(View.VISIBLE);

        api().getActiveDrivers().enqueue(new Callback<List<ActiveDriverDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ActiveDriverDto>> call,
                                   @NonNull Response<List<ActiveDriverDto>> res) {
                pbDrivers.setVisibility(View.GONE);
                if (!res.isSuccessful() || res.body() == null) return;

                allDrivers.clear();
                allDrivers.addAll(res.body());

                filterDrivers(etDriverSearch.getText().toString());
            }

            @Override
            public void onFailure(@NonNull Call<List<ActiveDriverDto>> call, @NonNull Throwable t) {
                pbDrivers.setVisibility(View.GONE);
            }
        });
    }

    private void navigateToRideTracking(ActiveDriverDto driver) {
        // Replace current fragment with AdminRideTrackingFragment
        AdminRideTrackingFragment trackingFragment = AdminRideTrackingFragment.newInstance(driver.id);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, trackingFragment)
                .addToBackStack(null)
                .commit();
    }
}