package com.example.uber3;

import android.os.Bundle;
import android.view.*;
import android.widget.ProgressBar;

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
    private RecyclerView rvUsers, rvDrivers;
    private ProgressBar pbUsers, pbDrivers;

    private AdminUsersAdapter usersAdapter;
    private ActiveDriversAdapter driversAdapter;

    private final List<AdminUserDetailsDto> users = new ArrayList<>();
    private final List<ActiveDriverDto> drivers = new ArrayList<>();

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
        tabLayout = view.findViewById(R.id.tabLayout);
        rvUsers = view.findViewById(R.id.rvUsers);
        rvDrivers = view.findViewById(R.id.rvDrivers);
        pbUsers = view.findViewById(R.id.pbUsers);
        pbDrivers = view.findViewById(R.id.pbDrivers);

        usersAdapter = new AdminUsersAdapter(users, user -> {
            UserDetailsDialog.show(requireContext(), user, () -> {
                loadUsers();
                loadDrivers();
            });
        });

        driversAdapter = new ActiveDriversAdapter(drivers);

        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUsers.setAdapter(usersAdapter);

        rvDrivers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDrivers.setAdapter(driversAdapter);

        tabLayout.addTab(tabLayout.newTab().setText("All Users"));
        tabLayout.addTab(tabLayout.newTab().setText("Active Drivers"));

        showUsersTab();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) showUsersTab();
                else showDriversTab();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadUsers();
        loadDrivers();
    }

    private void showUsersTab() {
        rvUsers.setVisibility(View.VISIBLE);
        rvDrivers.setVisibility(View.GONE);
    }

    private void showDriversTab() {
        rvDrivers.setVisibility(View.VISIBLE);
        rvUsers.setVisibility(View.GONE);
    }


    private ApiService api() {
        return ApiClient.getClient(requireContext()).create(ApiService.class);
    }

    private void loadUsers() {
        pbUsers.setVisibility(View.VISIBLE);

        api().getAdminUsersDetails().enqueue(new Callback<List<AdminUserDetailsDto>>() {
            @Override
            public void onResponse(Call<List<AdminUserDetailsDto>> call, Response<List<AdminUserDetailsDto>> res) {
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
            public void onResponse(@NonNull Call<List<ActiveDriverDto>> call, @NonNull Response<List<ActiveDriverDto>> res) {
                pbDrivers.setVisibility(View.GONE);
                if (!res.isSuccessful() || res.body() == null) return;

                drivers.clear();
                drivers.addAll(res.body());
                driversAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(@NonNull Call<List<ActiveDriverDto>> call, @NonNull Throwable t) {
                pbDrivers.setVisibility(View.GONE);
            }
        });
    }
}
