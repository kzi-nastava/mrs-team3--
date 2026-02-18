package com.example.uber3;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.adapter.AdminChatAdapter;
import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.ChatApiService;
import com.example.uber3.network.manager.TokenManager;
import com.example.uber3.network.model.ChatRoomDto;
import com.example.uber3.network.model.chat.AdminChatRoom;
import com.example.uber3.network.model.chat.ChatMessage;
import com.example.uber3.network.service.ChatService;

import java.util.ArrayList;
import java.util.List;

public class AdminChatListFragment extends Fragment {

    private static final String TAG = "AdminChatList";

    private RecyclerView      recycler;
    private AdminChatAdapter  chatAdapter;
    private final List<AdminChatRoom> rooms = new ArrayList<>();

    private Long adminId;

    private Handler refreshHandler;
    private Runnable refreshRunnable;

    private ChatService.IncomingMessageListener chatListener;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");

        View v = inflater.inflate(R.layout.fragment_admin_chat_list, container, false);

        recycler = v.findViewById(R.id.recyclerRooms);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        adminId = TokenManager.getUserId(requireContext());
        if (adminId == null) {
            Toast.makeText(getContext(), "Error: Not logged in", Toast.LENGTH_SHORT).show();
            return v;
        }

        chatAdapter = new AdminChatAdapter(rooms, room -> openChat(room.userId, room.userName));
        recycler.setAdapter(chatAdapter);

        loadChatRooms();
        setupPeriodicRefresh();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume — registering chat listener");
        registerChatListener();
        loadChatRooms();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (chatListener != null) {
            ChatService.getInstance().removeUiListener(chatListener);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");

        if (chatListener != null) {
            ChatService.getInstance().removeUiListener(chatListener);
            chatListener = null;
        }

        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }


    private void registerChatListener() {
        chatListener = message -> {
            if (!isAdded()) return;

            Log.d(TAG, "📨 Received message from user: " + message.fromUserId);

            boolean roomExists = false;
            synchronized (rooms) {
                for (AdminChatRoom room : rooms) {
                    if (room.userId != null && room.userId.equals(message.fromUserId)) {
                        roomExists = true;
                        break;
                    }
                }
            }

            if (!roomExists) {
                Log.d(TAG, "New sender — reloading room list");
                requireActivity().runOnUiThread(this::loadChatRooms);
            }
        };

        ChatService.getInstance().setUiListener(chatListener);
    }


    private void setupPeriodicRefresh() {
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    Log.d(TAG, "Periodic refresh");
                    loadChatRooms();
                    refreshHandler.postDelayed(this, 30_000);
                }
            }
        };
        refreshHandler.postDelayed(refreshRunnable, 30_000);
    }


    @SuppressLint("NotifyDataSetChanged")
    private void loadChatRooms() {
        ChatApiService api = ApiClient.getClient(requireContext())
                .create(ChatApiService.class);

        api.getAdminRooms(adminId).enqueue(new retrofit2.Callback<List<ChatRoomDto>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<List<ChatRoomDto>> call,
                                   @NonNull retrofit2.Response<List<ChatRoomDto>> res) {

                if (res.isSuccessful() && res.body() != null) {
                    List<ChatRoomDto> dtos = res.body();
                    Log.d(TAG, "✅ Loaded " + dtos.size() + " rooms");

                    List<AdminChatRoom> newRooms = new ArrayList<>();
                    for (ChatRoomDto dto : dtos) {
                        newRooms.add(new AdminChatRoom(dto.user.id, buildName(dto)));
                    }

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            int prev = rooms.size();
                            synchronized (rooms) {
                                rooms.clear();
                                rooms.addAll(newRooms);
                            }
                            chatAdapter.notifyDataSetChanged();

                            if (rooms.size() > prev) {
                                Toast.makeText(getContext(),
                                        "New conversation available!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "❌ Load rooms failed: " + res.code());
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<List<ChatRoomDto>> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "❌ Network error loading rooms", t);
            }
        });
    }

    private String buildName(ChatRoomDto dto) {
        if (dto.user == null) return "Unknown User";
        String fn = dto.user.firstName;
        String ln = dto.user.lastName;
        if (fn != null && ln != null) return fn + " " + ln;
        if (fn != null) return fn;
        if (ln != null) return ln;
        if (dto.user.email != null) return dto.user.email;
        return "User " + dto.user.id;
    }


    private void openChat(Long userId, String userName) {
        Log.d(TAG, "Opening chat with: " + userName + " (" + userId + ")");

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, ChatFragment.forUser(userId, userName))
                .addToBackStack(null)
                .commit();
    }
}