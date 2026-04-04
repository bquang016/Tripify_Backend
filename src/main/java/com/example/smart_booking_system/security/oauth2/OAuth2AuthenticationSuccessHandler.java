package com.example.smart_booking_system.security.oauth2;

import com.example.smart_booking_system.security.JwtTokenProvider;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.util.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.nio.charset.StandardCharsets;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired private JwtTokenProvider tokenProvider;
    @Value("${app.frontend.url}") private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) return;
        if (request.getCookies() != null) {
            try { CookieUtils.deleteCookie(request, response, "LINKING_TOKEN"); } catch (Exception e) {}
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = tokenProvider.generateToken(authentication);
        String action = "PENDING_EMAIL".equals(userDetails.getStatus()) ? "require_email" : "login_success";

        // ✅ THÊM ENCODE VÀO ĐÂY
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("token", token)
                .queryParam("action", action)
                .encode(java.nio.charset.StandardCharsets.UTF_8)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}