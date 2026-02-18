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
import com.example.uber3.network.model.chat.AdminDto;
import com.example.uber3.network.model.chat.ChatMessage;
import com.example.uber3.network.service.ChatService;
import com.example.uber3.network.websocket.ChatWebSocketManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";

    private RecyclerView   recyclerView;
    private EditText       inputMessage;
    private ImageButton    btnSend;
    private ImageButton    btnBack;
    private TextView       txtChatPartnerName;

    private Long   targetUserId;
    private String targetUserName;
    private boolean showBackButton;

    private Long myUserId;
    private ChatAdapter adapter;
    private final List<ChatMessage> messages =
            Collections.synchronizedList(new ArrayList<>());

    private ChatService.IncomingMessageListener chatListener;

    // ─── Factory methods ──────────────────────────────────────────────────────

    public static ChatFragment forAdmin() {
        ChatFragment f = new ChatFragment();
        Bundle b = new Bundle();
        b.putLong("targetUserId", 0L);
        b.putBoolean("showBackButton", false);
        f.setArguments(b);
        return f;
    }

    public static ChatFragment forUser(Long userId, String userName) {
        ChatFragment f = new ChatFragment();
        Bundle b = new Bundle();
        b.putLong("targetUserId", userId);
        b.putString("targetUserName", userName);
        b.putBoolean("showBackButton", true);
        f.setArguments(b);
        return f;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");

        if (getArguments() != null) {
            targetUserId   = getArguments().getLong("targetUserId", 0L);
            targetUserName = getArguments().getString("targetUserName", null);
            showBackButton = getArguments().getBoolean("showBackButton", false);
        }

        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        myUserId = TokenManager.getUserId(requireContext());
        if (myUserId == null) {
            Toast.makeText(getContext(), "Error: Not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }

        recyclerView       = view.findViewById(R.id.recyclerMessages);
        inputMessage       = view.findViewById(R.id.inputMessage);
        btnSend            = view.findViewById(R.id.btnSend);
        btnBack            = view.findViewById(R.id.btnBack);
        txtChatPartnerName = view.findViewById(R.id.txtChatPartnerName);

        hideActionBar();

        adapter = new ChatAdapter(messages, myUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        if (targetUserId == null || targetUserId == 0L) {
            fetchAdminAndInitialize();
        } else {
            initializeChat();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        ChatService.getInstance().setChatUiVisible(true);

        // Re-register listener in case we navigated away and back
        if (targetUserId != null && targetUserId != 0L) {
            registerChatListener();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ChatService.getInstance().setChatUiVisible(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");

        if (chatListener != null) {
            ChatService.getInstance().removeUiListener(chatListener);
            chatListener = null;
        }

        ChatService.getInstance().setChatUiVisible(false);
        showActionBar();
    }


    private void fetchAdminAndInitialize() {
        ChatApiService api = ApiClient.getClient(requireContext())
                .create(ChatApiService.class);

        api.getFirstAdmin().enqueue(new retrofit2.Callback<AdminDto>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<AdminDto> call,
                                   @NonNull retrofit2.Response<AdminDto> res) {
                if (res.isSuccessful() && res.body() != null) {
                    AdminDto admin = res.body();
                    targetUserId   = admin.id;
                    targetUserName = admin.name;
                    Log.d(TAG, "✅ Admin fetched: " + targetUserName + " (ID: " + targetUserId + ")");

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> initializeChat());
                    }
                } else {
                    Log.e(TAG, "❌ Failed to fetch admin: " + res.code());
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Could not find support", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<AdminDto> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Network error fetching admin", t);
                if (isAdded()) {
                    Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initializeChat() {
        // Set header name
        if (txtChatPartnerName != null && targetUserName != null) {
            txtChatPartnerName.setText(targetUserName);
        }

        if (btnBack != null) {
            if (showBackButton) {
                btnBack.setVisibility(View.VISIBLE);
                btnBack.setOnClickListener(v -> {
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                });
            } else {
                btnBack.setVisibility(View.GONE);
            }
        }

        loadHistory();
        registerChatListener();
        setupSendButton();

        ChatService.getInstance().setChatUiVisible(true);
    }

    private void registerChatListener() {
        Log.d(TAG, "Registering chat listener for partner: " + targetUserId);

        chatListener = message -> {
            if (!isAdded()) return;

            boolean fromPartner = message.fromUserId != null
                    && message.fromUserId.equals(targetUserId)
                    && message.toUserId != null
                    && message.toUserId.equals(myUserId);

            if (!fromPartner) {
                Log.d(TAG, "Ignoring message — not from current partner");
                return;
            }

            boolean duplicate = isDuplicate(message);
            if (duplicate) {
                Log.d(TAG, "Duplicate message — skipping");
                return;
            }

            Log.d(TAG, "✅ Adding incoming message to UI");
            messages.add(message);
            adapter.notifyItemInserted(messages.size() - 1);
            recyclerView.scrollToPosition(messages.size() - 1);
        };

        ChatService.getInstance().setUiListener(chatListener);
    }


    private void setupSendButton() {
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (text.isEmpty() || targetUserId == null) return;

        Log.d(TAG, "📤 Sending: \"" + text + "\" to " + targetUserId);

        ChatMessage local = new ChatMessage();
        local.fromUserId = myUserId;
        local.toUserId   = targetUserId;
        local.content    = text;
        local.timestamp  = Instant.now().toString();

        messages.add(local);
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.scrollToPosition(messages.size() - 1);

        ChatMessage wsMsg = new ChatMessage();
        wsMsg.toUserId = targetUserId;
        wsMsg.content  = text;
        ChatWebSocketManager.getInstance().sendMessage(wsMsg);

        inputMessage.setText("");
    }


    @SuppressLint("NotifyDataSetChanged")
    private void loadHistory() {
        Log.d(TAG, "Loading history for " + myUserId + " ↔ " + targetUserId);

        ChatApiService api = ApiClient.getClient(requireContext())
                .create(ChatApiService.class);

        api.getHistory(myUserId, targetUserId)
                .enqueue(new retrofit2.Callback<List<ChatMessage>>() {
                    @Override
                    public void onResponse(
                            @NonNull retrofit2.Call<List<ChatMessage>> call,
                            @NonNull retrofit2.Response<List<ChatMessage>> res) {

                        if (res.isSuccessful() && res.body() != null) {
                            Log.d(TAG, "✅ History: " + res.body().size() + " messages");
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    messages.clear();
                                    messages.addAll(res.body());
                                    adapter.notifyDataSetChanged();
                                    if (!messages.isEmpty()) {
                                        recyclerView.scrollToPosition(messages.size() - 1);
                                    }
                                });
                            }
                        } else {
                            Log.e(TAG, "❌ History failed: " + res.code());
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull retrofit2.Call<List<ChatMessage>> call,
                            @NonNull Throwable t) {
                        Log.e(TAG, "❌ History network error", t);
                    }
                });
    }


    private boolean isDuplicate(ChatMessage incoming) {
        if (incoming.timestamp == null) return false;
        long incomingMs = Instant.parse(incoming.timestamp).toEpochMilli();

        for (ChatMessage m : messages) {
            if (m.fromUserId == null || m.content == null || m.timestamp == null) continue;
            if (!m.fromUserId.equals(incoming.fromUserId)) continue;
            if (!m.content.equals(incoming.content)) continue;

            long existingMs = Instant.parse(m.timestamp).toEpochMilli();
            if (Math.abs(existingMs - incomingMs) < 2000) return true;
        }
        return false;
    }

    private void hideActionBar() {
        if (getActivity() instanceof AppCompatActivity) {
            ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (ab != null) ab.hide();
        }
    }

    private void showActionBar() {
        if (getActivity() instanceof AppCompatActivity) {
            ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (ab != null) ab.show();
        }
    }
}