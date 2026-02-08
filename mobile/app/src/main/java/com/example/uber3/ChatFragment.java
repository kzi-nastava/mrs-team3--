package com.example.uber3;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.adapter.ChatAdapter;
import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.ChatApiService;
import com.example.uber3.network.manager.TokenManager;
import com.example.uber3.network.model.chat.ChatMessage;
import com.example.uber3.network.model.chat.AdminDto;
import com.example.uber3.network.websocket.ChatWebSocketManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";

    private RecyclerView recyclerView;
    private EditText inputMessage;
    private ImageButton btnSend;
    private ImageButton btnBack;
    private TextView txtChatPartnerName;

    private Long targetUserId;
    private String targetUserName;
    private boolean showBackButton = false;

    private Long myUserId;
    private ChatAdapter adapter;
    private final List<ChatMessage> messages = Collections.synchronizedList(new ArrayList<>());

    public ChatFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Log.d(TAG, "========== onCreateView START ==========");

        if(getArguments() != null){
            targetUserId = getArguments().getLong("targetUserId", 0L);
            targetUserName = getArguments().getString("targetUserName", null);
            showBackButton = getArguments().getBoolean("showBackButton", false);
        }

        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        myUserId = TokenManager.getUserId(requireContext());

        if(myUserId == null) {
            Toast.makeText(getContext(), "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }

        Log.d(TAG, "My User ID: " + myUserId);

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerMessages);
        inputMessage = view.findViewById(R.id.inputMessage);
        btnSend = view.findViewById(R.id.btnSend);
        btnBack = view.findViewById(R.id.btnBack);
        txtChatPartnerName = view.findViewById(R.id.txtChatPartnerName);

        hideActionBar();

        adapter = new ChatAdapter(messages, myUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        if(targetUserId == null || targetUserId == 0L) {
            Log.d(TAG, "No target user specified, fetching admin from backend...");
            fetchAdminAndInitialize();
        } else {
            Log.d(TAG, "Chat with: " + targetUserName + " (ID: " + targetUserId + ")");
            initializeChat();
        }

        Log.d(TAG, "========== onCreateView END ==========");
        return view;
    }

    private void fetchAdminAndInitialize() {
        ChatApiService api = ApiClient.getClient(requireContext())
                .create(ChatApiService.class);

        api.getFirstAdmin().enqueue(new retrofit2.Callback<AdminDto>() {
            @Override
            public void onResponse(
                    @NonNull retrofit2.Call<AdminDto> call,
                    @NonNull retrofit2.Response<AdminDto> res) {

                if(res.isSuccessful() && res.body() != null) {
                    AdminDto admin = res.body();

                    targetUserId = admin.id;
                    targetUserName = admin.name;

                    Log.d(TAG, "✅ Admin fetched: " + targetUserName + " (ID: " + targetUserId + ")");

                    if(isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            initializeChat();
                        });
                    }
                } else {
                    Log.e(TAG, "❌ Failed to fetch admin: " + res.code());
                    Toast.makeText(getContext(), "Could not find admin", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(
                    @NonNull retrofit2.Call<AdminDto> call,
                    @NonNull Throwable t) {
                Log.e(TAG, "❌ Error fetching admin", t);
                Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeChat() {
        if(txtChatPartnerName != null) {
            txtChatPartnerName.setText(targetUserName);
        }

        if(btnBack != null) {
            if(showBackButton) {
                btnBack.setVisibility(View.VISIBLE);
                btnBack.setOnClickListener(v -> {
                    if(getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                });
            } else {
                btnBack.setVisibility(View.GONE);
            }
        }

        loadHistory();
        setupWebSocketListener();
        setupSendButton();
    }

    private void setupWebSocketListener() {
        Log.d(TAG, "Setting up WebSocket listener for chat with user " + targetUserId);

        ChatWebSocketManager wsManager = ChatWebSocketManager.getInstance();

        if (!wsManager.isSubscribed()) {
            Log.d(TAG, "Not subscribed yet, subscribing now");
            wsManager.subscribeToMessages();
        }

        wsManager.setUiListener(cm -> {

            if(!isAdded()) {
                Log.w(TAG, "Fragment not added, ignoring message");
                return;
            }

            Log.d(TAG, "📨 WebSocket received:");
            Log.d(TAG, "   From: " + cm.fromUserId + " → To: " + cm.toUserId);
            Log.d(TAG, "   Content: " + cm.content);
            Log.d(TAG, "   My ID: " + myUserId + ", Partner ID: " + targetUserId);

            boolean isFromMyPartner = cm.fromUserId.equals(targetUserId) && cm.toUserId.equals(myUserId);
            boolean isToMyPartner = cm.fromUserId.equals(myUserId) && cm.toUserId.equals(targetUserId);
            boolean isRelevant = isFromMyPartner || isToMyPartner;

            Log.d(TAG, "   Is from my partner? " + isFromMyPartner);
            Log.d(TAG, "   Is to my partner (echo)? " + isToMyPartner);
            Log.d(TAG, "   Is relevant? " + isRelevant);

            if(!isRelevant) {
                Log.d(TAG, "   ❌ Ignoring - not relevant to this chat");
                return;
            }

            if(isToMyPartner) {
                Log.d(TAG, "   ℹ️ Echo of my own message, already in UI");
                return;
            }

            Log.d(TAG, "   ✅ Adding message to UI");

            requireActivity().runOnUiThread(() -> {
                boolean isDuplicate = messages.stream()
                        .anyMatch(m ->
                                m.fromUserId.equals(cm.fromUserId) &&
                                        m.toUserId.equals(cm.toUserId) &&
                                        m.content.equals(cm.content) &&
                                        m.timestamp != null &&
                                        cm.timestamp != null &&
                                        Math.abs(
                                                java.time.Instant.parse(m.timestamp).toEpochMilli() -
                                                        java.time.Instant.parse(cm.timestamp).toEpochMilli()
                                        ) < 1000 // Within 1 second
                        );

                if(isDuplicate) {
                    Log.w(TAG, "   ⚠️ Duplicate message detected, skipping");
                    return;
                }

                messages.add(cm);
                adapter.notifyItemInserted(messages.size() - 1);
                recyclerView.scrollToPosition(messages.size() - 1);

                Log.d(TAG, "   Total messages now: " + messages.size());
            });
        });
    }

    private void setupSendButton() {
        btnSend.setOnClickListener(v -> {
            String text = inputMessage.getText().toString().trim();

            if(text.isEmpty()) {
                return;
            }

            Log.d(TAG, "📤 Sending message:");
            Log.d(TAG, "   From: " + myUserId + " → To: " + targetUserId);
            Log.d(TAG, "   Content: " + text);

            ChatMessage local = new ChatMessage();
            local.content = text;
            local.fromUserId = myUserId;
            local.toUserId = targetUserId;
            local.timestamp = java.time.Instant.now().toString();

            requireActivity().runOnUiThread(() -> {
                messages.add(local);
                adapter.notifyItemInserted(messages.size() - 1);
                recyclerView.scrollToPosition(messages.size() - 1);
                Log.d(TAG, "   ✅ Added to local UI, total: " + messages.size());
            });

            ChatMessage wsMessage = new ChatMessage();
            wsMessage.toUserId = targetUserId;
            wsMessage.content = text;
            ChatWebSocketManager.getInstance().sendMessage(wsMessage);

            inputMessage.setText("");
        });
    }

    private void loadHistory(){
        Log.d(TAG, "Loading chat history...");

        ChatApiService api = ApiClient.getClient(requireContext())
                .create(ChatApiService.class);

        api.getHistory(myUserId, targetUserId)
                .enqueue(new retrofit2.Callback<List<ChatMessage>>() {

                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onResponse(
                            @NonNull retrofit2.Call<List<ChatMessage>> call,
                            @NonNull retrofit2.Response<List<ChatMessage>> res){

                        if(res.isSuccessful() && res.body() != null){
                            List<ChatMessage> history = res.body();
                            Log.d(TAG, "✅ Loaded " + history.size() + " messages from history");

                            messages.clear();
                            messages.addAll(history);

                            if(isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    adapter.notifyDataSetChanged();
                                    if(messages.size() > 0) {
                                        recyclerView.scrollToPosition(messages.size() - 1);
                                    }
                                });
                            }
                        } else {
                            Log.e(TAG, "❌ History load failed: " + res.code());
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull retrofit2.Call<List<ChatMessage>> call,
                            @NonNull Throwable t){
                        Log.e(TAG, "❌ History load error", t);
                    }
                });
    }

    private void hideActionBar() {
        if(getActivity() != null && getActivity() instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
            if(actionBar != null) {
                actionBar.hide();
            }
        }
    }

    private void showActionBar() {
        if(getActivity() != null && getActivity() instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
            if(actionBar != null) {
                actionBar.show();
            }
        }
    }

    public static ChatFragment forUser(Long userId, String userName){
        ChatFragment f = new ChatFragment();
        Bundle b = new Bundle();
        b.putLong("targetUserId", userId);
        b.putString("targetUserName", userName);
        b.putBoolean("showBackButton", true);
        f.setArguments(b);
        return f;
    }

    public static ChatFragment forAdmin(){
        ChatFragment f = new ChatFragment();
        Bundle b = new Bundle();
        b.putLong("targetUserId", 0L); // Will be fetched from backend
        b.putString("targetUserName", null);
        b.putBoolean("showBackButton", false);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - re-establishing WebSocket listener");

        // FIXED: Re-establish listener when returning to this fragment
        if(targetUserId != null && targetUserId != 0L) {
            setupWebSocketListener();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView - removing listener");
        ChatWebSocketManager.getInstance().setUiListener(null);
        showActionBar();
    }
}