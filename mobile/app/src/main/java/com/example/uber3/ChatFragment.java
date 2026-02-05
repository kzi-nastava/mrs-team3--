package com.example.uber3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.uber3.adapter.ChatAdapter;
import com.example.uber3.network.manager.TokenManager;
import com.example.uber3.network.model.chat.ChatMessage;
import com.example.uber3.network.websocket.ChatWebSocketManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private EditText inputMessage;
    private ImageButton btnSend;

    private Long myUserId;
    private ChatAdapter adapter;
    private List<ChatMessage> messages = new ArrayList<>();
    public ChatFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        myUserId = Long.parseLong(
                Objects.requireNonNull(TokenManager.getUserId(requireContext())).toString()
        );

        recyclerView = view.findViewById(R.id.recyclerMessages);
        inputMessage = view.findViewById(R.id.inputMessage);
        btnSend = view.findViewById(R.id.btnSend);

        adapter = new ChatAdapter(messages, myUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // receive messages
        ChatWebSocketManager.getInstance().subscribeToMessages(msg -> {
            requireActivity().runOnUiThread(() -> {
                ChatMessage cm = new ChatMessage();
                cm.content = msg;
                cm.fromUserId = 0L;


                messages.add(cm);
                adapter.notifyItemInserted(messages.size()-1);
                recyclerView.scrollToPosition(messages.size()-1);
            });
        });

        btnSend.setOnClickListener(v -> {

            String text = inputMessage.getText().toString();
            if(text.isEmpty()) return;

            ChatMessage local = new ChatMessage();
            local.content = text;
            local.fromUserId = myUserId;

            messages.add(local);


            messages.add(local);
            adapter.notifyItemInserted(messages.size()-1);
            recyclerView.scrollToPosition(messages.size()-1);

            ChatMessage msg = new ChatMessage(1L, text);
            ChatWebSocketManager.getInstance().sendMessage(msg);

            inputMessage.setText("");
        });


        return view;
    }
}
