package com.st3.uber.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

  @NotBlank(message = "Token is required")
  private String token;

  @NotBlank(message = "New password is required")
  @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
  private String newPassword;
}
