package com.st3.uber.util;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

public class GetAddressFromLatLng {

  public static String addressFromLatLng(double lat, double lng) {
    if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
      return "Unknown Location";
    }

    String url = "https://nominatim.openstreetmap.org/reverse?lat="
            + lat + "&lon=" + lng + "&format=json";

    try {
      RestTemplate restTemplate = new RestTemplate();
      HttpHeaders headers = new HttpHeaders();
      headers.set("User-Agent", "ride-app");
      HttpEntity<String> entity = new HttpEntity<>(headers);

      ResponseEntity<Map> response = restTemplate.exchange(
              url, HttpMethod.GET, entity, Map.class);

      Map body = response.getBody();
      if (body != null && body.containsKey("display_name")) {
        return body.get("display_name").toString();
      }

      return "Unknown Location";

    } catch (Exception e) {
      System.err.println("Address lookup failed: " + e.getMessage());
      return "Unknown Location";
    }
  }
}