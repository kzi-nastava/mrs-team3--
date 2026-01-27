package com.st3.uber.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.CrossOriginResourcePolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.core.convert.converter.Converter;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // =========================
    // PASSWORD ENCODER
    // =========================
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =========================
    // SECURITY FILTER CHAIN
    // =========================
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ---- CSRF ----
                .csrf(AbstractHttpConfigurer::disable)

                // ---- HEADERS (FIX ZA FIREFOX IMAGE BLOCKING) ----
                .headers(headers -> headers
                        .crossOriginResourcePolicy(policy ->
                                policy.policy(
                                        CrossOriginResourcePolicyHeaderWriter
                                                .CrossOriginResourcePolicy.CROSS_ORIGIN
                                )
                        )
                )

                // ---- CORS ----
                .cors(Customizer.withDefaults())

                // ---- SESSION ----
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ---- AUTHORIZATION ----
                .authorizeHttpRequests(auth -> auth

                        // 🔓 STATIC FILES (PROFILE IMAGES)
                        .requestMatchers("/uploads/**").permitAll()

                        // 🔓 AUTH & WS
                        .requestMatchers("/api/auth/**", "/ws/**").permitAll()

                        // 🔓 PUBLIC ENDPOINTS
                        .requestMatchers("/api/vehicles/**").permitAll()
                        .requestMatchers("/simple-routes/**").permitAll()
                        .requestMatchers(
                                "/api/ride-tracking/validate/**",
                                "/api/ride-tracking/token/**"
                        ).permitAll()

                        // 🔓 PREFLIGHT
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 🔒 EVERYTHING ELSE
                        .anyRequest().authenticated()
                )

                // ---- JWT ----
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    // =========================
    // JWT ROLE CONVERTER
    // =========================
    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            String role = jwt.getClaimAsString("role"); // ADMIN / DRIVER / PASSENGER

            var authorities = (role == null)
                    ? List.<SimpleGrantedAuthority>of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + role));

            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    // =========================
    // CORS CONFIG
    // =========================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
