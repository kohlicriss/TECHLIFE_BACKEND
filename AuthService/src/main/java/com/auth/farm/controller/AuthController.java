package com.auth.farm.controller;

import com.auth.farm.dto.LoginRequest;
import com.auth.farm.dto.SignupRequest;
import com.auth.farm.service.AuthService;
import com.auth.farm.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/form_auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/vendor/signup")
    public ResponseEntity<String> vendorSignup(@RequestBody SignupRequest request) {
        String result = authService.registerVendor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/register")
    public ResponseEntity<String> registerAdmin(@RequestBody SignupRequest request) {
        String result = authService.registerAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestBody Map<String, String> request) {

        String token = request.get("token");

        if (!jwtService.isValidToken(token)) {
            return ResponseEntity.status(401).body("Invalid token");
        }

        Claims claims = jwtService.extractAllClaims(token);

        return ResponseEntity.ok(Map.of(
                "username", claims.getSubject(),
                "roles", claims.get("roles"),
                "fullName", claims.get("fullName"),
                "userId", claims.get("userId")   // Long type
        ));
    }


    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok("Logged out successfully");
    }
}








