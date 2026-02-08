package com.st3.uber.service;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.DriverProfileChangeRequest;
import com.st3.uber.dto.user.admin.AdminDriverProfileChangeRequestDto;
import com.st3.uber.dto.user.driver.DriverProfileChangeRequestDto;
import com.st3.uber.enums.ChangeRequestStatus;
import com.st3.uber.exception.PendingProfileChangeRequestException;
import com.st3.uber.repository.DriverProfileChangeRequestRepository;
import com.st3.uber.repository.DriverRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.st3.uber.dto.user.admin.AdminDriverProfileChangeRequestDetailsDto;
import com.st3.uber.domain.Vehicle;
import com.st3.uber.dto.user.admin.AdminProfileChangeDecisionDto;


import java.time.LocalDateTime;
import java.util.List;

@Service
public class DriverProfileChangeRequestService {

    private final DriverRepository driverRepository;
    private final DriverProfileChangeRequestRepository changeRequestRepository;
    private final ImageStorageService imageStorageService;


    public DriverProfileChangeRequestService(
            DriverRepository driverRepository,
            DriverProfileChangeRequestRepository changeRequestRepository, ImageStorageService imageStorageService
    ) {
        this.driverRepository = driverRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional
    public void submitChangeRequest(
            Long driverId,
            DriverProfileChangeRequestDto dto
    ) {

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        boolean hasPending = changeRequestRepository
                .findByDriverAndStatus(driver, ChangeRequestStatus.PENDING)
                .isPresent();

        if (hasPending) {
            throw new PendingProfileChangeRequestException(
                    "You already have a pending profile change request"
            );
        }

        DriverProfileChangeRequest request = new DriverProfileChangeRequest();
        request.setDriver(driver);

        request.setFirstName(dto.firstName());
        request.setLastName(dto.lastName());
        request.setPhoneNumber(dto.phoneNumber());
        request.setAddress(dto.address());
        request.setProfileImage(dto.profileImage());

        request.setVehicleModel(dto.vehicleModel());
        request.setVehicleRegistrationNumber(dto.vehicleRegistrationNumber());
        request.setVehicleSeatingCapacity(dto.vehicleSeatingCapacity());
        request.setVehicleType(dto.vehicleType());
        request.setBabyTransport(dto.babyTransport());
        request.setPetTransport(dto.petTransport());

        request.setStatus(ChangeRequestStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());

        if (dto.profileImage() != null && !dto.profileImage().startsWith("/uploads/")) {
            throw new IllegalArgumentException("Invalid profile image path");
        }

        changeRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<AdminDriverProfileChangeRequestDto> getPendingRequests() {

        return changeRequestRepository
                .findAllByStatus(ChangeRequestStatus.PENDING)
                .stream()
                .map(req -> new AdminDriverProfileChangeRequestDto(
                        req.getId(),
                        req.getDriver().getId(),
                        req.getDriver().getEmail(),
                        req.getDriver().getName(),
                        req.getDriver().getSurname(),
                        req.getRequestedAt(),
                        req.getStatus().name()
                ))
                .toList();
    }


    @Transactional(readOnly = true)
    public AdminDriverProfileChangeRequestDetailsDto getRequestDetails(Long requestId) {

        DriverProfileChangeRequest req = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Change request not found"));

        Driver driver = req.getDriver();
        Vehicle vehicle = driver.getVehicle();

        return new AdminDriverProfileChangeRequestDetailsDto(

                // ===== META =====
                req.getId(),
                req.getStatus().name(),
                req.getRequestedAt(),

                // ===== DRIVER INFO =====
                driver.getId(),
                driver.getEmail(),
                driver.getName(),
                driver.getSurname(),

                // ===== OLD USER DATA =====
                driver.getName(),
                driver.getSurname(),
                driver.getPhoneNumber(),
                driver.getAddress(),

                // ===== NEW USER DATA =====
                req.getFirstName(),
                req.getLastName(),
                req.getPhoneNumber(),
                req.getAddress(),

                // ===== OLD VEHICLE DATA =====
                vehicle.getModel(),
                vehicle.getRegistrationNumber(),
                vehicle.getSeatingCapacity(),
                vehicle.getType(),
                vehicle.isBabyTransport(),
                vehicle.isPetTransport(),

                // ===== NEW VEHICLE DATA =====
                req.getVehicleModel(),
                req.getVehicleRegistrationNumber(),
                req.getVehicleSeatingCapacity(),
                req.getVehicleType(),
                req.getBabyTransport(),
                req.getPetTransport()
        );
    }

    @Transactional
    public void decideRequest(
            Long requestId,
            AdminProfileChangeDecisionDto decision
    ) {

        DriverProfileChangeRequest req = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Change request not found"));

        if (req.getStatus() != ChangeRequestStatus.PENDING) {
            throw new RuntimeException("Request already processed");
        }

        if (decision.approved()) {

            Driver driver = req.getDriver();
            Vehicle vehicle = driver.getVehicle();

            if (req.getFirstName() != null) {
                driver.setName(req.getFirstName());
            }
            if (req.getLastName() != null) {
                driver.setSurname(req.getLastName());
            }
            if (req.getPhoneNumber() != null) {
                driver.setPhoneNumber(req.getPhoneNumber());
            }
            if (req.getAddress() != null) {
                driver.setAddress(req.getAddress());
            }
            if (req.getProfileImage() != null) {
                driver.setProfileImage(req.getProfileImage());
            }
            if (req.getVehicleModel() != null) {
                vehicle.setModel(req.getVehicleModel());
            }
            if (req.getVehicleRegistrationNumber() != null) {
                vehicle.setRegistrationNumber(req.getVehicleRegistrationNumber());
            }
            if (req.getVehicleSeatingCapacity() != null) {
                vehicle.setSeatingCapacity(req.getVehicleSeatingCapacity());
            }
            if (req.getVehicleType() != null) {
                vehicle.setType(req.getVehicleType());
            }
            if (req.getBabyTransport() != null) {
                vehicle.setBabyTransport(req.getBabyTransport());
            }
            if (req.getPetTransport() != null) {
                vehicle.setPetTransport(req.getPetTransport());
            }
            req.setStatus(ChangeRequestStatus.APPROVED);
            req.setReviewedAt(LocalDateTime.now());

        } else {

            if (req.getProfileImage() != null) {
                imageStorageService.delete(req.getProfileImage());
            }
            req.setStatus(ChangeRequestStatus.REJECTED);
            req.setReviewedAt(LocalDateTime.now());
            req.setRejectionReason(decision.rejectReason());

        }

        changeRequestRepository.save(req);
    }


    @Transactional(readOnly = true)
    public List<AdminDriverProfileChangeRequestDto> getAllRequests() {

        return changeRequestRepository
                .findAll()  // <-- Vraća SVE zahteve
                .stream()
                .map(req -> new AdminDriverProfileChangeRequestDto(
                        req.getId(),
                        req.getDriver().getId(),
                        req.getDriver().getEmail(),
                        req.getDriver().getName(),
                        req.getDriver().getSurname(),
                        req.getRequestedAt(),
                        req.getStatus().name()
                ))
                .toList();
    }





}
