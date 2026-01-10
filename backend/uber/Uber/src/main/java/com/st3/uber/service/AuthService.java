package com.st3.uber.service;

import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.User;
import com.st3.uber.domain.VerificationToken;
import com.st3.uber.dto.auth.ForgotPasswordRequest;
import com.st3.uber.dto.auth.LoginRequest;
import com.st3.uber.dto.auth.LoginResponse;
import com.st3.uber.dto.auth.RegisterPassengerRequest;
import com.st3.uber.enums.UserRole;
import com.st3.uber.enums.VerificationTokenType;
import com.st3.uber.exception.TokenAlreadyUsedException;
import com.st3.uber.exception.TokenExpiredException;
import com.st3.uber.exception.TokenInvalidException;
import com.st3.uber.repository.UserRepository;
import com.st3.uber.repository.VerificationTokenRepository;
import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.Base64.getDecoder;

@Service
public class AuthService {
  private final UserRepository userRepository;
  private final MailService mailService;
  private final VerificationTokenRepository tokenRepository;

  @Value("${app.backend.url}")
  private String backendUrl;

  public AuthService(UserRepository userRepository, MailService mailService, VerificationTokenRepository tokenRepository) {
    this.userRepository = userRepository;
    this.mailService = mailService;
    this.tokenRepository = tokenRepository;

  }

  public LoginResponse login(LoginRequest req) {
    User u = userRepository.findByEmail(req.email())
        .orElseThrow(() -> new RuntimeException("Invalid credentials"));

    if (u.isBlocked()) {
      throw new RuntimeException("User is blocked");
    }

    if (!req.password().equals(u.getPassword())) {
      throw new RuntimeException("Invalid credentials");
    }

    if (u instanceof Passenger passenger) {
      if (!passenger.isVerified()) {
        throw new RuntimeException("Email not verified");
      }
    }
    String role = u.getClass().getSimpleName().toUpperCase();

    return new LoginResponse(u.getId(), u.getEmail(), role);
  }

  @SneakyThrows
  @Transactional
  public Passenger createPassenger(RegisterPassengerRequest req) {
    if (userRepository.existsByEmail(req.getEmail())) {
      throw new ResponseStatusException(HttpStatusCode.valueOf(400), "Email already in use");
    }

    Passenger p = new Passenger();
    p.setEmail(req.getEmail());
    p.setPassword(req.getPassword());
    p.setName(req.getName());
    p.setSurname(req.getSurname());
    p.setPhoneNumber(req.getPhoneNumber());
    p.setAddress(req.getAddress());
    p.setVerified(false);
    p.setRole(UserRole.PASSENGER);

    if (req.getBase64Image() != null) {
      String fileName = UUID.randomUUID() + "." + req.getExtension();

      p.setImagePath("uploads/" + fileName);

      byte[] imageBytes = getDecoder().decode(req.getBase64Image());
      Files.write(Path.of("uploads/" + fileName), imageBytes);
    }

    userRepository.save(p);
    String token = UUID.randomUUID().toString();

    VerificationToken verificationToken = new VerificationToken();
    verificationToken.setToken(token);
    verificationToken.setPassenger(p);
    verificationToken.setUsed(false);
    verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
    verificationToken.setTokenType(VerificationTokenType.EMAIL_VERIFICATION);
    tokenRepository.save(verificationToken);

      String link = backendUrl + "/api/auth/verify?token=" + token;
      String subject = "Email verification";
      String body = "Thank you for registering! Please verify your email by clicking the button below.";
      mailService.sendText(
              p.getEmail(),
              subject,
              body + "\n" + link
      );

    return p;
  }

  @Transactional
  public void verifyToken(String token) {
    VerificationToken vt = tokenRepository.findByToken(token)
        .orElseThrow(() -> new TokenInvalidException("Invalid token"));

    if (vt.isUsed())
      throw new TokenAlreadyUsedException("Token already used");

    if (vt.getExpiresAt().isBefore(LocalDateTime.now()))
      throw new TokenExpiredException("Token expired");

    Passenger passenger = vt.getPassenger();
    passenger.setVerified(true);

    vt.setUsed(true);

    userRepository.save(passenger);
    tokenRepository.save(vt);
  }

  public void forgotPassword(ForgotPasswordRequest req) {
    // TODO if fails return EntityResponse with error message

    String token = UUID.randomUUID().toString();
    Passenger p = userRepository.findByEmail(req.getEmail())
        .filter(user -> user instanceof Passenger)
        .map(user -> (Passenger) user)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    VerificationToken verificationToken = new VerificationToken();
    verificationToken.setToken(token);
    verificationToken.setPassenger(p);
    verificationToken.setUsed(false);
    verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
    verificationToken.setTokenType(VerificationTokenType.PASSWORD_RESET);
    tokenRepository.save(verificationToken);

    String link = backendUrl + "/api/auth/reset?token=" + token;
    String subject = "Reset your password";
    String body = "Click the link below to reset your password.";
    mailService.sendText(
        p.getEmail(),
        subject,
        body + "\n" + link
    );

    ResponseEntity.ok().build();
  }


}
