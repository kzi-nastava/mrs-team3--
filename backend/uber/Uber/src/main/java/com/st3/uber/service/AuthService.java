package com.st3.uber.service;

import com.st3.uber.domain.*;
import com.st3.uber.dto.auth.*;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

  import static java.util.Base64.getDecoder;

@Service
public class AuthService {
  private final UserRepository userRepository;
  private final MailService mailService;
  private final VerificationTokenRepository tokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtEncoder jwtEncoder;

  @Value("${app.backend.url}")
  private String backendUrl;

  @Value("${app.frontend.url}")
  private String frontendUrl;

  @Value("${jwt.ttl-seconds:3600}")
  private long jwtTtlSeconds;

  public AuthService(UserRepository userRepository, MailService mailService, VerificationTokenRepository tokenRepository,
                     PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder) {
    this.userRepository = userRepository;
    this.mailService = mailService;
    this.tokenRepository = tokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtEncoder = jwtEncoder;
  }

  @Transactional
  public LoginResponse login(LoginRequest req) {
    User u = userRepository.findByEmail(req.email())
        .orElseThrow(() -> new RuntimeException("Invalid credentials"));

    if (u.isBlocked()) {
      throw new RuntimeException("User is blocked");
    }

    boolean ok = passwordEncoder.matches(req.password(), u.getPassword());
    if (!ok) throw new RuntimeException("Bad credentials");

    if (u instanceof Passenger passenger) {
      if (!passenger.isVerified()) {
        throw new RuntimeException("Email not verified");
      }
    }

    if(u instanceof Driver driver){
      driver.setAvailable(true);
      driver.setFree(true);
      driver.setActive(true);

      userRepository.save(driver);
    }
   // String role2 = u.getClass().getSimpleName().toUpperCase();
    String role = u.getRole().name();

    String token = generateToken(u, role);
    return new LoginResponse(u.getId(), u.getEmail(), role, token);
  }

  private String generateToken(User u, String role) {
    Instant now = Instant.now();

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("st3-uber")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(jwtTtlSeconds))
        .subject(u.getEmail())
        .claim("uid", u.getId())
        .claim("role", role)
        .build();

    return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }

  @SneakyThrows
  @Transactional
  public Passenger createPassenger(RegisterPassengerRequest req) {
    if (userRepository.existsByEmail(req.getEmail())) {
      throw new ResponseStatusException(HttpStatusCode.valueOf(400), "Email already in use");
    }

    String hashedPassword = passwordEncoder.encode(req.getPassword());
    Passenger p = new Passenger();
    p.setEmail(req.getEmail());
    p.setPassword(hashedPassword);
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
    verificationToken.setUser(p);
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
  public VerificationToken checkTokenValidity(String token) {
    VerificationToken vt = tokenRepository.findByToken(token)
        .orElseThrow(() -> new TokenInvalidException("Invalid token"));

    if (vt.isUsed())
      throw new TokenAlreadyUsedException("Token already used");

    if (vt.getExpiresAt().isBefore(LocalDateTime.now()))
      throw new TokenExpiredException("Token expired");
    return vt;
  }

  @Transactional
  public void verifyToken(String token) {
    VerificationToken vt = checkTokenValidity(token);

    User u = vt.getUser();

    if(vt.getTokenType() == VerificationTokenType.EMAIL_VERIFICATION){
      u.setVerified(true);
    }

    vt.setUsed(true);

    userRepository.save(u);
    tokenRepository.save(vt);
  }

  @Transactional
  public ResponseEntity<Void> forgotPassword(ForgotPasswordRequest req) {
    String token = UUID.randomUUID().toString();
    User p = userRepository.findByEmail(req.getEmail())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    VerificationToken verificationToken = new VerificationToken();
    verificationToken.setToken(token);
    verificationToken.setUser(p);
    verificationToken.setUsed(false);
    verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
    verificationToken.setTokenType(VerificationTokenType.PASSWORD_RESET);
    tokenRepository.save(verificationToken);

    String link = backendUrl + "/api/auth/reset-password/verify?token=" + token;
    String subject = "Reset your password";
    String body = "Click the link below to reset your password.";
    mailService.sendText(
        p.getEmail(),
        subject,
        body + "\n" + link
    );

    return ResponseEntity.ok().build();
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest req) {

      VerificationToken vt = checkTokenValidity(req.getToken());

      if (vt.getTokenType() != VerificationTokenType.PASSWORD_RESET) {
          throw new TokenInvalidException("Invalid token type");
      }

      User u = vt.getUser();

      u.setPassword(passwordEncoder.encode(req.getNewPassword()));

      if (u instanceof Driver driver) {
          driver.setActive(true);
      }

      vt.setUsed(true);

      userRepository.save(u);
      tokenRepository.save(vt);
  }
}
