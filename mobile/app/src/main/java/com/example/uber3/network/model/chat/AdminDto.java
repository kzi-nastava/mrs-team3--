package com.example.uber3.network.model.chat;

public class AdminDto {
    public Long id;
    public String name;
    public String role;

    public AdminDto() {}

    public AdminDto(Long id, String name, String role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }
}