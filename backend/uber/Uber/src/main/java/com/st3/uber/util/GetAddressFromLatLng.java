package com.st3.uber.util;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

public class GetAddressFromLatLng {
  public static String addressFromLatLng(double lat, double lng) {
    String url = "https://nominatim.openstreetmap.org/reverse?lat="
        + lat + "&lon=" + lng + "&format=json";

    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", "ride-app");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    ResponseEntity<Map> response = restTemplate.exchange(
        url, HttpMethod.GET, entity, Map.class);

    return response.getBody().get("display_name").toString();
  }

}
