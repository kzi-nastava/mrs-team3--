package com.st3.uber.dto.register;

import com.st3.uber.dto.vehicle.VehicleRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record RegisterDriverRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email is not valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must have at least 6 characters")
        String password,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Phone number is required")
        String phoneNumber,

        @NotBlank(message = "Address is required")
        String address,

        @NotNull(message = "Vehicle is required")
        @Valid
        VehicleRequest request
) {}
