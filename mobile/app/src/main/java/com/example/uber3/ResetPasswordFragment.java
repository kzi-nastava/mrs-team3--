package com.example.uber3;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.example.uber3.network.api.ApiService;
import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.model.mails.ForgotPasswordRequest;
import com.example.uber3.network.model.mails.ResetPasswordRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ResetPasswordFragment extends Fragment {

    private static final String ARG_TOKEN = "token";
    private static final String ARG_MODE = "mode";


    private String token;

    private String mode;

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private TextInputLayout confirmPasswordLayout;

    private MaterialButton btnAction;

    private ApiService apiService;

    public ResetPasswordFragment() {}

    public static ResetPasswordFragment newInstance(String token, String mode) {
        ResetPasswordFragment fragment = new ResetPasswordFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TOKEN, token);
        args.putString(ARG_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            token = getArguments().getString(ARG_TOKEN);
            mode = getArguments().getString(ARG_MODE, "RESET");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_reset_password, container, false);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvDesc = view.findViewById(R.id.tvDesc);

        if ("ACTIVATE".equals(mode)) {
            tvTitle.setText("Create Password");
            tvDesc.setText("Set your password to activate account");
        }


        apiService = ApiClient
                .getClient(requireContext())
                .create(ApiService.class);

        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordLayout);

        btnAction = view.findViewById(R.id.btnResetPassword);


        TextInputLayout passwordLayout =
                view.findViewById(R.id.passwordLayout);

        if (token != null) {
            etEmail.setVisibility(View.GONE);
            view.findViewById(R.id.passwordLayout).setVisibility(View.VISIBLE);
            confirmPasswordLayout.setVisibility(View.VISIBLE);
            btnAction.setText("Set New Password");
        }

        else {
            etPassword.setVisibility(View.GONE);
            btnAction.setText("Send Reset Link");
        }

        btnAction.setOnClickListener(v -> handleAction());

        return view;
    }

    private void handleAction() {
        if (token == null) {
            sendResetLink();
        } else {
            setNewPassword();
        }
    }

    private void sendResetLink() {
        String email = etEmail.getText().toString().trim();

        if (!isValidEmail(email)) {
            etEmail.setError("Enter valid email");
            return;
        }


        apiService.forgotPassword(
                        new ForgotPasswordRequest(email))
                .enqueue(new Callback<Void>() {

                    @Override
                    public void onResponse(Call<Void> call,
                                           Response<Void> response) {

                        Toast.makeText(getContext(),
                                "Reset email sent!",
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(Call<Void> call,
                                          Throwable t) {

                        Toast.makeText(getContext(),
                                "Network error",
                                Toast.LENGTH_LONG).show();
                    }
                });

    }

    private void setNewPassword() {

        String pass = etPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();

        if (pass.length() < 6) {
            etPassword.setError("Min 6 characters");
            return;
        }

        if (!pass.equals(confirm)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        apiService.resetPassword(
                        new ResetPasswordRequest(token, pass))
                .enqueue(new Callback<Void>() {

                    @Override
                    public void onResponse(Call<Void> call,
                                           Response<Void> response) {

                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(),
                                    "Password changed! Please login.",
                                    Toast.LENGTH_LONG).show();

                            requireActivity()
                                    .getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragmentContainer, new LoginFragment())
                                    .commit();

                        } else {
                            Toast.makeText(getContext(),
                                    "Reset failed",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call,
                                          Throwable t) {

                        Toast.makeText(getContext(),
                                "Network error",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email)
                && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
