package com.example.uber3;

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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RideBookingFragment extends Fragment {

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

        return view;
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
        if (pickupInput != null) {
            pickupInput.setText("");
        }
        if (destinationInput != null) {
            destinationInput.setText("");
        }

        stopsContainer.removeAllViews();
        stopCount = 0;

        priceInfoLayout.setVisibility(View.GONE);

        Toast.makeText(requireContext(), "Form reset", Toast.LENGTH_SHORT).show();
    }
}