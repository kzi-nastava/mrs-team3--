package com.example.uber3;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.adapter.ActiveDriversAdapter;
import com.example.uber3.adapter.AdminUsersAdapter;
import com.example.uber3.network.api.*;
import com.example.uber3.network.model.user.admin.*;
import com.example.uber3.network.model.pricing.*;
import com.example.uber3.network.service.PricingService;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.*;

public class AdminUsersFragment extends Fragment {

    private TabLayout tabLayout;
    private FrameLayout layoutUsersTab;
    private FrameLayout layoutDriversTab;
    private FrameLayout layoutPricingTab;

    private RecyclerView rvUsers, rvDrivers;
    private ProgressBar pbUsers, pbDrivers;
    private EditText etDriverSearch;
    private ImageButton btnClearSearch;
    private LinearLayout layoutDriversEmpty;
    private TextView tvDriversEmptyMessage;

    // Pricing views
    private ProgressBar pbPricing;
    private LinearLayout layoutPricingContent;
    private EditText etStandardPrice, etLuxuryPrice, etVanPrice, etPricePerKm;
    private TextView tvCurrentPricing;
    private Button btnSave, btnReset;

    private AdminUsersAdapter usersAdapter;
    private ActiveDriversAdapter driversAdapter;

    private final List<AdminUserDetailsDto> users = new ArrayList<>();
    private final List<ActiveDriverDto> allDrivers = new ArrayList<>();
    private final List<ActiveDriverDto> filteredDrivers = new ArrayList<>();

    // Pricing data
    private PricingResponse currentPricing;
    private PricingConstraints constraints;

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
        setupPricingListeners();

        showUsersTab();
        loadUsers();
        loadDrivers();
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tabLayout);
        layoutUsersTab = view.findViewById(R.id.layoutUsersTab);
        layoutDriversTab = view.findViewById(R.id.layoutDriversTab);
        layoutPricingTab = view.findViewById(R.id.layoutPricingTab);

        rvUsers = view.findViewById(R.id.rvUsers);
        rvDrivers = view.findViewById(R.id.rvDrivers);
        pbUsers = view.findViewById(R.id.pbUsers);
        pbDrivers = view.findViewById(R.id.pbDrivers);
        etDriverSearch = view.findViewById(R.id.etDriverSearch);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        layoutDriversEmpty = view.findViewById(R.id.layoutDriversEmpty);
        tvDriversEmptyMessage = view.findViewById(R.id.tvDriversEmptyMessage);

        // Pricing views
        pbPricing = view.findViewById(R.id.pbPricing);
        layoutPricingContent = view.findViewById(R.id.layoutPricingContent);
        etStandardPrice = view.findViewById(R.id.etStandardPrice);
        etLuxuryPrice = view.findViewById(R.id.etLuxuryPrice);
        etVanPrice = view.findViewById(R.id.etVanPrice);
        etPricePerKm = view.findViewById(R.id.etPricePerKm);
        tvCurrentPricing = view.findViewById(R.id.tvCurrentPricing);
        btnSave = view.findViewById(R.id.btnSave);
        btnReset = view.findViewById(R.id.btnReset);
    }

    private void setupAdapters() {
        usersAdapter = new AdminUsersAdapter(users, user -> {
            UserDetailsDialog.show(requireContext(), user, () -> {
                loadUsers();
                loadDrivers();
            });
        });

        driversAdapter = new ActiveDriversAdapter(filteredDrivers, this::navigateToRideTracking);

        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUsers.setAdapter(usersAdapter);

        rvDrivers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDrivers.setAdapter(driversAdapter);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All Users"));
        tabLayout.addTab(tabLayout.newTab().setText("Active Drivers"));
        tabLayout.addTab(tabLayout.newTab().setText("📊 Pricing"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        showUsersTab();
                        break;
                    case 1:
                        showDriversTab();
                        break;
                    case 2:
                        showPricingTab();
                        break;
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

    private void setupPricingListeners() {
        btnSave.setOnClickListener(v -> savePricing());
        btnReset.setOnClickListener(v -> resetPricingForm());
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
        layoutPricingTab.setVisibility(View.GONE);
    }

    private void showDriversTab() {
        layoutDriversTab.setVisibility(View.VISIBLE);
        layoutUsersTab.setVisibility(View.GONE);
        layoutPricingTab.setVisibility(View.GONE);
    }

    private void showPricingTab() {
        layoutPricingTab.setVisibility(View.VISIBLE);
        layoutUsersTab.setVisibility(View.GONE);
        layoutDriversTab.setVisibility(View.GONE);
        loadPricingData();
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

    private void loadPricingData() {
        pbPricing.setVisibility(View.VISIBLE);
        layoutPricingContent.setVisibility(View.GONE);

        PricingService.getCurrentPricing(requireContext(), new PricingService.PricingCallback() {
            @Override
            public void onSuccess(PricingResponse response) {
                currentPricing = response;
                loadConstraints();
            }

            @Override
            public void onError(String message) {
                pbPricing.setVisibility(View.GONE);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadConstraints() {
        PricingService.getPricingConstraints(requireContext(), new PricingService.ConstraintsCallback() {
            @Override
            public void onSuccess(PricingConstraints pricingConstraints) {
                constraints = pricingConstraints;
                pbPricing.setVisibility(View.GONE);
                layoutPricingContent.setVisibility(View.VISIBLE);
                updatePricingUI();
            }

            @Override
            public void onError(String message) {
                pbPricing.setVisibility(View.GONE);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePricingUI() {
        if (currentPricing == null) return;

        etStandardPrice.setText(String.valueOf(currentPricing.standardBasePrice));
        etLuxuryPrice.setText(String.valueOf(currentPricing.luxuryBasePrice));
        etVanPrice.setText(String.valueOf(currentPricing.vanBasePrice));
        etPricePerKm.setText(String.valueOf(currentPricing.pricePerKm));

        String summary = String.format(Locale.US,
                "Standard: %.2f RSD\nLuxury: %.2f RSD\nVan: %.2f RSD\nPer Km: %.2f RSD\n\nExample (10km ride):\n" +
                        "• Standard: %.2f RSD\n• Luxury: %.2f RSD\n• Van: %.2f RSD",
                currentPricing.standardBasePrice,
                currentPricing.luxuryBasePrice,
                currentPricing.vanBasePrice,
                currentPricing.pricePerKm,
                currentPricing.standardBasePrice + (10 * currentPricing.pricePerKm),
                currentPricing.luxuryBasePrice + (10 * currentPricing.pricePerKm),
                currentPricing.vanBasePrice + (10 * currentPricing.pricePerKm)
        );

        tvCurrentPricing.setText(summary);
    }

    private void savePricing() {
        if (currentPricing == null || constraints == null) {
            Toast.makeText(requireContext(), "Pricing data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double standardPrice = Double.parseDouble(etStandardPrice.getText().toString().trim());
            double luxuryPrice = Double.parseDouble(etLuxuryPrice.getText().toString().trim());
            double vanPrice = Double.parseDouble(etVanPrice.getText().toString().trim());
            double pricePerKm = Double.parseDouble(etPricePerKm.getText().toString().trim());

            PricingChangeRequest request = new PricingChangeRequest();

            if (standardPrice != currentPricing.standardBasePrice) {
                request.standardBasePrice = standardPrice;
            }
            if (luxuryPrice != currentPricing.luxuryBasePrice) {
                request.luxuryBasePrice = luxuryPrice;
            }
            if (vanPrice != currentPricing.vanBasePrice) {
                request.vanBasePrice = vanPrice;
            }
            if (pricePerKm != currentPricing.pricePerKm) {
                request.pricePerKm = pricePerKm;
            }

            // Validate
            String validationError = PricingService.validatePricing(request, constraints, currentPricing);
            if (validationError != null) {
                Toast.makeText(requireContext(), validationError, Toast.LENGTH_LONG).show();
                return;
            }

            // Show confirmation dialog
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("💰 Confirm Changes")
                    .setMessage("Are you sure you want to update the pricing? This will affect all new rides.")
                    .setPositiveButton("Confirm", (dialog, which) -> performPricingUpdate(request))
                    .setNegativeButton("Cancel", null)
                    .show();

        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    private void performPricingUpdate(PricingChangeRequest request) {
        btnSave.setEnabled(false);
        btnReset.setEnabled(false);

        PricingService.updatePricing(requireContext(), request, new PricingService.UpdateCallback() {
            @Override
            public void onSuccess(PricingResponse response) {
                btnSave.setEnabled(true);
                btnReset.setEnabled(true);
                currentPricing = response;
                updatePricingUI();
                Toast.makeText(requireContext(), "✅ Pricing updated successfully", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                btnSave.setEnabled(true);
                btnReset.setEnabled(true);
                Toast.makeText(requireContext(), "❌ " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetPricingForm() {
        if (currentPricing != null) {
            updatePricingUI();
            Toast.makeText(requireContext(), "Form reset to current values", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToRideTracking(ActiveDriverDto driver) {
        AdminRideTrackingFragment trackingFragment = AdminRideTrackingFragment.newInstance(driver.id);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, trackingFragment)
                .addToBackStack(null)
                .commit();
    }
}