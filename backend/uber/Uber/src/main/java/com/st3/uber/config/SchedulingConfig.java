package com.st3.uber.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable Spring's scheduled task execution
 * This allows @Scheduled annotations to work
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}