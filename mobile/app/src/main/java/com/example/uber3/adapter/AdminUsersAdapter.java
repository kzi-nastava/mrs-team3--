package com.example.uber3.adapter;

import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.R;
import com.example.uber3.network.model.user.admin.AdminUserDetailsDto;

import java.util.List;

public class AdminUsersAdapter extends RecyclerView.Adapter<AdminUsersAdapter.VH> {

    public interface OnUserClick {
        void onClick(AdminUserDetailsDto user);
    }

    private final List<AdminUserDetailsDto> data;
    private final OnUserClick onUserClick;

    public AdminUsersAdapter(List<AdminUserDetailsDto> data, OnUserClick onUserClick) {
        this.data = data;
        this.onUserClick = onUserClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(   R.layout.item_admin_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AdminUserDetailsDto u = data.get(position);
        h.tvName.setText(u.name + " " + u.surname);
        h.tvEmail.setText(u.email);

        h.itemView.setOnClickListener(v -> onUserClick.onClick(u));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
        }
    }
}
