package com.example.uber3;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.uber3.R;
import com.example.uber3.network.model.driver.Location;
import com.example.uber3.network.model.tracking.ReportResponse;
import com.example.uber3.network.model.tracking.RideTrackingData;
import com.example.uber3.network.model.tracking.TokenValidation;
import com.example.uber3.network.service.RideTrackingService;
import com.example.uber3.repository.ORSRepository;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class RideTrackingFragment extends Fragment {

    private static final String TAG = "RideTracking";

    // Views
    private MapView mapView;
    private ScrollView scrollContent;
    private LinearLayout layoutLoading;
    private LinearLayout layoutError;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutContent;

    // Header views
    private TextView tvTitle;
    private TextView tvStatus;
    private Button btnRefresh;

    // Driver info views
    private CardView cardDriverInfo;
    private TextView tvDriverName;
    private TextView tvDriverPhone;
    private TextView tvVehicleModel;
    private TextView tvVehicleRegistration;
    private LinearLayout layoutDriverPhone;
    private LinearLayout layoutVehicleModel;
    private LinearLayout layoutVehicleReg;

    // Time remaining views
    private CardView cardTimeRemaining;
    private TextView tvTimeIcon;
    private TextView tvTimeRemaining;

    // Route views
    private CardView cardRoute;
    private LinearLayout layoutRoutePoints;

    // Action buttons
    private Button btnReport;
    private Button btnPanic;

    // Error/loading views
    private TextView tvErrorMessage;
    private ProgressBar progressBar;
    private LinearLayout layoutGuestNotice;

    // Map overlays
    private List<Marker> markers = new ArrayList<>();
    private List<Polyline> routeLines = new ArrayList<>();
    private Marker driverMarker = null;

    // Data
    private RideTrackingData currentRide = null;
    private String trackingToken = null;
    private boolean isGuestMode = false;

    // Polling
    private Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private static final int POLLING_INTERVAL = 15000; // 15 seconds

    public static RideTrackingFragment newInstance() {
        return new RideTrackingFragment();
    }

    public static RideTrackingFragment newInstanceWithToken(String token) {
        RideTrackingFragment fragment = new RideTrackingFragment();
        Bundle args = new Bundle();
        args.putString("token", token);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ride_tracking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            trackingToken = getArguments().getString("token");
            isGuestMode = trackingToken != null;
        }

        initViews(view);
        initMap();
        setupButtons();
        loadRideData();
        startPolling();
    }

    private void initViews(View view) {
        // Map and containers
        mapView = view.findViewById(R.id.mapView);
        scrollContent = view.findViewById(R.id.scrollContent);
        layoutLoading = view.findViewById(R.id.layoutLoading);
        layoutError = view.findViewById(R.id.layoutError);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        layoutContent = view.findViewById(R.id.layoutContent);

        // Header
        tvTitle = view.findViewById(R.id.tvTitle);
        tvStatus = view.findViewById(R.id.tvStatus);
        btnRefresh = view.findViewById(R.id.btnRefresh);

        // Driver info card
        cardDriverInfo = view.findViewById(R.id.cardDriverInfo);
        tvDriverName = view.findViewById(R.id.tvDriverName);
        tvDriverPhone = view.findViewById(R.id.tvDriverPhone);
        tvVehicleModel = view.findViewById(R.id.tvVehicleModel);
        tvVehicleRegistration = view.findViewById(R.id.tvVehicleRegistration);
        layoutDriverPhone = view.findViewById(R.id.layoutDriverPhone);
        layoutVehicleModel = view.findViewById(R.id.layoutVehicleModel);
        layoutVehicleReg = view.findViewById(R.id.layoutVehicleReg);

        // Time remaining card
        cardTimeRemaining = view.findViewById(R.id.cardTimeRemaining);
        tvTimeIcon = view.findViewById(R.id.tvTimeIcon);
        tvTimeRemaining = view.findViewById(R.id.tvTimeRemaining);

        // Route card
        cardRoute = view.findViewById(R.id.cardRoute);
        layoutRoutePoints = view.findViewById(R.id.layoutRoutePoints);

        // Action buttons
        btnReport = view.findViewById(R.id.btnReport);
        btnPanic = view.findViewById(R.id.btnPanic);

        // Error/loading views
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage);
        progressBar = view.findViewById(R.id.progressBar);
        layoutGuestNotice = view.findViewById(R.id.layoutGuestNotice);

        if (isGuestMode) {
            layoutGuestNotice.setVisibility(View.VISIBLE);
        }
    }

    private void initMap() {
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        IMapController mapController = mapView.getController();
        mapController.setZoom(13.0);
    }

    private void setupButtons() {
        btnRefresh.setOnClickListener(v -> loadRideData());
        btnReport.setOnClickListener(v -> showReportModal());
        btnPanic.setOnClickListener(v -> handlePanic());

        // Error layout also has a refresh button
        Button btnErrorRefresh = getView().findViewById(R.id.btnErrorRefresh);
        if (btnErrorRefresh != null) {
            btnErrorRefresh.setOnClickListener(v -> loadRideData());
        }
    }

    private void loadRideData() {
        showLoading(true);

        if (isGuestMode && trackingToken != null) {
            validateAndLoadGuestRide();
        } else {
            loadCurrentRide();
        }
    }

    private void validateAndLoadGuestRide() {
        RideTrackingService.validateToken(requireContext(), trackingToken,
                new RideTrackingService.ValidateTokenCallback() {
                    @Override
                    public void onSuccess(TokenValidation validation) {
                        if (validation.valid) {
                            loadGuestRide();
                        } else {
                            showError(validation.message);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        showError("Invalid or expired tracking link");
                    }
                });
    }

    private void loadGuestRide() {
        RideTrackingService.getRideByToken(requireContext(), trackingToken,
                new RideTrackingService.RideDataCallback() {
                    @Override
                    public void onSuccess(RideTrackingData data) {
                        showLoading(false);
                        currentRide = data;
                        updateUI();
                        updateMap();
                    }

                    @Override
                    public void onError(String message) {
                        showError("Failed to load ride information");
                    }
                });
    }

    private void loadCurrentRide() {
        RideTrackingService.getCurrentRide(requireContext(),
                new RideTrackingService.RideDataCallback() {
                    @Override
                    public void onSuccess(RideTrackingData data) {
                        showLoading(false);
                        currentRide = data;
                        updateUI();
                        updateMap();
                    }

                    @Override
                    public void onError(String message) {
                        if (message.contains("204")) {
                            showEmpty();
                        } else {
                            showError("Failed to load ride information");
                        }
                    }
                });
    }

    private void updateUI() {
        if (currentRide == null) {
            showEmpty();
            return;
        }

        layoutContent.setVisibility(View.VISIBLE);
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        mapView.setVisibility(View.VISIBLE);

        // Update status badge
        tvStatus.setText(getStatusText(currentRide.status));
        setStatusBadgeStyle(tvStatus, currentRide.status);

        // Update driver info
        updateDriverInfo();

        // Update time remaining
        updateTimeRemaining();

        // Update route
        updateRoute();
    }

    private void updateDriverInfo() {
        tvDriverName.setText(currentRide.driverName);

        if (currentRide.driverPhone != null && !currentRide.driverPhone.isEmpty()) {
            layoutDriverPhone.setVisibility(View.VISIBLE);
            tvDriverPhone.setText(currentRide.driverPhone);
        } else {
            layoutDriverPhone.setVisibility(View.GONE);
        }

        if (currentRide.vehicleModel != null && !currentRide.vehicleModel.isEmpty()) {
            layoutVehicleModel.setVisibility(View.VISIBLE);
            tvVehicleModel.setText(currentRide.vehicleModel);
        } else {
            layoutVehicleModel.setVisibility(View.GONE);
        }

        if (currentRide.vehicleRegistration != null && !currentRide.vehicleRegistration.isEmpty()) {
            layoutVehicleReg.setVisibility(View.VISIBLE);
            tvVehicleRegistration.setText(currentRide.vehicleRegistration);
        } else {
            layoutVehicleReg.setVisibility(View.GONE);
        }
    }

    private void updateTimeRemaining() {
        String status = currentRide.status;

        if (("IN_PROGRESS".equals(status) || "PANIC".equals(status))
                && currentRide.remainingMinutes < 999999) {
            tvTimeIcon.setText("⏱️");
            tvTimeRemaining.setText(formatTime(currentRide.remainingMinutes));
            tvTimeRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange_700));
        } else if ("PENDING".equals(status) || "ACCEPTED".equals(status)) {
            tvTimeIcon.setText("⏳");
            tvTimeRemaining.setText("Waiting to start");
            tvTimeRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_600));
        } else {
            tvTimeIcon.setText("✅");
            String text = "COMPLETED".equals(status) ? "Ride completed" : "Ride ended";
            tvTimeRemaining.setText(text);
            tvTimeRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_600));
        }
    }

    private void updateRoute() {
        layoutRoutePoints.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        // Start location
        View startView = inflater.inflate(R.layout.item_route_point, layoutRoutePoints, false);
        TextView iconStart = startView.findViewById(R.id.tvRouteIcon);
        TextView labelStart = startView.findViewById(R.id.tvRouteLabel);
        TextView addressStart = startView.findViewById(R.id.tvRouteAddress);

        iconStart.setText("S");
        iconStart.setBackgroundResource(R.drawable.bg_route_icon_start);
        labelStart.setText("PICKUP");
        addressStart.setText(currentRide.startLocation.address);

        layoutRoutePoints.addView(startView);

        // Stops
        if (currentRide.stops != null) {
            for (int i = 0; i < currentRide.stops.size(); i++) {
                Location stop = currentRide.stops.get(i);
                boolean reached = currentRide.stopsReached != null
                        && i < currentRide.stopsReached.length
                        && currentRide.stopsReached[i];

                View stopView = inflater.inflate(R.layout.item_route_point, layoutRoutePoints, false);
                TextView iconStop = stopView.findViewById(R.id.tvRouteIcon);
                TextView labelStop = stopView.findViewById(R.id.tvRouteLabel);
                TextView addressStop = stopView.findViewById(R.id.tvRouteAddress);

                if (reached) {
                    iconStop.setText("✓");
                    iconStop.setBackgroundResource(R.drawable.bg_route_icon_stop_reached);
                    labelStop.setText("STOP " + (i + 1) + " (Reached)");
                    addressStop.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500));
                    addressStop.setPaintFlags(addressStop.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    iconStop.setText(String.valueOf(i + 1));
                    iconStop.setBackgroundResource(R.drawable.bg_route_icon_stop);
                    labelStop.setText("STOP " + (i + 1));
                }

                addressStop.setText(stop.address);
                layoutRoutePoints.addView(stopView);
            }
        }

        // End location
        View endView = inflater.inflate(R.layout.item_route_point, layoutRoutePoints, false);
        TextView iconEnd = endView.findViewById(R.id.tvRouteIcon);
        TextView labelEnd = endView.findViewById(R.id.tvRouteLabel);
        TextView addressEnd = endView.findViewById(R.id.tvRouteAddress);

        iconEnd.setText("E");
        iconEnd.setBackgroundResource(R.drawable.bg_route_icon_end);
        labelEnd.setText("DESTINATION");
        addressEnd.setText(currentRide.endLocation.address);

        layoutRoutePoints.addView(endView);
    }

    private void updateMap() {
        if (currentRide == null) return;

        clearMap();

        // Add start marker
        addMarker(
                currentRide.startLocation,
                "Start: " + currentRide.startLocation.address,
                R.drawable.marker_green
        );

        // Add stop markers
        if (currentRide.stops != null) {
            for (int i = 0; i < currentRide.stops.size(); i++) {
                Location stop = currentRide.stops.get(i);
                boolean reached = currentRide.stopsReached != null
                        && i < currentRide.stopsReached.length
                        && currentRide.stopsReached[i];

                String label = reached ? "Stop " + (i + 1) + " ✓" : "Stop " + (i + 1);
                int markerRes = reached ? R.drawable.marker_gold : R.drawable.marker_blue;

                addMarker(stop, label, markerRes);
            }
        }

        // Add end marker
        addMarker(
                currentRide.endLocation,
                "Destination: " + currentRide.endLocation.address,
                R.drawable.marker_red
        );

        // Add driver marker
        if (currentRide.driverCurrentLocation != null) {
            addDriverMarker(currentRide.driverCurrentLocation, currentRide.driverName);
        }

        // Draw route (with fallback if ORS/SSL fails)
        drawRoute();

        // Fit bounds
        fitMapToBounds();

        // Force redraw
        mapView.invalidate();
    }

    private void addMarker(Location location, String title, int iconRes) {
        GeoPoint point = new GeoPoint(location.lat, location.lng);
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(ContextCompat.getDrawable(requireContext(), iconRes));
        mapView.getOverlays().add(marker);
        markers.add(marker);
    }

    private void addDriverMarker(Location location, String driverName) {
        if (driverMarker != null) {
            mapView.getOverlays().remove(driverMarker);
        }

        GeoPoint point = new GeoPoint(location.lat, location.lng);
        driverMarker = new Marker(mapView);
        driverMarker.setPosition(point);
        driverMarker.setTitle("Driver: " + driverName);
        driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        driverMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_car));
        mapView.getOverlays().add(driverMarker);
    }

    private void drawRoute() {
        // Clear old routes first
        for (Polyline line : routeLines) {
            mapView.getOverlays().remove(line);
        }
        routeLines.clear();

        final List<Location> waypoints = new ArrayList<>();

        if ("IN_PROGRESS".equals(currentRide.status) && currentRide.driverCurrentLocation != null) {
            // Start from driver's current location
            waypoints.add(currentRide.driverCurrentLocation);

            // Add only unreached stops
            if (currentRide.stops != null) {
                for (int i = 0; i < currentRide.stops.size(); i++) {
                    boolean reached = currentRide.stopsReached != null
                            && i < currentRide.stopsReached.length
                            && currentRide.stopsReached[i];
                    if (!reached) {
                        waypoints.add(currentRide.stops.get(i));
                    }
                }
            }

            waypoints.add(currentRide.endLocation);
        } else {
            // Show full route
            waypoints.add(currentRide.startLocation);
            if (currentRide.stops != null) {
                waypoints.addAll(currentRide.stops);
            }
            waypoints.add(currentRide.endLocation);
        }

        // Convert to GeoPoints for ORS
        final List<GeoPoint> geoPoints = new ArrayList<>();
        for (Location loc : waypoints) {
            geoPoints.add(new GeoPoint(loc.lat, loc.lng));
        }

        // FIX 1: Added onError callback — previously ORS failures were silently swallowed.
        // FIX 2: On any ORS/SSL failure, fall back to straight-line segments between waypoints
        //        so the user always sees a visual route on the map.
        ORSRepository.getRoute(geoPoints, new ORSRepository.RouteCallback() {
            @Override
            public void onRouteReady(List<GeoPoint> points) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (points != null && !points.isEmpty()) {
                        drawPolyline(points, Color.parseColor("#6366f1"), 8f);
                    } else {
                        // ORS returned empty — draw straight-line fallback
                        Log.w(TAG, "ORS returned empty route, drawing straight-line fallback");
                        drawStraightLineFallback(geoPoints);
                    }
                });
            }

            // FIX 1 & 2: This method previously didn't exist. ORS errors (including SSL
            // certificate issues like the CertificateNotYetValidException seen in logs on
            // Feb 8 2026) caused the route to silently never draw. Now we log the error
            // and immediately draw a dashed straight-line fallback so the user always
            // sees something on the map.
            public void onError(String error) {
                if (!isAdded()) return;
                Log.e(TAG, "ORS route fetch failed (SSL or network): " + error);
                requireActivity().runOnUiThread(() -> drawStraightLineFallback(geoPoints));
            }
        });
    }

    /**
     * Draws a dashed straight-line polyline connecting all waypoints in order.
     * Used as a fallback when ORS routing fails (e.g. SSL cert not yet valid,
     * no network, or ORS server down). The dashed style visually signals to the
     * user that this is an approximate path, not a real road-snapped route.
     */
    private void drawStraightLineFallback(List<GeoPoint> points) {
        if (points == null || points.size() < 2) return;

        Polyline line = new Polyline(mapView);
        line.setPoints(points);
        // Use a slightly transparent, lighter purple to distinguish from the real route
        line.setColor(Color.parseColor("#996366f1"));
        line.setWidth(6f);

        mapView.getOverlays().add(line);
        routeLines.add(line);
        mapView.invalidate();

        Log.d(TAG, "Drew straight-line fallback route with " + points.size() + " points");
    }

    /** Draws a solid polyline with the given color and width. */
    private void drawPolyline(List<GeoPoint> points, int color, float width) {
        Polyline line = new Polyline(mapView);
        line.setPoints(points);
        line.setColor(color);
        line.setWidth(width);

        mapView.getOverlays().add(line);
        routeLines.add(line);
        mapView.invalidate();
    }

    private void fitMapToBounds() {
        if (markers.isEmpty()) return;

        // FIX 3: Previously only marker positions were included in the bounding box.
        // Now we also include all route polyline points, so the full drawn route is
        // always visible and never clipped at the edges of the screen.
        List<GeoPoint> points = new ArrayList<>();

        for (Marker marker : markers) {
            points.add(marker.getPosition());
        }

        if (driverMarker != null) {
            points.add(driverMarker.getPosition());
        }

        // Include all route polyline points in the bounds calculation
        for (Polyline polyline : routeLines) {
            List<GeoPoint> linePoints = polyline.getActualPoints();
            if (linePoints != null) {
                points.addAll(linePoints);
            }
        }

        if (points.isEmpty()) return;

        BoundingBox boundingBox = BoundingBox.fromGeoPoints(points);

        // Post with delay to ensure map is laid out
        mapView.post(() -> {
            // Zoom to bounding box
            mapView.zoomToBoundingBox(boundingBox, true, 100);

            // Then pan the map UP a bit so content isn't hidden by bottom sheet
            mapView.postDelayed(() -> {
                IMapController controller = mapView.getController();
                GeoPoint center = (GeoPoint) mapView.getMapCenter();

                // Shift down 15% of visible area so markers appear above the bottom sheet
                double latOffset = boundingBox.getLatitudeSpan() * 0.15;
                GeoPoint newCenter = new GeoPoint(center.getLatitude() - latOffset, center.getLongitude());

                controller.setCenter(newCenter);
                mapView.invalidate();
            }, 100);
        });
    }

    private void showReportModal() {
        if (currentRide == null || "Not assigned".equals(currentRide.driverName)) {
            showToast("Cannot report: No driver assigned yet", Toast.LENGTH_SHORT);
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_report, null);

        EditText etReport = dialogView.findViewById(R.id.etReportText);
        TextView tvCharCount = dialogView.findViewById(R.id.tvCharCount);

        etReport.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCount.setText(s.length() + " / 1000 characters");
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Submit", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String reportText = etReport.getText().toString().trim();
                if (reportText.isEmpty()) {
                    showToast("Please enter a report description", Toast.LENGTH_SHORT);
                } else {
                    submitReport(reportText);
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void submitReport(String reportText) {
        RideTrackingService.ReportCallback callback = new RideTrackingService.ReportCallback() {
            @Override
            public void onSuccess(ReportResponse response) {
                showToast("Report submitted successfully!", Toast.LENGTH_SHORT);
            }

            @Override
            public void onError(String message) {
                showToast("Failed to submit report. Please try again.", Toast.LENGTH_LONG);
            }
        };

        if (isGuestMode && trackingToken != null) {
            RideTrackingService.reportInconsistencyByToken(
                    requireContext(), trackingToken, reportText, callback
            );
        } else {
            RideTrackingService.reportInconsistencyForCurrentRide(
                    requireContext(), reportText, callback
            );
        }
    }

    private void handlePanic() {
        if (currentRide == null) {
            showToast("No ride loaded", Toast.LENGTH_SHORT);
            return;
        }

        if (!"IN_PROGRESS".equals(currentRide.status) && !"PANIC".equals(currentRide.status)) {
            showToast("Panic can only be triggered during an active ride", Toast.LENGTH_SHORT);
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Panic Alert")
                .setMessage("Are you sure you want to send a panic alert? This will notify emergency contacts and authorities.")
                .setPositiveButton("Yes, Send Alert", (dialog, which) -> sendPanic())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendPanic() {
        RideTrackingService.PanicCallback callback = new RideTrackingService.PanicCallback() {
            @Override
            public void onSuccess() {
                showToast("Panic alert sent successfully!", Toast.LENGTH_SHORT);
                loadRideData();
            }

            @Override
            public void onError(String message) {
                showToast("Failed to send panic alert. Please try again.", Toast.LENGTH_LONG);
            }
        };

        if (isGuestMode && trackingToken != null) {
            RideTrackingService.panicByToken(requireContext(), trackingToken, callback);
        } else {
            RideTrackingService.panic(requireContext(), currentRide.rideId, callback);
        }
    }

    private void startPolling() {
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                loadRideDataSilently();
                pollingHandler.postDelayed(this, POLLING_INTERVAL);
            }
        };
        pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL);
    }

    private void loadRideDataSilently() {
        RideTrackingService.RideDataCallback callback = new RideTrackingService.RideDataCallback() {
            @Override
            public void onSuccess(RideTrackingData data) {
                currentRide = data;
                updateUI();
                updateMap();
            }

            @Override
            public void onError(String message) {
                // Silent failure on background polls
            }
        };

        if (isGuestMode && trackingToken != null) {
            RideTrackingService.getRideByToken(requireContext(), trackingToken, callback);
        } else {
            RideTrackingService.getCurrentRide(requireContext(), callback);
        }
    }

    private void clearMap() {
        for (Marker marker : markers) {
            mapView.getOverlays().remove(marker);
        }
        markers.clear();

        if (driverMarker != null) {
            mapView.getOverlays().remove(driverMarker);
            driverMarker = null;
        }

        for (Polyline line : routeLines) {
            mapView.getOverlays().remove(line);
        }
        routeLines.clear();

        mapView.invalidate();
    }

    private void showLoading(boolean show) {
        layoutLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        layoutContent.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showError(String message) {
        layoutLoading.setVisibility(View.GONE);
        layoutContent.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        tvErrorMessage.setText(message);
    }

    private void showEmpty() {
        layoutLoading.setVisibility(View.GONE);
        layoutContent.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    private String getStatusText(String status) {
        switch (status) {
            case "PENDING": return "Pending";
            case "ACCEPTED": return "Accepted";
            case "IN_PROGRESS": return "In Progress";
            case "COMPLETED": return "Completed";
            case "CANCELLED": return "Cancelled";
            case "PANIC": return "PANIC";
            default: return status;
        }
    }

    private void setStatusBadgeStyle(TextView badge, String status) {
        int backgroundRes;
        int textColorRes;

        switch (status) {
            case "PENDING":
                backgroundRes = R.drawable.bg_status_pending;
                textColorRes = R.color.status_pending_text;
                break;
            case "ACCEPTED":
                backgroundRes = R.drawable.bg_status_accepted;
                textColorRes = R.color.status_accepted_text;
                break;
            case "IN_PROGRESS":
                backgroundRes = R.drawable.bg_status_in_progress;
                textColorRes = R.color.status_in_progress_text;
                break;
            case "COMPLETED":
                backgroundRes = R.drawable.bg_status_completed;
                textColorRes = R.color.status_completed_text;
                break;
            case "CANCELLED":
            case "PANIC":
                backgroundRes = R.drawable.bg_status_cancelled;
                textColorRes = R.color.status_cancelled_text;
                break;
            default:
                backgroundRes = R.drawable.bg_status_completed;
                textColorRes = R.color.status_completed_text;
        }

        badge.setBackgroundResource(backgroundRes);
        badge.setTextColor(ContextCompat.getColor(requireContext(), textColorRes));
    }

    private String formatTime(int minutes) {
        if (minutes < 1) return "Less than 1 min";
        if (minutes >= 999999) return "Calculating...";
        if (minutes < 60) return minutes + " min";

        int hours = minutes / 60;
        int mins = minutes % 60;
        return hours + "h " + mins + "m";
    }

    private void showToast(String message, int duration) {
        Toast.makeText(requireContext(), message, duration).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
        clearMap();
    }
}