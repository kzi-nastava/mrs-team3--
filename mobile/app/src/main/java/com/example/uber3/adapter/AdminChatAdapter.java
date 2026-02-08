package com.example.uber3.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.R;
import com.example.uber3.network.model.chat.AdminChatRoom;

import java.util.List;

public class AdminChatAdapter
        extends RecyclerView.Adapter<AdminChatAdapter.VH>{

    public interface OnClick{
        void onClick(AdminChatRoom room);
    }

    private final List<AdminChatRoom> rooms;
    private final OnClick listener;

    public AdminChatAdapter(List<AdminChatRoom> rooms, OnClick listener){
        this.rooms = rooms;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int v){
        View view = LayoutInflater.from(p.getContext())
                .inflate(android.R.layout.simple_list_item_1, p, false);

        return new VH(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull VH h, int pos){
        AdminChatRoom r = rooms.get(pos);
        h.txt.setText(r.userName);
        h.itemView.setOnClickListener(v -> listener.onClick(r));
    }

    @Override
    public int getItemCount(){ return rooms.size(); }

    static class VH extends RecyclerView.ViewHolder{
        TextView txt;
        VH(View v){
            super(v);
            txt = v.findViewById(android.R.id.text1);
        }
    }
}