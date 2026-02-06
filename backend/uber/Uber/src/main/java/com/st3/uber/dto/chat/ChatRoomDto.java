package com.st3.uber.dto.chat;

import com.st3.uber.domain.ChatRoom;

public class ChatRoomDto {

    public Long id;
    public UserDto user;

    public ChatRoomDto(ChatRoom r){
        this.id = r.getId();
        this.user = new UserDto(
                r.getUser().getId(),
                r.getUser().getEmail(),
                r.getUser().getName(),
                r.getUser().getSurname()
        );
    }

    public static class UserDto{
        public Long id;
        public String email;
        public String firstName;
        public String lastName;

        public UserDto(Long id, String email, String firstName, String lastName){
            this.id = id;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }
}