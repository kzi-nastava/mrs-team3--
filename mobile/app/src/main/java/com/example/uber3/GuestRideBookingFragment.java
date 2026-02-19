package com.example.uber3;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.uber3.helpers.GeocodingHelper;
import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.ApiService;
import com.example.uber3.network.model.location.LocationRequest;
import com.example.uber3.network.model.ride.RouteEstimateRequest;
import com.example.uber3.network.model.ride.RouteEstimateResponse;
import com.example.uber3.repository.ORSRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GuestRideBookingFragment extends BottomSheetDialogFragment {

    private AutoCompleteTextView pickupInput;
    private AutoCompleteTextView destinationInput;
    private boolean isAutoFilling = false;
    private ApiService apiService;

    public static GuestRideBookingFragment newInstance() {
        return new GuestRideBookingFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_guest_ride_booking, container, false);

        apiService = ApiClient.getClient(requireContext()).create(ApiService.class);

        pickupInput = view.findViewById(R.id.guestPickupInput);
        destinationInput = view.findViewById(R.id.guestDestinationInput);
        MaterialButton estimateButton = view.findViewById(R.id.guestEstimateButton);

        setupLiveAutocomplete(pickupInput, MapFragment.PointType.PICKUP);
        setupLiveAutocomplete(destinationInput, MapFragment.PointType.DESTINATION);

        fillFromMapPoints();

        estimateButton.setOnClickListener(v -> estimateRide());

        return view;
    }

    private void fillFromMapPoints() {
        if (MapFragment.selectedPoints.isEmpty()) return;

        GeoPoint first = MapFragment.selectedPoints.get(0);
        new Thread(() -> {
            String addr = GeocodingHelper.getAddress(requireContext(), first.getLatitude(), first.getLongitude());
            if (isAdded()) requireActivity().runOnUiThread(() -> {
                isAutoFilling = true;
                pickupInput.setText(addr);
                pickupInput.dismissDropDown();
                isAutoFilling = false;
            });
        }).start();

        if (MapFragment.selectedPoints.size() > 1) {
            GeoPoint last = MapFragment.selectedPoints.get(MapFragment.selectedPoints.size() - 1);
            new Thread(() -> {
                String addr = GeocodingHelper.getAddress(requireContext(), last.getLatitude(), last.getLongitude());
                if (isAdded()) requireActivity().runOnUiThread(() -> {
                    isAutoFilling = true;
                    destinationInput.setText(addr);
                    destinationInput.dismissDropDown();
                    isAutoFilling = false;
                });
            }).start();
        }
    }

    public void setPickupFromMap(double lat, double lng) {
        if (!isAdded() || pickupInput == null) return;
        new Thread(() -> {
            String address = GeocodingHelper.getAddress(requireContext(), lat, lng);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                isAutoFilling = true;
                pickupInput.setText(address);
                pickupInput.dismissDropDown();
                isAutoFilling = false;
            });
        }).start();
    }

    private void estimateRide() {
        String pickup = pickupInput.getText() != null ? pickupInput.getText().toString().trim() : "";
        String destination = destinationInput.getText() != null ? destinationInput.getText().toString().trim() : "";

        if (pickup.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter pickup location", Toast.LENGTH_SHORT).show();
            return;
        }
        if (destination.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter destination", Toast.LENGTH_SHORT).show();
            return;
        }
        if (MapFragment.selectedPoints.size() < 2) {
            Toast.makeText(requireContext(), "Select route on map first", Toast.LENGTH_SHORT).show();
            return;
        }

        ORSRepository.getRouteWithDetails(
                MapFragment.selectedPoints,
                (distanceKm, durationMinutes) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (distanceKm == 0) {
                            Toast.makeText(requireContext(), "Could not calculate route", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Prilagodi formulu svojoj ceni
                        double price = distanceKm * 100;
                        showEstimateResult(durationMinutes, price);
                    });
                }
        );
    }
    @SuppressLint("DefaultLocale")
    private void showEstimateResult(int durationMinutes, double price) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Price estimate")
                .setMessage(
                        "Estimated time: " + durationMinutes + " min\n\n" +
                                "Estimated price: " + String.format("%.2f", price) + " RSD\n\n" +
                                "To book a ride, please register or log in."
                )
                .setPositiveButton("OK", null)
                .show();
    }
    private void setupLiveAutocomplete(AutoCompleteTextView field, MapFragment.PointType type) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isAutoFilling) return;
                String query = s.toString();
                if (query.length() < 3) return;
                ORSRepository.searchPlaces(query, places -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                places
                        );
                        field.setAdapter(adapter);
                        field.showDropDown();
                    });
                });
            }
        });

        field.setOnItemClickListener((parent, view, position, id) -> {
            String item = (String) parent.getItemAtPosition(position);
            String[] parts = item.split("\\|");
            if (parts.length < 3) return;

            String label = parts[0];
            double lat = Double.parseDouble(parts[1]);
            double lon = Double.parseDouble(parts[2]);

            isAutoFilling = true;
            field.setText(label);
            field.setSelection(label.length());
            field.dismissDropDown();
            field.clearFocus();

            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(field.getWindowToken(), 0);
            isAutoFilling = false;

            if (MapFragment.instance != null) {
                MapFragment.instance.addTypedPoint(new GeoPoint(lat, lon), type);
            }
        });
    }

    private Runnable onDismissCallback;

    public void setOnDismissCallback(Runnable callback) {
        this.onDismissCallback = callback;
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (onDismissCallback != null) onDismissCallback.run();
    }

    @Override
    public void onStart() {
        super.onStart();
        View view = getView();
        if (view == null) return;
        View parent = (View) view.getParent();
        var behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(parent);
        behavior.setPeekHeight(400);
        behavior.setFitToContents(true);
        behavior.setSkipCollapsed(false);
        behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
    }
}