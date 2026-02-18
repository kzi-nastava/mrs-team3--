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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.adapter.DriverRidesAdapter;
import com.example.uber3.network.model.driver.CancelRideRequest;
import com.example.uber3.network.model.driver.DriverRide;
import com.example.uber3.network.model.driver.FinishRideResponse;
import com.example.uber3.network.model.driver.Location;
import com.example.uber3.network.model.driver.PendingRide;
import com.example.uber3.network.model.driver.StopStatus;
import com.example.uber3.network.service.DriverRideService;
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

public class DriverDashboardFragment extends Fragment {

    private static final String TAG = "DriverDashboard";

    // Views
    private MapView mapView;
    private RecyclerView rvMyRides;
    private RecyclerView rvPendingRides;
    private LinearLayout layoutLoading;
    private LinearLayout layoutRideInfo;
    private LinearLayout layoutControls;
    private LinearLayout layoutStopButtons;
    private FloatingActionButton btnRefresh;
    private Button btnMoveToStart;
    private Button btnStartRide;
    private Button btnFinishRide;
    private Button btnCancelRide;
    private Button btnPanic;
    private TextView tvDistance;
    private TextView tvPrice;
    private TextView tvPassengers;
    private TextView tvMyRidesTitle;
    private TextView tvPendingRidesTitle;
    private TextView tvEmptyState;
    private ProgressBar progressBar;

    // Adapters
    private DriverRidesAdapter myRidesAdapter;
    private DriverRidesAdapter pendingRidesAdapter;

    // Data
    private List<DriverRide> myRides = new ArrayList<>();
    private List<PendingRide> pendingRides = new ArrayList<>();
    private Object selectedRide = null; // DriverRide or PendingRide

    // Map overlays
    private List<Marker> markers = new ArrayList<>();
    private List<Polyline> routeLines = new ArrayList<>();
    private Marker driverMarker = null;

    // Driver location tracking (always tracks current position)
    private Location currentDriverLocation = null;

    // Polling
    private Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private static final int POLLING_INTERVAL = 20000; // 20 seconds

    public static DriverDashboardFragment newInstance() {
        return new DriverDashboardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driver_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initMap();
        setupAdapters();
        setupButtons();
        loadData();
        startPolling();
    }

    // ─────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────

    private void initViews(View view) {
        mapView = view.findViewById(R.id.mapView);
        rvMyRides = view.findViewById(R.id.rvMyRides);
        rvPendingRides = view.findViewById(R.id.rvPendingRides);
        layoutLoading = view.findViewById(R.id.layoutLoading);
        layoutRideInfo = view.findViewById(R.id.layoutRideInfo);
        layoutControls = view.findViewById(R.id.layoutControls);
        layoutStopButtons = view.findViewById(R.id.layoutStopButtons);
        btnRefresh = view.findViewById(R.id.btnRefresh);
        btnMoveToStart = view.findViewById(R.id.btnMoveToStart);
        btnStartRide = view.findViewById(R.id.btnStartRide);
        btnFinishRide = view.findViewById(R.id.btnFinishRide);
        btnCancelRide = view.findViewById(R.id.btnCancelRide);
        btnPanic = view.findViewById(R.id.btnPanic);
        tvDistance = view.findViewById(R.id.tvDistance);
        tvPrice = view.findViewById(R.id.tvPrice);
        tvPassengers = view.findViewById(R.id.tvPassengers);
        tvMyRidesTitle = view.findViewById(R.id.tvMyRidesTitle);
        tvPendingRidesTitle = view.findViewById(R.id.tvPendingRidesTitle);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void initMap() {
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Set default view to Novi Sad, Serbia (or your city)
        IMapController controller = mapView.getController();
        controller.setZoom(13.0);
        controller.setCenter(new GeoPoint(45.2671, 19.8335)); // Novi Sad coordinates

        // Initialize driver's default location (Novi Sad)
        currentDriverLocation = new Location(45.2671, 19.8335, "");
    }

    private void setupAdapters() {
        myRidesAdapter = new DriverRidesAdapter(
                new ArrayList<>(),
                ride -> selectRide(ride),
                null
        );
        rvMyRides.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMyRides.setAdapter(myRidesAdapter);
        rvMyRides.setNestedScrollingEnabled(false);

        pendingRidesAdapter = new DriverRidesAdapter(
                new ArrayList<>(),
                null,
                this::acceptRide
        );
        rvPendingRides.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPendingRides.setAdapter(pendingRidesAdapter);
        rvPendingRides.setNestedScrollingEnabled(false);
    }

    private void setupButtons() {
        btnRefresh.setOnClickListener(v -> loadData());
        btnMoveToStart.setOnClickListener(v -> moveToStart());
        btnStartRide.setOnClickListener(v -> startRide());
        btnFinishRide.setOnClickListener(v -> showFinishModal());
        btnCancelRide.setOnClickListener(v -> showCancelModal());
        btnPanic.setOnClickListener(v -> confirmAndPanic());

        // Map tap → update driver location when IN_PROGRESS.
        // We only treat it as a tap (not a pan/zoom) if the finger barely moved.
        final float[] touchDownXY = new float[2];
        final float TAP_SLOP_PX = 20f; // pixels of movement allowed before it's a drag
        mapView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    touchDownXY[0] = event.getX();
                    touchDownXY[1] = event.getY();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                    float dx = Math.abs(event.getX() - touchDownXY[0]);
                    float dy = Math.abs(event.getY() - touchDownXY[1]);
                    boolean isTap = dx < TAP_SLOP_PX && dy < TAP_SLOP_PX;
                    if (isTap && selectedRide instanceof DriverRide) {
                        DriverRide ride = (DriverRide) selectedRide;
                        if ("IN_PROGRESS".equals(ride.status)) {
                            GeoPoint tapped = (GeoPoint) mapView.getProjection()
                                    .fromPixels((int) event.getX(), (int) event.getY());
                            if (tapped != null) {
                                updateDriverLocationAndCenter(tapped.getLatitude(), tapped.getLongitude());
                            }
                        }
                    }
                    break;
            }
            return false; // let map still handle zoom/pan
        });
    }

    // ─────────────────────────────────────────────
    // DATA LOADING
    // ─────────────────────────────────────────────

    private void loadData() {
        showLoading(true);

        DriverRideService.getMyRides(requireContext(), new DriverRideService.MyRidesCallback() {
            @Override
            public void onSuccess(List<DriverRide> rides) {
                if (!isAdded()) return;
                showLoading(false);
                myRides = rides;
                myRidesAdapter.updateRides(rides);

                if (rides.isEmpty()) {
                    // No active rides - hide the "My Rides" section completely
                    tvMyRidesTitle.setVisibility(View.GONE);
                    rvMyRides.setVisibility(View.GONE);
                    loadPendingRides();
                } else {
                    tvMyRidesTitle.setVisibility(View.VISIBLE);
                    tvMyRidesTitle.setText("My Rides");
                    rvMyRides.setVisibility(View.VISIBLE);

                    // Hide pending rides when showing My Rides
                    tvPendingRidesTitle.setVisibility(View.GONE);
                    rvPendingRides.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.GONE);

                    // Auto-select the first active ride (prefer IN_PROGRESS > ACCEPTED)
                    DriverRide autoSelect = null;
                    for (DriverRide r : rides) {
                        if ("IN_PROGRESS".equals(r.status)) { autoSelect = r; break; }
                    }
                    if (autoSelect == null) {
                        for (DriverRide r : rides) {
                            if ("ACCEPTED".equals(r.status)) { autoSelect = r; break; }
                        }
                    }
                    if (autoSelect == null && !rides.isEmpty()) autoSelect = rides.get(0);
                    if (autoSelect != null) selectRide(autoSelect);
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                showLoading(false);
                Log.e(TAG, "Failed to load my rides: " + message);
                loadPendingRides();
            }
        });
    }

    private void loadPendingRides() {
        DriverRideService.getPendingRides(requireContext(), new DriverRideService.PendingRidesCallback() {
            @Override
            public void onSuccess(List<PendingRide> rides) {
                if (!isAdded()) return;
                pendingRides = rides;
                pendingRidesAdapter.updatePendingRides(rides);

                boolean hasPendingRides = !rides.isEmpty();
                tvPendingRidesTitle.setVisibility(hasPendingRides ? View.VISIBLE : View.GONE);
                rvPendingRides.setVisibility(hasPendingRides ? View.VISIBLE : View.GONE);

                // Show empty state only if both my rides and pending rides are empty
                boolean hasNoRides = myRides.isEmpty() && rides.isEmpty();
                tvEmptyState.setVisibility(hasNoRides ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to load pending rides: " + message);

                // Show empty state if both lists are empty
                boolean hasNoRides = myRides.isEmpty() && pendingRides.isEmpty();
                tvEmptyState.setVisibility(hasNoRides ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void loadDataSilently() {
        DriverRideService.getMyRides(requireContext(), new DriverRideService.MyRidesCallback() {
            @Override
            public void onSuccess(List<DriverRide> rides) {
                if (!isAdded()) return;
                myRides = rides;
                myRidesAdapter.updateRides(rides);

                // Refresh the currently selected ride with fresh data
                if (selectedRide instanceof DriverRide) {
                    DriverRide current = (DriverRide) selectedRide;
                    for (DriverRide fresh : rides) {
                        if (fresh.rideId == current.rideId) {
                            selectedRide = fresh;
                            updateMap(false); // Don't recenter during polling
                            updateControlsVisibility();
                            updateRideInfo();
                            break;
                        }
                    }
                }
            }

            @Override
            public void onError(String message) {
                // Silent failure on background polls
            }
        });
    }

    // ─────────────────────────────────────────────
    // RIDE SELECTION
    // ─────────────────────────────────────────────

    private void selectRide(Object ride) {
        selectedRide = ride;
        updateMap();
        updateControlsVisibility();
        updateRideInfo();
    }

    // ─────────────────────────────────────────────
    // RIDE ACTIONS
    // ─────────────────────────────────────────────

    private void acceptRide(PendingRide ride) {
        DriverRideService.acceptRide(requireContext(), ride.rideId,
                new DriverRideService.AcceptRideCallback() {
                    @Override
                    public void onSuccess(DriverRide accepted) {
                        showToast("✅ Ride accepted!", Toast.LENGTH_SHORT);
                        loadData();
                    }

                    @Override
                    public void onError(String message) {
                        showToast("Failed to accept ride: " + message, Toast.LENGTH_LONG);
                    }
                });
    }

    private void moveToStart() {
        if (!(selectedRide instanceof DriverRide)) return;
        final DriverRide ride = (DriverRide) selectedRide;

        DriverRideService.moveToStart(requireContext(), new DriverRideService.MoveToStartCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                showToast("📍 Moved to pickup location", Toast.LENGTH_SHORT);

                // Update current driver location to the pickup
                currentDriverLocation = new Location(
                        ride.startLocation.lat,
                        ride.startLocation.lng,
                        ""
                );

                // Update the driver marker to the pickup location
                updateDriverMarker(currentDriverLocation);

                // Center the map on the new driver location
                GeoPoint pickupPoint = new GeoPoint(ride.startLocation.lat, ride.startLocation.lng);
                mapView.getController().setCenter(pickupPoint);
                mapView.getController().setZoom(15.0);
                mapView.invalidate();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                showToast("Failed to move to start: " + message, Toast.LENGTH_SHORT);
            }
        });
    }

    private void startRide() {
        if (!(selectedRide instanceof DriverRide)) return;
        DriverRide ride = (DriverRide) selectedRide;

        DriverRideService.startRide(requireContext(), ride.rideId,
                new DriverRideService.StartRideCallback() {
                    @Override
                    public void onSuccess(DriverRide updated) {
                        showToast("▶️ Ride started!", Toast.LENGTH_SHORT);
                        loadData();
                    }

                    @Override
                    public void onError(String message) {
                        showToast("Failed to start ride: " + message, Toast.LENGTH_SHORT);
                    }
                });
    }

    private void showFinishModal() {
        if (!(selectedRide instanceof DriverRide)) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("✅ Finish Ride")
                .setMessage("Confirm ride completion.\nHas the customer paid and exited the vehicle?")
                .setPositiveButton("Confirm & Finish", (dialog, which) -> finishRide())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void finishRide() {
        if (!(selectedRide instanceof DriverRide)) return;
        DriverRide ride = (DriverRide) selectedRide;

        // Use current driver location as the actual end location
        final Location actualEndLocation;
        if (currentDriverLocation != null) {
            actualEndLocation = new Location(
                    currentDriverLocation.lat,
                    currentDriverLocation.lng,
                    "" // Will be filled by geocoding
            );

            // Get address in background using GeocodingHelper
            new Thread(() -> {
                String address = com.example.uber3.helpers.GeocodingHelper.getAddress(
                        requireContext(),
                        currentDriverLocation.lat,
                        currentDriverLocation.lng
                );
                actualEndLocation.address = address; // Update the address (empty string if not found)

                // Now call the finish ride API on the main thread
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        finishRideWithLocation(ride.rideId, actualEndLocation);
                    });
                }
            }).start();
        } else {
            actualEndLocation = ride.endLocation;
            finishRideWithLocation(ride.rideId, actualEndLocation);
        }
    }

    private void finishRideWithLocation(long rideId, Location actualEndLocation) {
        DriverRideService.finishRide(requireContext(), rideId, actualEndLocation,
                new DriverRideService.FinishRideCallback() {
                    @Override
                    public void onSuccess(FinishRideResponse response) {
                        if (!isAdded()) return;

                        // Clear everything before loading new data
                        clearMap();
                        selectedRide = null;
                        updateControlsVisibility();

                        if (response.hasNextRide) {
                            showToast("🎉 Ride completed! Your next ride is now active.", Toast.LENGTH_LONG);
                        } else {
                            showToast("🎉 Ride completed! You are now available for new rides.", Toast.LENGTH_LONG);
                        }
                        loadData();
                    }

                    @Override
                    public void onError(String message) {
                        showToast("Failed to finish ride: " + message, Toast.LENGTH_LONG);
                    }
                });
    }

    private void showCancelModal() {
        if (!(selectedRide instanceof DriverRide)) return;
        DriverRide ride = (DriverRide) selectedRide;

        if (!"ACCEPTED".equals(ride.status)) {
            showToast("Ride cannot be cancelled at this stage", Toast.LENGTH_SHORT);
            return;
        }

        // Inflate the same dialog_report layout we already have — it has etReportText + tvCharCount
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_report, null);
        EditText etReason = dialogView.findViewById(R.id.etReportText);
        TextView tvCharCount = dialogView.findViewById(R.id.tvCharCount);

        etReason.setHint("e.g. Passenger not at pickup, health issue...");
        etReason.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCount.setText(s.length() + " / 500 characters");
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("❌ Cancel Ride")
                .setView(dialogView)
                .setPositiveButton("Confirm Cancel", null)
                .setNegativeButton("Back", null)
                .create();

        dialog.setOnShowListener(di -> {
            Button btnConfirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnConfirm.setTextColor(Color.parseColor("#DC2626"));
            btnConfirm.setOnClickListener(v -> {
                String reason = etReason.getText().toString().trim();
                if (reason.isEmpty()) {
                    showToast("Please provide a cancellation reason", Toast.LENGTH_SHORT);
                } else {
                    dialog.dismiss();
                    confirmCancelRide(reason);
                }
            });
        });

        dialog.show();
    }

    private void confirmCancelRide(String reason) {
        if (!(selectedRide instanceof DriverRide)) return;
        DriverRide ride = (DriverRide) selectedRide;

        CancelRideRequest request = new CancelRideRequest(reason);
        DriverRideService.cancelRide(requireContext(), ride.rideId, request,
                new DriverRideService.CancelRideCallback() {
                    @Override
                    public void onSuccess() {
                        showToast("Ride cancelled.", Toast.LENGTH_SHORT);
                        selectedRide = null;
                        clearMap();
                        loadData();
                    }

                    @Override
                    public void onError(String message) {
                        showToast("Failed to cancel ride: " + message, Toast.LENGTH_LONG);
                    }
                });
    }

    private void markStopReached(int stopIndex) {
        DriverRideService.reachStop(requireContext(), stopIndex,
                new DriverRideService.ReachStopCallback() {
                    @Override
                    public void onSuccess(DriverRide updated) {
                        showToast("Stop " + (stopIndex + 1) + " reached ✓", Toast.LENGTH_SHORT);
                        selectedRide = updated;
                        updateMap();
                        updateControlsVisibility();
                    }

                    @Override
                    public void onError(String message) {
                        showToast("Failed to mark stop as reached", Toast.LENGTH_SHORT);
                    }
                });
    }

    private void confirmAndPanic() {
        if (!(selectedRide instanceof DriverRide)) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Panic Alert")
                .setMessage("Send a panic alert? This will notify emergency contacts and authorities.")
                .setPositiveButton("Yes, Send Alert", (dialog, which) -> panicRide())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void panicRide() {
        if (!(selectedRide instanceof DriverRide)) return;
        DriverRide ride = (DriverRide) selectedRide;

        DriverRideService.panicRide(requireContext(), ride.rideId,
                new DriverRideService.PanicCallback() {
                    @Override
                    public void onSuccess() {
                        showToast("🚨 Panic activated! Help is on the way.", Toast.LENGTH_LONG);
                        loadDataSilently();
                    }

                    @Override
                    public void onError(String message) {
                        showToast("Failed to activate panic: " + message, Toast.LENGTH_LONG);
                    }
                });
    }

    private void updateDriverLocationAndCenter(double lat, double lng) {
        // Update the current driver location tracking variable
        currentDriverLocation = new Location(lat, lng, "");

        DriverRideService.updateLocation(requireContext(), lat, lng,
                new DriverRideService.UpdateLocationCallback() {
                    @Override
                    public void onSuccess() {
                        // Update the marker immediately on the map without waiting for a poll
                        updateDriverMarker(currentDriverLocation);

                        // Center the map on the new driver location with offset for bottom sheet
                        centerMapOnDriver(new GeoPoint(lat, lng));
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Failed to update driver location: " + message);
                    }
                });
    }

    /**
     * Center the map on the driver's position, accounting for the bottom sheet.
     * The bottom sheet covers approximately 360dp at the bottom, so we offset the center upward.
     */
    private void centerMapOnDriver(GeoPoint driverPosition) {
        mapView.post(() -> {
            IMapController controller = mapView.getController();

            // Get the map's height in pixels
            int mapHeight = mapView.getHeight();

            // Bottom sheet is ~360dp, convert to pixels
            float density = getResources().getDisplayMetrics().density;
            int bottomSheetHeightPx = (int) (360 * density);

            // Calculate offset to shift center point up by half the bottom sheet height
            // This ensures the driver marker appears in the visible area above the bottom sheet
            int offsetPx = bottomSheetHeightPx / 2;

            // Get current projection
            org.osmdroid.views.Projection projection = mapView.getProjection();

            // Convert driver position to screen coordinates
            android.graphics.Point screenPoint = projection.toPixels(driverPosition, null);

            // Shift the point down (so when centered, it appears higher on screen)
            screenPoint.y += offsetPx;

            // Convert back to geo coordinates
            GeoPoint offsetPosition = (GeoPoint) projection.fromPixels(screenPoint.x, screenPoint.y);

            // Set the center to the offset position
            controller.setCenter(offsetPosition);
            controller.setZoom(15.0);

            // Apply padding to keep marker visible
            mapView.setPadding(0, 0, 0, bottomSheetHeightPx + 40);
            mapView.invalidate();
        });
    }

    // ─────────────────────────────────────────────
    // MAP
    // ─────────────────────────────────────────────

    private void updateMap() {
        updateMap(true); // Default: do recenter
    }

    private void updateMap(boolean recenter) {
        clearMap(recenter); // Only reset padding when recentering (full refresh)
        if (selectedRide == null) return;

        if (selectedRide instanceof DriverRide) {
            updateMapForDriverRide((DriverRide) selectedRide, recenter);
        } else if (selectedRide instanceof PendingRide) {
            updateMapForPendingRide((PendingRide) selectedRide, recenter);
        }
    }

    private void updateMapForDriverRide(DriverRide ride) {
        updateMapForDriverRide(ride, true);
    }

    private void updateMapForDriverRide(DriverRide ride, boolean recenter) {
        // Start marker
        addMarker(ride.startLocation, "Pickup: " + ride.startLocation.address, R.drawable.marker_green);

        // Stop markers
        if (ride.stops != null) {
            for (int i = 0; i < ride.stops.size(); i++) {
                Location stop = ride.stops.get(i);
                boolean reached = ride.stopStatuses != null
                        && i < ride.stopStatuses.size()
                        && ride.stopStatuses.get(i).reached;
                String label = (reached ? "✓ " : "") + "Stop " + (i + 1) + ": " + stop.address;
                int icon = reached ? R.drawable.marker_gold : R.drawable.marker_blue;
                addMarker(stop, label, icon);
            }
        }

        // End marker
        addMarker(ride.endLocation, "Destination: " + ride.endLocation.address, R.drawable.marker_red);

        // Driver marker - use tracked location or fallback
        if ("IN_PROGRESS".equals(ride.status) || "ACCEPTED".equals(ride.status)) {
            Location driverLoc = currentDriverLocation != null
                    ? currentDriverLocation
                    : (ride.driverCurrentLocation != null ? ride.driverCurrentLocation : ride.startLocation);
            addDriverMarkerInternal(driverLoc, "You are here");
        }

        drawRoute(ride);
        if (recenter) {
            fitMapToBounds();
        }
        mapView.invalidate();
    }

    private void updateMapForPendingRide(PendingRide ride) {
        updateMapForPendingRide(ride, true);
    }

    private void updateMapForPendingRide(PendingRide ride, boolean recenter) {
        addMarker(ride.startLocation, "Pickup: " + ride.startLocation.address, R.drawable.marker_green);

        if (ride.stops != null) {
            for (int i = 0; i < ride.stops.size(); i++) {
                Location stop = ride.stops.get(i);
                addMarker(stop, "Stop " + (i + 1) + ": " + stop.address, R.drawable.marker_blue);
            }
        }

        addMarker(ride.endLocation, "Destination: " + ride.endLocation.address, R.drawable.marker_red);

        // Draw straight-line preview route for pending rides
        List<GeoPoint> points = new ArrayList<>();
        points.add(new GeoPoint(ride.startLocation.lat, ride.startLocation.lng));
        if (ride.stops != null) {
            for (Location s : ride.stops) points.add(new GeoPoint(s.lat, s.lng));
        }
        points.add(new GeoPoint(ride.endLocation.lat, ride.endLocation.lng));
        drawStraightLineFallback(points);

        if (recenter) {
            fitMapToBounds();
        }
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

    private void addDriverMarkerInternal(Location location, String title) {
        if (driverMarker != null) {
            mapView.getOverlays().remove(driverMarker);
        }
        driverMarker = new Marker(mapView);
        driverMarker.setPosition(new GeoPoint(location.lat, location.lng));
        driverMarker.setTitle(title);
        driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        driverMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_car));
        mapView.getOverlays().add(driverMarker);
    }

    /** Called after moveToStart() or updateLocation() to instantly reposition the driver marker. */
    private void updateDriverMarker(Location location) {
        if (driverMarker == null) {
            addDriverMarkerInternal(location, "You are here");
        } else {
            driverMarker.setPosition(new GeoPoint(location.lat, location.lng));
        }
        mapView.invalidate();
    }

    private void drawRoute(DriverRide ride) {
        for (Polyline line : routeLines) mapView.getOverlays().remove(line);
        routeLines.clear();

        final List<GeoPoint> geoPoints = new ArrayList<>();

        if ("IN_PROGRESS".equals(ride.status) && currentDriverLocation != null) {
            geoPoints.add(new GeoPoint(currentDriverLocation.lat, currentDriverLocation.lng));
            if (ride.stops != null) {
                for (int i = 0; i < ride.stops.size(); i++) {
                    boolean reached = ride.stopStatuses != null
                            && i < ride.stopStatuses.size()
                            && ride.stopStatuses.get(i).reached;
                    if (!reached) {
                        geoPoints.add(new GeoPoint(ride.stops.get(i).lat, ride.stops.get(i).lng));
                    }
                }
            }
        } else {
            geoPoints.add(new GeoPoint(ride.startLocation.lat, ride.startLocation.lng));
            if (ride.stops != null) {
                for (Location s : ride.stops) geoPoints.add(new GeoPoint(s.lat, s.lng));
            }
        }
        geoPoints.add(new GeoPoint(ride.endLocation.lat, ride.endLocation.lng));

        ORSRepository.getRoute(geoPoints, new ORSRepository.RouteCallback() {
            @Override
            public void onRouteReady(List<GeoPoint> points) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (points != null && !points.isEmpty()) {
                        drawPolyline(points, Color.parseColor("#6366f1"), 8f);
                    } else {
                        drawStraightLineFallback(geoPoints);
                    }
                });
            }

            public void onError(String error) {
                if (!isAdded()) return;
                Log.e(TAG, "ORS route failed: " + error);
                requireActivity().runOnUiThread(() -> drawStraightLineFallback(geoPoints));
            }
        });
    }

    private void drawPolyline(List<GeoPoint> points, int color, float width) {
        Polyline line = new Polyline(mapView);
        line.setPoints(points);
        line.setColor(color);
        line.setWidth(width);
        mapView.getOverlays().add(line);
        routeLines.add(line);
        mapView.invalidate();
    }

    private void drawStraightLineFallback(List<GeoPoint> points) {
        if (points == null || points.size() < 2) return;
        Polyline line = new Polyline(mapView);
        line.setPoints(points);
        line.setColor(Color.parseColor("#996366f1"));
        line.setWidth(6f);
        mapView.getOverlays().add(line);
        routeLines.add(line);
        mapView.invalidate();
    }

    private void fitMapToBounds() {
        // If driver marker exists, center on driver using the same reliable offset approach
        if (driverMarker != null) {
            GeoPoint driverPos = driverMarker.getPosition();
            mapView.post(() -> {
                mapView.getController().setZoom(15.0);
                // Post again after zoom is applied so the projection is accurate
                mapView.post(() -> centerMapOnDriver(driverPos));
            });
            return;
        }

        // Otherwise, fit to all markers and routes
        if (markers.isEmpty()) {
            // Reset padding when no markers
            mapView.setPadding(0, 0, 0, 0);
            return;
        }

        List<GeoPoint> points = new ArrayList<>();
        for (Marker m : markers) points.add(m.getPosition());
        for (Polyline p : routeLines) {
            List<GeoPoint> lp = p.getActualPoints();
            if (lp != null) points.addAll(lp);
        }
        if (points.isEmpty()) return;

        BoundingBox box = BoundingBox.fromGeoPoints(points);
        mapView.post(() -> mapView.zoomToBoundingBox(box, true, 80));
    }

    private void clearMap() {
        clearMap(true); // Default: reset padding
    }

    private void clearMap(boolean resetPadding) {
        for (Marker m : markers) mapView.getOverlays().remove(m);
        markers.clear();

        if (driverMarker != null) {
            mapView.getOverlays().remove(driverMarker);
            driverMarker = null;
        }

        for (Polyline line : routeLines) mapView.getOverlays().remove(line);
        routeLines.clear();

        // Only reset map padding when explicitly requested (e.g., after finishing ride)
        if (resetPadding) {
            mapView.setPadding(0, 0, 0, 0);
        }

        mapView.invalidate();
    }

    // ─────────────────────────────────────────────
    // UI STATE
    // ─────────────────────────────────────────────

    private void updateControlsVisibility() {
        if (!(selectedRide instanceof DriverRide)) {
            layoutRideInfo.setVisibility(View.GONE);
            layoutControls.setVisibility(View.GONE);
            layoutStopButtons.setVisibility(View.GONE);
            return;
        }

        DriverRide ride = (DriverRide) selectedRide;
        layoutRideInfo.setVisibility(View.VISIBLE);

        boolean isAccepted = "ACCEPTED".equals(ride.status);
        boolean isInProgress = "IN_PROGRESS".equals(ride.status);
        boolean isActive = isAccepted || isInProgress;

        // Show/hide each button group based on status
        btnMoveToStart.setVisibility(isAccepted ? View.VISIBLE : View.GONE);
        btnStartRide.setVisibility(isAccepted ? View.VISIBLE : View.GONE);
        btnCancelRide.setVisibility(isAccepted ? View.VISIBLE : View.GONE);

        btnFinishRide.setVisibility(isInProgress ? View.VISIBLE : View.GONE);
        btnPanic.setVisibility(isInProgress ? View.VISIBLE : View.GONE);

        layoutControls.setVisibility(isActive ? View.VISIBLE : View.GONE);

        // Rebuild stop buttons for IN_PROGRESS
        layoutStopButtons.removeAllViews();
        if (isInProgress && ride.stopStatuses != null && !ride.stopStatuses.isEmpty()) {
            layoutStopButtons.setVisibility(View.VISIBLE);

            // Label
            TextView tvLabel = new TextView(requireContext());
            tvLabel.setText("Mark Stops as Reached:");
            tvLabel.setTextSize(13f);
            tvLabel.setTextColor(Color.parseColor("#6B7280"));
            tvLabel.setPadding(0, 0, 0, 8);
            layoutStopButtons.addView(tvLabel);

            for (int i = 0; i < ride.stopStatuses.size(); i++) {
                final int stopIndex = i;
                StopStatus ss = ride.stopStatuses.get(i);
                boolean reached = ss.reached;

                Button btn = new Button(requireContext());
                btn.setText(reached ? "✓  Stop " + (i + 1) + " (Reached)" : "○  Stop " + (i + 1));
                btn.setEnabled(!reached);

                if (reached) {
                    btn.setBackgroundColor(Color.parseColor("#D1FAE5"));
                    btn.setTextColor(Color.parseColor("#065F46"));
                } else {
                    btn.setBackgroundColor(Color.WHITE);
                    btn.setTextColor(Color.parseColor("#374151"));
                }

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 4, 0, 4);
                btn.setLayoutParams(params);
                btn.setOnClickListener(v -> markStopReached(stopIndex));
                layoutStopButtons.addView(btn);
            }
        } else {
            layoutStopButtons.setVisibility(View.GONE);
        }
    }

    private void updateRideInfo() {
        if (selectedRide instanceof DriverRide) {
            DriverRide ride = (DriverRide) selectedRide;
            tvDistance.setText(String.format("%.1f km", ride.distance));
            tvPrice.setText(String.format("%.2f RSD", ride.calculatedPrice));
            tvPassengers.setText(String.valueOf(ride.passengerCount));
        } else if (selectedRide instanceof PendingRide) {
            PendingRide ride = (PendingRide) selectedRide;
            tvDistance.setText(String.format("%.1f km", ride.distance));
            tvPrice.setText(String.format("%.2f RSD", ride.calculatedPrice));
            tvPassengers.setText(String.valueOf(ride.passengerCount));
        }
    }

    private void showLoading(boolean show) {
        layoutLoading.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showToast(String message, int duration) {
        Toast.makeText(requireContext(), message, duration).show();
    }

    // ─────────────────────────────────────────────
    // POLLING
    // ─────────────────────────────────────────────

    private void startPolling() {
        pollingRunnable = () -> {
            // Only poll when there's an active ride to watch
            if (selectedRide instanceof DriverRide) {
                DriverRide ride = (DriverRide) selectedRide;
                if ("ACCEPTED".equals(ride.status) || "IN_PROGRESS".equals(ride.status)) {
                    loadDataSilently();
                }
            }
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