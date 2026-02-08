package com.example.uber3.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.R;
import com.example.uber3.network.model.chat.ChatMessage;

import java.util.List;
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ME = 1;
    private static final int TYPE_OTHER = 2;

    private final List<ChatMessage> messages;
    private final Long myUserId;

    public ChatAdapter(List<ChatMessage> messages, Long myUserId) {
        this.messages = messages;
        this.myUserId = myUserId;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).fromUserId.equals(myUserId)
                ? TYPE_ME
                : TYPE_OTHER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if(viewType == TYPE_ME){
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_me, parent, false);
            return new MeHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_other, parent, false);
            return new OtherHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        ChatMessage msg = messages.get(position);

        if(holder instanceof MeHolder){
            ((MeHolder)holder).txt.setText(msg.content);
        } else {
            ((OtherHolder)holder).txt.setText(msg.content);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class MeHolder extends RecyclerView.ViewHolder{
        TextView txt;
        MeHolder(View v){
            super(v);
            txt = v.findViewById(R.id.txtMessage);
        }
    }

    static class OtherHolder extends RecyclerView.ViewHolder{
        TextView txt;
        OtherHolder(View v){
            super(v);
            txt = v.findViewById(R.id.txtMessage);
        }
    }
}
