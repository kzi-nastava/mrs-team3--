package com.st3.uber.dto.chat;

import com.st3.uber.domain.ChatRoom;

public class ChatRoomDto {

    public Long id;
    public UserDto user;

    public ChatRoomDto(ChatRoom r){
        this.id = r.getId();
        this.user = new UserDto(
                r.getUser().getId(),
                r.getUser().getEmail()
        );
    }

    public static class UserDto{
        public Long id;
        public String email;

        public UserDto(Long id,String email){
            this.id=id;
            this.email=email;
        }
    }
}
