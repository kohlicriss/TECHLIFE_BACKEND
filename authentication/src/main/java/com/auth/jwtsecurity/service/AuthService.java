package com.auth.jwtsecurity.service;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.auth.jwtsecurity.Client.TicketsUpdate;
import com.auth.jwtsecurity.dto.*;
//import com.auth.jwtsecurity.dto.RefreshTokenRequest;
import com.auth.jwtsecurity.model.User;
import com.auth.jwtsecurity.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TicketsUpdate ticketsUpdate;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    @Transactional
    public void registerUser(@Valid RegisterRequest registerRequest, String tenant_id) {
        String username = registerRequest.getUsername().toLowerCase().trim();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already in use");
        }

        if (!isStrongPassword(registerRequest.getPassword())) {
            throw new IllegalArgumentException("Password must be at least 8 characters long, include 1 uppercase, 1 lowercase, 1 digit, and 1 special character");
        }

        User user = User.builder()
                .fullName(registerRequest.getFullName())
                .username(username)
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(registerRequest.getRole())  // Should be "ADMIN", not "ROLE_ADMIN"
                .email(registerRequest.getEmail())
                .phoneNumber(registerRequest.getPhoneNumber())
                .enabled(registerRequest.isEnabled())
                .tenantId(tenant_id)
                .createdAt(registerRequest.getCreatedAt())
                .build();
        if (tenant_id.equals("TECHLIFE")){
            Tickets tickets = Tickets.builder()
                    .employeeId(username.toUpperCase().trim())
                    .roles(registerRequest.getRole())
                    .build();
            ResponseEntity<Tickets> re = ticketsUpdate.createAuth(tickets);
            if (!re.getStatusCode().is2xxSuccessful()) throw new RuntimeException("Cant Update Tickets branch");
        }
       userRepository.save(user);
    }

    public void deleteUser(String id, String tenant_id) {
        User user = userRepository.findByUsernameAndTenantId(id, tenant_id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if(tenant_id.equals("TECHLIFE")) {
            ResponseEntity<Void> re = ticketsUpdate.deleteAuth(user.getUsername().toUpperCase());
            if (!re.getStatusCode().is2xxSuccessful()) throw new RuntimeException("Cant Update Tickets branch");
        }
        userRepository.delete(user);
    }

    public void updateUser(String id, UpdateRequest registerRequest) {
        User user = userRepository.findByUsername(id.toLowerCase().trim()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setUsername(registerRequest.getUsername()!=null?registerRequest.getUsername().toLowerCase().trim() : user.getUsername().toLowerCase().trim());
        user.setFullName(registerRequest.getFullName()!=null?registerRequest.getFullName(): user.getFullName());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()!=null?registerRequest.getPassword() : user.getPassword()));
        user.setPhoneNumber(registerRequest.getPhoneNumber()!=null?registerRequest.getPhoneNumber(): user.getPhoneNumber());
        user.setEmail(registerRequest.getEmail()!=null?registerRequest.getEmail(): user.getEmail());
        user.setRole(registerRequest.getRole()!=null?registerRequest.getRole() : user.getRole());
        user.setUpdatedAt(LocalDateTime.now());
        user.setCreatedAt(registerRequest.getCreatedAt()!=null?registerRequest.getCreatedAt(): user.getCreatedAt());
        user.setTenantId(registerRequest.getTenantId()!=null?registerRequest.getTenantId(): user.getTenantId());
        user.setEnabled(registerRequest.isEnabled());
        user.setAccountNonLocked(registerRequest.isAccountNonLocked() == user.isAccountNonLocked() && registerRequest.isAccountNonLocked());
        if (registerRequest.getTenantId().equals("TECHLIFE")) {
            Tickets tickets = Tickets.builder()
                    .employeeId(user.getUsername().toUpperCase())
                    .roles(registerRequest.getRole())
                    .build();
            ResponseEntity<Tickets> re = ticketsUpdate.updateAuth(user.getUsername().toUpperCase(), tickets);
            if (!re.getStatusCode().is2xxSuccessful()) throw new RuntimeException("Cant Update Tickets branch");
        }
        userRepository.save(user);
    }

    public TokenPair login(@Valid LoginRequest loginRequest, String tenant_id) throws BadRequestException {

        Optional<User> userOptional = Optional.empty();

        if (loginRequest.getUsername() != null && !loginRequest.getUsername().isBlank()) {
            userOptional = userRepository.findByUsernameAndTenantId(loginRequest.getUsername().toLowerCase().trim(), tenant_id);
        } else if (loginRequest.getEmail() != null && !loginRequest.getEmail().isBlank()) {
            userOptional = userRepository.findByEmailAndTenantId(loginRequest.getEmail().trim(), tenant_id);
        } else if (loginRequest.getPhone() != null && !loginRequest.getPhone().isBlank()) {
            userOptional = userRepository.findByPhoneNumberAndTenantId(loginRequest.getPhone().trim(),  tenant_id);
        } else {
            throw new BadRequestException("At least one field (username, email, phone) must be provided");
        }

        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("Invalid login details");
        }

        User user = userOptional.get();

        String loginUsername = user.getUsername().toLowerCase().trim();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginUsername,
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return jwtService.generateTokenPair(authentication);
    }


    public TokenPair refreshTokenFromCookie(String refreshToken, String tenant_id) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String username = jwtService.extractUsernameFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (userDetails == null) {
            throw new IllegalArgumentException("User not found");
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        String accessToken = jwtService.generateAccessToken(authentication);
        return new TokenPair(accessToken, refreshToken); // reusing the same refresh token
    }


    private boolean isStrongPassword(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }


}
