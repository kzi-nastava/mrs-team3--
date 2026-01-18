package com.st3.uber.service;

import com.st3.uber.domain.Location;
import com.st3.uber.dto.route.RouteInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class RouteCalculationService {

    private final WebClient webClient;

    @Value("${ors.api.key}")
    private String orsApiKey;

    public RouteCalculationService(WebClient webClient) {
        this.webClient = webClient;
    }

    public RouteInfo calculateRoute(
            Location start,
            Location end,
            List<Location> stops
    ) {
        String coordinates = buildCoordinates(start, end, stops);

        Map response = webClient.post()
                .uri("https://api.openrouteservice.org/v2/directions/driving-car")
                .header("Authorization", orsApiKey)
                .header("Content-Type", "application/json")
                .bodyValue("""
                    {
                      "coordinates": %s
                    }
                    """.formatted(coordinates))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new IllegalStateException("ORS response is null");
        }

        Map route = (Map) ((List<?>) response.get("routes")).get(0);
        Map summary = (Map) route.get("summary");

        Number distanceMetersNum = (Number) summary.get("distance");
        Number durationSecondsNum = (Number) summary.get("duration");

        double distanceMeters = distanceMetersNum.doubleValue();
        double durationSeconds = durationSecondsNum.doubleValue();

        return new RouteInfo(
                distanceMeters / 1000.0,
                (int) Math.round(durationSeconds / 60)
        );
    }

    private String buildCoordinates(
            Location start,
            Location end,
            List<Location> stops
    ) {
        StringBuilder sb = new StringBuilder("[");
        sb.append(coord(start));

        if (stops != null) {
            for (Location stop : stops) {
                sb.append(",").append(coord(stop));
            }
        }

        sb.append(",").append(coord(end));
        sb.append("]");
        return sb.toString();
    }

    private String coord(Location l) {
        return "[%f,%f]".formatted(l.getLng(), l.getLat());
    }
}
