package com.example.uber3;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class VerificationResultFragment extends Fragment {

    private static final String ARG_STATUS = "status";

    public static VerificationResultFragment newInstance(String status){
        VerificationResultFragment f = new VerificationResultFragment();
        Bundle b = new Bundle();
        b.putString(ARG_STATUS, status);
        f.setArguments(b);
        return f;
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(
                R.layout.fragment_verification_result,
                container,
                false
        );

        TextView tvTitle = v.findViewById(R.id.tvTitle);
        TextView tvMessage = v.findViewById(R.id.tvMessage);
        MaterialButton btnLogin = v.findViewById(R.id.btnLogin);

        String status = getArguments() != null
                ? getArguments().getString(ARG_STATUS, "success")
                : "success";

        switch (status){

            case "expired":
                tvTitle.setText("Link Expired ⏰");
                tvMessage.setText("This verification link has expired.");
                break;

            case "used":
                tvTitle.setText("Already Used ✅");
                tvMessage.setText("This link has already been used.");
                break;

            case "invalid":
                tvTitle.setText("Invalid Link ❌");
                tvMessage.setText("This verification link is invalid.");
                break;

            default:
                tvTitle.setText("Success 🎉");
                tvMessage.setText("Operation completed successfully. You can now log in.");
        }

        btnLogin.setOnClickListener(v1 -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer,
                            new LoginFragment())
                    .commit();
        });

        return v;
    }
}
