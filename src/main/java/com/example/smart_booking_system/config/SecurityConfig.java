package com.example.smart_booking_system.config;

import com.example.smart_booking_system.security.CustomUserDetailsService;
import com.example.smart_booking_system.security.JwtAuthenticationEntryPoint;
import com.example.smart_booking_system.security.JwtAuthenticationFilter;
// ✅ IMPORT MỚI
import com.example.smart_booking_system.security.oauth2.CustomOAuth2UserService;
import com.example.smart_booking_system.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.example.smart_booking_system.security.oauth2.OAuth2AuthenticationSuccessHandler;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        // ✅ THÊM CÁC TÊN MIỀN PRODUCTION CỦA BẠN VÀO ĐÂY
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://tripify.click",
                "https://www.tripify.click",
                "https://tripify-frontend-two.vercel.app" // Sửa lại đúng link Vercel của bạn nếu cần
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // ===== 1. PUBLIC ROUTES (Phải được định nghĩa đầu tiên) =====
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()

                        .requestMatchers("/uploads/**", "/images/**", "/properties/**", "/api/v1/files/**",
                                "/ratingImage/**")
                        .permitAll()
                        .requestMatchers("/api/v1/properties/search").permitAll()
                        .requestMatchers("/api/v1/properties/featured").permitAll()
                        .requestMatchers("/api/v1/properties/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/properties/*/policies").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/property-details/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/property-images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/propertyAmenity/**").permitAll()

                        // Public access for ratings
                        .requestMatchers(HttpMethod.GET, "/api/v1/rating/property/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/rating/booking/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/rating/{id}").permitAll()

                        // Public access for room details
                        .requestMatchers(HttpMethod.GET, "/api/v1/rooms/property/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/rooms/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/room-details/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/room-images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/roomAmenity/**").permitAll()

                        .requestMatchers("/api/v1/payments/**").authenticated()
                        .requestMatchers("/api/v1/auth/owner/**").permitAll()
                        .requestMatchers("/api/v1/owner/register/**").permitAll()
                        .requestMatchers("/api/v1/owner-registration/**").permitAll()

                        // ===== 2. GENERAL AUTHENTICATED ROUTES =====
                        .requestMatchers(
                                "/api/v1/user-details/me",
                                "/api/v1/user-details/update",
                                "/api/v1/user-details/profile-status")
                        .authenticated()

                        // ===== 3. ADMIN ROUTES =====
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/v1/roles/**", "/api/v1/permissions/**").hasRole("SUPER_ADMIN")

                        // ===== 4. OWNER ROUTES =====
                        .requestMatchers("/api/v1/properties/submit-application").hasRole("OWNER")
                        .requestMatchers("/api/v1/owner/**").hasRole("OWNER")

                        // ===== 5. CUSTOMER ROUTES =====
                        .requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER")

                        // ===== 6. MIXED/COMBO ROUTES (Owner & Admin) =====
                        .requestMatchers("/api/v1/properties/add").hasAnyRole("OWNER", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/v1/properties/update/**").hasAnyRole("OWNER", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/v1/room/**").hasAnyRole("OWNER", "ADMIN", "SUPER_ADMIN")

                        // ===== 7.n8n routes=====
                        .requestMatchers("/api/v1/internal/**").permitAll()
                        .requestMatchers("/api/v1/ai/**").permitAll()

                        // ===== 8. DEFAULT =====
                        .anyRequest().authenticated()

                )
                // ✅ CẤU HÌNH OAUTH2 LOGIN Ở ĐÂY
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService) // Dùng service custom để lưu user vào DB
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler) // Dùng handler để tạo JWT và redirect về FE
                        .failureHandler(oAuth2AuthenticationFailureHandler) // Dùng handler để trả lỗi về FE khi login thất bại
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}