package com.st3.uber.dto.ride;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InconsistencyReportItemResponse {
  Long id;
  String reportText;
  LocalDateTime createdAt;
}
