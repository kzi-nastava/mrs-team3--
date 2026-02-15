package com.st3.uber.service;

import com.st3.uber.domain.Driver;
import com.st3.uber.repository.DriverRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DailyResetService {

    private final DriverRepository driverRepository;

    public DailyResetService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void resetWorkingMinutes(){
        List<Driver> drivers = driverRepository.findAll();

        for (Driver d : drivers) {
            d.setWorkingMinutesPerDay(0);
        }

        driverRepository.saveAll(drivers);

        System.out.println("🔥 Daily reset executed");
    }
}
