package com.st3.uber.service;

import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.User;
import com.st3.uber.dto.auth.ForgotPasswordRequest;
import com.st3.uber.dto.auth.LoginRequest;
import com.st3.uber.dto.auth.LoginResponse;
import com.st3.uber.dto.auth.RegisterPassengerRequest;
import com.st3.uber.repository.UserRepository;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.EntityResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static java.util.Base64.getDecoder;

@Service
public class UserService {

    public UserService(UserRepository userRepository) {
    }


}
