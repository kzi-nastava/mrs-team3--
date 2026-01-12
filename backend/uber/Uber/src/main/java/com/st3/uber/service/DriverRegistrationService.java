package com.st3.uber.service;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.Vehicle;
import com.st3.uber.dto.register.RegisterDriverRequest;
import com.st3.uber.dto.register.RegisterDriverResponse;
import com.st3.uber.dto.vehicle.VehicleResponse;
import com.st3.uber.enums.VehicleType;
import com.st3.uber.repository.DriverRepository;
import com.st3.uber.repository.UserRepository;
import com.st3.uber.repository.VehicleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DriverRegistrationService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DriverRegistrationService(
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public RegisterDriverResponse register(RegisterDriverRequest request) {

        // provera email-a
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // provera registracije vozila
        if (vehicleRepository.existsByRegistrationNumber(
                request.request().registrationNumber()
        )) {
            throw new IllegalArgumentException("Vehicle with this registration already exists");
        }

        // 3️⃣ kreiranje Vehicle
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

        // 4️⃣ kreiranje Driver-a
        Driver driver = new Driver();
        driver.setEmail(request.email());
        driver.setPassword(passwordEncoder.encode(request.password()));
        driver.setName(request.firstName());
        driver.setSurname(request.lastName());
        driver.setPhoneNumber(request.phoneNumber());
        driver.setAddress(request.address());

        driver.setActive(false);
        driver.setAvailable(false);
        driver.setFree(false);

        driver.setVehicle(vehicle); // 🔑 KLJUČNO

        // 5️⃣ save (Vehicle će se snimiti preko veze)
        driver = driverRepository.save(driver);

        Vehicle savedVehicle = driver.getVehicle();

        // 6️⃣ response
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
