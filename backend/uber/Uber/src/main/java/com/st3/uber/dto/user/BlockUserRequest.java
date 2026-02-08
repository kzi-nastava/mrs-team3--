package com.st3.uber.dto.user;

import lombok.Data;

@Data
public class BlockUserRequest {

    private boolean blocked;
    private String reason;

}
