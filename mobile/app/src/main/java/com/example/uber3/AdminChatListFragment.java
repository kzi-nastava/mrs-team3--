package com.example.uber3;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import android.view.*;
import com.example.uber3.adapter.AdminChatAdapter;
import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.ChatApiService;
import com.example.uber3.network.manager.TokenManager;
import com.example.uber3.network.model.ChatRoomDto;
import com.example.uber3.network.model.chat.AdminChatRoom;

import java.util.*;

public class AdminChatListFragment extends Fragment {

    private static final String TAG = "AdminChatList";

    private RecyclerView recycler;
    private List<AdminChatRoom> rooms = new ArrayList<>();

    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState){

        Log.d(TAG, "onCreateView called");

        View v = inflater.inflate(
                R.layout.fragment_admin_chat_list,
                container, false);

        recycler = v.findViewById(R.id.recyclerRooms);
        recycler.setLayoutManager(
                new LinearLayoutManager(getContext()));

        ChatApiService api =
                ApiClient.getClient(requireContext())
                        .create(ChatApiService.class);

        Long adminId = TokenManager.getUserId(requireContext());

        Log.d(TAG, "Admin ID: " + adminId);

        if(adminId == null) {
            Toast.makeText(getContext(), "Error: Not logged in", Toast.LENGTH_SHORT).show();
            return v;
        }

        api.getAdminRooms(adminId)
                .enqueue(new retrofit2.Callback<java.util.List<ChatRoomDto>>() {

                    @Override
                    public void onResponse(
                            retrofit2.Call<java.util.List<ChatRoomDto>> call,
                            retrofit2.Response<java.util.List<ChatRoomDto>> res){

                        Log.d(TAG, "Response code: " + res.code());

                        if(res.isSuccessful() && res.body() != null){
                            List<ChatRoomDto> roomDtos = res.body();
                            Log.d(TAG, "Received " + roomDtos.size() + " chat rooms");

                            rooms.clear();

                            for(ChatRoomDto dto : roomDtos){
                                // Build full name from firstName and lastName
                                String userName = buildUserName(dto);

                                Log.d(TAG, "Adding room: " + userName + " (ID: " + dto.user.id + ")");

                                rooms.add(
                                        new AdminChatRoom(dto.user.id, userName)
                                );
                            }

                            if(rooms.isEmpty()) {
                                Toast.makeText(getContext(),
                                        "No chat rooms yet",
                                        Toast.LENGTH_SHORT).show();
                            }

                            recycler.setAdapter(
                                    new AdminChatAdapter(
                                            rooms,
                                            room -> {
                                                Log.d(TAG, "Opening chat with: " + room.userName);
                                                openChat(room.userId, room.userName);
                                            }
                                    )
                            );

                        } else {
                            Log.e(TAG, "Failed to load rooms: " + res.code());
                            Toast.makeText(getContext(),
                                    "Failed to load chat rooms",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            retrofit2.Call<java.util.List<ChatRoomDto>> call,
                            Throwable t){
                        Log.e(TAG, "Network error", t);
                        Toast.makeText(getContext(),
                                "Network error: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        return v;
    }

    private String buildUserName(ChatRoomDto dto) {
        if(dto.user == null) {
            return "Unknown User";
        }

        String firstName = dto.user.firstName;
        String lastName = dto.user.lastName;

        // Handle null values
        if(firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if(firstName != null) {
            return firstName;
        } else if(lastName != null) {
            return lastName;
        } else if(dto.user.email != null) {
            return dto.user.email; // Fallback to email
        } else {
            return "User " + dto.user.id; // Ultimate fallback
        }
    }

    private void openChat(Long userId, String userName){
        Log.d(TAG, "openChat called for: " + userName + " (ID: " + userId + ")");

        ChatFragment f = ChatFragment.forUser(userId, userName);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, f)
                .addToBackStack(null)
                .commit();
    }
}