package com.example.RealTime_Attendance.Security;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.example.RealTime_Attendance.Client.AuthClient;
import com.example.RealTime_Attendance.Exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PermissionAspect {

    private final AuthClient authClient;

    @Before("@annotation(checkPermission)")
    public void checkPermission(JoinPoint joinPoint, CheckPermission checkPermission) throws CustomException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("checking if the person has the permission {}", auth);

        if (auth == null) {
            throw new CustomException("Unauthorized: No authentication context found", HttpStatus.UNAUTHORIZED);
        }

        Set<String> userRoles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        String permission = checkPermission.value();

        boolean hasAccess = userRoles.stream()
                .anyMatch(role -> authClient.checkPermission(role, permission));

        if (!hasAccess) {
            log.warn("❌ Access denied: roles={} attempted permission={}", userRoles, permission);
            throw new CustomException("You don’t have permission to perform this action: " + permission,
                    HttpStatus.FORBIDDEN);
        }
        log.info("✅ Access granted: roles={} attempted permission={}", userRoles, permission);

        Set<String> HasRoles = new HashSet<>(Arrays.asList(checkPermission.MatchParmForRoles()));
        boolean userHasRole = userRoles.stream().anyMatch(HasRoles::contains);

        if (!userHasRole ||  checkPermission.type() == TypeVar.NONE) {
            log.info("User has restricted role: {}", HasRoles);
        } else {
            String claimValue = null;
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                log.info("Claim value reached into instanceOf: {}",
                        jwtAuth.getToken().getClaimAsString(checkPermission.MatchParmName()));
                claimValue = jwtAuth.getToken().getClaimAsString(checkPermission.MatchParmName());
                if (claimValue == null) {
                    claimValue = jwtAuth.getToken().getSubject();
                }
            }

            switch (checkPermission.type()) {
                case TypeVar.VARIABLE -> {
                    log.info("Checking claim {} against URL parameter {}", checkPermission.MatchParmName(),
                            checkPermission.MatchParmFromType());
                    ParmImpl(checkPermission, claimValue, joinPoint);
                }
                case TypeVar.BODY -> {
                    log.info("Checking claim {} against URL parameter {}", checkPermission.MatchParmName(),
                            checkPermission.MatchParmFromType());
                    BodyImpl(checkPermission, claimValue, joinPoint);
                }
                case TypeVar.NONE -> {
                    log.info("Considering to No params to check");
                }
            }

        }
    }

    public void ParmImpl(CheckPermission checkPermission, String claimValue, JoinPoint joinPoint) throws CustomException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = methodSignature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(checkPermission.MatchParmFromType())) {
                String urlValue = (String) paramValues[i];
                log.info("URL parameter value: {} ans claim value: {}", urlValue, claimValue);
                if (urlValue != null && !urlValue.equalsIgnoreCase(claimValue)) {
                    throw new CustomException(
                            String.format("Access denied: Claim %s (%s) does not match URL parameter %s (%s)",
                                    checkPermission.MatchParmName(), claimValue,
                                    checkPermission.MatchParmFromType(), urlValue),
                            HttpStatus.FORBIDDEN);
                } else {
                    log.info("Access granted: Claim {} ({}) matches URL parameter {} ({})", checkPermission.MatchParmName(),
                            claimValue, checkPermission.MatchParmFromType(), urlValue);
                    return;
                }
            }
        }
        throw new CustomException("Given parameter not found", HttpStatus.FORBIDDEN);
    }

    public void BodyImpl(CheckPermission checkPermission, String claimValue, JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = methodSignature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();

        String[] bodyPath = checkPermission.MatchParmFromType().split("\\.");

        Object currentObject = null;

        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(bodyPath[0])) {
                currentObject = paramValues[i];
                break;
            }
        }

        if (currentObject == null) {
            log.warn("Top-level body parameter '{}' not found", bodyPath[0]);
            return;
        }

        // Traverse recursively
        currentObject = resolveNestedField(currentObject, bodyPath, 1);

        String bodyValue = currentObject != null ? currentObject.toString() : null;
        log.info("Resolved body field value: {} | Claim value: {}", bodyValue, claimValue);

        if (bodyValue != null && !bodyValue.equalsIgnoreCase(claimValue)) {
            throw new CustomException(
                    String.format("Access denied: Claim %s (%s) does not match body field %s (%s)",
                            checkPermission.MatchParmName(), claimValue,
                            checkPermission.MatchParmFromType(), bodyValue),
                    HttpStatus.FORBIDDEN);
        }
    }

    private Object resolveNestedField(Object current, String[] path, int index) {
        if (current == null || index >= path.length) {
            return current;
        }

        String fieldName = path[index];

        try {
            // Handle Map
            if (current instanceof Map<?, ?> map) {
                Object next = map.get(fieldName);
                return resolveNestedField(next, path, index + 1);
            }

            // Handle List with numeric index, e.g., users[0].email
            if (current instanceof List<?> list) {
                int ind = Integer.parseInt(fieldName.replaceAll("[^0-9]", ""));
                if (ind < list.size()) {
                    return resolveNestedField(list.get(ind), path, ind + 1);
                }
            }
            // Handle regular field
            Field field = current.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object next = field.get(current);

            return resolveNestedField(next, path, index + 1);

        } catch (NoSuchFieldException e) {
            // If it's a Map or JSON-like structure inside object
            if (current instanceof Map<?, ?> map && map.containsKey(fieldName)) {
                return resolveNestedField(map.get(fieldName), path, index + 1);
            }
            log.error("No such field '{}' in class {}", fieldName, current.getClass().getSimpleName());
            return null;
        } catch (Exception e) {
            log.error("Error accessing field '{}': {}", fieldName, e.getMessage());
            return null;
        }
    }

}

