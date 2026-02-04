package com.example.uber3;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.adapter.RideHistoryAdapter;
import com.example.uber3.network.manager.TokenManager;
import com.example.uber3.network.model.history.DriverRideHistoryDetailResponse;
import com.example.uber3.network.model.history.DriverRideHistoryResponse;
import com.example.uber3.network.model.location.LocationDto;
import com.example.uber3.network.model.ride.InconsistencyReportDto;
import com.example.uber3.network.model.ride.ReviewDto;
import com.example.uber3.network.service.DriverHistoryService;
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

public class DriverHistoryFragment extends Fragment implements RideHistoryAdapter.OnRideClickListener {

    private static final String TAG = "DriverHistoryFragment";

    // UI Components
    private RecyclerView recyclerView;
    private RideHistoryAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout tvEmptyState;
    private TextInputEditText etStartDate;
    private TextInputEditText etEndDate;
    private Button btnViewReport;

    // Service Layer
    private DriverHistoryService historyService;

    // Data
    private Date startDate = null;
    private Date endDate = null;
    private Long driverId;
    private List<DriverRideHistoryResponse> allRides = new ArrayList<>();

    public DriverHistoryFragment() {
    }

    public static DriverHistoryFragment newInstance() {
        return new DriverHistoryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_history, container, false);

        initializeViews(view);
        setupRecyclerView();
        setupDatePickers();
        setupService();
        loadRideHistory();

        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewRides);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        etStartDate = view.findViewById(R.id.etStartDate);
        etEndDate = view.findViewById(R.id.etEndDate);
        btnViewReport = view.findViewById(R.id.btnViewReport);

        btnViewReport.setOnClickListener(v -> showReportDialog());
    }

    private void setupRecyclerView() {
        adapter = new RideHistoryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupDatePickers() {
        etStartDate.setOnClickListener(v -> showDatePicker(true));
        etEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void setupService() {
        // Initialize service
        historyService = new DriverHistoryService(requireContext());

        // Get driver ID from token
        String token = TokenManager.getToken(requireContext());
        if (token != null) {
            driverId = extractDriverIdFromToken(token);
        }
    }

    private Long extractDriverIdFromToken(String token) {
        // Decode JWT token to get driver ID from "uid" claim
        try {
            String[] parts = token.split("\\.");
            if (parts.length > 1) {
                String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT));
                // Parse JSON to get "uid" field
                int uidIndex = payload.indexOf("\"uid\":");
                if (uidIndex != -1) {
                    int start = uidIndex + 6;
                    int end = payload.indexOf(",", start);
                    if (end == -1) end = payload.indexOf("}", start);
                    String uidStr = payload.substring(start, end).trim();
                    return Long.parseLong(uidStr);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error decoding token", e);
        }
        return null;
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

                    // Reload rides with new filter
                    loadRideHistory();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void loadRideHistory() {
        if (driverId == null) {
            Toast.makeText(requireContext(), "Driver ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Use service to fetch rides
        historyService.getDriverRideHistory(driverId, startDate, endDate,
                new DriverHistoryService.RideHistoryCallback() {
                    @Override
                    public void onSuccess(List<DriverRideHistoryResponse> rides) {
                        showLoading(false);
                        allRides = rides;
                        updateUI(rides);

                        // Log basic statistics
                        Log.d(TAG, "Total rides: " + rides.size());
                        Log.d(TAG, "Completed rides: " + historyService.getCompletedRides(rides).size());
                    }

                    @Override
                    public void onError(String errorMessage) {
                        showLoading(false);
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                        updateUI(new ArrayList<>());
                    }
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void updateUI(List<DriverRideHistoryResponse> rides) {
        if (rides.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.setRides(rides);
        }
    }

    @Override
    public void onRideClick(DriverRideHistoryResponse ride) {
        if (driverId == null || ride.rideId == null) {
            Toast.makeText(requireContext(), "Cannot load ride details", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading dialog
        Dialog loadingDialog = new Dialog(requireContext());
        loadingDialog.setContentView(android.R.layout.simple_list_item_1);
        loadingDialog.setCancelable(false);
        loadingDialog.show();

        // Fetch detailed ride information
        historyService.getDriverRideDetail(driverId, ride.rideId,
                new DriverHistoryService.RideDetailCallback() {
                    @Override
                    public void onSuccess(DriverRideHistoryDetailResponse rideDetail) {
                        loadingDialog.dismiss();
                        showRideDetailDialog(rideDetail);
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

    private void showRideDetailDialog(DriverRideHistoryDetailResponse ride) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_ride_detail);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Initialize views
        TextView tvDialogDate = dialog.findViewById(R.id.tvDialogDate);
        TextView tvDialogStatus = dialog.findViewById(R.id.tvDialogStatus);
        TextView tvDialogStartAddress = dialog.findViewById(R.id.tvDialogStartAddress);
        TextView tvDialogEndAddress = dialog.findViewById(R.id.tvDialogEndAddress);
        TextView tvDialogPrice = dialog.findViewById(R.id.tvDialogPrice);
        TextView tvDialogDistance = dialog.findViewById(R.id.tvDialogDistance);
        TextView tvDialogDuration = dialog.findViewById(R.id.tvDialogDuration);
        TextView tvDialogVehicleType = dialog.findViewById(R.id.tvDialogVehicleType);
        MapView mapView = dialog.findViewById(R.id.mapView);
        Button btnClose = dialog.findViewById(R.id.btnClose);
        LinearLayout layoutPlannedStops = dialog.findViewById(R.id.layoutPlannedStops);
        LinearLayout layoutActualStops = dialog.findViewById(R.id.layoutActualStops);
        TextView tvPlannedStopsLabel = dialog.findViewById(R.id.tvPlannedStopsLabel);
        TextView tvActualStopsLabel = dialog.findViewById(R.id.tvActualStopsLabel);
        LinearLayout layoutPassengers = dialog.findViewById(R.id.layoutPassengers);
        LinearLayout layoutReviews = dialog.findViewById(R.id.layoutReviews);
        LinearLayout layoutReports = dialog.findViewById(R.id.layoutReports);
        LinearLayout layoutCancellation = dialog.findViewById(R.id.layoutCancellation);

        // Set basic information
        tvDialogDate.setText(formatDateTime(ride.startedAt));

        String status = ride.getFormattedStatus();
        tvDialogStatus.setText(getStatusText(status));
        setStatusBadgeStyle(tvDialogStatus, status);

        tvDialogStartAddress.setText(ride.startAddress != null ? ride.startAddress : "N/A");
        tvDialogEndAddress.setText(ride.endAddress != null ? ride.endAddress : "N/A");
        tvDialogPrice.setText(String.format(Locale.getDefault(), "%.0f din",
                ride.price != null ? ride.price : 0));
        tvDialogDistance.setText(String.format(Locale.getDefault(), "%.1f km",
                ride.distance != null ? ride.distance : 0));
        tvDialogDuration.setText(String.format(Locale.getDefault(), "%d min",
                ride.getDurationMinutes()));
        tvDialogVehicleType.setText(ride.vehicleType != null ? ride.vehicleType : "Standard");

        // Setup map
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        displayRouteOnMap(mapView, ride);

        // Display planned and actual stops
        displayPlannedStops(layoutPlannedStops, tvPlannedStopsLabel, ride);
        displayActualStops(layoutActualStops, tvActualStopsLabel, ride);

        // Display passengers
        displayPassengers(layoutPassengers, ride);

        // Display reviews
        displayReviews(layoutReviews, ride);

        // Display reports
        displayReports(layoutReports, ride);

        // Display cancellation info
        displayCancellationInfo(layoutCancellation, ride);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void displayRouteOnMap(MapView mapView, DriverRideHistoryDetailResponse ride) {
        if (ride.startLatitude == null || ride.startLongitude == null) {
            return;
        }

        IMapController mapController = mapView.getController();
        mapController.setZoom(13.0);

        GeoPoint startPoint = new GeoPoint(ride.startLatitude, ride.startLongitude);
        List<GeoPoint> allRoutePoints = new ArrayList<>();
        allRoutePoints.add(startPoint);

        // Add start marker (green)
        Marker startMarker = new Marker(mapView);
        startMarker.setPosition(startPoint);
        startMarker.setTitle("Start: " + (ride.startAddress != null ? ride.startAddress : ""));
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setIcon(null); // Use default marker
        mapView.getOverlays().add(startMarker);

        // Add planned stops (blue markers)
        if (ride.plannedStops != null && !ride.plannedStops.isEmpty()) {
            for (int i = 0; i < ride.plannedStops.size(); i++) {
                LocationDto stop = ride.plannedStops.get(i);
                if (stop.latitude != null && stop.longitude != null) {
                    GeoPoint stopPoint = new GeoPoint(stop.latitude, stop.longitude);
                    allRoutePoints.add(stopPoint);

                    Marker stopMarker = new Marker(mapView);
                    stopMarker.setPosition(stopPoint);
                    stopMarker.setTitle("Planned Stop " + (i + 1) + ": " +
                            (stop.address != null ? stop.address : ""));
                    stopMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    stopMarker.setTextIcon("P" + (i + 1)); // Planned stop label
                    mapView.getOverlays().add(stopMarker);
                }
            }
        }

        // Add actual stops (orange markers) - overlay on planned if same
        if (ride.actualStops != null && !ride.actualStops.isEmpty()) {
            for (int i = 0; i < ride.actualStops.size(); i++) {
                LocationDto stop = ride.actualStops.get(i);
                if (stop.latitude != null && stop.longitude != null) {
                    GeoPoint stopPoint = new GeoPoint(stop.latitude, stop.longitude);

                    // Only add to route if not already there (from planned)
                    boolean alreadyInRoute = false;
                    for (GeoPoint existing : allRoutePoints) {
                        if (Math.abs(existing.getLatitude() - stopPoint.getLatitude()) < 0.0001 &&
                                Math.abs(existing.getLongitude() - stopPoint.getLongitude()) < 0.0001) {
                            alreadyInRoute = true;
                            break;
                        }
                    }
                    if (!alreadyInRoute) {
                        allRoutePoints.add(stopPoint);
                    }

                    Marker stopMarker = new Marker(mapView);
                    stopMarker.setPosition(stopPoint);
                    stopMarker.setTitle("Actual Stop " + (i + 1) + ": " +
                            (stop.address != null ? stop.address : ""));
                    stopMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    stopMarker.setTextIcon("A" + (i + 1)); // Actual stop label
                    mapView.getOverlays().add(stopMarker);
                }
            }
        }

        // Add end marker if available (red)
        if (ride.endLatitude != null && ride.endLongitude != null) {
            GeoPoint endPoint = new GeoPoint(ride.endLatitude, ride.endLongitude);
            allRoutePoints.add(endPoint);

            Marker endMarker = new Marker(mapView);
            endMarker.setPosition(endPoint);
            endMarker.setTitle("End: " + (ride.endAddress != null ? ride.endAddress : ""));
            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(endMarker);
        }

        // Draw route line connecting all points
        if (allRoutePoints.size() >= 2) {
            Polyline routeLine = new Polyline(mapView);
            routeLine.setPoints(allRoutePoints);
            routeLine.setColor(Color.parseColor("#6366F1"));
            routeLine.setWidth(8f);
            mapView.getOverlays().add(routeLine);
        }

        // Center map to show all points
        if (allRoutePoints.size() > 0) {
            // Calculate bounds
            double minLat = allRoutePoints.get(0).getLatitude();
            double maxLat = allRoutePoints.get(0).getLatitude();
            double minLon = allRoutePoints.get(0).getLongitude();
            double maxLon = allRoutePoints.get(0).getLongitude();

            for (GeoPoint point : allRoutePoints) {
                minLat = Math.min(minLat, point.getLatitude());
                maxLat = Math.max(maxLat, point.getLatitude());
                minLon = Math.min(minLon, point.getLongitude());
                maxLon = Math.max(maxLon, point.getLongitude());
            }

            double centerLat = (minLat + maxLat) / 2;
            double centerLon = (minLon + maxLon) / 2;
            mapController.setCenter(new GeoPoint(centerLat, centerLon));

            // Adjust zoom based on bounds
            double latSpan = maxLat - minLat;
            double lonSpan = maxLon - minLon;
            double maxSpan = Math.max(latSpan, lonSpan);

            if (maxSpan < 0.01) {
                mapController.setZoom(15.0);
            } else if (maxSpan < 0.05) {
                mapController.setZoom(13.0);
            } else {
                mapController.setZoom(11.0);
            }
        } else {
            mapController.setCenter(startPoint);
        }
    }

    private void displayPlannedStops(LinearLayout layout, TextView label, DriverRideHistoryDetailResponse ride) {
        layout.removeAllViews();

        if (ride.plannedStops != null && !ride.plannedStops.isEmpty()) {
            label.setVisibility(View.VISIBLE);
            layout.setVisibility(View.VISIBLE);

            for (int i = 0; i < ride.plannedStops.size(); i++) {
                LocationDto stop = ride.plannedStops.get(i);

                LinearLayout stopCard = new LinearLayout(requireContext());
                stopCard.setOrientation(LinearLayout.VERTICAL);
                stopCard.setPadding(16, 12, 16, 12);
                stopCard.setBackgroundColor(Color.parseColor("#EFF6FF"));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 8);
                stopCard.setLayoutParams(params);

                TextView tvStopNumber = new TextView(requireContext());
                tvStopNumber.setText("Stop " + (i + 1));
                tvStopNumber.setTextSize(12);
                tvStopNumber.setTextColor(Color.parseColor("#1E40AF"));
                tvStopNumber.setTypeface(null, android.graphics.Typeface.BOLD);
                stopCard.addView(tvStopNumber);

                if (stop.address != null && !stop.address.isEmpty()) {
                    TextView tvAddress = new TextView(requireContext());
                    tvAddress.setText("📍 " + stop.address);
                    tvAddress.setTextSize(14);
                    tvAddress.setTextColor(Color.parseColor("#111827"));
                    tvAddress.setPadding(0, 4, 0, 0);
                    stopCard.addView(tvAddress);
                }

                if (stop.latitude != null && stop.longitude != null) {
                    TextView tvCoords = new TextView(requireContext());
                    tvCoords.setText(String.format(Locale.getDefault(),
                            "%.6f, %.6f", stop.latitude, stop.longitude));
                    tvCoords.setTextSize(11);
                    tvCoords.setTextColor(Color.parseColor("#6B7280"));
                    tvCoords.setPadding(0, 2, 0, 0);
                    stopCard.addView(tvCoords);
                }

                layout.addView(stopCard);
            }
        } else {
            label.setVisibility(View.GONE);
            layout.setVisibility(View.GONE);
        }
    }

    private void displayActualStops(LinearLayout layout, TextView label, DriverRideHistoryDetailResponse ride) {
        layout.removeAllViews();

        if (ride.actualStops != null && !ride.actualStops.isEmpty()) {
            label.setVisibility(View.VISIBLE);
            layout.setVisibility(View.VISIBLE);

            for (int i = 0; i < ride.actualStops.size(); i++) {
                LocationDto stop = ride.actualStops.get(i);

                LinearLayout stopCard = new LinearLayout(requireContext());
                stopCard.setOrientation(LinearLayout.VERTICAL);
                stopCard.setPadding(16, 12, 16, 12);
                stopCard.setBackgroundColor(Color.parseColor("#FEF3C7"));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 8);
                stopCard.setLayoutParams(params);

                TextView tvStopNumber = new TextView(requireContext());
                tvStopNumber.setText("Stop " + (i + 1));
                tvStopNumber.setTextSize(12);
                tvStopNumber.setTextColor(Color.parseColor("#D97706"));
                tvStopNumber.setTypeface(null, android.graphics.Typeface.BOLD);
                stopCard.addView(tvStopNumber);

                if (stop.address != null && !stop.address.isEmpty()) {
                    TextView tvAddress = new TextView(requireContext());
                    tvAddress.setText("📍 " + stop.address);
                    tvAddress.setTextSize(14);
                    tvAddress.setTextColor(Color.parseColor("#111827"));
                    tvAddress.setPadding(0, 4, 0, 0);
                    stopCard.addView(tvAddress);
                }

                if (stop.latitude != null && stop.longitude != null) {
                    TextView tvCoords = new TextView(requireContext());
                    tvCoords.setText(String.format(Locale.getDefault(),
                            "%.6f, %.6f", stop.latitude, stop.longitude));
                    tvCoords.setTextSize(11);
                    tvCoords.setTextColor(Color.parseColor("#6B7280"));
                    tvCoords.setPadding(0, 2, 0, 0);
                    stopCard.addView(tvCoords);
                }

                layout.addView(stopCard);
            }
        } else {
            label.setVisibility(View.GONE);
            layout.setVisibility(View.GONE);
        }
    }

    private void displayPassengers(LinearLayout layout, DriverRideHistoryDetailResponse ride) {
        layout.removeAllViews();

        if (ride.passengerNames != null) {
            for (String name : ride.passengerNames) {
                TextView tv = new TextView(requireContext());
                tv.setText("👤 " + name);
                tv.setPadding(16, 8, 16, 8);
                tv.setTextSize(14);
                layout.addView(tv);
            }
        }

        if (ride.invitedPassengers != null) {
            for (String email : ride.invitedPassengers) {
                TextView tv = new TextView(requireContext());
                tv.setText("✉️ " + email + " (Invited)");
                tv.setPadding(16, 8, 16, 8);
                tv.setTextSize(14);
                layout.addView(tv);
            }
        }

        if (layout.getChildCount() == 0) {
            layout.setVisibility(View.GONE);
        }
    }

    private void displayReviews(LinearLayout layout, DriverRideHistoryDetailResponse ride) {
        layout.removeAllViews();

        if (ride.reviews != null && !ride.reviews.isEmpty()) {
            for (ReviewDto review : ride.reviews) {
                LinearLayout reviewCard = new LinearLayout(requireContext());
                reviewCard.setOrientation(LinearLayout.VERTICAL);
                reviewCard.setPadding(16, 12, 16, 12);

                if (review.driverRating != null) {
                    TextView tvDriverRating = new TextView(requireContext());
                    tvDriverRating.setText("Driver: " + getStars(review.driverRating));
                    tvDriverRating.setTextSize(14);
                    reviewCard.addView(tvDriverRating);
                }

                if (review.vehicleRating != null) {
                    TextView tvVehicleRating = new TextView(requireContext());
                    tvVehicleRating.setText("Vehicle: " + getStars(review.vehicleRating));
                    tvVehicleRating.setTextSize(14);
                    reviewCard.addView(tvVehicleRating);
                }

                if (review.comment != null && !review.comment.isEmpty()) {
                    TextView tvComment = new TextView(requireContext());
                    tvComment.setText(review.comment);
                    tvComment.setTextSize(13);
                    tvComment.setPadding(0, 8, 0, 0);
                    tvComment.setTextColor(Color.parseColor("#6b7280"));
                    reviewCard.addView(tvComment);
                }

                layout.addView(reviewCard);
            }
        } else {
            layout.setVisibility(View.GONE);
        }
    }

    private void displayReports(LinearLayout layout, DriverRideHistoryDetailResponse ride) {
        layout.removeAllViews();

        if (ride.inconsistencyReports != null && !ride.inconsistencyReports.isEmpty()) {
            for (InconsistencyReportDto report : ride.inconsistencyReports) {
                LinearLayout reportCard = new LinearLayout(requireContext());
                reportCard.setOrientation(LinearLayout.VERTICAL);
                reportCard.setPadding(16, 12, 16, 12);
                reportCard.setBackgroundColor(Color.parseColor("#fef2f2"));

                TextView tvReportDate = new TextView(requireContext());
                tvReportDate.setText(formatDateTime(report.reportedAt));
                tvReportDate.setTextSize(12);
                tvReportDate.setTextColor(Color.parseColor("#991b1b"));
                reportCard.addView(tvReportDate);

                TextView tvReportMessage = new TextView(requireContext());
                tvReportMessage.setText(report.message);
                tvReportMessage.setTextSize(14);
                tvReportMessage.setPadding(0, 4, 0, 0);
                reportCard.addView(tvReportMessage);

                layout.addView(reportCard);
            }
        } else {
            layout.setVisibility(View.GONE);
        }
    }

    private void displayCancellationInfo(LinearLayout layout, DriverRideHistoryDetailResponse ride) {
        if (ride.wasCancelled) {
            layout.setVisibility(View.VISIBLE);
            layout.removeAllViews();

            TextView tvCancelledBy = new TextView(requireContext());
            tvCancelledBy.setText("Cancelled by: " + ride.cancelledBy);
            tvCancelledBy.setPadding(0, 0, 0, 8);
            layout.addView(tvCancelledBy);

            if (ride.terminationReason != null && !ride.terminationReason.isEmpty()) {
                TextView tvCancelReason = new TextView(requireContext());
                tvCancelReason.setText("Reason: " + ride.terminationReason);
                layout.addView(tvCancelReason);
            }
        } else {
            layout.setVisibility(View.GONE);
        }
    }

    private String getStars(int rating) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            stars.append(i < rating ? "★" : "☆");
        }
        return stars.toString();
    }

    private String getStatusText(String status) {
        switch (status) {
            case "COMPLETED":
                return "Completed";
            case "CANCELLED_BY_DRIVER":
                return "Cancelled by Driver";
            case "CANCELLED_BY_PASSENGER":
                return "Cancelled by Passenger";
            case "FINISHED_EARLY":
                return "Finished Early";
            default:
                return status;
        }
    }

    private void setStatusBadgeStyle(TextView badge, String status) {
        int backgroundColor;
        int textColor;

        if (status.equals("COMPLETED")) {
            backgroundColor = Color.parseColor("#d1fae5");
            textColor = Color.parseColor("#065f46");
        } else if (status.contains("CANCELLED")) {
            backgroundColor = Color.parseColor("#fee2e2");
            textColor = Color.parseColor("#991b1b");
        } else if (status.equals("FINISHED_EARLY")) {
            backgroundColor = Color.parseColor("#dbeafe");
            textColor = Color.parseColor("#1e40af");
        } else {
            backgroundColor = Color.parseColor("#e5e7eb");
            textColor = Color.parseColor("#374151");
        }

        badge.setBackgroundColor(backgroundColor);
        badge.setTextColor(textColor);
        badge.setPadding(12, 6, 12, 6);
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

        // Calculate statistics manually
        int totalRides = allRides.size();
        int completedRides = historyService.getCompletedRides(allRides).size();
        int cancelledRides = historyService.getCancelledRides(allRides).size();
        int panicRides = historyService.getPanicRides(allRides).size();

        double totalEarnings = 0;
        double totalDistance = 0;
        int totalDuration = 0;

        for (DriverRideHistoryResponse ride : allRides) {
            if (ride.price != null) totalEarnings += ride.price;
            if (ride.distance != null) totalDistance += ride.distance;
            totalDuration += ride.getDurationMinutes();
        }

        double avgEarnings = totalRides > 0 ? totalEarnings / totalRides : 0;
        double avgDistance = totalRides > 0 ? totalDistance / totalRides : 0;
        double completionRate = totalRides > 0 ? (completedRides * 100.0 / totalRides) : 0;
        double cancellationRate = totalRides > 0 ? (cancelledRides * 100.0 / totalRides) : 0;

        // Create simple statistics dialog
        String reportMessage = String.format(Locale.getDefault(),
                "Ride Statistics:\n\n" +
                        "Total Rides: %d\n" +
                        "Completed: %d (%.1f%%)\n" +
                        "Cancelled: %d (%.1f%%)\n" +
                        "Panic Events: %d\n\n" +
                        "Total Earnings: %.0f din\n" +
                        "Average Earnings: %.0f din\n\n" +
                        "Total Distance: %.1f km\n" +
                        "Average Distance: %.1f km\n\n" +
                        "Total Duration: %d minutes",
                totalRides,
                completedRides, completionRate,
                cancelledRides, cancellationRate,
                panicRides,
                totalEarnings,
                avgEarnings,
                totalDistance,
                avgDistance,
                totalDuration
        );

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("📊 Ride Report")
                .setMessage(reportMessage)
                .setPositiveButton("OK", null)
                .show();
    }
}