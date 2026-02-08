package com.example.uber3;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import androidx.fragment.app.Fragment;
import android.widget.Toast;

import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.DriverApi;
import com.example.uber3.network.model.register.RegisterDriverRequest;
import com.example.uber3.network.model.vehicle.VehicleRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

public class RegisterDriverFragment extends Fragment {

    private EditText etFirstName, etLastName, etEmail, etPhone,
            etAddress, etVehicleModel, etPlate, etSeats;

    private Spinner spVehicleType;
    private MaterialCheckBox cbBaby, cbPet;
    private MaterialButton btnRegister;

    public RegisterDriverFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_register_driver, container, false);

        initViews(view);
        setupVehicleTypeSpinner();
        setupButton();

        return view;
    }

    private void initViews(View v) {
        etFirstName = v.findViewById(R.id.etFirstName);
        etLastName = v.findViewById(R.id.etLastName);
        etEmail = v.findViewById(R.id.etEmail);
        etPhone = v.findViewById(R.id.etPhone);
        etAddress = v.findViewById(R.id.etAddress);
        etVehicleModel = v.findViewById(R.id.etVehicleModel);
        etPlate = v.findViewById(R.id.etPlate);
        etSeats = v.findViewById(R.id.etSeats);

        spVehicleType = v.findViewById(R.id.spVehicleType);

        cbBaby = v.findViewById(R.id.cbBaby);
        cbPet = v.findViewById(R.id.cbPet);

        btnRegister = v.findViewById(R.id.btnRegister);
    }

    private void setupButton() {
        btnRegister.setOnClickListener(v -> submit());
    }

    private void submit() {

        VehicleRequest vehicle = new VehicleRequest(
                etVehicleModel.getText().toString(),
                spVehicleType.getSelectedItem().toString(),
                etPlate.getText().toString(),
                Integer.parseInt(etSeats.getText().toString()),
                cbBaby.isChecked(),
                cbPet.isChecked()
        );

        RegisterDriverRequest req =
                new RegisterDriverRequest(
                        etEmail.getText().toString(),
                        "temp123",
                        etFirstName.getText().toString(),
                        etLastName.getText().toString(),
                        etPhone.getText().toString(),
                        etAddress.getText().toString(),
                        vehicle
                );

        DriverApi api =
                ApiClient
                        .getClient(requireContext())
                        .create(DriverApi.class);

        api.registerDriver(req).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(),
                            "Driver created! Email sent 📧",
                            Toast.LENGTH_LONG).show();
                    clearForm();
                } else {
                    Toast.makeText(getContext(),
                            "Error: " + response.code(),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(getContext(),
                        "Network error",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupVehicleTypeSpinner() {
        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(
                        requireContext(),
                        R.array.vehicle_types,
                        android.R.layout.simple_spinner_item
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        spVehicleType.setAdapter(adapter);
    }

    private void clearForm() {

        etFirstName.setText("");
        etLastName.setText("");
        etEmail.setText("");
        etPhone.setText("");
        etAddress.setText("");
        etVehicleModel.setText("");
        etPlate.setText("");
        etSeats.setText("");

        spVehicleType.setSelection(0);

        cbBaby.setChecked(false);
        cbPet.setChecked(false);
    }


}
