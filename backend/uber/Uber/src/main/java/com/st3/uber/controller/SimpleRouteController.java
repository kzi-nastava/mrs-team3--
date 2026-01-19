package com.st3.uber.controller;

import com.st3.uber.domain.Location;
import com.st3.uber.dto.route.CalculateSimpleRouteRequest;
import com.st3.uber.dto.route.RouteInfo;
import com.st3.uber.service.RouteCalculationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/simple-routes")
public class SimpleRouteController {
  private final RouteCalculationService routeCalculationService;

  public SimpleRouteController(RouteCalculationService routeCalculationService) {
    this.routeCalculationService = routeCalculationService;
  }

  @PostMapping("/time")
  public RouteInfo time(@RequestBody CalculateSimpleRouteRequest req) {
    Location start = req.getStartLocation();
    Location end = req.getEndLocation();

    return routeCalculationService.calculateRoute(start, end, null);
  }
}
