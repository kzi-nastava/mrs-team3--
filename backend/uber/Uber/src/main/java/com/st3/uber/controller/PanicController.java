package com.st3.uber.controller;

import com.st3.uber.domain.Ride;
import com.st3.uber.domain.RideInvite;
import com.st3.uber.repository.RideInviteRepository;
import com.st3.uber.service.PanicService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/ride-tracking")
public class PanicController {

  private PanicService panicService;
  private RideInviteRepository rideInviteRepository;
  public PanicController(PanicService panicService, RideInviteRepository rideInviteRepository) {
    this.panicService = panicService;
    this.rideInviteRepository = rideInviteRepository;
  }

  @PostMapping("/{rideId}/panic")
  void handlePanicEvent(@PathVariable Long rideId,
                        @AuthenticationPrincipal Jwt jwt) {
    Long id = jwt.getClaim("uid");
    panicService.handlePanicPressed(rideId, id);
  }

  @PostMapping("/token/{token}/panic")
  void handlePanicEventWithToken(@PathVariable String token) {
    panicService.handleGuestPanicPressed(token);
  }
}
