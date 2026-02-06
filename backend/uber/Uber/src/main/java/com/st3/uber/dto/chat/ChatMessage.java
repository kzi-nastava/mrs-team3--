package com.st3.uber.dto.chat;

public class ChatMessage {

    private Long fromUserId;
    private Long toUserId;
    private String content;
    private String timestamp;


    public ChatMessage() {}

    public ChatMessage(Long fromUserId, Long toUserId, String content, String timestamp) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }

    public Long getToUserId() { return toUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
