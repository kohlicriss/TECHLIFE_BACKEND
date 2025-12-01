package com.auth.jwtsecurity.service;

import com.auth.jwtsecurity.dto.TokenPair;
import com.auth.jwtsecurity.model.User;
import com.auth.jwtsecurity.repository.UserRepository;
import com.auth.jwtsecurity.util.RsaKeyUtil;
import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Value("${app.jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${app.jwt.public-key-path}")
    private String publicKeyPath;

    private final RsaKeyUtil rsaKeyUtil;
    private final UserRepository userRepository;
    private final RoleAccessService roleAccessService; // ✅ Added

    private PrivateKey privateKey;
    private RSAPublicKey publicKey;

    public JwtService(RsaKeyUtil rsaKeyUtil,
                      UserRepository userRepository,
                      RoleAccessService roleAccessService) {  // ✅ Inject RoleAccessService
        this.rsaKeyUtil = rsaKeyUtil;
        this.userRepository = userRepository;
        this.roleAccessService = roleAccessService;
    }

    @PostConstruct
    public void initKeys() {
        try {
            privateKey = rsaKeyUtil.loadPrivateKey(privateKeyPath);
            publicKey = (RSAPublicKey) rsaKeyUtil.loadPublicKey(publicKeyPath);
        } catch (Exception e) {
            log.error("Failed to load RSA keys", e);
            throw new RuntimeException("Error loading RSA keys", e);
        }
    }

    public TokenPair generateTokenPair(Authentication authentication) {
        String username = authentication.getName();
        String fullName = "";

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isPresent()) {
            fullName = optionalUser.get().getFullName();
        }

        Map<String, String> additionalClaims = new HashMap<>();
        additionalClaims.put("fullName", fullName);

        String accessToken = generateToken(authentication, jwtExpirationMs, additionalClaims);
        String refreshToken = generateRefreshToken(authentication);

        return new TokenPair(accessToken, refreshToken);
    }

    public String generateAccessToken(Authentication authentication) {
        return generateToken(authentication, jwtExpirationMs, new HashMap<>());
    }

    public String generateRefreshToken(Authentication authentication) {
        Map<String, String> claims = new HashMap<>();
        claims.put("tokenType", "refresh");
        return generateToken(authentication, refreshExpirationMs, claims);
    }

    private String generateToken(Authentication authentication,
                                 long expirationInMs,
                                 Map<String, String> additionalClaims) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationInMs);
        Map<String, Object> claims = new HashMap<>(additionalClaims);
        // ✅ Fetch user from DB for correct formatting
        String employeeId = userRepository.findByUsername(userPrincipal.getUsername())
                .map(user -> user.getUsername().toUpperCase())
                .orElse(userPrincipal.getUsername().toUpperCase());

        claims.put("employeeId", employeeId);

        // ✅ Extract roles from authorities
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))
                .toList();
        claims.put("roles", roles);

        return Jwts.builder()
                .header().add("typ", "JWT").add("kid", "default").and()
                .subject(employeeId)
                .issuer("auth-service")
                .audience().add("hrm-service").and()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public boolean validateTokenForUser(String token, UserDetails userDetails) {
        final String username = extractUsernameFromToken(token);
        return username != null && username.equals(userDetails.getUsername());
    }

    public boolean isValidToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsernameFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    public boolean isRefreshToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims != null && "refresh".equals(claims.get("tokenType"));
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            throw new RuntimeException("Token expired");
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getMessage());
            throw new RuntimeException("Invalid token");
        }
    }

    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public byte[] getEncodedPublicKey() {
        return publicKey.getEncoded();
    }

    public RSAPublicKey getRsaPublicKey() {
        return publicKey;
    }

}
