package com.st3.uber.repository;

import com.st3.uber.domain.User;
import com.st3.uber.domain.VerificationToken;
import com.st3.uber.enums.VerificationTokenType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    @Modifying
    @Transactional
    void deleteByUserAndTokenType(User user, VerificationTokenType tokenType);

}
