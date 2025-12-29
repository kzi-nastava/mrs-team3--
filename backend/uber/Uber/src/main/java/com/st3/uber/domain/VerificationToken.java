package com.st3.uber.domain;

import com.st3.uber.enums.VerificationTokenType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "verification_tokens")

public class VerificationToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  @Column(nullable = false)
  private boolean used = false;

  @Column(nullable = false)
  private VerificationTokenType tokenType;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "passenger_id", nullable = false, unique = true)
  private Passenger passenger;

}
