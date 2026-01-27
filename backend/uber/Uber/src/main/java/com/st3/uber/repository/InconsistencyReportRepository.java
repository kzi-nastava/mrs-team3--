package com.st3.uber.repository;

import com.st3.uber.domain.InconsistencyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InconsistencyReportRepository extends JpaRepository<InconsistencyReport, Long> {
}