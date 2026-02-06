package com.example.uber3;

import android.os.Bundle;
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

    private RecyclerView recycler;
    private List<AdminChatRoom> rooms = new ArrayList<>();

    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState){

        View v = inflater.inflate(
                R.layout.fragment_admin_chat_list,
                container,false);

        recycler = v.findViewById(R.id.recyclerRooms);
        recycler.setLayoutManager(
                new LinearLayoutManager(getContext()));

        ChatApiService api =
                ApiClient.getClient(requireContext())
                        .create(ChatApiService.class);

        Long adminId =
                TokenManager.getUserId(requireContext());

        api.getAdminRooms(adminId)
                .enqueue(new retrofit2.Callback<java.util.List<ChatRoomDto>>() {

                    @Override
                    public void onResponse(
                            retrofit2.Call<java.util.List<ChatRoomDto>> call,
                            retrofit2.Response<java.util.List<ChatRoomDto>> res){

                        if(res.isSuccessful() && res.body()!=null){

                            for(ChatRoomDto dto : res.body()){
                                rooms.add(
                                        new AdminChatRoom(dto.user.id)
                                );
                            }

                            recycler.setAdapter(
                                    new AdminChatAdapter(
                                            rooms,
                                            room -> openChat(room.userId)
                                    )
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            retrofit2.Call<java.util.List<ChatRoomDto>> call,
                            Throwable t){
                        t.printStackTrace();
                    }
                });


        recycler.setAdapter(
                new AdminChatAdapter(rooms, room -> {
                    ChatFragment f =
                            ChatFragment.forUser(room.userId);

                    requireActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainer,f)
                            .commit();
                })
        );

        return v;
    }

    private void openChat(Long userId){
        ChatFragment f =
                ChatFragment.forUser(userId);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer,f)
                .addToBackStack(null)
                .commit();

    }
}
