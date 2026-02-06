package com.example.uber3;

import android.annotation.SuppressLint;
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
import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.ChatApiService;
import com.example.uber3.network.manager.TokenManager;
import com.example.uber3.network.model.chat.ChatMessage;
import com.example.uber3.network.websocket.ChatWebSocketManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private EditText inputMessage;
    private ImageButton btnSend;

    private Long targetUserId;

    private Long myUserId;
    private ChatAdapter adapter;
    private final List<ChatMessage> messages =
            Collections.synchronizedList(new ArrayList<>());
    public ChatFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if(getArguments()!=null){
            targetUserId =
                    getArguments().getLong("targetUserId");
        }


        if(targetUserId == null){
            targetUserId = 3L;
        }


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

        loadHistory();


        ChatWebSocketManager.getInstance().setUiListener(cm -> {

            if(!isAdded()) return;

            requireActivity().runOnUiThread(() -> {

                if(cm.fromUserId.equals(myUserId)) return;
                messages.add(cm);
                adapter.notifyItemInserted(messages.size()-1);

                if(!messages.isEmpty()){
                    recyclerView.scrollToPosition(messages.size()-1);
                }

            });
        });


        ChatWebSocketManager.getInstance().subscribeToMessages();



        btnSend.setOnClickListener(v -> {

            String text = inputMessage.getText().toString();
            if(text.isEmpty()) return;

            ChatMessage local = new ChatMessage();
            local.content = text;
            local.fromUserId = myUserId;

            requireActivity().runOnUiThread(() -> {
                messages.add(local);
                adapter.notifyItemInserted(messages.size()-1);
                recyclerView.scrollToPosition(messages.size()-1);
            });

            ChatMessage msg = new ChatMessage();
            msg.toUserId = targetUserId;
            msg.content = text;
            ChatWebSocketManager.getInstance().sendMessage(msg);

            inputMessage.setText("");
        });


        return view;
    }

    public static ChatFragment forUser(Long userId){
        ChatFragment f = new ChatFragment();
        Bundle b = new Bundle();
        b.putLong("targetUserId", userId);
        f.setArguments(b);
        return f;
    }

    private void loadHistory(){

        ChatApiService api =
                ApiClient.getClient(requireContext())
                        .create(ChatApiService.class);

        Long myId =
                TokenManager.getUserId(requireContext());

        api.getHistory(myId,targetUserId)
                .enqueue(new retrofit2.Callback<List<ChatMessage>>() {

                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onResponse(
                            retrofit2.Call<List<ChatMessage>> call,
                            retrofit2.Response<List<ChatMessage>> res){

                        if(res.isSuccessful() && res.body()!=null){

                            messages.clear();
                            messages.addAll(res.body());

                            adapter.notifyDataSetChanged();

                            recyclerView.scrollToPosition(
                                    messages.size()-1
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull retrofit2.Call<List<ChatMessage>> call,
                            @NonNull Throwable t){
                        t.printStackTrace();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ChatWebSocketManager.getInstance().setUiListener(null);
    }





}
