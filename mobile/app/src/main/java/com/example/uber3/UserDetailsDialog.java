package com.example.uber3;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;

import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.ApiService;
import com.example.uber3.network.model.user.admin.AdminUserDetailsDto;
import com.example.uber3.network.model.user.BlockUserRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserDetailsDialog {

    public interface OnBlockChanged {
        void onChanged();
    }

    public static void show(
            @NonNull Context context,
            @NonNull AdminUserDetailsDto user,
            @NonNull OnBlockChanged onBlockChanged
    ) {
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_user_details, null);

        TextView tvName = v.findViewById(R.id.tvName);
        TextView tvEmail = v.findViewById(R.id.tvEmail);
        TextView tvPhone = v.findViewById(R.id.tvPhone);
        TextView tvAddress = v.findViewById(R.id.tvAddress);
        TextView tvRole = v.findViewById(R.id.tvRole);
        TextView tvStatus = v.findViewById(R.id.tvStatus);

        Button btnBlock = v.findViewById(R.id.btnBlock);
        ProgressBar pbBlock = v.findViewById(R.id.pbBlock);

        tvName.setText(user.name + " " + user.surname);
        tvEmail.setText("Email: " + safe(user.email));

        if (user.phoneNumber != null && !user.phoneNumber.isBlank()) {
            tvPhone.setVisibility(View.VISIBLE);
            tvPhone.setText("Phone: " + user.phoneNumber);
        }

        if (user.address != null && !user.address.isBlank()) {
            tvAddress.setVisibility(View.VISIBLE);
            tvAddress.setText("Address: " + user.address);
        }

        tvRole.setText("Role: " + safe(user.role));
        updateStatusText(tvStatus, user);

        updateBlockButtonText(btnBlock, user);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("User details")
                .setView(v)
                .setNegativeButton("Close", (d, w) -> d.dismiss())
                .create();

        btnBlock.setOnClickListener(click -> {
            if (pbBlock.getVisibility() == View.VISIBLE) return;

            if (user.blocked) {
                doBlockCall(context, user, false, null, pbBlock, btnBlock, tvStatus, onBlockChanged);
            } else {
                showReasonDialog(context, reason -> {
                    if (reason == null || reason.trim().isEmpty()) {
                        Toast.makeText(context, "Reason required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    doBlockCall(context, user, true, reason.trim(), pbBlock, btnBlock, tvStatus, onBlockChanged);
                });
            }
        });

        dialog.show();
    }

    private interface ReasonCallback {
        void onReason(String reason);
    }

    private static void showReasonDialog(@NonNull Context context, @NonNull ReasonCallback cb) {
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_block_reason, null);
        EditText etReason = v.findViewById(R.id.etReason);

        new AlertDialog.Builder(context)
                .setTitle("Block user")
                .setView(v)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Confirm", (d, w) -> cb.onReason(etReason.getText().toString()))
                .show();
    }

    private static void doBlockCall(
            @NonNull Context context,
            @NonNull AdminUserDetailsDto user,
            boolean blocked,
            String reason,
            @NonNull ProgressBar pb,
            @NonNull Button btnBlock,
            @NonNull TextView tvStatus,
            @NonNull OnBlockChanged onBlockChanged
    ) {
        pb.setVisibility(View.VISIBLE);
        btnBlock.setEnabled(false);

        ApiService api = ApiClient.getClient(context).create(ApiService.class);

        BlockUserRequest req = new BlockUserRequest(blocked, reason);

        api.blockUser(user.id, req).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> res) {
                pb.setVisibility(View.GONE);
                btnBlock.setEnabled(true);

                if (!res.isSuccessful()) {
                    Toast.makeText(context, "Operation failed (" + res.code() + ")", Toast.LENGTH_SHORT).show();
                    return;
                }

                user.blocked = blocked;
                user.blockReason = blocked ? reason : null;

                updateStatusText(tvStatus, user);
                updateBlockButtonText(btnBlock, user);

                Toast.makeText(context, blocked ? "User blocked" : "User unblocked", Toast.LENGTH_SHORT).show();

                onBlockChanged.onChanged();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                pb.setVisibility(View.GONE);
                btnBlock.setEnabled(true);
                Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static void updateBlockButtonText(@NonNull Button btn, @NonNull AdminUserDetailsDto user) {
        btn.setText(user.blocked ? "Unblock user" : "Block user");
    }

    private static void updateStatusText(@NonNull TextView tv, @NonNull AdminUserDetailsDto user) {
        if (user.blocked) {
            String reason = (user.blockReason != null && !user.blockReason.isBlank())
                    ? user.blockReason
                    : "No reason";
            tv.setText("Status: BLOCKED\nReason: " + reason);
        } else {
            tv.setText("Status: ACTIVE");
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
