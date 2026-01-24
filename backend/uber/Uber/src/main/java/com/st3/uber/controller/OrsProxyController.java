package com.st3.uber.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/simple-routes")
public class OrsProxyController {

  @Value("${ors.api.key}")
  private String orsKey;

  private final RestTemplate restTemplate = new RestTemplate();
  @PostMapping(value = "/route", produces = "application/geo+json")

  public ResponseEntity<String> route(
      @RequestParam(defaultValue = "driving-car") String profile,
      @RequestBody Map<String, Object> body
  ) {
    String url = "https://api.openrouteservice.org/v2/directions/" + profile + "/geojson";

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", orsKey);
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.valueOf("application/geo+json")));

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
      return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    } catch (HttpStatusCodeException e) {
      // Pass through ORS error response
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("{\"error\":\"Proxy failed\",\"message\":\"" + e.getMessage() + "\"}");
    }
  }

}
