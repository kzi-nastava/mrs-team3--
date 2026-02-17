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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.uber3.network.model.admin.AdminRideTrackingData;
import com.example.uber3.network.model.admin.PassengerInfo;
import com.example.uber3.network.model.admin.RideInviteInfo;
import com.example.uber3.network.model.driver.Location;
import com.example.uber3.network.model.driver.StopStatus;
import com.example.uber3.network.service.AdminRideTrackingService;
import com.example.uber3.repository.ORSRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

public class AdminRideTrackingFragment extends Fragment {

    private static final String TAG = "AdminRideTracking";
    private static final int POLLING_INTERVAL = 20000; // 20 seconds

    // Views
    private MapView mapView;
    private LinearLayout layoutLoading;
    private LinearLayout layoutError;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutContent;
    private TextView tvStatus;
    private TextView tvPanicBadge;
    private TextView tvDriverName;
    private TextView tvDriverEmail;
    private TextView tvDriverPhone;
    private TextView tvVehicleModel;
    private TextView tvVehicleRegistration;
    private TextView tvDistance;
    private TextView tvPrice;
    private TextView tvTimeRemaining;
    private TextView tvErrorMessage;
    private TextView tvPassengersTitle;
    private TextView tvInvitesTitle;
    private LinearLayout layoutRoute;
    private LinearLayout layoutDriverPhone;
    private LinearLayout layoutVehicleModel;
    private LinearLayout layoutVehicleReg;
    private LinearLayout layoutPassengers;
    private LinearLayout layoutInvites;
    private Button btnBack;
    private Button btnBackFromEmpty;
    private Button btnRetry;
    private FloatingActionButton btnRefresh;

    // Map overlays
    private List<Marker> markers = new ArrayList<>();
    private List<Polyline> routeLines = new ArrayList<>();
    private Marker driverMarker = null;

    // Data
    private AdminRideTrackingData currentRide = null;
    private long driverId;

    // Polling
    private Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;

    public static AdminRideTrackingFragment newInstance(long driverId) {
        AdminRideTrackingFragment fragment = new AdminRideTrackingFragment();
        Bundle args = new Bundle();
        args.putLong("driverId", driverId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_ride_tracking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            driverId = getArguments().getLong("driverId");
        }

        initViews(view);
        initMap();
        setupButtons();
        loadRideData();
        startPolling();
    }

    private void initViews(View view) {
        mapView = view.findViewById(R.id.mapView);
        layoutLoading = view.findViewById(R.id.layoutLoading);
        layoutError = view.findViewById(R.id.layoutError);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        layoutContent = view.findViewById(R.id.layoutContent);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvPanicBadge = view.findViewById(R.id.tvPanicBadge);
        tvDriverName = view.findViewById(R.id.tvDriverName);
        tvDriverEmail = view.findViewById(R.id.tvDriverEmail);
        tvDriverPhone = view.findViewById(R.id.tvDriverPhone);
        tvVehicleModel = view.findViewById(R.id.tvVehicleModel);
        tvVehicleRegistration = view.findViewById(R.id.tvVehicleRegistration);
        tvDistance = view.findViewById(R.id.tvDistance);
        tvPrice = view.findViewById(R.id.tvPrice);
        tvTimeRemaining = view.findViewById(R.id.tvTimeRemaining);
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage);
        tvPassengersTitle = view.findViewById(R.id.tvPassengersTitle);
        tvInvitesTitle = view.findViewById(R.id.tvInvitesTitle);
        layoutRoute = view.findViewById(R.id.layoutRoute);
        layoutDriverPhone = view.findViewById(R.id.layoutDriverPhone);
        layoutVehicleModel = view.findViewById(R.id.layoutVehicleModel);
        layoutVehicleReg = view.findViewById(R.id.layoutVehicleReg);
        layoutPassengers = view.findViewById(R.id.layoutPassengers);
        layoutInvites = view.findViewById(R.id.layoutInvites);
        btnBack = view.findViewById(R.id.btnBack);
        btnBackFromEmpty = view.findViewById(R.id.btnBackFromEmpty);
        btnRetry = view.findViewById(R.id.btnRetry);
        btnRefresh = view.findViewById(R.id.btnRefresh);
    }

    private void initMap() {
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        IMapController controller = mapView.getController();
        controller.setZoom(13.0);
        controller.setCenter(new GeoPoint(45.2671, 19.8335)); // Novi Sad default
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> goBack());
        btnBackFromEmpty.setOnClickListener(v -> goBack());
        btnRetry.setOnClickListener(v -> loadRideData());
        btnRefresh.setOnClickListener(v -> loadRideData());
    }

    private void goBack() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    // ─────────────────────────────────────────────
    // DATA LOADING
    // ─────────────────────────────────────────────

    private void loadRideData() {
        showLoading();

        AdminRideTrackingService.getRideByDriverId(requireContext(), driverId,
                new AdminRideTrackingService.RideDataCallback() {
                    @Override
                    public void onSuccess(AdminRideTrackingData data) {
                        if (!isAdded()) return;
                        currentRide = data;
                        showContent();
                        displayRideData();
                        updateMap();
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) return;
                        if ("204".equals(message)) {
                            showEmpty();
                        } else {
                            showError(message);
                        }
                    }
                });
    }

    private void loadRideDataSilently() {
        AdminRideTrackingService.getRideByDriverId(requireContext(), driverId,
                new AdminRideTrackingService.RideDataCallback() {
                    @Override
                    public void onSuccess(AdminRideTrackingData data) {
                        if (!isAdded()) return;
                        currentRide = data;
                        displayRideData();
                        updateMap(false); // Don't recenter during polling
                    }

                    @Override
                    public void onError(String message) {
                        // Silent failure on background polls
                    }
                });
    }

    // ─────────────────────────────────────────────
    // DISPLAY
    // ─────────────────────────────────────────────

    private void displayRideData() {
        if (currentRide == null) return;

        // Status badge
        tvStatus.setText(getStatusText(currentRide.status));
        setStatusBadgeStyle(tvStatus, currentRide.status);

        // Panic badge
        tvPanicBadge.setVisibility(currentRide.panic ? View.VISIBLE : View.GONE);

        // Driver info
        tvDriverName.setText(currentRide.driverName);
        tvDriverEmail.setText(currentRide.driverEmail);

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

        // Stats
        tvDistance.setText(String.format("%.1f km", currentRide.distanceKm));
        tvPrice.setText(String.format("%.2f RSD", currentRide.calculatedPrice));

        if ("IN_PROGRESS".equals(currentRide.status) && currentRide.remainingMinutes < 999999) {
            tvTimeRemaining.setText(formatTime(currentRide.remainingMinutes));
        } else {
            tvTimeRemaining.setText("--");
        }

        buildPassengersView();
        buildInvitesView();
        buildRouteView();
    }

    private void buildPassengersView() {
        layoutPassengers.removeAllViews();

        if (currentRide.passengers == null || currentRide.passengers.isEmpty()) {
            tvPassengersTitle.setVisibility(View.GONE);
            return;
        }

        tvPassengersTitle.setText("Passengers (" + currentRide.passengers.size() + ")");
        tvPassengersTitle.setVisibility(View.VISIBLE);

        for (PassengerInfo p : currentRide.passengers) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 2, 0, 2);
            row.setLayoutParams(rowParams);

            TextView tvName = new TextView(requireContext());
            tvName.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvName.setText("👤 " + p.name + " " + p.surname);
            tvName.setTextSize(12f);
            tvName.setTextColor(0xFF374151);

            TextView tvEmail = new TextView(requireContext());
            tvEmail.setText(p.email);
            tvEmail.setTextSize(11f);
            tvEmail.setTextColor(0xFF6B7280);

            row.addView(tvName);
            row.addView(tvEmail);
            layoutPassengers.addView(row);
        }
    }

    private void buildInvitesView() {
        layoutInvites.removeAllViews();

        if (currentRide.invites == null || currentRide.invites.isEmpty()) {
            tvInvitesTitle.setVisibility(View.GONE);
            return;
        }

        tvInvitesTitle.setText("Email Invites (" + currentRide.invites.size() + ")");
        tvInvitesTitle.setVisibility(View.VISIBLE);

        for (RideInviteInfo invite : currentRide.invites) {
            TextView tvInvite = new TextView(requireContext());
            tvInvite.setText("✉ " + invite.email);
            tvInvite.setTextSize(12f);
            tvInvite.setTextColor(0xFF374151);
            tvInvite.setPadding(0, 2, 0, 2);
            layoutInvites.addView(tvInvite);
        }
    }

    private void buildRouteView() {
        layoutRoute.removeAllViews();

        addRoutePoint("🟢 Pickup", currentRide.startLocation.address, false);

        if (currentRide.stopStatuses != null) {
            for (int i = 0; i < currentRide.stopStatuses.size(); i++) {
                StopStatus stop = currentRide.stopStatuses.get(i);
                String icon = stop.reached ? "🟡" : "🔵";
                String label = "Stop " + (i + 1) + (stop.reached ? " ✓" : "");
                addRoutePoint(icon + " " + label, stop.location.address, stop.reached);
            }
        }

        addRoutePoint("🔴 Destination", currentRide.endLocation.address, false);
    }

    private void addRoutePoint(String label, String address, boolean reached) {
        TextView tvPoint = new TextView(requireContext());
        tvPoint.setText(label + "\n" + address);
        tvPoint.setTextSize(12f);
        tvPoint.setTextColor(reached ? 0xFF065F46 : 0xFF374151);
        tvPoint.setPadding(0, 6, 0, 6);
        layoutRoute.addView(tvPoint);
    }

    // ─────────────────────────────────────────────
    // MAP
    // ─────────────────────────────────────────────

    private void updateMap() {
        updateMap(true);
    }

    private void updateMap(boolean recenter) {
        clearMap();
        if (currentRide == null) return;

        addMarker(currentRide.startLocation, "Pickup", R.drawable.marker_green);

        if (currentRide.stopStatuses != null) {
            for (int i = 0; i < currentRide.stopStatuses.size(); i++) {
                StopStatus stop = currentRide.stopStatuses.get(i);
                String label = "Stop " + (i + 1) + (stop.reached ? " ✓" : "");
                int icon = stop.reached ? R.drawable.marker_gold : R.drawable.marker_blue;
                addMarker(stop.location, label, icon);
            }
        }

        addMarker(currentRide.endLocation, "Destination", R.drawable.marker_red);

        if (currentRide.driverCurrentLocation != null) {
            addDriverMarker(currentRide.driverCurrentLocation, currentRide.driverName);
        }

        drawRoute();

        if (recenter) {
            centerOnDriver();
        }

        mapView.invalidate();
    }

    /**
     * Mirrors DriverDashboard.fitMapToBounds():
     * - If driver location is known → center on driver at zoom 15 with bottom padding
     * - Otherwise → fit all markers with bottom padding for the sheet
     */
    private void centerOnDriver() {
        if (driverMarker != null) {
            GeoPoint driverPos = driverMarker.getPosition();
            mapView.post(() -> {
                IMapController controller = mapView.getController();
                controller.setCenter(driverPos);
                controller.setZoom(15.0);
                mapView.setPadding(0, 0, 0, 360);
            });
            return;
        }

        if (markers.isEmpty()) return;

        List<GeoPoint> points = new ArrayList<>();
        for (Marker m : markers) points.add(m.getPosition());

        mapView.setPadding(40, 40, 40, 380);
        BoundingBox box = BoundingBox.fromGeoPoints(points);
        mapView.post(() -> mapView.zoomToBoundingBox(box, true));
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
        driverMarker = new Marker(mapView);
        driverMarker.setPosition(new GeoPoint(location.lat, location.lng));
        driverMarker.setTitle("Driver: " + driverName);
        driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        driverMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_car));
        mapView.getOverlays().add(driverMarker);
    }

    private void drawRoute() {
        for (Polyline line : routeLines) mapView.getOverlays().remove(line);
        routeLines.clear();

        List<GeoPoint> waypoints = new ArrayList<>();

        if ("IN_PROGRESS".equals(currentRide.status) && currentRide.driverCurrentLocation != null) {
            waypoints.add(new GeoPoint(
                    currentRide.driverCurrentLocation.lat,
                    currentRide.driverCurrentLocation.lng));

            if (currentRide.stopStatuses != null) {
                for (StopStatus stop : currentRide.stopStatuses) {
                    if (!stop.reached) {
                        waypoints.add(new GeoPoint(stop.location.lat, stop.location.lng));
                    }
                }
            }
        } else {
            waypoints.add(new GeoPoint(currentRide.startLocation.lat, currentRide.startLocation.lng));

            if (currentRide.stops != null) {
                for (Location stop : currentRide.stops) {
                    waypoints.add(new GeoPoint(stop.lat, stop.lng));
                }
            }
        }

        waypoints.add(new GeoPoint(currentRide.endLocation.lat, currentRide.endLocation.lng));

        ORSRepository.getRoute(waypoints, new ORSRepository.RouteCallback() {
            @Override
            public void onRouteReady(List<GeoPoint> points) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (points != null && !points.isEmpty()) {
                        drawPolyline(points);
                    } else {
                        drawStraightLine(waypoints);
                    }
                });
            }
        });
    }

    private void drawPolyline(List<GeoPoint> points) {
        Polyline line = new Polyline(mapView);
        line.setPoints(points);
        line.setColor(Color.parseColor("#6366f1"));
        line.setWidth(6f);
        mapView.getOverlays().add(line);
        routeLines.add(line);
        mapView.invalidate();
    }

    private void drawStraightLine(List<GeoPoint> points) {
        Polyline line = new Polyline(mapView);
        line.setPoints(points);
        line.setColor(Color.parseColor("#996366f1"));
        line.setWidth(4f);
        mapView.getOverlays().add(line);
        routeLines.add(line);
        mapView.invalidate();
    }

    private void clearMap() {
        for (Marker m : markers) mapView.getOverlays().remove(m);
        markers.clear();

        if (driverMarker != null) {
            mapView.getOverlays().remove(driverMarker);
            driverMarker = null;
        }

        for (Polyline line : routeLines) mapView.getOverlays().remove(line);
        routeLines.clear();

        mapView.invalidate();
    }

    // ─────────────────────────────────────────────
    // UI STATE
    // ─────────────────────────────────────────────

    private void showLoading() {
        layoutLoading.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        layoutContent.setVisibility(View.GONE);
    }

    private void showError(String message) {
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        layoutContent.setVisibility(View.GONE);
        tvErrorMessage.setText(message);
    }

    private void showEmpty() {
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        layoutContent.setVisibility(View.GONE);
    }

    private void showContent() {
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private String getStatusText(String status) {
        switch (status) {
            case "PENDING":     return "Pending";
            case "ACCEPTED":    return "Accepted";
            case "IN_PROGRESS": return "In Progress";
            case "COMPLETED":   return "Completed";
            case "CANCELLED":   return "Cancelled";
            case "PANIC":       return "PANIC";
            default:            return status;
        }
    }

    private void setStatusBadgeStyle(TextView badge, String status) {
        int backgroundColor, textColor;
        switch (status) {
            case "PENDING":
                backgroundColor = 0xFFFEF3C7; textColor = 0xFF92400E; break;
            case "ACCEPTED":
                backgroundColor = 0xFFDBEAFE; textColor = 0xFF1E40AF; break;
            case "IN_PROGRESS":
                backgroundColor = 0xFFD1FAE5; textColor = 0xFF065F46; break;
            case "COMPLETED":
                backgroundColor = 0xFFE5E7EB; textColor = 0xFF1F2937; break;
            case "CANCELLED":
            case "PANIC":
                backgroundColor = 0xFFFEE2E2; textColor = 0xFF991B1B; break;
            default:
                backgroundColor = 0xFFE5E7EB; textColor = 0xFF374151;
        }
        badge.setBackgroundColor(backgroundColor);
        badge.setTextColor(textColor);
    }

    private String formatTime(int minutes) {
        if (minutes >= 999999) return "Waiting";
        if (minutes <= 0) return "Arrived";
        if (minutes < 60) return minutes + " min";
        int hours = minutes / 60;
        int mins = minutes % 60;
        return mins > 0 ? hours + "h " + mins + "m" : hours + "h";
    }

    // ─────────────────────────────────────────────
    // POLLING
    // ─────────────────────────────────────────────

    private void startPolling() {
        pollingRunnable = () -> {
            loadRideDataSilently();
            pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL);
        };
        pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL);
    }

    // ─────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
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