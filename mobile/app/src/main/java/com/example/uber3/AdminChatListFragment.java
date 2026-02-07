package com.example.uber3;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import android.view.*;
import com.example.uber3.adapter.AdminChatAdapter;
import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.ChatApiService;
import com.example.uber3.network.manager.TokenManager;
import com.example.uber3.network.model.ChatRoomDto;
import com.example.uber3.network.model.chat.AdminChatRoom;
import com.example.uber3.network.model.chat.ChatMessage;
import com.example.uber3.network.websocket.ChatWebSocketManager;

import java.util.*;

public class AdminChatListFragment extends Fragment {

    private static final String TAG = "AdminChatList";

    private RecyclerView recycler;
    private List<AdminChatRoom> rooms = new ArrayList<>();
    private AdminChatAdapter chatAdapter;

    private Handler refreshHandler;
    private Runnable refreshRunnable;

    private Long adminId;

    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState){

        Log.d(TAG, "onCreateView called");

        View v = inflater.inflate(
                R.layout.fragment_admin_chat_list,
                container, false);

        recycler = v.findViewById(R.id.recyclerRooms);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        adminId = TokenManager.getUserId(requireContext());

        if(adminId == null) {
            Toast.makeText(getContext(), "Error: Not logged in", Toast.LENGTH_SHORT).show();
            return v;
        }

        Log.d(TAG, "Admin ID: " + adminId);

        chatAdapter = new AdminChatAdapter(rooms, room -> {
            Log.d(TAG, "Opening chat with: " + room.userName);
            openChat(room.userId, room.userName);
        });
        recycler.setAdapter(chatAdapter);

        loadChatRooms();

        setupWebSocketForAdminList();

        setupPeriodicRefresh();

        return v;
    }

    private void setupWebSocketForAdminList() {
        Log.d(TAG, "Setting up WebSocket for admin chat list");

        ChatWebSocketManager wsManager = ChatWebSocketManager.getInstance();

        if (!wsManager.isSubscribed()) {
            Log.d(TAG, "Not subscribed yet, subscribing now");
            wsManager.subscribeToMessages();
        }

        wsManager.setUiListener(message -> {
            if(!isAdded()) {
                Log.d(TAG, "Fragment not added, ignoring message");
                return;
            }

            Log.d(TAG, "📨 New message received from user: " + message.fromUserId);

            boolean roomExists = false;
            synchronized (rooms) {
                for (AdminChatRoom room : rooms) {
                    if (room.userId.equals(message.fromUserId)) {
                        roomExists = true;
                        break;
                    }
                }
            }

            if(!roomExists) {
                Log.d(TAG, "New chat room detected! Reloading rooms...");
                // Use explicit Runnable instead of method reference
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isAdded()) {
                            loadChatRooms();
                        }
                    }
                });
            } else {
                Log.d(TAG, "Message from existing room - no reload needed");
            }
        });
    }

    private void setupPeriodicRefresh() {
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if(isAdded()) {
                    Log.d(TAG, "Periodic refresh: checking for new chat rooms");
                    loadChatRooms();
                    refreshHandler.postDelayed(this, 30000); // Refresh every 30 seconds
                }
            }
        };

        refreshHandler.postDelayed(refreshRunnable, 30000);
    }

    private void loadChatRooms() {
        ChatApiService api = ApiClient.getClient(requireContext())
                .create(ChatApiService.class);

        api.getAdminRooms(adminId)
                .enqueue(new retrofit2.Callback<List<ChatRoomDto>>() {

                    @Override
                    public void onResponse(
                            @NonNull retrofit2.Call<List<ChatRoomDto>> call,
                            @NonNull retrofit2.Response<List<ChatRoomDto>> res){

                        if(res.isSuccessful() && res.body() != null){
                            List<ChatRoomDto> roomDtos = res.body();
                            Log.d(TAG, "✅ Loaded " + roomDtos.size() + " chat rooms");

                            // Build new list first (thread-safe)
                            List<AdminChatRoom> newRooms = new ArrayList<>();
                            for(ChatRoomDto dto : roomDtos){
                                String userName = buildUserName(dto);
                                newRooms.add(new AdminChatRoom(dto.user.id, userName));
                            }

                            if(isAdded() && chatAdapter != null) {
                                requireActivity().runOnUiThread(() -> {
                                    int previousCount = rooms.size();

                                    // Thread-safe update
                                    synchronized (rooms) {
                                        rooms.clear();
                                        rooms.addAll(newRooms);
                                    }

                                    chatAdapter.notifyDataSetChanged();

                                    if(rooms.size() > previousCount) {
                                        Toast.makeText(getContext(),
                                                "New chat room available!",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            if(rooms.isEmpty()) {
                                Log.d(TAG, "No chat rooms yet");
                            }
                        } else {
                            Log.e(TAG, "❌ Failed to load rooms: " + res.code());
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull retrofit2.Call<List<ChatRoomDto>> call,
                            @NonNull Throwable t){
                        Log.e(TAG, "❌ Network error", t);
                    }
                });
    }

    private String buildUserName(ChatRoomDto dto) {
        if(dto.user == null) {
            return "Unknown User";
        }

        String firstName = dto.user.firstName;
        String lastName = dto.user.lastName;

        if(firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if(firstName != null) {
            return firstName;
        } else if(lastName != null) {
            return lastName;
        } else if(dto.user.email != null) {
            return dto.user.email;
        } else {
            return "User " + dto.user.id;
        }
    }

    private void openChat(Long userId, String userName){
        ChatFragment f = ChatFragment.forUser(userId, userName);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, f)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - re-establishing WebSocket listener");

        setupWebSocketForAdminList();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        Log.d(TAG, "onDestroyView");

        if(refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }

        ChatWebSocketManager.getInstance().setUiListener(null);
    }
}