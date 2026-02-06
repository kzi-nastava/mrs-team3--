package com.st3.uber.repository;

import com.st3.uber.domain.User;
import com.st3.uber.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);
    Optional<User> findById(Long id);
}
