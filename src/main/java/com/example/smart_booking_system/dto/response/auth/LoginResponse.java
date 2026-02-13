package com.example.smart_booking_system.dto.response.auth;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserResponse user;
    private boolean is2faRequired = false;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private String userId;
        private String fullName;
        private String email;
        private String phoneNumber;
        private Boolean isEmailVerified;
        private String status;
        private Set<String> roles;
    }

    public LoginResponse(String accessToken, Long expiresIn, UserResponse user) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.user = user;
        this.tokenType = "Bearer";
        this.is2faRequired = false;
    }

    public static LoginResponse twoFaRequired() {
        LoginResponse response = new LoginResponse();
        response.set2faRequired(true);
        return response;
    }
}