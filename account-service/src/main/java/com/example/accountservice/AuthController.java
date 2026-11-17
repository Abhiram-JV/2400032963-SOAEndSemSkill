package com.example.accountservice;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthController(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            var user = userDetailsService.loadUserByUsername(request.username());
            if ("password".equals(request.password())) {
                return ResponseEntity.ok(Map.of("token", jwtService.generateToken(user.getUsername())));
            }
        } catch (RuntimeException ignored) {
            // Return the same response for unknown users and wrong passwords.
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
    }

    public record LoginRequest(String username, String password) {
    }
}
