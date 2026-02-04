package com.example.uber3;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.uber3.helpers.GeocodingHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.osmdroid.util.GeoPoint;

public class RideBookingFragment extends BottomSheetDialogFragment {
    private LinearLayout stopsContainer;
    private LinearLayout priceInfoLayout;
    private TextInputEditText pickupInput;
    private TextInputEditText destinationInput;
    private MaterialButton bookRideButton;
    private MaterialButton resetButton;
    private int stopCount = 0;

    public RideBookingFragment() {}

    public static RideBookingFragment newInstance() {
        return new RideBookingFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_ride_booking, container, false);

        stopsContainer = view.findViewById(R.id.stopsContainer);
        pickupInput = view.findViewById(R.id.pickupInput);
        destinationInput = view.findViewById(R.id.destinationInput);
        priceInfoLayout = view.findViewById(R.id.priceInfoLayout);
        bookRideButton = view.findViewById(R.id.bookRideButton);
        resetButton = view.findViewById(R.id.resetButton);

        MaterialButton addStopButton = view.findViewById(R.id.addStopButton);
        addStopButton.setOnClickListener(v -> addStop());

        bookRideButton.setOnClickListener(v -> bookRide());
        resetButton.setOnClickListener(v -> resetForm());

        fillFromMapPoints();

        return view;
    }

    @SuppressLint("SetTextI18n")
    private void fillFromMapPoints() {

        if (MapFragment.selectedPoints.isEmpty()) return;

        // ---------- PICKUP ----------
        GeoPoint first = MapFragment.selectedPoints.get(0);

        new Thread(() -> {

            String addr = GeocodingHelper.getAddress(
                    requireContext(),
                    first.getLatitude(),
                    first.getLongitude()
            );

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (pickupInput != null) {
                        pickupInput.setText(addr);
                    }
                });
            }


        }).start();


        // ---------- DESTINATION ----------
        if (MapFragment.selectedPoints.size() > 1) {

            GeoPoint last = MapFragment.selectedPoints.get(
                    MapFragment.selectedPoints.size() - 1
            );

            new Thread(() -> {

                String addr = GeocodingHelper.getAddress(
                        requireContext(),
                        last.getLatitude(),
                        last.getLongitude()
                );

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (destinationInput != null) {
                            destinationInput.setText(addr);
                        }
                    });
                }


            }).start();
        }


        // ---------- STOPS ----------
        for (int i = 1; i < MapFragment.selectedPoints.size() - 1; i++) {

            GeoPoint p = MapFragment.selectedPoints.get(i);

            addStop();

            LinearLayout row =
                    (LinearLayout) stopsContainer.getChildAt(
                            stopsContainer.getChildCount() - 1
                    );

            TextInputLayout til = (TextInputLayout) row.getChildAt(0);
            TextInputEditText et =
                    (TextInputEditText) til.getEditText();

            if (et == null) continue;

            new Thread(() -> {

                String addr = GeocodingHelper.getAddress(
                        requireContext(),
                        p.getLatitude(),
                        p.getLongitude()
                );

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        et.setText(addr);
                    });
                }


            }).start();
        }
    }


    private void addStop() {
        stopCount++;

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        row.setPadding(0, 8, 0, 8);

        TextInputLayout inputLayout = new TextInputLayout(requireContext());
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        inputLayout.setLayoutParams(inputParams);
        inputLayout.setHint("Stop " + stopCount);
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        inputLayout.setBoxCornerRadii(12, 12, 12, 12);

        TextInputEditText editText = new TextInputEditText(requireContext());
        inputLayout.addView(editText);

        ImageButton removeBtn = new ImageButton(requireContext());
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(8, 0, 0, 0);
        removeBtn.setLayoutParams(btnParams);
        removeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        removeBtn.setBackground(null);

        removeBtn.setOnClickListener(v -> {
            stopsContainer.removeView(row);
            stopCount--;
        });

        row.addView(inputLayout);
        row.addView(removeBtn);

        stopsContainer.addView(row);
    }

    private void bookRide() {
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

        for (int i = 0; i < stopsContainer.getChildCount(); i++) {
            View child = stopsContainer.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                TextInputLayout inputLayout = (TextInputLayout) row.getChildAt(0);
                TextInputEditText editText = (TextInputEditText) inputLayout.getEditText();
                if (editText != null) {
                    String stopText = editText.getText() != null ? editText.getText().toString().trim() : "";
                    if (stopText.isEmpty()) {
                        Toast.makeText(requireContext(), "Please fill all stops", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
        }

        priceInfoLayout.setVisibility(View.VISIBLE);
        Toast.makeText(requireContext(), "Ride booked successfully!", Toast.LENGTH_SHORT).show();
    }

    private void resetForm() {

        pickupInput.setText("");
        destinationInput.setText("");

        stopsContainer.removeAllViews();
        stopCount = 0;

        priceInfoLayout.setVisibility(View.GONE);

        if (MapFragment.instance != null) {
            MapFragment.instance.clearMap();
        }

        Toast.makeText(requireContext(),
                "Form reset",
                Toast.LENGTH_SHORT).show();
    }



    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view == null) return;

        View parent = (View) view.getParent();

        com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior =
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(parent);

        behavior.setPeekHeight(600);

        // OMOGUĆAVA full expand
        behavior.setFitToContents(true);

        // DOZVOLJAVA više stanja
        behavior.setSkipCollapsed(false);

        // početno stanje
        behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
    }


    private Runnable onDismissCallback;

    public void setOnDismissCallback(Runnable callback) {
        this.onDismissCallback = callback;
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);

        if (onDismissCallback != null) {
            onDismissCallback.run();
        }
    }

    @SuppressLint("SetTextI18n")
    public void setPickupFromMap(double lat, double lng) {

        if (!isAdded() || pickupInput == null) return;

        pickupInput.setText("Loading address...");

        // uzmi context dok je fragment živ
        final android.content.Context ctx = getContext();

        new Thread(() -> {

            if (ctx == null) return;

            String address =
                    GeocodingHelper.getAddress(
                            ctx,
                            lat,
                            lng
                    );

            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                if (pickupInput != null) {
                    pickupInput.setText(address);
                }
            });

        }).start();
    }





}