package com.st3.uber.controller;

import com.st3.uber.dto.user.BlockUserRequest;
import com.st3.uber.dto.user.UserDto;
import com.st3.uber.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public List<UserDto> getAllUsers() {
        return userService.getAllUsers();
    }



    @PutMapping("/users/{id}/block")
    public UserDto blockUser(
            @PathVariable Long id,
            @RequestBody BlockUserRequest request
    ) {
        return userService.blockUser(id, request);
    }

}
