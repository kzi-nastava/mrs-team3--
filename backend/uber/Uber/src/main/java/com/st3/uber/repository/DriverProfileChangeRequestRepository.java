package com.st3.uber.repository;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.DriverProfileChangeRequest;
import com.st3.uber.enums.ChangeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverProfileChangeRequestRepository
        extends JpaRepository<DriverProfileChangeRequest, Long> {

    Optional<DriverProfileChangeRequest>
    findByDriverAndStatus(Driver driver, ChangeRequestStatus status);

    List<DriverProfileChangeRequest>
    findAllByStatus(ChangeRequestStatus status);
}
