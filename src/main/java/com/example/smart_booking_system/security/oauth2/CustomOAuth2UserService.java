package com.example.smart_booking_system.security.oauth2;

import com.example.smart_booking_system.entity.Role;
import com.example.smart_booking_system.entity.SocialAccount;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.entity.UserDetail;
import com.example.smart_booking_system.enums.AuthProvider;
import com.example.smart_booking_system.repository.RoleRepository;
import com.example.smart_booking_system.repository.SocialAccountRepository;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.security.JwtTokenProvider;
import com.example.smart_booking_system.security.oauth2.user.FacebookOAuth2UserInfo;
import com.example.smart_booking_system.security.oauth2.user.GoogleOAuth2UserInfo;
import com.example.smart_booking_system.security.oauth2.user.OAuth2UserInfo;
import com.example.smart_booking_system.util.CookieUtils; // Đảm bảo đã import
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private SocialAccountRepository socialAccountRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private HttpServletRequest request;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toLowerCase());
        OAuth2UserInfo oAuth2UserInfo = getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        // LOG DEBUG: Kiểm tra xem có nhận được Cookie không
        String linkingToken = CookieUtils.getCookie(request, "LINKING_TOKEN")
                .map(Cookie::getValue)
                .orElse(null);

        System.out.println("--- OAuth2 Flow Debug ---");
        System.out.println("Email from Provider: " + oAuth2UserInfo.getEmail());
        System.out.println("LINKING_TOKEN cookie: " + (linkingToken != null ? "FOUND" : "NOT FOUND"));

        // ✅ LOGIC XỬ LÝ LIÊN KẾT (Khi có Cookie)
        if (StringUtils.hasText(linkingToken)) {
            if (jwtTokenProvider.validateToken(linkingToken)) {
                String userId = jwtTokenProvider.getUserIdFromToken(linkingToken);
                User currentUser = userRepository.findById(userId).orElse(null);
                System.out.println("User ID from Token: " + userId);

                if (currentUser != null) {
                    Optional<SocialAccount> existingSocial =
                            socialAccountRepository.findByProviderAndProviderId(provider, oAuth2UserInfo.getId());

                    if (existingSocial.isPresent()) {
                        if (!existingSocial.get().getUser().getUserId().equals(currentUser.getUserId())) {
                            throw new OAuth2AuthenticationException("Tài khoản này đã được liên kết với người khác!");
                        }
                        return CustomUserDetails.create(currentUser, oAuth2User.getAttributes());
                    }

                    // Thực hiện liên kết
                    linkSocialAccount(currentUser, provider, oAuth2UserInfo);
                    System.out.println(">> LINKING SUCCESSFUL for user: " + currentUser.getEmail());
                    return CustomUserDetails.create(currentUser, oAuth2User.getAttributes());
                }
            } else {
                // Có token nhưng sai -> Báo lỗi để tránh tạo user mới nhầm lẫn
                System.out.println(">> Invalid Linking Token!");
                throw new OAuth2AuthenticationException("Phiên liên kết không hợp lệ. Vui lòng thử lại.");
            }
        }

        // ✅ LOGIC ĐĂNG NHẬP / ĐĂNG KÝ (Khi KHÔNG có Cookie)
        System.out.println(">> Normal Login/Register Flow");

        Optional<SocialAccount> socialAccountOptional =
                socialAccountRepository.findByProviderAndProviderId(provider, oAuth2UserInfo.getId());

        User user;
        if (socialAccountOptional.isPresent()) {
            user = socialAccountOptional.get().getUser();
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            String email = oAuth2UserInfo.getEmail();
            if (!StringUtils.hasText(email)) {
                email = oAuth2UserInfo.getId() + "@temp." + registrationId + ".com";
            }

            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isPresent()) {
                user = userOptional.get();
                linkSocialAccount(user, provider, oAuth2UserInfo);
            } else {
                user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo, email);
                linkSocialAccount(user, provider, oAuth2UserInfo);
            }
        }

        return CustomUserDetails.create(user, oAuth2User.getAttributes());
    }

    // (Giữ nguyên các hàm helper: linkSocialAccount, getOAuth2UserInfo, registerNewUser, updateExistingUser...)
    // Bạn hãy copy lại các hàm helper từ code cũ của bạn vào đây.
    private void linkSocialAccount(User user, AuthProvider provider, OAuth2UserInfo oAuth2UserInfo) {
        SocialAccount socialAccount = new SocialAccount();
        socialAccount.setProvider(provider);
        socialAccount.setProviderId(oAuth2UserInfo.getId());
        socialAccount.setEmail(oAuth2UserInfo.getEmail());
        socialAccount.setName(oAuth2UserInfo.getName());
        user.addSocialAccount(socialAccount);
        socialAccountRepository.save(socialAccount);
    }

    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, java.util.Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase(AuthProvider.google.toString())) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase(AuthProvider.facebook.toString())) {
            return new FacebookOAuth2UserInfo(attributes);
        } else {
            throw new OAuth2AuthenticationException("Login with " + registrationId + " is not supported yet.");
        }
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo, String email) {
        User user = new User();
        user.setProvider(AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId()));
        user.setProviderId(oAuth2UserInfo.getId());
        user.setFullName(oAuth2UserInfo.getName());
        user.setEmail(email);
        user.setIsEmailVerified(true);
        if (email.contains("@temp.") && email.endsWith(".com")) {
            user.setStatus("PENDING_EMAIL");
        } else {
            user.setStatus("ACTIVE");
        }
        user.setPasswordHash(UUID.randomUUID().toString());
        Role userRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new InternalAuthenticationServiceException("Role 'CUSTOMER' not set."));
        user.addRole(userRole);
        UserDetail userDetail = new UserDetail();
        userDetail.setProfilePhotoUrl(oAuth2UserInfo.getImageUrl());
        userDetail.setUser(user);
        user.setUserDetail(userDetail);
        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        return userRepository.save(existingUser);
    }
}