package com.example.uber3.network.model.chat;

public class ChatMessage {

    public Long fromUserId;
    public Long toUserId;
    public String content;
    public String timestamp;

    public ChatMessage(Long toUserId, String content) {
        this.toUserId = toUserId;
        this.content = content;
    }

    public ChatMessage() {}
}
