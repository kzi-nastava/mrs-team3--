package com.example.uber3;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.adapter.AdminRideHistoryAdapter;
import com.example.uber3.network.model.history.AdminRideHistoryExtendedResponse;
import com.example.uber3.network.model.history.AdminRideHistoryResponse;
import com.example.uber3.network.model.location.LocationDto;
import com.example.uber3.network.model.ride.InconsistencyReportDto;
import com.example.uber3.network.service.AdminHistoryService;
import com.example.uber3.repository.ORSRepository;
import com.google.android.material.textfield.TextInputEditText;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminRideHistoryFragment extends Fragment implements AdminRideHistoryAdapter.OnRideClickListener {

    private RecyclerView recyclerView;
    private AdminRideHistoryAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout tvEmptyState;
    private TextView tvAdminHistoryTitle;

    private TextInputEditText etStartDate;
    private TextInputEditText etEndDate;
    private Button btnViewReport;

    private AutoCompleteTextView actSort;

    private AdminHistoryService historyService;

    private Date startDate = null;
    private Date endDate = null;

    private String statusFilter = "All";
    private String panicFilter = "All";

    private enum SortOption {
        START_TIME_DESC,
        START_TIME_ASC,
        END_TIME_DESC,
        END_TIME_ASC,
        ROUTE_ASC,
        ROUTE_DESC,
        PRICE_ASC,
        PRICE_DESC,
        STATUS_ASC,
        STATUS_DESC,
        PANIC_ASC,
        PANIC_DESC
    }

    private SortOption sortOption = SortOption.START_TIME_DESC;

    private List<AdminRideHistoryResponse> allRides = new ArrayList<>();

    public AdminRideHistoryFragment() {}

    public static AdminRideHistoryFragment newInstance() {
        return new AdminRideHistoryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_ride_history, container, false);

        initializeViews(view);
        setupRecyclerView();
        setupDatePickers();
        setupService();
        setupDropdowns();

        loadRideHistory();

        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewRides);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        tvAdminHistoryTitle = view.findViewById(R.id.tvAdminHistoryTitle);

        etStartDate = view.findViewById(R.id.etStartDate);
        etEndDate = view.findViewById(R.id.etEndDate);
        btnViewReport = view.findViewById(R.id.btnViewReport);

        actSort = view.findViewById(R.id.actSort);

        btnViewReport.setOnClickListener(v -> showReportDialog());
    }

    private void setupRecyclerView() {
        adapter = new AdminRideHistoryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupDatePickers() {
        etStartDate.setOnClickListener(v -> showDatePicker(true));
        etEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void setupService() {
        historyService = new AdminHistoryService(requireContext());
    }

    private void setupDropdowns() {
        setupSortDropdown();
    }

    private void setupSortDropdown() {
        String[] items = new String[] {
                "Start time ↓",
                "Start time ↑",
                "End time ↓",
                "End time ↑",
                "Route A→Z",
                "Route Z→A",
                "Price ↑",
                "Price ↓",
                "Status A→Z",
                "Status Z→A",
                "No panic",
                "Panic"
        };

        ArrayAdapter<String> a = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                items
        );

        actSort.setAdapter(a);
        actSort.setText(items[0], false);

        actSort.setOnClickListener(v -> actSort.showDropDown());
        actSort.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actSort.showDropDown(); });

        actSort.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
                case 0: sortOption = SortOption.START_TIME_DESC; break;
                case 1: sortOption = SortOption.START_TIME_ASC; break;
                case 2: sortOption = SortOption.END_TIME_DESC; break;
                case 3: sortOption = SortOption.END_TIME_ASC; break;
                case 4: sortOption = SortOption.ROUTE_ASC; break;
                case 5: sortOption = SortOption.ROUTE_DESC; break;
                case 6: sortOption = SortOption.PRICE_ASC; break;
                case 7: sortOption = SortOption.PRICE_DESC; break;
                case 8: sortOption = SortOption.STATUS_ASC; break;
                case 9: sortOption = SortOption.STATUS_DESC; break;
                case 10: sortOption = SortOption.PANIC_ASC; break;
                case 11: sortOption = SortOption.PANIC_DESC; break;
                default: sortOption = SortOption.START_TIME_DESC; break;
            }
            applyFiltersAndSort();
        });
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    String displayDate = displayFormat.format(calendar.getTime());

                    if (isStartDate) {
                        etStartDate.setText(displayDate);
                        startDate = calendar.getTime();
                    } else {
                        etEndDate.setText(displayDate);
                        endDate = calendar.getTime();
                    }

                    applyFiltersAndSort();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void loadRideHistory() {
        showLoading(true);

        historyService.getAdminRideHistory(new AdminHistoryService.RideHistoryCallback() {
            @Override
            public void onSuccess(List<AdminRideHistoryResponse> rides) {
                showLoading(false);
                allRides = rides != null ? rides : new ArrayList<>();
                applyFiltersAndSort();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                allRides = new ArrayList<>();
                applyFiltersAndSort();
            }
        });
    }

    private void applyFiltersAndSort() {
        List<AdminRideHistoryResponse> filtered = filterByDate(allRides, startDate, endDate);
        filtered = filterByStatus(filtered, statusFilter);
        filtered = filterByPanic(filtered, panicFilter);
        List<AdminRideHistoryResponse> sorted = sortRides(filtered);
        updateUI(sorted);
    }

    private List<AdminRideHistoryResponse> filterByDate(List<AdminRideHistoryResponse> rides, Date from, Date to) {
        if ((from == null) && (to == null)) return new ArrayList<>(rides);

        List<AdminRideHistoryResponse> out = new ArrayList<>();
        for (AdminRideHistoryResponse r : rides) {
            Date rideStart = parseIsoToDate(r.startTime);
            if (rideStart == null) continue;

            boolean okFrom = (from == null) || !rideStart.before(stripTime(from));
            boolean okTo = (to == null) || !rideStart.after(endOfDay(to));

            if (okFrom && okTo) out.add(r);
        }
        return out;
    }

    private List<AdminRideHistoryResponse> filterByStatus(List<AdminRideHistoryResponse> rides, String status) {
        if (status == null || status.equalsIgnoreCase("All")) return rides;

        List<AdminRideHistoryResponse> out = new ArrayList<>();
        for (AdminRideHistoryResponse r : rides) {
            String s = r.status != null ? r.status : "";
            if (s.equalsIgnoreCase(status)) out.add(r);
        }
        return out;
    }

    private List<AdminRideHistoryResponse> filterByPanic(List<AdminRideHistoryResponse> rides, String panic) {
        if (panic == null || panic.equalsIgnoreCase("All")) return rides;

        boolean wantPanic = panic.equalsIgnoreCase("Yes");

        List<AdminRideHistoryResponse> out = new ArrayList<>();
        for (AdminRideHistoryResponse r : rides) {
            if (r.panic == wantPanic) out.add(r);
        }
        return out;
    }

    private List<AdminRideHistoryResponse> sortRides(List<AdminRideHistoryResponse> rides) {
        List<AdminRideHistoryResponse> out = new ArrayList<>(rides);

        out.sort((a, b) -> {
            switch (sortOption) {
                case START_TIME_ASC:
                    return Long.compare(safeMillis(a.startTime), safeMillis(b.startTime));
                case START_TIME_DESC:
                    return Long.compare(safeMillis(b.startTime), safeMillis(a.startTime));

                case END_TIME_ASC:
                    return Long.compare(safeEndMillis(a.endTime), safeEndMillis(b.endTime));
                case END_TIME_DESC:
                    return Long.compare(safeEndMillis(b.endTime), safeEndMillis(a.endTime));

                case ROUTE_ASC:
                    return routeText(a).compareToIgnoreCase(routeText(b));
                case ROUTE_DESC:
                    return routeText(b).compareToIgnoreCase(routeText(a));

                case PRICE_ASC:
                    return Double.compare(a.price, b.price);
                case PRICE_DESC:
                    return Double.compare(b.price, a.price);

                case STATUS_ASC:
                    return safeStr(a.status).compareToIgnoreCase(safeStr(b.status));
                case STATUS_DESC:
                    return safeStr(b.status).compareToIgnoreCase(safeStr(a.status));

                case PANIC_ASC:
                    return Boolean.compare(a.panic, b.panic);
                case PANIC_DESC:
                    return Boolean.compare(b.panic, a.panic);
            }
            return 0;
        });

        return out;
    }

    private long safeMillis(String iso) {
        Date d = parseIsoToDate(iso);
        return d != null ? d.getTime() : 0L;
    }

    private long safeEndMillis(String iso) {
        Date d = parseIsoToDate(iso);
        return d != null ? d.getTime() : Long.MAX_VALUE;
    }

    private String routeText(AdminRideHistoryResponse r) {
        String s = (r.startLocation != null && r.startLocation.address != null) ? r.startLocation.address : "";
        String e = (r.endLocation != null && r.endLocation.address != null) ? r.endLocation.address : "";
        return s + " -> " + e;
    }

    private String safeStr(String s) {
        return s == null ? "" : s;
    }

    private Date stripTime(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private Date endOfDay(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    private Date parseIsoToDate(String iso) {
        if (iso == null) return null;
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            return inputFormat.parse(iso);
        } catch (ParseException e) {
            return null;
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void updateUI(List<AdminRideHistoryResponse> rides) {
        if (rides == null || rides.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            adapter.setRides(new ArrayList<>());
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.setRides(rides);
        }
    }

    @Override
    public void onRideClick(AdminRideHistoryResponse ride) {
        if (ride == null || ride.id == null) {
            Toast.makeText(requireContext(), "Cannot load ride details", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog loadingDialog = new Dialog(requireContext());
        loadingDialog.setContentView(android.R.layout.simple_list_item_1);
        loadingDialog.setCancelable(false);
        loadingDialog.show();

        historyService.getAdminRideDetail(ride.id, new AdminHistoryService.RideDetailCallback() {
            @Override
            public void onSuccess(AdminRideHistoryExtendedResponse rideDetail) {
                loadingDialog.dismiss();
                showAdminRideDetailDialog(rideDetail);
            }

            @Override
            public void onError(String errorMessage) {
                loadingDialog.dismiss();
                Toast.makeText(requireContext(),
                        "Failed to load ride details: " + errorMessage,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAdminRideDetailDialog(AdminRideHistoryExtendedResponse ride) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_admin_ride_details);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvDialogDate = dialog.findViewById(R.id.tvDialogDate);
        TextView tvDialogStatus = dialog.findViewById(R.id.tvDialogStatus);
        TextView tvDialogDriverName = dialog.findViewById(R.id.tvDialogDriverName);
        TextView tvDialogStartAddress = dialog.findViewById(R.id.tvDialogStartAddress);
        TextView tvDialogEndAddress = dialog.findViewById(R.id.tvDialogEndAddress);
        TextView tvDialogPrice = dialog.findViewById(R.id.tvDialogPrice);
        TextView tvDialogPanicBadge = dialog.findViewById(R.id.tvDialogPanicBadge);

        MapView mapView = dialog.findViewById(R.id.mapView);
        Button btnClose = dialog.findViewById(R.id.btnClose);

        LinearLayout layoutStops = dialog.findViewById(R.id.layoutStops);
        LinearLayout layoutPassengers = dialog.findViewById(R.id.layoutPassengers);
        LinearLayout layoutReviews = dialog.findViewById(R.id.layoutReviews);
        LinearLayout layoutReports = dialog.findViewById(R.id.layoutReports);
        LinearLayout layoutCancellation = dialog.findViewById(R.id.layoutCancellation);

        tvDialogDate.setText(formatDateTime(ride.startTime));
        tvDialogStatus.setText(ride.status != null ? ride.status : "");
        tvDialogDriverName.setText(ride.driverName != null ? ("Driver: " + ride.driverName) : "Driver: N/A");

        String startAddr = (ride.startLocation != null && ride.startLocation.address != null) ? ride.startLocation.address : "N/A";
        String endAddr = (ride.endLocation != null && ride.endLocation.address != null) ? ride.endLocation.address : "N/A";
        tvDialogStartAddress.setText(startAddr);
        tvDialogEndAddress.setText(endAddr);

        tvDialogPrice.setText(String.format(Locale.getDefault(), "%.0f din", ride.price));

        if (ride.panic) {
            tvDialogPanicBadge.setVisibility(View.VISIBLE);
            tvDialogPanicBadge.setText("PANIC");
        } else {
            tvDialogPanicBadge.setVisibility(View.GONE);
        }

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        displayAdminRouteOnMap(mapView, ride);

        displayStops(layoutStops, ride.stops);
        displayPassengerEmails(layoutPassengers, ride.passengerEmails);
        displayReviews(layoutReviews, ride.driverReview, ride.rideReview);
        displayReports(layoutReports, ride.inconsistencyReports);
        displayCancellation(layoutCancellation, ride.cancellationReason);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void displayAdminRouteOnMap(MapView mapView, AdminRideHistoryExtendedResponse ride) {
        if (ride.startLocation == null || ride.startLocation.latitude == null || ride.startLocation.longitude == null) return;

        IMapController mapController = mapView.getController();
        mapController.setZoom(15.0);

        GeoPoint startPoint = new GeoPoint(ride.startLocation.latitude, ride.startLocation.longitude);
        List<GeoPoint> points = new ArrayList<>();
        points.add(startPoint);

        Marker startMarker = new Marker(mapView);
        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        startMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.circle_start));
        mapView.getOverlays().add(startMarker);

        if (ride.stops != null) {
            for (LocationDto s : ride.stops) {
                if (s != null && s.latitude != null && s.longitude != null) {
                    GeoPoint p = new GeoPoint(s.latitude, s.longitude);
                    points.add(p);

                    Marker m = new Marker(mapView);
                    m.setPosition(p);
                    m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                    m.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.circle_stop));
                    m.setTitle("Stop " + (points.size() - 1));
                    mapView.getOverlays().add(m);
                }
            }
        }

        if (ride.endLocation != null && ride.endLocation.latitude != null && ride.endLocation.longitude != null) {
            GeoPoint endPoint = new GeoPoint(ride.endLocation.latitude, ride.endLocation.longitude);
            points.add(endPoint);

            Marker endMarker = new Marker(mapView);
            endMarker.setPosition(endPoint);
            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            endMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.circle_end));
            mapView.getOverlays().add(endMarker);
        }

        if (points.size() >= 2) {
            ORSRepository.getRoute(points, routePoints -> {
                if (!routePoints.isEmpty()) {
                    requireActivity().runOnUiThread(() -> {
                        Polyline line = new Polyline(mapView);
                        line.setPoints(routePoints);
                        line.setColor(Color.parseColor("#6366F1"));
                        line.setWidth(8f);
                        mapView.getOverlays().add(line);
                        mapView.invalidate();
                    });
                }
            });
        }

        mapView.invalidate();
        mapController.setCenter(startPoint);
    }

    private void displayStops(LinearLayout layout, List<LocationDto> stops) {
        layout.removeAllViews();
        if (stops == null || stops.isEmpty()) {
            layout.setVisibility(View.GONE);
            return;
        }
        layout.setVisibility(View.VISIBLE);

        for (int i = 0; i < stops.size(); i++) {
            LocationDto stop = stops.get(i);
            TextView tv = new TextView(requireContext());
            tv.setText((i + 1) + ". " + (stop != null && stop.address != null ? stop.address : "N/A"));
            tv.setPadding(16, 8, 16, 8);
            tv.setTextSize(14);
            layout.addView(tv);
        }
    }

    private void displayPassengerEmails(LinearLayout layout, List<String> emails) {
        layout.removeAllViews();
        if (emails == null || emails.isEmpty()) {
            layout.setVisibility(View.GONE);
            return;
        }
        layout.setVisibility(View.VISIBLE);

        for (String e : emails) {
            TextView tv = new TextView(requireContext());
            tv.setText("✉️ " + e);
            tv.setPadding(16, 8, 16, 8);
            tv.setTextSize(14);
            layout.addView(tv);
        }
    }

    private void displayReviews(LinearLayout layout, Double driverReview, Double rideReview) {
        layout.removeAllViews();

        if (driverReview == null && rideReview == null) {
            layout.setVisibility(View.GONE);
            return;
        }
        layout.setVisibility(View.VISIBLE);

        if (driverReview != null) {
            TextView tv = new TextView(requireContext());
            tv.setText("Driver ⭐ " + String.format(Locale.getDefault(), "%.1f", driverReview));
            tv.setPadding(16, 8, 16, 8);
            layout.addView(tv);
        }

        if (rideReview != null) {
            TextView tv = new TextView(requireContext());
            tv.setText("Ride ⭐ " + String.format(Locale.getDefault(), "%.1f", rideReview));
            tv.setPadding(16, 8, 16, 8);
            layout.addView(tv);
        }
    }

    private void displayReports(LinearLayout layout, List<InconsistencyReportDto> reports) {
        layout.removeAllViews();
        if (reports == null || reports.isEmpty()) {
            layout.setVisibility(View.GONE);
            return;
        }
        layout.setVisibility(View.VISIBLE);

        for (InconsistencyReportDto r : reports) {
            TextView tv = new TextView(requireContext());
            tv.setText("⚠️ " + (r.reportText != null ? r.reportText : "Report"));
            tv.setPadding(16, 8, 16, 8);
            tv.setTextSize(14);
            layout.addView(tv);
        }
    }

    private void displayCancellation(LinearLayout layout, String reason) {
        layout.removeAllViews();
        if (reason == null || reason.trim().isEmpty()) {
            layout.setVisibility(View.GONE);
            return;
        }
        layout.setVisibility(View.VISIBLE);

        TextView tv = new TextView(requireContext());
        tv.setText("Cancellation: " + reason);
        tv.setPadding(16, 8, 16, 8);
        tv.setTextSize(14);
        tv.setTextColor(Color.parseColor("#991b1b"));
        layout.addView(tv);
    }

    private String formatDateTime(String isoDate) {
        if (isoDate == null) return "";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(isoDate);
            return date != null ? outputFormat.format(date) : "";
        } catch (ParseException e) {
            return isoDate;
        }
    }

    private void showReportDialog() {
        if (allRides.isEmpty()) {
            Toast.makeText(requireContext(), "No rides to generate report", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalRides = allRides.size();
        int panicRides = 0;
        int cancelledRides = 0;

        double totalPrice = 0;

        for (AdminRideHistoryResponse r : allRides) {
            if (r.panic) panicRides++;
            if (r.status != null && r.status.contains("CANCEL")) cancelledRides++;
            totalPrice += r.price;
        }

        String reportMessage = String.format(Locale.getDefault(),
                "Admin Ride Statistics:\n\n" +
                        "Total Rides: %d\n" +
                        "Cancelled: %d\n" +
                        "Panic: %d\n\n" +
                        "Total Price: %.0f din\n",
                totalRides,
                cancelledRides,
                panicRides,
                totalPrice
        );

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("📊 Admin Report")
                .setMessage(reportMessage)
                .setPositiveButton("OK", null)
                .show();
    }
}
