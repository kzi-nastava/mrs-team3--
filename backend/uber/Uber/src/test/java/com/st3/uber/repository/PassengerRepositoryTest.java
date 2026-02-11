package com.st3.uber.repository;

import com.st3.uber.domain.Passenger;
import com.st3.uber.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PassengerRepositoryTest {

    @Autowired
    private PassengerRepository passengerRepository;

    private Passenger buildPassenger(String email) {
        Passenger p = new Passenger();

        p.setEmail(email);
        p.setPassword("pass123");
        p.setName("Marko");
        p.setSurname("Markovic");
        p.setPhoneNumber("123456");
        p.setAddress("Test address");
        p.setRole(UserRole.PASSENGER);
        p.setVerified(true);
        p.setBlocked(false);

        return p;
    }

    @Test
    void findByEmail_shouldReturnPassenger_whenExists() {

        Passenger p = buildPassenger("test@gmail.com");
        passengerRepository.save(p);

        Optional<Passenger> found =
                passengerRepository.findByEmail("test@gmail.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@gmail.com");
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenNotExists() {

        Optional<Passenger> found =
                passengerRepository.findByEmail("none@gmail.com");

        assertThat(found).isEmpty();
    }
}
