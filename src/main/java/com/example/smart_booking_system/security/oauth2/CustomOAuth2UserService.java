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
import com.example.smart_booking_system.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            ex.printStackTrace();
            String msg = ex.getMessage() != null ? ex.getMessage() : "Unknown_NullPointerException";
            throw new InternalAuthenticationServiceException(msg, ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toLowerCase());

        OAuth2UserInfo oAuth2UserInfo;
        if (registrationId.equalsIgnoreCase("google")) {
            oAuth2UserInfo = new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
        } else if (registrationId.equalsIgnoreCase("facebook")) {
            oAuth2UserInfo = new FacebookOAuth2UserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("unsupported_provider");
        }

        // Lấy LINKING_TOKEN (kiểm tra xem có phải hành động liên kết từ Profile không)
        String linkingToken = null;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null && attributes.getRequest().getCookies() != null) {
            for (Cookie cookie : attributes.getRequest().getCookies()) {
                if ("LINKING_TOKEN".equals(cookie.getName())) {
                    linkingToken = cookie.getValue();
                    break;
                }
            }
        }

        Optional<SocialAccount> existingSocial = socialAccountRepository.findByProviderAndProviderId(provider, oAuth2UserInfo.getId());

        // Xác định rõ Intent (Mục đích) của request này
        boolean isLinkingIntent = StringUtils.hasText(linkingToken) && jwtTokenProvider.validateToken(linkingToken);

        if (isLinkingIntent) {
            // ==========================================
            // LUỒNG 1: NGƯỜI DÙNG CHỦ ĐỘNG LIÊN KẾT (TỪ PROFILE)
            // ==========================================
            String currentUserId = jwtTokenProvider.getUserIdFromToken(linkingToken);
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new OAuth2AuthenticationException("user_not_found"));

            if (existingSocial.isPresent()) {
                if (!existingSocial.get().getUser().getUserId().equals(currentUserId)) {
                    // LỖI: Tài khoản MXH này đã gắn với người khác!
                    throw new OAuth2AuthenticationException("account_already_linked");
                }
                // Đã liên kết với chính user này rồi -> Bỏ qua, cho đăng nhập lại session
                return CustomUserDetails.create(currentUser, oAuth2User.getAttributes());
            }

            // Kiểm tra xem email MXH có bị trùng với một user khác trong hệ thống không
            Optional<User> userWithSameEmail = userRepository.findByEmail(oAuth2UserInfo.getEmail());
            if (userWithSameEmail.isPresent() && !userWithSameEmail.get().getUserId().equals(currentUserId)) {
                throw new OAuth2AuthenticationException("email_used_by_another_user");
            }

            linkSocialAccount(currentUser, provider, oAuth2UserInfo);
            return CustomUserDetails.create(currentUser, oAuth2User.getAttributes());

        } else {
            // ==========================================
            // LUỒNG 2: ĐĂNG NHẬP / ĐĂNG KÝ BÌNH THƯỜNG
            // ==========================================
            if (existingSocial.isPresent()) {
                // Đã có tài khoản MXH -> Cho phép đăng nhập
                User user = existingSocial.get().getUser();
                if (user == null) {
                    socialAccountRepository.delete(existingSocial.get());
                    throw new OAuth2AuthenticationException("orphan_record_deleted_please_retry");
                }
                return CustomUserDetails.create(user, oAuth2User.getAttributes());
            }

            // Chưa có tài khoản MXH -> Kiểm tra Email để Đăng ký
            String email = oAuth2UserInfo.getEmail();
            if (!StringUtils.hasText(email)) {
                email = oAuth2UserInfo.getId() + "@temp." + registrationId + ".com";
            }

            Optional<User> existingUserByEmail = userRepository.findByEmail(email);

            if (existingUserByEmail.isPresent()) {
                throw new OAuth2AuthenticationException("email_exists_require_manual_link");
            }

            // Đăng ký mới hoàn toàn
            User newUser = registerNewUser(oAuth2UserRequest, oAuth2UserInfo, email);
            linkSocialAccount(newUser, provider, oAuth2UserInfo);
            return CustomUserDetails.create(newUser, oAuth2User.getAttributes());
        }
    }

    private void linkSocialAccount(User user, AuthProvider provider, OAuth2UserInfo oAuth2UserInfo) {
        SocialAccount socialAccount = new SocialAccount();
        socialAccount.setProvider(provider);
        socialAccount.setProviderId(oAuth2UserInfo.getId());
        socialAccount.setEmail(oAuth2UserInfo.getEmail());
        socialAccount.setName(oAuth2UserInfo.getName());
        user.addSocialAccount(socialAccount);
        socialAccountRepository.save(socialAccount); // Lưu xuống DB
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
        user.setProvider(AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId().toLowerCase()));
        user.setProviderId(oAuth2UserInfo.getId());
        user.setFullName(oAuth2UserInfo.getName());
        user.setEmail(email);
        user.setIsEmailVerified(true);
        if (email.contains("@temp.") && email.endsWith(".com")) {
            user.setStatus("PENDING_EMAIL");
        } else {
            user.setStatus("ACTIVE");
        }
        // Generate ngẫu nhiên password để đảm bảo rule DB
        user.setPasswordHash(UUID.randomUUID().toString());

        Role userRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new InternalAuthenticationServiceException("Role 'CUSTOMER' not found."));
        user.addRole(userRole);

        UserDetail userDetail = new UserDetail();
        userDetail.setProfilePhotoUrl(oAuth2UserInfo.getImageUrl());
        userDetail.setUser(user);
        user.setUserDetail(userDetail);

        return userRepository.save(user);
    }
}