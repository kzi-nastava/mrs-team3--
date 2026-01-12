package com.st3.uber.dto.auth;

import lombok.Data;

@Data
public class ResetPasswordRequest {
  private String token;
  private String newPassword;
}
