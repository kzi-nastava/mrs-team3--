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

import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.ApiService;
import com.example.uber3.network.model.mails.ForgotPasswordRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        performForgotPassword(email);
    }

    private void performForgotPassword(String email) {
        btnSubmit.setEnabled(false);

        ApiService apiService = ApiClient.getClient(requireContext()).create(ApiService.class);

        apiService.forgotPassword(new ForgotPasswordRequest(email))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        btnSubmit.setEnabled(true);

                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(),
                                    "Password reset link sent to " + email + ". Please check your email.",
                                    Toast.LENGTH_LONG).show();

                            if (getActivity() != null) {
                                ((MainActivity) getActivity()).loadLoginFragment();
                            }
                        } else {
                            Toast.makeText(requireContext(),
                                    "Failed to send reset link. Please try again.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        btnSubmit.setEnabled(true);

                        Toast.makeText(requireContext(),
                                "Network error: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}