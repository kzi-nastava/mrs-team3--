package com.st3.uber.controller;

import com.st3.uber.dto.route.FavoriteRouteRequest;
import com.st3.uber.dto.route.FavoriteRouteResponse;
import com.st3.uber.service.FavoriteRouteService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "http://localhost:4200")
public class FavoriteRouteController {

    private final FavoriteRouteService favoriteRouteService;

    public FavoriteRouteController(FavoriteRouteService favoriteRouteService) {
        this.favoriteRouteService = favoriteRouteService;
    }

    @GetMapping
    @RolesAllowed("PASSENGER")
    public ResponseEntity<List<FavoriteRouteResponse>> getAll(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String email = jwt.getSubject();
        return ResponseEntity.ok(
                favoriteRouteService.getAllByEmail(email)
        );
    }

    @PostMapping
    @RolesAllowed("PASSENGER")
    public ResponseEntity<Void> add(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody FavoriteRouteRequest request
    ) {
        String email = jwt.getSubject();
        favoriteRouteService.addByEmail(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping
    @RolesAllowed("PASSENGER")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody FavoriteRouteRequest request
    ) {
        String email = jwt.getSubject();
        favoriteRouteService.removeByEmail(email, request);
        return ResponseEntity.noContent().build();
    }
}
