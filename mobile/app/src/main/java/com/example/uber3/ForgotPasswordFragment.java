package com.example.uber3;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class ForgotPasswordFragment extends Fragment {

    private TextInputEditText etEmail;
    private MaterialButton btnSubmit;
    private android.widget.TextView tvBackToLogin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etEmail = view.findViewById(R.id.etEmail);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        tvBackToLogin = view.findViewById(R.id.tvBackToLogin);

        setupListeners();
    }
    private void setupListeners() {
        btnSubmit.setOnClickListener(v -> onSubmitClick());

        tvBackToLogin.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((MainActivity) getActivity()).loadLoginFragment();
            }
        });
    }
    private void onSubmitClick() {
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        performForgotPassword(email);
    }

    private void performForgotPassword(String email) {
        Toast.makeText(requireContext(), "Password reset link sent to " + email, Toast.LENGTH_LONG).show();

        if (getActivity() != null) {
            ((MainActivity) getActivity()).loadLoginFragment();
        }
    }
}