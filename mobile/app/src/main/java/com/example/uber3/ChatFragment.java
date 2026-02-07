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
    private boolean showBackButton = false; // Only show for admin

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

        Log.d(TAG, "onCreateView called");

        // Get arguments
        if(getArguments() != null){
            targetUserId = getArguments().getLong("targetUserId", 0L);
            targetUserName = getArguments().getString("targetUserName", "Admin");
            showBackButton = getArguments().getBoolean("showBackButton", false);
            Log.d(TAG, "Target User ID: " + targetUserId + ", Name: " + targetUserName);
        }

        // Default to admin if not specified
        if(targetUserId == null || targetUserId == 0L){
            targetUserId = 1L;
            targetUserName = "Admin";
            Log.d(TAG, "Using default admin ID: 1");
        }

        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Get current user ID
        myUserId = TokenManager.getUserId(requireContext());
        Log.d(TAG, "My User ID: " + myUserId);

        if(myUserId == null) {
            Toast.makeText(getContext(), "Error: User not logged in", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "myUserId is null!");
            return view;
        }

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerMessages);
        inputMessage = view.findViewById(R.id.inputMessage);
        btnSend = view.findViewById(R.id.btnSend);
        btnBack = view.findViewById(R.id.btnBack);
        txtChatPartnerName = view.findViewById(R.id.txtChatPartnerName);

        // Set chat partner name in header
        if(txtChatPartnerName != null) {
            txtChatPartnerName.setText(targetUserName);
        }

        // Show/hide back button based on user role
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

        // Hide ActionBar to avoid double header
        hideActionBar();

        // Setup RecyclerView
        adapter = new ChatAdapter(messages, myUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        Log.d(TAG, "RecyclerView setup complete");

        // Load chat history
        loadHistory();

        // Setup WebSocket listener
        ChatWebSocketManager.getInstance().setUiListener(cm -> {
            Log.d(TAG, "WebSocket message received from: " + cm.fromUserId);

            if(!isAdded()) {
                Log.w(TAG, "Fragment not added, ignoring message");
                return;
            }

            requireActivity().runOnUiThread(() -> {
                // Don't show our own messages twice
                if(cm.fromUserId.equals(myUserId)) {
                    Log.d(TAG, "Ignoring own message");
                    return;
                }

                messages.add(cm);
                adapter.notifyItemInserted(messages.size()-1);
                Log.d(TAG, "Message added to list, total: " + messages.size());

                if(!messages.isEmpty()){
                    recyclerView.scrollToPosition(messages.size()-1);
                }
            });
        });

        // Subscribe to WebSocket messages
        ChatWebSocketManager.getInstance().subscribeToMessages();
        Log.d(TAG, "Subscribed to WebSocket messages");

        // Setup send button
        btnSend.setOnClickListener(v -> {
            String text = inputMessage.getText().toString().trim();

            if(text.isEmpty()) {
                Log.d(TAG, "Empty message, not sending");
                return;
            }

            Log.d(TAG, "Sending message: " + text);

            // Add to local UI immediately
            ChatMessage local = new ChatMessage();
            local.content = text;
            local.fromUserId = myUserId;
            local.toUserId = targetUserId;

            requireActivity().runOnUiThread(() -> {
                messages.add(local);
                adapter.notifyItemInserted(messages.size()-1);
                recyclerView.scrollToPosition(messages.size()-1);
                Log.d(TAG, "Message added to UI, total: " + messages.size());
            });

            // Send via WebSocket
            ChatMessage msg = new ChatMessage();
            msg.toUserId = targetUserId;
            msg.content = text;
            ChatWebSocketManager.getInstance().sendMessage(msg);

            inputMessage.setText("");
        });

        return view;
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

    // Factory method for admin (with back button)
    public static ChatFragment forUser(Long userId, String userName){
        ChatFragment f = new ChatFragment();
        Bundle b = new Bundle();
        b.putLong("targetUserId", userId);
        b.putString("targetUserName", userName);
        b.putBoolean("showBackButton", true); // Admin sees back button
        f.setArguments(b);
        Log.d("ChatFragment", "Created fragment for admin chatting with: " + userName);
        return f;
    }

    // Factory method for passengers/drivers (no back button)
    public static ChatFragment forAdmin(){
        ChatFragment f = new ChatFragment();
        Bundle b = new Bundle();
        b.putLong("targetUserId", 1L);
        b.putString("targetUserName", "Admin");
        b.putBoolean("showBackButton", false); // No back button for regular users
        f.setArguments(b);
        Log.d("ChatFragment", "Created fragment for user chatting with Admin");
        return f;
    }

    private void loadHistory(){
        Log.d(TAG, "Loading chat history...");

        ChatApiService api =
                ApiClient.getClient(requireContext())
                        .create(ChatApiService.class);

        Long myId = TokenManager.getUserId(requireContext());

        Log.d(TAG, "Fetching history for myId=" + myId + ", targetId=" + targetUserId);

        api.getHistory(myId, targetUserId)
                .enqueue(new retrofit2.Callback<List<ChatMessage>>() {

                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onResponse(
                            @NonNull retrofit2.Call<List<ChatMessage>> call,
                            @NonNull retrofit2.Response<List<ChatMessage>> res){

                        Log.d(TAG, "History response code: " + res.code());

                        if(res.isSuccessful() && res.body() != null){
                            List<ChatMessage> history = res.body();
                            Log.d(TAG, "Loaded " + history.size() + " messages from history");

                            messages.clear();
                            messages.addAll(history);

                            if(isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    adapter.notifyDataSetChanged();

                                    if(messages.size() > 0) {
                                        recyclerView.scrollToPosition(messages.size() - 1);
                                        Log.d(TAG, "Scrolled to position: " + (messages.size() - 1));
                                    }
                                });
                            }

                        } else {
                            Log.e(TAG, "History load failed: " + res.code() + " - " + res.message());
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull retrofit2.Call<List<ChatMessage>> call,
                            @NonNull Throwable t){
                        Log.e(TAG, "History load error", t);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ChatWebSocketManager.getInstance().setUiListener(null);
        Log.d(TAG, "onDestroyView - removed WebSocket listener");

        // Show ActionBar again when leaving chat
        showActionBar();
    }
}