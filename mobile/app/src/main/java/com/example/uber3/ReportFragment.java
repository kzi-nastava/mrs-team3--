package com.example.uber3;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;

import com.example.uber3.network.api.*;
import com.example.uber3.network.manager.TokenManager;
import com.example.uber3.network.model.report.*;
import com.example.uber3.network.model.user.UserDto;
import com.example.uber3.network.util.DateUtil;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.*;

import retrofit2.*;

public class ReportFragment extends Fragment {


    private TextView tvTotalRides, tvTotalDistance, tvTotalMoney, tvAvgRides, tvStatus;
    private Button btnFrom, btnTo, btnLoad;
    private LinearLayout adminUserRow;
    private Spinner spUser;

    private BarChart ridesChart, distanceChart, moneyChart;

    private Date fromDate;
    private Date toDate;

    private boolean isAdmin = false;
    private Long selectedUserId = null;
    private final List<UserDto> users = new ArrayList<>();
    private ArrayAdapter<String> userAdapter;

    private com.google.android.material.tabs.TabLayout tabLayout;
    private TextView titleRides, titleDistance, titleMoney;


    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    private final SimpleDateFormat btnFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTotalRides = view.findViewById(R.id.tvTotalRides);
        tvTotalDistance = view.findViewById(R.id.tvTotalDistance);
        tvTotalMoney = view.findViewById(R.id.tvTotalMoney);
        tvAvgRides = view.findViewById(R.id.tvAvgRides);
        tvStatus = view.findViewById(R.id.tvStatus);

        btnFrom = view.findViewById(R.id.btnFrom);
        btnTo = view.findViewById(R.id.btnTo);
        btnLoad = view.findViewById(R.id.btnLoad);

        adminUserRow = view.findViewById(R.id.adminUserRow);
        spUser = view.findViewById(R.id.spUser);

        ridesChart = view.findViewById(R.id.ridesChart);
        distanceChart = view.findViewById(R.id.distanceChart);
        moneyChart = view.findViewById(R.id.moneyChart);

        tabLayout = view.findViewById(R.id.tabLayout);

        titleRides = view.findViewById(R.id.titleRides);
        titleDistance = view.findViewById(R.id.titleDistance);
        titleMoney = view.findViewById(R.id.titleMoney);



        Calendar c1 = Calendar.getInstance();
        c1.set(2026, Calendar.JANUARY, 1, 0, 0, 0);
        c1.set(Calendar.MILLISECOND, 0);
        fromDate = c1.getTime();

        Calendar c2 = Calendar.getInstance();
        c2.set(2026, Calendar.FEBRUARY, 1, 0, 0, 0);
        c2.set(Calendar.MILLISECOND, 0);
        toDate = c2.getTime();

        // Role logic
        String role = TokenManager.getRole(requireContext());
        Long myId = TokenManager.getUserId(requireContext());
        isAdmin = "ADMIN".equals(role);

        if (!isAdmin && myId != null) {
            selectedUserId = myId;
        }

        updateDateButtons();

        btnFrom.setOnClickListener(v -> openDatePicker(true));
        btnTo.setOnClickListener(v -> openDatePicker(false));

        btnLoad.setOnClickListener(v -> loadReportNow());

        if (isAdmin) {
            adminUserRow.setVisibility(View.VISIBLE);
            setupUserSpinner();
            loadUsers();
        } else {
            adminUserRow.setVisibility(View.GONE);
        }

        initChart(ridesChart);
        initChart(distanceChart);
        initChart(moneyChart);

        setupTabs();
        loadReportNow();
    }

    private void setupTabs() {

        tabLayout.addTab(tabLayout.newTab().setText("Rides"));
        tabLayout.addTab(tabLayout.newTab().setText("Distance"));
        tabLayout.addTab(tabLayout.newTab().setText("Money"));

        showTab(0);

        tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                showTab(tab.getPosition());
            }

            @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });
    }


    private void showTab(int pos) {

        ridesChart.setVisibility(pos == 0 ? View.VISIBLE : View.GONE);
        titleRides.setVisibility(pos == 0 ? View.VISIBLE : View.GONE);

        distanceChart.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
        titleDistance.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);

        moneyChart.setVisibility(pos == 2 ? View.VISIBLE : View.GONE);
        titleMoney.setVisibility(pos == 2 ? View.VISIBLE : View.GONE);
    }


    private void setupUserSpinner() {
        userAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUser.setAdapter(userAdapter);

        spUser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isAdmin) return;

                if (position == 0) {
                    selectedUserId = null;
                } else {
                    UserDto u = users.get(position - 1);
                    selectedUserId = u.id;
                }

                debounceLoad();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        refreshUserSpinnerOptions();
    }

    private void refreshUserSpinnerOptions() {
        if (userAdapter == null) return;

        List<String> items = new ArrayList<>();
        items.add("All users");

        for (UserDto u : users) {
            String name = u.firstName != null ? u.firstName : "";
            String surname = u.lastName != null ? u.lastName : "";

            String fullName = (name + " " + surname).trim();

            if (!fullName.isEmpty()) {
                items.add(fullName);
            } else {
                items.add("User #" + u.id);
            }

        }

        userAdapter.clear();
        userAdapter.addAll(items);
        userAdapter.notifyDataSetChanged();
    }

    private void loadUsers() {
        ApiService api = ApiClient.getClient(requireContext()).create(ApiService.class);

        api.getAllUsers().enqueue(new Callback<List<UserDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserDto>> call, @NonNull Response<List<UserDto>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    users.clear();
                    users.addAll(response.body());
                    refreshUserSpinnerOptions();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserDto>> call, @NonNull Throwable t) {
            }
        });
    }

    private void openDatePicker(boolean isFrom) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(isFrom ? fromDate : toDate);

        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dlg = new DatePickerDialog(
                requireContext(),
                (picker, year, month, dayOfMonth) -> {
                    Calendar c = Calendar.getInstance();
                    c.set(year, month, dayOfMonth, 0, 0, 0);
                    c.set(Calendar.MILLISECOND, 0);

                    if (isFrom) {
                        fromDate = c.getTime();
                    } else {
                        toDate = c.getTime();
                    }

                    normalizeRange();
                    updateDateButtons();
                    debounceLoad();
                },
                y, m, d
        );

        dlg.show();
    }

    private void updateDateButtons() {
        btnFrom.setText(btnFmt.format(fromDate));
        btnTo.setText(btnFmt.format(toDate));
    }

    private void normalizeRange() {
        if (fromDate.after(toDate)) {
            Date tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }
    }

    private void debounceLoad() {
        if (debounceRunnable != null) handler.removeCallbacks(debounceRunnable);

        debounceRunnable = this::loadReportNow;
        handler.postDelayed(debounceRunnable, 250);
    }

    private void showStatus(String msg, boolean isError) {
        if (!isAdded()) return;

        if (msg == null || msg.trim().isEmpty()) {
            tvStatus.setVisibility(View.GONE);
            tvStatus.setText("");
            return;
        }

        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(msg);
        tvStatus.setTextColor(isError ? 0xFFEF4444 : 0xFF64748B);
    }

    @SuppressLint("SetTextI18n")
    private void setSummaryZero() {
        tvTotalRides.setText("0");
        tvTotalDistance.setText("0.00 km");
        tvTotalMoney.setText("0 RSD");
        tvAvgRides.setText("0.00");
    }

    private void loadReportNow() {
        ApiService api = ApiClient.getClient(requireContext()).create(ApiService.class);

        String fromIso = DateUtil.toIsoStartOfDay(fromDate);
        String toIso = DateUtil.toIsoEndOfDay(toDate);


        showStatus("Loading...", false);

        api.getReport(fromIso, toIso, selectedUserId)
                .enqueue(new Callback<RideReportResponse>() {

                    @SuppressLint("DefaultLocale")
                    @Override
                    public void onResponse(
                            @NonNull Call<RideReportResponse> call,
                            @NonNull Response<RideReportResponse> response
                    ) {
                        if (!isAdded()) return;

                        if (!response.isSuccessful() || response.body() == null) {
                            showStatus("Error: " + response.code(), true);
                            setSummaryZero();
                            clearCharts();
                            return;
                        }

                        RideReportResponse r = response.body();

                        if (r.daily == null || r.daily.isEmpty()) {
                            showStatus("No data for selected range.", false);
                            setSummaryZero();
                            clearCharts();
                            return;
                        }

                        tvTotalRides.setText(String.valueOf(r.totalRides));
                        tvTotalDistance.setText(String.format(Locale.getDefault(), "%.2f km", r.totalDistance));
                        tvTotalMoney.setText(String.format(Locale.getDefault(), "%.0f RSD", r.totalMoney));
                        tvAvgRides.setText(String.format(Locale.getDefault(), "%.2f", r.avgRidesPerDay));

                        setupChart(ridesChart, r.daily, 0);
                        setupChart(distanceChart, r.daily, 1);
                        setupChart(moneyChart, r.daily, 2);

                        showStatus("", false);
                    }

                    @Override
                    public void onFailure(@NonNull Call<RideReportResponse> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        showStatus("Network error: " + t.getMessage(), true);
                        setSummaryZero();
                        clearCharts();
                    }
                });
    }

    private void initChart(BarChart chart) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f);
        x.setGranularityEnabled(true);
        x.setDrawGridLines(false);

        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setDrawGridLines(true);

        chart.setNoDataText("No data");
    }

    private void clearCharts() {
        ridesChart.clear();
        distanceChart.clear();
        moneyChart.clear();
        ridesChart.invalidate();
        distanceChart.invalidate();
        moneyChart.invalidate();
    }

    private void setupChart(BarChart chart, List<DailyReportItem> daily, int type) {

        List<DailyReportItem> sorted = new ArrayList<>(daily);
        Collections.sort(sorted, Comparator.comparing(a -> a.date));

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int i = 0;
        for (DailyReportItem d : sorted) {

            float val =
                    type == 0 ? (float) d.rideCount :
                            type == 1 ? (float) d.totalDistance :
                                    (float) d.totalMoney;

            entries.add(new BarEntry(i, val));

            String s = d.date != null ? d.date.toString() : "";
            if (s.length() >= 10) labels.add(s.substring(5, 10));
            else labels.add(s);

            i++;
        }

        BarData data = getBarData(type, entries);

        chart.setData(data);

        XAxis x = chart.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));

        chart.animateY(600);
        chart.invalidate();
    }

    @NonNull
    private static BarData getBarData(int type, List<BarEntry> entries) {
        String label =
                type == 0 ? "Rides" :
                        type == 1 ? "Distance (km)" :
                                "Money (RSD)";

        BarDataSet set = new BarDataSet(entries, label);

        int color =
                type == 0 ? 0xFF3B82F6 :
                        type == 1 ? 0xFF10B981 :
                                0xFF8B5CF6;

        set.setColor(color);
        set.setValueTextSize(10f);

        BarData data = new BarData(set);
        data.setBarWidth(0.7f);
        return data;
    }
}
