package com.example.uber3.adapter;

import android.annotation.SuppressLint;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.R;
import com.example.uber3.network.model.user.admin.ActiveDriverDto;

import java.util.List;

public class ActiveDriversAdapter extends RecyclerView.Adapter<ActiveDriversAdapter.VH> {

    private final List<ActiveDriverDto> data;

    public ActiveDriversAdapter(List<ActiveDriverDto> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_active_driver, parent, false);
        return new VH(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ActiveDriverDto d = data.get(position);
        h.tvName.setText(d.name + " " + d.surname);
        h.tvEmail.setText(d.email);

        h.tvStatus.setText(d.blocked ? "BLOCKED" : "ACTIVE");
        h.itemView.setAlpha(d.blocked ? 0.6f : 1.0f);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }

}
