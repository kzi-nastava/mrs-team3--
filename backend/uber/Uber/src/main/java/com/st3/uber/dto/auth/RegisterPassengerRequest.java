package com.st3.uber.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterPassengerRequest{
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
        private String password;

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
        private String name;

        @NotBlank(message = "Surname is required")
        @Size(min = 2, max = 50, message = "Surname must be between 2 and 50 characters")
        private String surname;

        @NotBlank(message = "Phone number is required")
        @Pattern(
            regexp = "^[0-9+\\-() ]{6,20}$",
            message = "Phone number format is invalid"
        )
        private String phoneNumber;

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address cannot exceed 255 characters")
        private String address;

        private String base64Image;

        @Pattern(
            regexp = "^(jpg|jpeg|png)$",
            message = "Allowed extensions are jpg, jpeg, png"
        )
        private String extension;
}

