package com.example.smart_booking_system.security;

import com.example.smart_booking_system.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collection;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PermissionAspect {

    @Before("execution(* com.example.smart_booking_system.controller..*(..))")
    public void checkPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. Get annotation from method or class
        CheckPermission checkPermission = method.getAnnotation(CheckPermission.class);
        if (checkPermission == null) {
            checkPermission = method.getDeclaringClass().getAnnotation(CheckPermission.class);
        }

        // 2. If no annotation, skip (or you could enforce a default permission here)
        if (checkPermission == null) {
            return;
        }

        String requiredPermission = checkPermission.value();
        log.info("Checking permission: {} for method: {}", requiredPermission, method.getName());

        // 3. Get current user's authorities
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // 4. Check if user has required permission or is ADMIN
        boolean hasPermission = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals(requiredPermission) || a.getAuthority().equals("ROLE_ADMIN"));

        if (!hasPermission) {
            log.warn("User {} does not have permission: {}", authentication.getName(), requiredPermission);
            throw new AccessDeniedException("You do not have permission to perform this action: " + requiredPermission);
        }
    }
}
