package com.example.uber3.network.model;

public class ChatRoomDto {

    public Long id;
    public UserDto user;

    public static class UserDto{
        public Long id;
        public String email;
        public String firstName;
        public String lastName;
    }
}