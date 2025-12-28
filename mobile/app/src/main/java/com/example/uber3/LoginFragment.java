package com.example.uber3;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginFragment extends Fragment {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvForgotPassword, tvCreateAccount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword);
        tvCreateAccount = view.findViewById(R.id.tvCreateAccount);

        setupListeners();
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> onLoginClick());

        tvForgotPassword.setOnClickListener(v -> {
            // TODO: Navigate to forgot password screen
            Toast.makeText(requireContext(), "Forgot password clicked", Toast.LENGTH_SHORT).show();
        });

        tvCreateAccount.setOnClickListener(v -> {
            // Navigate to RegisterFragment
            if (getActivity() != null) {
                ((MainActivity) getActivity()).loadRegisterFragment();
            }
        });
    }

    private void onLoginClick() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Validation
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        // TODO: Implement actual login logic with API call
        performLogin(email, password);
    }

    private void performLogin(String email, String password) {
        // Mock login - replace with actual API call
        Toast.makeText(requireContext(), "Logging in...", Toast.LENGTH_SHORT).show();

        // Simulate successful login
        // After successful login, navigate to MainActivity
        Intent intent = new Intent(requireActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}