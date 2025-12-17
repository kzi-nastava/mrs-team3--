package com.st3.uber.controller;

import com.st3.uber.domain.Passenger;
import com.st3.uber.dto.auth.LoginRequest;
import com.st3.uber.dto.auth.LoginResponse;
import com.st3.uber.dto.auth.RegisterPassengerRequest;
import com.st3.uber.service.UserService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
//@RequestMapping()
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register/passenger")
    public Passenger registerPassenger(@RequestBody RegisterPassengerRequest req) {
        return userService.createPassenger(req);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        return userService.login(req);
    }

    @PostMapping("/hello")
    public String hello() {
        return "Hello, world!";
    }
}
