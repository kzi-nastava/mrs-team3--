package com.example.uber3;

import android.annotation.SuppressLint;
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
import java.util.Objects;

public class RegisterFragment extends Fragment {

    private static final String STATE_EMAIL = "email";
    private static final String STATE_FIRST_NAME = "first_name";
    private static final String STATE_LAST_NAME = "last_name";
    private static final String STATE_ADDRESS = "address";
    private static final String STATE_PHONE = "phone";
    private static final String STATE_PASSWORD = "password";
    private static final String STATE_CONFIRM_PASSWORD = "confirm_password";
    private static final String STATE_BASE64_IMAGE = "base64_image";
    private static final String STATE_IMAGE_VISIBLE = "image_visible";

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

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }

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

        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String firstName = Objects.requireNonNull(etFirstName.getText()).toString().trim();
        String lastName = Objects.requireNonNull(etLastName.getText()).toString().trim();
        String address = Objects.requireNonNull(etAddress.getText()).toString().trim();
        String phone = Objects.requireNonNull(etPhone.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString();
        String confirmPassword = Objects.requireNonNull(etConfirmPassword.getText()).toString();

        if (!checkPasswords(password, confirmPassword)){
            return;
        }

        performRegistration(email, firstName, lastName, address, phone, password);
    }

    @SuppressLint("SetTextI18n")
    private boolean checkPasswords(String password, String confirmPassword){
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(STATE_EMAIL, Objects.requireNonNull(etEmail.getText()).toString());
        outState.putString(STATE_FIRST_NAME, Objects.requireNonNull(etFirstName.getText()).toString());
        outState.putString(STATE_LAST_NAME, Objects.requireNonNull(etLastName.getText()).toString());
        outState.putString(STATE_ADDRESS, Objects.requireNonNull(etAddress.getText()).toString());
        outState.putString(STATE_PHONE, Objects.requireNonNull(etPhone.getText()).toString());
        outState.putString(STATE_PASSWORD, Objects.requireNonNull(etPassword.getText()).toString());
        outState.putString(STATE_CONFIRM_PASSWORD, Objects.requireNonNull(etConfirmPassword.getText()).toString());

        outState.putString(STATE_BASE64_IMAGE, base64Image);
        outState.putBoolean(STATE_IMAGE_VISIBLE, ivImagePreview.getVisibility() == View.VISIBLE);
    }

    private void restoreState(Bundle savedState) {
        String email = savedState.getString(STATE_EMAIL);
        String firstName = savedState.getString(STATE_FIRST_NAME);
        String lastName = savedState.getString(STATE_LAST_NAME);
        String address = savedState.getString(STATE_ADDRESS);
        String phone = savedState.getString(STATE_PHONE);
        String password = savedState.getString(STATE_PASSWORD);
        String confirmPassword = savedState.getString(STATE_CONFIRM_PASSWORD);

        if (email != null) etEmail.setText(email);
        if (firstName != null) etFirstName.setText(firstName);
        if (lastName != null) etLastName.setText(lastName);
        if (address != null) etAddress.setText(address);
        if (phone != null) etPhone.setText(phone);
        if (password != null) etPassword.setText(password);
        if (confirmPassword != null) etConfirmPassword.setText(confirmPassword);

        base64Image = savedState.getString(STATE_BASE64_IMAGE);
        boolean imageVisible = savedState.getBoolean(STATE_IMAGE_VISIBLE, false);

        if (imageVisible && base64Image != null) {
            try {
                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                ivImagePreview.setImageBitmap(bitmap);
                ivImagePreview.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
