package com.example.uber3;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

import com.example.uber3.adapter.PassengerRideHistoryAdapter;
import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.ApiService;
import com.example.uber3.network.manager.TokenManager;
import com.example.uber3.network.model.favorite.FavoriteRouteRequest;
import com.example.uber3.network.model.history.PassengerRideSummaryExtendedResponse;
import com.example.uber3.network.model.history.PassengerRideSummaryResponse;
import com.example.uber3.network.model.location.LocationRequest;
import com.example.uber3.network.model.ride.InconsistencyReportDto;
import com.example.uber3.network.service.PassengerHistoryService;
import com.example.uber3.repository.ORSRepository;
import com.google.android.material.textfield.TextInputEditText;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;

public class PassengerRideHistoryFragment extends Fragment implements PassengerRideHistoryAdapter.OnRideClickListener {

    private RecyclerView recyclerView;
    private PassengerRideHistoryAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout tvEmptyState;
    private TextInputEditText etStartDate;
    private TextInputEditText etEndDate;
    private AutoCompleteTextView actSort;
    private PassengerHistoryService historyService;
    private Date startDate = null;
    private Date endDate = null;

    private enum SortOption {START_TIME_DESC, START_TIME_ASC, END_TIME_DESC, END_TIME_ASC, ROUTE_ASC, ROUTE_DESC}

    private SortOption sortOption = SortOption.START_TIME_DESC;

    private List<PassengerRideSummaryResponse> allRides = new ArrayList<>();

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener shakeListener;
    private long lastShakeMs = 0;
    private float lastX, lastY, lastZ;
    private boolean hasLast = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_passenger_ride_history, container, false);

        initializeViews(view);
        setupRecyclerView();
        setupDatePickers();
        setupService();
        setupSortDropdown();
        setupShake();

        loadRideHistory();

        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewRides);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        etStartDate = view.findViewById(R.id.etStartDate);
        etEndDate = view.findViewById(R.id.etEndDate);

        actSort = view.findViewById(R.id.actSort);

        recyclerView.setNestedScrollingEnabled(false);
    }

    private void setupRecyclerView() {
        adapter = new PassengerRideHistoryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupDatePickers() {
        etStartDate.setOnClickListener(v -> showDatePicker(true));
        etEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void setupService() {
        historyService = new PassengerHistoryService(requireContext());
    }

    private void setupSortDropdown() {
        String[] items = new String[]{
                "Start time ↓ (newest)",
                "Start time ↑ (oldest)",
                "End time ↓",
                "End time ↑",
                "Route A→Z",
                "Route Z→A"
        };

        ArrayAdapter<String> a = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                items
        );

        actSort.setAdapter(a);
        actSort.setText(items[0], false);

        actSort.setOnClickListener(v -> actSort.showDropDown());
        actSort.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) actSort.showDropDown();
        });

        actSort.setOnItemClickListener((parent, v, position, id) -> {
            switch (position) {
                case 0:
                    sortOption = SortOption.START_TIME_DESC;
                    break;
                case 1:
                    sortOption = SortOption.START_TIME_ASC;
                    break;
                case 2:
                    sortOption = SortOption.END_TIME_DESC;
                    break;
                case 3:
                    sortOption = SortOption.END_TIME_ASC;
                    break;
                case 4:
                    sortOption = SortOption.ROUTE_ASC;
                    break;
                case 5:
                    sortOption = SortOption.ROUTE_DESC;
                    break;
            }
            applyFiltersAndSort();
        });
    }

    private void showDatePicker(boolean isStart) {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(
                requireContext(),
                (view, year, month, day) -> {
                    c.set(year, month, day);
                    String display = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(c.getTime());
                    if (isStart) {
                        etStartDate.setText(display);
                        startDate = c.getTime();
                    } else {
                        etEndDate.setText(display);
                        endDate = c.getTime();
                    }
                    applyFiltersAndSort();
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        );
        dp.show();
    }

    private void loadRideHistory() {
        showLoading(true);

        historyService.getPassengerRideHistory(new PassengerHistoryService.RideHistoryCallback() {
            @Override
            public void onSuccess(List<PassengerRideSummaryResponse> rides) {
                showLoading(false);
                allRides = rides != null ? rides : new ArrayList<>();
                sortOption = SortOption.START_TIME_DESC;
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
        List<PassengerRideSummaryResponse> filtered = filterByDate(allRides, startDate, endDate);
        List<PassengerRideSummaryResponse> sorted = sortRides(filtered);
        updateUI(sorted);
    }

    private List<PassengerRideSummaryResponse> filterByDate(List<PassengerRideSummaryResponse> rides, Date from, Date to) {
        if (from == null && to == null) return new ArrayList<>(rides);

        List<PassengerRideSummaryResponse> out = new ArrayList<>();
        for (PassengerRideSummaryResponse r : rides) {
            Date rideStart = parseIsoToDate(r.startTime);
            if (rideStart == null) continue;

            boolean okFrom = (from == null) || !rideStart.before(stripTime(from));
            boolean okTo = (to == null) || !rideStart.after(endOfDay(to));
            if (okFrom && okTo) out.add(r);
        }
        return out;
    }

    private List<PassengerRideSummaryResponse> sortRides(List<PassengerRideSummaryResponse> rides) {
        List<PassengerRideSummaryResponse> out = new ArrayList<>(rides);

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
            }
            return 0;
        });

        return out;
    }

    private String routeText(PassengerRideSummaryResponse r) {
        String s = (r.startLocation != null && r.startLocation.address != null) ? r.startLocation.address : "";
        String e = (r.endLocation != null && r.endLocation.address != null) ? r.endLocation.address : "";
        return s + " -> " + e;
    }

    private long safeMillis(String iso) {
        Date d = parseIsoToDate(iso);
        return d != null ? d.getTime() : 0L;
    }

    private long safeEndMillis(String iso) {
        Date d = parseIsoToDate(iso);
        return d != null ? d.getTime() : Long.MAX_VALUE;
    }

    private Date parseIsoToDate(String iso) {
        if (iso == null) return null;
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        };
        for (String p : patterns) {
            try {
                return new SimpleDateFormat(p, Locale.getDefault()).parse(iso);
            } catch (Exception ignored) {
            }
        }
        return null;
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

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void updateUI(List<PassengerRideSummaryResponse> rides) {
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

    private void setupShake() {
        sensorManager = ContextCompat.getSystemService(requireContext(), SensorManager.class);
        if (sensorManager == null) return;

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null) return;

        shakeListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                if (!hasLast) {
                    lastX = x;
                    lastY = y;
                    lastZ = z;
                    hasLast = true;
                    return;
                }

                float dx = Math.abs(x - lastX);
                float dy = Math.abs(y - lastY);
                float dz = Math.abs(z - lastZ);

                lastX = x;
                lastY = y;
                lastZ = z;

                float delta = dx + dy + dz;

                long now = System.currentTimeMillis();
                if (delta > 15f && (now - lastShakeMs) > 800) {
                    lastShakeMs = now;
                    toggleDateSort();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
    }

    private void toggleDateSort() {
        if (sortOption == SortOption.START_TIME_DESC) {
            sortOption = SortOption.START_TIME_ASC;
            actSort.setText("Start time ↑ (oldest)", false);
        } else {
            sortOption = SortOption.START_TIME_DESC;
            actSort.setText("Start time ↓ (newest)", false);
        }
        applyFiltersAndSort();
        Toast.makeText(requireContext(), "Shake: sort by date toggled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null && shakeListener != null) {
            sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null && shakeListener != null) {
            sensorManager.unregisterListener(shakeListener);
        }
    }

    @Override
    public void onRideClick(PassengerRideSummaryResponse ride) {
        if (ride == null || ride.id == null) {
            Toast.makeText(requireContext(), "Cannot load ride details", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog loadingDialog = new Dialog(requireContext());
        loadingDialog.setContentView(android.R.layout.simple_list_item_1);
        loadingDialog.setCancelable(false);
        loadingDialog.show();

        historyService.getPassengerRideDetail(ride.id, new PassengerHistoryService.RideDetailCallback() {
            @Override
            public void onSuccess(PassengerRideSummaryExtendedResponse detail) {
                loadingDialog.dismiss();
                showPassengerRideDetailDialog(detail);
            }

            @Override
            public void onError(String errorMessage) {
                loadingDialog.dismiss();
                Toast.makeText(requireContext(), "Failed to load ride details: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showPassengerRideDetailDialog(PassengerRideSummaryExtendedResponse ride) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_passenger_ride_details);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvDialogDate = dialog.findViewById(R.id.tvDialogDate);
        TextView tvDialogDriverName = dialog.findViewById(R.id.tvDialogDriverName);

        TextView tvDialogStartAddress = dialog.findViewById(R.id.tvDialogStartAddress);
        TextView tvDialogEndAddress = dialog.findViewById(R.id.tvDialogEndAddress);

        TextView tvDialogStartTime = dialog.findViewById(R.id.tvDialogStartTime);
        TextView tvDialogEndTime = dialog.findViewById(R.id.tvDialogEndTime);

        MapView mapView = dialog.findViewById(R.id.mapView);

        Button btnAddFavorite = dialog.findViewById(R.id.btnAddFavorite);
        updateFavoriteButton(btnAddFavorite, ride.favorite);

        btnAddFavorite.setOnClickListener(v -> {
            toggleFavorite(ride, btnAddFavorite);
        });

        Button btnClose = dialog.findViewById(R.id.btnClose);

        Button btnOrderAgain = dialog.findViewById(R.id.btnOrderAgain);

        btnOrderAgain.setOnClickListener(v -> {
            androidx.fragment.app.FragmentActivity activity = getActivity();
            if (activity == null || !isAdded()) return;

            dialog.dismiss();
            openOrderAgain(ride);
        });



        LinearLayout layoutStops = dialog.findViewById(R.id.layoutStops);
        LinearLayout layoutReviews = dialog.findViewById(R.id.layoutReviews);
        LinearLayout layoutReports = dialog.findViewById(R.id.layoutReports);

        LinearLayout layoutPassengers = dialog.findViewById(R.id.layoutPassengers);
        LinearLayout layoutCancellation = dialog.findViewById(R.id.layoutCancellation);
        if (layoutPassengers != null) layoutPassengers.setVisibility(View.GONE);
        if (layoutCancellation != null) layoutCancellation.setVisibility(View.GONE);

        tvDialogDate.setText(formatOnlyDate(ride.startTime));
        tvDialogDriverName.setText(ride.driverName != null ? ("Driver: " + ride.driverName) : "Driver: N/A");

        String startAddr = (ride.startLocation != null && ride.startLocation.address != null) ? ride.startLocation.address : "N/A";
        String endAddr = (ride.endLocation != null && ride.endLocation.address != null) ? ride.endLocation.address : "N/A";
        tvDialogStartAddress.setText(startAddr);
        tvDialogEndAddress.setText(endAddr);

        tvDialogStartTime.setText(formatDateTime(ride.startTime));
        tvDialogEndTime.setText(formatDateTime(ride.endTime));

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        displayRouteOnMap(mapView, ride);

        displayStops(layoutStops, ride.stops);

        displayReviews(layoutReviews, ride.driverReview, ride.rideReview);

        displayReports(layoutReports, ride.inconsistencyReports);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void displayRouteOnMap(MapView mapView, PassengerRideSummaryExtendedResponse ride) {
        if (ride.startLocation == null || ride.startLocation.latitude == null || ride.startLocation.longitude == null)
            return;

        IMapController controller = mapView.getController();
        controller.setZoom(15.0);

        GeoPoint start = new GeoPoint(ride.startLocation.latitude, ride.startLocation.longitude);
        List<GeoPoint> points = new ArrayList<>();
        points.add(start);

        Marker startMarker = new Marker(mapView);
        startMarker.setPosition(start);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        startMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.circle_start));
        mapView.getOverlays().add(startMarker);

        if (ride.endLocation != null && ride.endLocation.latitude != null && ride.endLocation.longitude != null) {
            GeoPoint end = new GeoPoint(ride.endLocation.latitude, ride.endLocation.longitude);
            points.add(end);

            Marker endMarker = new Marker(mapView);
            endMarker.setPosition(end);
            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            endMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.circle_end));
            mapView.getOverlays().add(endMarker);
        }

        if (points.size() >= 2) {
            ORSRepository.getRoute(points, routePoints -> {
                if (!routePoints.isEmpty() && isAdded()) {
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

        controller.setCenter(start);
        mapView.invalidate();
    }

    private void displayReports(LinearLayout layout, List<InconsistencyReportDto> reports) {
        layout.removeAllViews();
        if (reports == null || reports.isEmpty()) {
            TextView tv = new TextView(requireContext());
            tv.setText("No inconsistency reports.");
            tv.setPadding(16, 8, 16, 8);
            layout.addView(tv);
            return;
        }
        for (InconsistencyReportDto r : reports) {
            TextView tv = new TextView(requireContext());
            tv.setText("⚠️ " + (r.reportText != null ? r.reportText : "Report"));
            tv.setPadding(16, 8, 16, 8);
            tv.setTextSize(14);
            layout.addView(tv);
        }
    }

    private String formatDateTime(String iso) {
        if (iso == null) return "-";
        Date d = parseIsoToDate(iso);
        if (d == null) return iso;
        return new SimpleDateFormat("dd. MMMM yyyy, HH:mm", Locale.getDefault()).format(d);
    }

    private String formatOnlyDate(String iso) {
        if (iso == null || iso.trim().isEmpty()) return "/";
        Date d = parseIsoToDate(iso);
        if (d == null) return "/";
        return new SimpleDateFormat("dd. MMMM yyyy.", new Locale("sr", "RS")).format(d);
    }
    private void displayStops(LinearLayout layout, List<com.example.uber3.network.model.location.LocationDto> stops) {

        layout.removeAllViews();
        layout.setVisibility(View.VISIBLE);

        if (stops == null || stops.isEmpty()) {

            TextView tv = new TextView(requireContext());
            tv.setText("/");
            tv.setPadding(16, 8, 16, 8);
            tv.setTextSize(14);

            layout.addView(tv);
            return;
        }

        for (int i = 0; i < stops.size(); i++) {
            com.example.uber3.network.model.location.LocationDto s = stops.get(i);

            TextView tv = new TextView(requireContext());

            String addr = (s != null && s.address != null && !s.address.trim().isEmpty())
                    ? s.address
                    : "/";

            tv.setText((i + 1) + ". " + addr);
            tv.setPadding(16, 8, 16, 8);
            tv.setTextSize(14);

            layout.addView(tv);
        }
    }

    private void displayReviews(LinearLayout layout, Double driverReview, Double rideReview) {

        layout.removeAllViews();
        layout.setVisibility(View.VISIBLE);

        TextView tv = new TextView(requireContext());

        if (driverReview == null && rideReview == null) {
            tv.setText("Driver: /");
        }
        else{
            tv.setText("Driver: ⭐ " + String.format(Locale.getDefault(), "%.1f", driverReview));
        }
        tv.setPadding(16, 8, 16, 8);
        tv.setTextSize(14);

        layout.addView(tv);
        TextView tvRide = new TextView(requireContext());

        if (rideReview == null) {
            tvRide.setText("Ride: /");
        }
        else {
            tvRide.setText("Ride: ⭐ " + String.format(Locale.getDefault(), "%.1f", rideReview));
        }
        tvRide.setPadding(16, 8, 16, 8);
        tvRide.setTextSize(14);

        layout.addView(tvRide);
    }

    private void addToFavorites(PassengerRideSummaryExtendedResponse ride) {

        if (ride.startLocation == null || ride.endLocation == null) {
            Toast.makeText(requireContext(), "Invalid route", Toast.LENGTH_SHORT).show();
            return;
        }

        FavoriteRouteRequest req = new FavoriteRouteRequest();
        req.rideId = ride.id;

        req.from = new LocationRequest(
                ride.startLocation.latitude,
                ride.startLocation.longitude,
                ride.startLocation.address
        );

        req.to = new LocationRequest(
                ride.endLocation.latitude,
                ride.endLocation.longitude,
                ride.endLocation.address
        );

        req.stops = new ArrayList<>();
        if (ride.stops != null) {
            for (var s : ride.stops) {
                req.stops.add(new LocationRequest(
                        s.latitude,
                        s.longitude,
                        s.address
                ));
            }
        }

        req.vehicleType = "STANDARD";
        req.babyTransport = false;
        req.petTransport = false;

        ApiService api = ApiClient.getClient(requireContext()).create(ApiService.class);

        api.addFavorite(req).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<Void> call,
                                   @NonNull retrofit2.Response<Void> response) {

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(),
                            "❤️ Added to favorites",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(),
                            "Failed to add favorite",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                Toast.makeText(requireContext(),
                        "Network error",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateFavoriteButton(Button btn, boolean isFavorite) {
        if (isFavorite) {
            btn.setText("💔 Remove from favorites");
        } else {
            btn.setText("❤️ Add to favorites");
        }
    }

    private void toggleFavorite(PassengerRideSummaryExtendedResponse ride,
                                Button btn) {

        FavoriteRouteRequest req = new FavoriteRouteRequest();
        req.rideId = ride.id;

        req.from = new LocationRequest(
                ride.startLocation.latitude,
                ride.startLocation.longitude,
                ride.startLocation.address
        );

        req.to = new LocationRequest(
                ride.endLocation.latitude,
                ride.endLocation.longitude,
                ride.endLocation.address
        );

        req.stops = new ArrayList<>();
        if (ride.stops != null) {
            for (var s : ride.stops) {
                req.stops.add(new LocationRequest(
                        s.latitude,
                        s.longitude,
                        s.address
                ));
            }
        }

        req.vehicleType = "STANDARD";
        req.babyTransport = false;
        req.petTransport = false;

        ApiService api = ApiClient.getClient(requireContext()).create(ApiService.class);

        Call<Void> call = ride.favorite
                ? api.removeFavorite(req)
                : api.addFavorite(req);

        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull retrofit2.Response<Void> response) {

                if (response.isSuccessful()) {

                    ride.favorite = !ride.favorite;

                    updateFavoriteButton(btn, ride.favorite);
                    syncFavoriteToList(ride);


                    Toast.makeText(requireContext(),
                            ride.favorite
                                    ? "❤️ Added to favorites"
                                    : "Removed from favorites",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(),
                        "Network error",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onFavoriteToggle(PassengerRideSummaryResponse ride) {
        toggleFavoriteFromList(ride);
    }

    private void toggleFavoriteFromList(PassengerRideSummaryResponse ride) {

        FavoriteRouteRequest req = new FavoriteRouteRequest();
        req.rideId = ride.id;

        req.from = new LocationRequest(
                ride.startLocation.latitude,
                ride.startLocation.longitude,
                ride.startLocation.address
        );

        req.to = new LocationRequest(
                ride.endLocation.latitude,
                ride.endLocation.longitude,
                ride.endLocation.address
        );

        req.stops = new ArrayList<>();

        req.vehicleType = "STANDARD";
        req.babyTransport = false;
        req.petTransport = false;

        ApiService api = ApiClient.getClient(requireContext()).create(ApiService.class);

        Call<Void> call = ride.favorite
                ? api.removeFavorite(req)
                : api.addFavorite(req);

        call.enqueue(new retrofit2.Callback<Void>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull retrofit2.Response<Void> response) {

                if (response.isSuccessful()) {
                    ride.favorite = !ride.favorite;
                    adapter.notifyDataSetChanged();

                    Toast.makeText(requireContext(),
                            ride.favorite ? "❤️ Added" : "Removed",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(),
                        "Network error",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void syncFavoriteToList(PassengerRideSummaryExtendedResponse detailRide) {

        for (PassengerRideSummaryResponse r : allRides) {
            if (r.id.equals(detailRide.id)) {
                r.favorite = detailRide.favorite;
                break;
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void openOrderAgain(PassengerRideSummaryExtendedResponse ride) {
        androidx.fragment.app.FragmentActivity activity = getActivity();
        if (activity == null || !isAdded() || ride == null) return;

        HomeFragment home = HomeFragment.newInstance(
                TokenManager.getRole(activity)
        );

        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, home)
                .commit();

        activity.getSupportFragmentManager().executePendingTransactions();

        RideBookingFragment sheet = RideBookingFragment.newInstanceWithPrefill(ride);
        sheet.show(activity.getSupportFragmentManager(), "RideBooking");
    }
}
