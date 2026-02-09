package com.example.uber3.network.model.user;

public class BlockUserRequest {
    public boolean blocked;
    public String reason;

    public BlockUserRequest(boolean blocked, String reason) {
        this.blocked = blocked;
        this.reason = reason;
    }
}
