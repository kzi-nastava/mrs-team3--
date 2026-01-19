package com.st3.uber.repository;

import com.st3.uber.domain.RidePricing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RidePricingRepository extends JpaRepository<RidePricing, Long> {
}
