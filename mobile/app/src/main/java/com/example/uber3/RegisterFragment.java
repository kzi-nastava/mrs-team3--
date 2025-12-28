package com.example.uber3;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;

public class RegisterFragment extends Fragment {

    private TextInputEditText etEmail, etFirstName, etLastName, etAddress, etPhone, etPassword, etConfirmPassword;
    private MaterialButton btnChooseImage, btnRegister;
    private ImageView ivImagePreview;
    private TextView tvPasswordError, tvLoginLink;

    private String base64Image = null;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupImagePicker();
        setupListeners();
    }

    private void initializeViews(View view) {
        etEmail = view.findViewById(R.id.etEmail);
        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName = view.findViewById(R.id.etLastName);
        etAddress = view.findViewById(R.id.etAddress);
        etPhone = view.findViewById(R.id.etPhone);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);

        btnChooseImage = view.findViewById(R.id.btnChooseImage);
        btnRegister = view.findViewById(R.id.btnRegister);
        ivImagePreview = view.findViewById(R.id.ivImagePreview);
        tvPasswordError = view.findViewById(R.id.tvPasswordError);
        tvLoginLink = view.findViewById(R.id.tvLoginLink);
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleImageSelection(uri);
                    }
                }
        );
    }

    private void setupListeners() {
        btnChooseImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnRegister.setOnClickListener(v -> onRegisterClick());

        tvLoginLink.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((MainActivity) getActivity()).loadLoginFragment();
            }
        });
    }

    private void handleImageSelection(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                    requireActivity().getContentResolver(),
                    imageUri
            );

            ivImagePreview.setImageBitmap(bitmap);
            ivImagePreview.setVisibility(View.VISIBLE);

            base64Image = bitmapToBase64(bitmap);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void onRegisterClick() {
        tvPasswordError.setVisibility(View.GONE);

        String email = etEmail.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (!validateInputs(email, firstName, lastName, address, phone, password, confirmPassword)) {
            return;
        }

        performRegistration(email, firstName, lastName, address, phone, password);
    }

    private boolean validateInputs(String email, String firstName, String lastName,
                                   String address, String phone, String password, String confirmPassword) {

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("First name is required");
            etFirstName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(lastName)) {
            etLastName.setError("Last name is required");
            etLastName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(address)) {
            etAddress.setError("Address is required");
            etAddress.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone number is required");
            etPhone.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Please confirm your password");
            etConfirmPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            tvPasswordError.setText("Passwords do not match");
            tvPasswordError.setVisibility(View.VISIBLE);
            return false;
        }

        return true;
    }

    private void performRegistration(String email, String firstName, String lastName,
                                     String address, String phone, String password) {
        Toast.makeText(requireContext(), "Registering...", Toast.LENGTH_SHORT).show();

        Toast.makeText(requireContext(),
                "Registration successful! Please check your email to activate your account.",
                Toast.LENGTH_LONG).show();

        if (getActivity() != null) {
            ((MainActivity) getActivity()).loadLoginFragment();
        }
    }
}