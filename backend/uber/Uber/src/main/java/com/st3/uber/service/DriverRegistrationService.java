package com.st3.uber.service;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.Vehicle;
import com.st3.uber.domain.VerificationToken;
import com.st3.uber.dto.register.RegisterDriverRequest;
import com.st3.uber.dto.register.RegisterDriverResponse;
import com.st3.uber.dto.vehicle.VehicleResponse;
import com.st3.uber.enums.UserRole;
import com.st3.uber.enums.VehicleType;
import com.st3.uber.enums.VerificationTokenType;
import com.st3.uber.repository.DriverRepository;
import com.st3.uber.repository.UserRepository;
import com.st3.uber.repository.VehicleRepository;
import com.st3.uber.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DriverRegistrationService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public DriverRegistrationService(
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository,
            UserRepository userRepository,
            VerificationTokenRepository tokenRepository,
            MailService mailService,
            PasswordEncoder passwordEncoder
    ) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
    }

    public RegisterDriverResponse register(RegisterDriverRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (vehicleRepository.existsByRegistrationNumber(
                request.request().registrationNumber()
        )) {
            throw new IllegalArgumentException("Vehicle with this registration already exists");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setModel(request.request().model());
        vehicle.setType(
                request.request().type() != null
                        ? request.request().type()
                        : VehicleType.STANDARD
        );
        vehicle.setRegistrationNumber(request.request().registrationNumber());
        vehicle.setSeatingCapacity(request.request().seatingCapacity());
        vehicle.setBabyTransport(request.request().babyTransport());
        vehicle.setPetTransport(request.request().petTransport());

        Driver driver = new Driver();
        driver.setEmail(request.email());

        driver.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        driver.setName(request.firstName());
        driver.setSurname(request.lastName());
        driver.setPhoneNumber(request.phoneNumber());
        driver.setAddress(request.address());

        driver.setActive(false);
        driver.setAvailable(false);
        driver.setFree(false);

        driver.setVehicle(vehicle);
        driver.setRole(UserRole.DRIVER);

        driver = driverRepository.save(driver);

        String token = UUID.randomUUID().toString();

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(driver);
        verificationToken.setUsed(false);
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        verificationToken.setTokenType(VerificationTokenType.PASSWORD_RESET);

        tokenRepository.save(verificationToken);

        String link = frontendUrl + "/reset-password?token=" + token + "&mode=activate";

        String subject = "Activate your driver account";
        String body =
                "An administrator has created your driver account.\n\n" +
                        "Please click the link below to set your password.\n" +
                        "This link is valid for 24 hours.\n\n" +
                        link;

        mailService.sendText(
                driver.getEmail(),
                subject,
                body
        );

        Vehicle savedVehicle = driver.getVehicle();

        // 8️⃣ response
        return new RegisterDriverResponse(
                driver.getId(),
                driver.getEmail(),
                driver.getName(),
                driver.getSurname(),
                new VehicleResponse(
                        savedVehicle.getId(),
                        savedVehicle.getModel(),
                        savedVehicle.getType(),
                        savedVehicle.getRegistrationNumber(),
                        savedVehicle.getSeatingCapacity(),
                        savedVehicle.isBabyTransport(),
                        savedVehicle.isPetTransport()
                ),
                driver.isActive()
        );
    }
}
