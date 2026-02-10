package com.example.smart_booking_system.security;

import com.example.smart_booking_system.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtUtil; // JWT helper
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");

            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                // kiểm tra token đã bị thu hồi chưa
                if (tokenBlacklistService.isBlacklisted(token)) {
                    logger.warn("Token đã bị thu hồi");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\": \"Token is blacklisted. Please login again.\"}");
                    return;
                }

                // nếu chưa bị thu hồi thì kiểm tra token có hợp lệ không
                if (jwtUtil.validateToken(token)) {
                    String type = null;
                    try {
                        type = jwtUtil.getClaimFromToken(token, "type", String.class);
                    } catch (Exception ignored) {}

                    if ("temporary".equals(type)) {
                        // Skip user loading for temporary registration tokens
                        filterChain.doFilter(request, response);
                        return;
                    }

                    String userId = jwtUtil.getUserIdFromToken(token);

                    if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        try {
                            UserDetails userDetails = userDetailsService.loadUserById(userId);
                            if (!userDetails.isAccountNonLocked()) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json");
                                response.setCharacterEncoding("UTF-8");
                                response.getWriter().write("{\"error\": \"Tài khoản của bạn đã bị khóa hoặc tạm ngưng.\"}");
                                return;
                            }

                            if (!userDetails.isEnabled()) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.setCharacterEncoding("UTF-8");
                                response.getWriter().write("{\"error\": \"Tài khoản chưa được kích hoạt.\"}");
                                return;
                            }
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities());

                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        } catch (Exception e) {
                            // Log lỗi nhưng vẫn cho đi tiếp để SecurityConfig xử lý quyền truy cập
                            logger.error("User not found or invalid ID in token: " + userId);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Lỗi nghiêm trọng khi xử lý JWT", ex);
            // Không trả về 401 ở đây để tránh chặn các request public
        }

        filterChain.doFilter(request, response);
    }
}
