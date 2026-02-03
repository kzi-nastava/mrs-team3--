package com.example.uber3;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.network.model.AdminDriverProfileChangeRequestDto;

import java.util.List;

public class ChangeRequestAdapter
        extends RecyclerView.Adapter<ChangeRequestAdapter.VH> {

    public interface OnRequestClick {
        void onClick(AdminDriverProfileChangeRequestDto req);
    }

    private List<AdminDriverProfileChangeRequestDto> list;
    private OnRequestClick listener;

    public ChangeRequestAdapter(
            List<AdminDriverProfileChangeRequestDto> list,
            OnRequestClick listener
    ) {
        this.list = list;
        this.listener = listener;
    }

    class VH extends RecyclerView.ViewHolder {

        TextView tvRequestId, tvStatus,
                tvDriver, tvEmail, tvDate;

        public VH(View v) {
            super(v);

            tvRequestId = v.findViewById(R.id.tvRequestId);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvDriver = v.findViewById(R.id.tvDriver);
            tvEmail = v.findViewById(R.id.tvEmail);
            tvDate = v.findViewById(R.id.tvDate);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_change_request,
                        parent,
                        false
                );

        return new VH(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(
            @NonNull VH h,
            int i
    ) {

        var r = list.get(i);

        h.tvRequestId.setText("Request #" + r.requestId);
        h.tvStatus.setText(r.status);
        h.tvStatus.setPadding(20,8,20,8);

        switch (r.status.toUpperCase()) {

            case "PENDING":
                h.tvStatus.setBackgroundResource(R.drawable.status_pending);
                h.tvStatus.setTextColor(
                        android.graphics.Color.parseColor("#92400e"));
                break;

            case "APPROVED":
                h.tvStatus.setBackgroundResource(R.drawable.status_approved);
                h.tvStatus.setTextColor(
                        android.graphics.Color.parseColor("#166534"));
                break;

            case "REJECTED":
                h.tvStatus.setBackgroundResource(R.drawable.status_rejected);
                h.tvStatus.setTextColor(
                        android.graphics.Color.parseColor("#991b1b"));
                break;
        }

        h.tvDriver.setText(r.driverName + " " + r.driverSurname);
        h.tvEmail.setText(r.driverEmail);
        h.tvDate.setText("Requested: " + r.requestedAt);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(r);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public AdminDriverProfileChangeRequestDto getItem(int pos) {
        return list.get(pos);
    }

}
