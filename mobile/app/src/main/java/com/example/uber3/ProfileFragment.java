package com.example.uber3;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class ProfileFragment extends Fragment {

    private static final String ARG_ROLE = "role";
    private static final String STATE_EDIT_MODE = "edit_mode";
    private static final String STATE_FIRST_NAME = "first_name";
    private static final String STATE_LAST_NAME = "last_name";
    private static final String STATE_EMAIL = "email";
    private static final String STATE_PHONE = "phone";
    private static final String STATE_ADDRESS = "address";
    private static final String STATE_VEHICLE_MODEL = "vehicle_model";
    private static final String STATE_LICENSE_PLATE = "license_plate";
    private static final String STATE_SEATS = "seats";
    private static final String STATE_CHANGE_VISIBLE = "change_visible";
    private static final String STATE_CHANGE_FIELD = "change_field";
    private static final String STATE_CHANGE_VALUES = "change_values";
    private static final String STATE_CHANGE_STATUS = "change_status";

    private boolean editMode = false;
    private boolean isDriver = false;


    private MaterialButton btnEdit;
    private TextView tvFirstName, tvLastName, tvEmail, tvPhone, tvAddress;
    private EditText etFirstName, etLastName, etEmail, etPhone, etAddress;
    private TextView tvVehicleModel, tvLicensePlate, tvSeats;
    private EditText etVehicleModel, etLicensePlate, etSeats;
    private View changeRequestCard;
    private TextView tvChangeField, tvChangeValues, tvChangeStatus;

    public static ProfileFragment newInstance(String role) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROLE, role);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);

        String role = getArguments() != null ? getArguments().getString(ARG_ROLE) : "PASSENGER";
        isDriver = "DRIVER".equals(role);

        LinearLayout driverSection = view.findViewById(R.id.driverSection);
        driverSection.setVisibility(isDriver ? View.VISIBLE : View.GONE);

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }

        setupEditButton();
    }

    private void initializeViews(View view) {
        btnEdit = view.findViewById(R.id.btnEdit);

        tvFirstName = view.findViewById(R.id.tvFirstName);
        etFirstName = view.findViewById(R.id.etFirstName);
        tvLastName = view.findViewById(R.id.tvLastName);
        etLastName = view.findViewById(R.id.etLastName);
        tvEmail = view.findViewById(R.id.tvEmail);
        etEmail = view.findViewById(R.id.etEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        etPhone = view.findViewById(R.id.etPhone);
        tvAddress = view.findViewById(R.id.tvAddress);
        etAddress = view.findViewById(R.id.etAddress);

        tvVehicleModel = view.findViewById(R.id.tvVehicleModel);
        etVehicleModel = view.findViewById(R.id.etVehicleModel);
        tvLicensePlate = view.findViewById(R.id.tvLicensePlate);
        etLicensePlate = view.findViewById(R.id.etLicensePlate);
        tvSeats = view.findViewById(R.id.tvSeats);
        etSeats = view.findViewById(R.id.etSeats);

        changeRequestCard = view.findViewById(R.id.changeRequestCard);
        tvChangeField = view.findViewById(R.id.tvChangeField);
        tvChangeValues = view.findViewById(R.id.tvChangeValues);
        tvChangeStatus = view.findViewById(R.id.tvChangeStatus);
    }

    private void setupEditButton() {
        btnEdit.setOnClickListener(v -> {
            editMode = !editMode;

            if (editMode) {
                enterEditMode();
            } else {
                exitEditMode();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void enterEditMode() {
        btnEdit.setText("💾 Save");

        tvFirstName.setVisibility(View.GONE);
        etFirstName.setVisibility(View.VISIBLE);

        tvLastName.setVisibility(View.GONE);
        etLastName.setVisibility(View.VISIBLE);

        tvEmail.setVisibility(View.GONE);
        etEmail.setVisibility(View.VISIBLE);

        tvPhone.setVisibility(View.GONE);
        etPhone.setVisibility(View.VISIBLE);

        tvAddress.setVisibility(View.GONE);
        etAddress.setVisibility(View.VISIBLE);

        if (isDriver) {
            tvVehicleModel.setVisibility(View.GONE);
            etVehicleModel.setVisibility(View.VISIBLE);

            tvLicensePlate.setVisibility(View.GONE);
            etLicensePlate.setVisibility(View.VISIBLE);

            tvSeats.setVisibility(View.GONE);
            etSeats.setVisibility(View.VISIBLE);
        }

        etFirstName.requestFocus();
    }

    @SuppressLint("SetTextI18n")
    private void exitEditMode() {
        btnEdit.setText("✏️ Edit");

        if (!isDriver) {
            tvFirstName.setText(etFirstName.getText().toString());
            tvLastName.setText(etLastName.getText().toString());
            tvEmail.setText(etEmail.getText().toString());
            tvPhone.setText(etPhone.getText().toString());
            tvAddress.setText(etAddress.getText().toString());

        } else {
            createChangeRequest();
        }

        etFirstName.setVisibility(View.GONE);
        tvFirstName.setVisibility(View.VISIBLE);

        etLastName.setVisibility(View.GONE);
        tvLastName.setVisibility(View.VISIBLE);

        etEmail.setVisibility(View.GONE);
        tvEmail.setVisibility(View.VISIBLE);

        etPhone.setVisibility(View.GONE);
        tvPhone.setVisibility(View.VISIBLE);

        etAddress.setVisibility(View.GONE);
        tvAddress.setVisibility(View.VISIBLE);

        if (isDriver) {
            etVehicleModel.setVisibility(View.GONE);
            tvVehicleModel.setVisibility(View.VISIBLE);

            etLicensePlate.setVisibility(View.GONE);
            tvLicensePlate.setVisibility(View.VISIBLE);

            etSeats.setVisibility(View.GONE);
            tvSeats.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("SetTextI18n")
    private void createChangeRequest() {
        StringBuilder changes = new StringBuilder();
        int changeCount = 0;

        changeCount += addChangeIfDifferent(changes, "First Name", tvFirstName, etFirstName);
        changeCount += addChangeIfDifferent(changes, "Last Name", tvLastName, etLastName);
        changeCount += addChangeIfDifferent(changes, "Email", tvEmail, etEmail);
        changeCount += addChangeIfDifferent(changes, "Phone Number", tvPhone, etPhone);
        changeCount += addChangeIfDifferent(changes, "Address", tvAddress, etAddress);

        if (isDriver) {
            changeCount += addChangeIfDifferent(changes, "Vehicle Model", tvVehicleModel, etVehicleModel);
            changeCount += addChangeIfDifferent(changes, "License Plate", tvLicensePlate, etLicensePlate);
            changeCount += addChangeIfDifferent(changes, "Seats", tvSeats, etSeats);
        }

        if (changeCount > 0) {
            if (changeCount == 1) {
                tvChangeField.setText("Change Request");
            } else {
                tvChangeField.setText(changeCount + " Change Requests");
            }
            tvChangeValues.setText(changes.toString().trim());
            tvChangeStatus.setText("Pending");
            changeRequestCard.setVisibility(View.VISIBLE);
        }
    }

    private int addChangeIfDifferent(StringBuilder builder, String fieldName, TextView tv, EditText et) {
        String oldValue = tv.getText().toString();
        String newValue = et.getText().toString();

        if (!oldValue.equals(newValue)) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(fieldName).append(": ")
                    .append(oldValue).append("  →  ").append(newValue);
            return 1;
        }
        return 0;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(STATE_EDIT_MODE, editMode);

        outState.putString(STATE_FIRST_NAME, etFirstName.getText().toString());
        outState.putString(STATE_LAST_NAME, etLastName.getText().toString());
        outState.putString(STATE_EMAIL, etEmail.getText().toString());
        outState.putString(STATE_PHONE, etPhone.getText().toString());
        outState.putString(STATE_ADDRESS, etAddress.getText().toString());

        if (isDriver) {
            outState.putString(STATE_VEHICLE_MODEL, etVehicleModel.getText().toString());
            outState.putString(STATE_LICENSE_PLATE, etLicensePlate.getText().toString());
            outState.putString(STATE_SEATS, etSeats.getText().toString());
        }

        outState.putBoolean(STATE_CHANGE_VISIBLE, changeRequestCard.getVisibility() == View.VISIBLE);
        outState.putString(STATE_CHANGE_FIELD, tvChangeField.getText().toString());
        outState.putString(STATE_CHANGE_VALUES, tvChangeValues.getText().toString());
        outState.putString(STATE_CHANGE_STATUS, tvChangeStatus.getText().toString());
    }

    @SuppressLint("SetTextI18n")
    private void restoreState(Bundle savedState) {
        editMode = savedState.getBoolean(STATE_EDIT_MODE, false);

        String firstName = savedState.getString(STATE_FIRST_NAME);
        String lastName = savedState.getString(STATE_LAST_NAME);
        String email = savedState.getString(STATE_EMAIL);
        String phone = savedState.getString(STATE_PHONE);
        String address = savedState.getString(STATE_ADDRESS);

        if (firstName != null) etFirstName.setText(firstName);
        if (lastName != null) etLastName.setText(lastName);
        if (email != null) etEmail.setText(email);
        if (phone != null) etPhone.setText(phone);
        if (address != null) etAddress.setText(address);

        if (isDriver) {
            String vehicleModel = savedState.getString(STATE_VEHICLE_MODEL);
            String licensePlate = savedState.getString(STATE_LICENSE_PLATE);
            String seats = savedState.getString(STATE_SEATS);

            if (vehicleModel != null) etVehicleModel.setText(vehicleModel);
            if (licensePlate != null) etLicensePlate.setText(licensePlate);
            if (seats != null) etSeats.setText(seats);
        }

        boolean changeVisible = savedState.getBoolean(STATE_CHANGE_VISIBLE, false);
        if (changeVisible) {
            String changeField = savedState.getString(STATE_CHANGE_FIELD);
            String changeValues = savedState.getString(STATE_CHANGE_VALUES);
            String changeStatus = savedState.getString(STATE_CHANGE_STATUS);

            if (changeField != null) tvChangeField.setText(changeField);
            if (changeValues != null) tvChangeValues.setText(changeValues);
            if (changeStatus != null) tvChangeStatus.setText(changeStatus);

            changeRequestCard.setVisibility(View.VISIBLE);
        }

        if (editMode) {
            btnEdit.setText("💾 Save");
            enterEditMode();
        }
    }
}