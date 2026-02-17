package com.example.uber3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.osmdroid.views.overlay.Marker;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;

import com.example.uber3.network.model.location.ActiveVehicle;
import com.example.uber3.network.service.VehicleService;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import org.osmdroid.views.overlay.Polyline;
import android.graphics.Color;

import com.example.uber3.repository.ORSRepository;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;




public class MapFragment extends Fragment {

    public enum PointType {
        PICKUP,
        STOP,
        DESTINATION
    }


    private MapView mapView;
    private final List<Marker> markers = new ArrayList<>();

    private final List<Marker> vehicleMarkers = new ArrayList<>();

    private Polyline routeLine;

    public static MapFragment instance;


    public static List<GeoPoint> selectedPoints = new ArrayList<>();



    public interface OnLocationSelectedListener {
        void onLocationSelected(double lat, double lng);
    }

    private static OnLocationSelectedListener listener;

    public static void setOnLocationSelectedListener(OnLocationSelectedListener l) {
        listener = l;
    }



    public MapFragment() {
    }

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Configuration.getInstance().setUserAgentValue(
                requireContext().getPackageName()
        );

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = view.findViewById(R.id.mapView);
        instance = this;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        IMapController mapController = mapView.getController();
        mapController.setZoom(14.0);

        GeoPoint startPoint = new GeoPoint(45.2671, 19.8335);
        mapController.setCenter(startPoint);


        MapEventsOverlay overlayEvents = getMapEventsOverlay();
        mapView.getOverlays().add(overlayEvents);



        return view;
    }

    @NonNull
    private MapEventsOverlay getMapEventsOverlay() {
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {

                selectedPoints.add(p);

                Marker marker = new Marker(mapView);
                marker.setPosition(p);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                mapView.getOverlays().add(marker);
                markers.add(marker);

                for (int i = 0; i < markers.size(); i++) {

                    Drawable icon = ContextCompat.getDrawable(
                            requireContext(),
                            org.osmdroid.library.R.drawable.marker_default
                    );

                    if (icon == null) continue;

                    if (i == 0) {
                        icon.setTint(android.graphics.Color.GREEN);
                    }
                    else if (i == markers.size() - 1) {
                        icon.setTint(android.graphics.Color.RED);
                    }
                    else {
                        icon.setTint(android.graphics.Color.BLUE);
                    }

                    markers.get(i).setIcon(icon);
                }

                if (listener != null) {
                    listener.onLocationSelected(
                            p.getLatitude(),
                            p.getLongitude()
                    );
                }

                requestRoute();
                zoomToPoints();
                mapView.invalidate();

                return true;
            }


            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };

        return new MapEventsOverlay(mReceive);
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

    private void requestRoute() {

        if (selectedPoints.size() < 2) return;

        ORSRepository.getRoute(
                selectedPoints,
                points -> {

                    if (points.isEmpty()) return;

                    requireActivity().runOnUiThread(() -> {

                        if (routeLine != null) {
                            mapView.getOverlays()
                                    .remove(routeLine);
                        }

                        routeLine = new Polyline();
                        routeLine.setPoints(points);
                        routeLine.setWidth(10f);
                        routeLine.setColor(
                                android.graphics.Color.BLUE
                        );

                        mapView.getOverlays()
                                .add(routeLine);

                        mapView.invalidate();
                    });
                }
        );
    }


    private void zoomToPoints() {

        if (selectedPoints.size() < 2) return;

        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;

        for (GeoPoint p : selectedPoints) {
            minLat = Math.min(minLat, p.getLatitude());
            maxLat = Math.max(maxLat, p.getLatitude());
            minLon = Math.min(minLon, p.getLongitude());
            maxLon = Math.max(maxLon, p.getLongitude());
        }

        org.osmdroid.util.BoundingBox box =
                new org.osmdroid.util.BoundingBox(
                        maxLat,
                        maxLon,
                        minLat,
                        minLon
                );

        mapView.zoomToBoundingBox(box, true, 100);
    }


    public void clearMap() {

        selectedPoints.clear();

        for (Marker m : markers) {
            mapView.getOverlays().remove(m);
        }
        markers.clear();

        if (routeLine != null) {
            mapView.getOverlays().remove(routeLine);
            routeLine = null;
        }

        mapView.invalidate();
    }

    public MapView getMapView() {
        return mapView;
    }

    public void addPointFromSearch(GeoPoint p) {

        selectedPoints.add(p);

        Marker marker = new Marker(mapView);
        marker.setPosition(p);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        mapView.getOverlays().add(marker);
        markers.add(marker);

            for (int i = 0; i < markers.size(); i++) {

            Drawable icon = ContextCompat.getDrawable(
                    requireContext(),
                    org.osmdroid.library.R.drawable.marker_default
            );

            if (icon == null) continue;

            if (i == 0) {
                icon.setTint(android.graphics.Color.GREEN);
            } else if (i == markers.size() - 1) {
                icon.setTint(android.graphics.Color.RED);
            } else {
                icon.setTint(android.graphics.Color.BLUE);
            }

            markers.get(i).setIcon(icon);
        }

        requestRoute();
        zoomToPoints();
        mapView.invalidate();
    }

    public void addTypedPoint(
            GeoPoint p,
            PointType type
    ) {

        if (type == PointType.PICKUP) {

            if (selectedPoints.isEmpty())
                selectedPoints.add(p);
            else
                selectedPoints.set(0, p);

        } else if (type == PointType.DESTINATION) {

            if (selectedPoints.size() < 2)
                selectedPoints.add(p);
            else
                selectedPoints.set(
                        selectedPoints.size() - 1,
                        p
                );

        } else {

            if (selectedPoints.size() < 2) {
                selectedPoints.add(p);
            } else {
                selectedPoints.add(
                        selectedPoints.size() - 1,
                        p
                );
            }
        }

        redrawMarkers();
    }


    void redrawMarkers() {

        for (Marker m : markers) {
            mapView.getOverlays().remove(m);
        }
        markers.clear();

        for (int i = 0; i < selectedPoints.size(); i++) {

            GeoPoint p = selectedPoints.get(i);

            Marker marker = new Marker(mapView);
            marker.setPosition(p);
            marker.setAnchor(
                    Marker.ANCHOR_CENTER,
                    Marker.ANCHOR_BOTTOM
            );

            Drawable icon = ContextCompat.getDrawable(
                    requireContext(),
                    org.osmdroid.library.R.drawable.marker_default
            );

            if (icon != null) {

                if (i == 0)
                    icon.setTint(Color.GREEN);
                else if (i == selectedPoints.size() - 1)
                    icon.setTint(Color.RED);
                else
                    icon.setTint(Color.BLUE);

                marker.setIcon(icon);
            }

            markers.add(marker);
            mapView.getOverlays().add(marker);
        }

        requestRoute();
        zoomToPoints();
        mapView.invalidate();
    }


    public void removeStopAt(int stopIndex) {

        int realIndex = stopIndex + 1;

        if (selectedPoints.size() <= realIndex + 1)
            return;

        selectedPoints.remove(realIndex);

        redrawMarkers();
    }



    public void loadAndShowActiveVehicles() {
        VehicleService.getActiveVehicles(requireContext(), new VehicleService.VehiclesCallback() {

            @Override
            public void onSuccess(List<ActiveVehicle> vehicles) {
                requireActivity().runOnUiThread(() -> {
                    clearVehicleMarkers();
                    for (ActiveVehicle vehicle : vehicles) {
                        addVehicleMarker(vehicle);
                    }
                    mapView.invalidate();
                });
            }

            @Override
            public void onError(String message) {
                android.util.Log.e("MapFragment", "Vehicle load error: " + message);
            }
        });
    }

    private void addVehicleMarker(ActiveVehicle vehicle) {
        GeoPoint point = new GeoPoint(vehicle.latitude, vehicle.longitude);

        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        // Green = available, Red = unavailable
        int drawableRes = vehicle.available
                ? R.drawable.ic_car_green
                : R.drawable.ic_car_red;

        Drawable icon = ContextCompat.getDrawable(requireContext(), drawableRes);
        marker.setIcon(icon);

        // Popup info
        String status = vehicle.available ? "Available ✓" : "Unavailable";
        marker.setTitle(vehicle.registrationNumber);
        marker.setSnippet(status);
        marker.setInfoWindow(new org.osmdroid.views.overlay.infowindow.BasicInfoWindow(
                org.osmdroid.library.R.layout.bonuspack_bubble, mapView));

        vehicleMarkers.add(marker);
        mapView.getOverlays().add(marker);
    }

    private void clearVehicleMarkers() {
        for (Marker m : vehicleMarkers) {
            mapView.getOverlays().remove(m);
        }
        vehicleMarkers.clear();
    }

    public void refreshVehicles() {
        loadAndShowActiveVehicles();
    }



}
