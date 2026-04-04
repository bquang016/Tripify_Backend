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
            // ✅ ĐỔI THÀNH TIẾNG ANH KHÔNG DẤU HOẶC LẤY LỖI GỐC
            String msg = ex.getMessage() != null ? ex.getMessage() : "Unknown_NullPointerException";
            throw new InternalAuthenticationServiceException(msg, ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();

        AuthProvider provider;
        try {
            provider = AuthProvider.valueOf(registrationId.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException("Provider không hợp lệ: " + registrationId);
        }

        // ✅ LẤY INFO TRỰC TIẾP (BỎ CƠ CHẾ .toString() DỄ GÂY LỖI)
        OAuth2UserInfo oAuth2UserInfo;
        if (registrationId.equalsIgnoreCase("google")) {
            oAuth2UserInfo = new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
        } else if (registrationId.equalsIgnoreCase("facebook")) {
            oAuth2UserInfo = new FacebookOAuth2UserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("Không hỗ trợ đăng nhập: " + registrationId);
        }

        // ✅ LẤY COOKIE AN TOÀN TUYỆT ĐỐI CHỐNG NULL
        String linkingToken = null;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            if (request != null && request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("LINKING_TOKEN".equals(cookie.getName())) {
                        linkingToken = cookie.getValue();
                        break;
                    }
                }
            }
        }

        System.out.println("--- OAuth2 Flow Debug ---");
        System.out.println("Email from Provider: " + oAuth2UserInfo.getEmail());
        System.out.println("LINKING_TOKEN cookie: " + (linkingToken != null ? "FOUND" : "NOT FOUND"));

        // ===== USE-CASE 3: CHỦ ĐỘNG LIÊN KẾT TÀI KHOẢN TỪ PROFILE =====
        if (StringUtils.hasText(linkingToken)) {
            if (jwtTokenProvider.validateToken(linkingToken)) {
                String userId = jwtTokenProvider.getUserIdFromToken(linkingToken);
                User currentUser = userRepository.findById(userId)
                        .orElseThrow(() -> new OAuth2AuthenticationException("Không tìm thấy User để liên kết."));

                System.out.println("User ID from Token: " + userId);

                // Kiểm tra xem providerId này đã bị ai khác chiếm chưa
                Optional<SocialAccount> existingSocial = socialAccountRepository.findByProviderAndProviderId(provider, oAuth2UserInfo.getId());
                if (existingSocial.isPresent() && !existingSocial.get().getUser().getUserId().equals(currentUser.getUserId())) {
                    throw new OAuth2AuthenticationException("Tài khoản MXH này đã được liên kết với một người dùng khác!");
                }

                // Thực hiện liên kết nếu chưa tồn tại
                if (existingSocial.isEmpty()) {
                    linkSocialAccount(currentUser, provider, oAuth2UserInfo);
                    System.out.println(">> LINKING SUCCESSFUL for user: " + currentUser.getEmail());
                }
                return CustomUserDetails.create(currentUser, oAuth2User.getAttributes());
            } else {
                System.out.println(">> Invalid Linking Token!");
                throw new OAuth2AuthenticationException("Phiên liên kết không hợp lệ hoặc đã hết hạn.");
            }
        }

        // ===== USE-CASE 1 & 2: LOGIN / REGISTER BÌNH THƯỜNG =====
        System.out.println(">> Normal Login/Register Flow");

        Optional<SocialAccount> socialAccountOptional = socialAccountRepository.findByProviderAndProviderId(provider, oAuth2UserInfo.getId());

        User user = null;

        // BƯỚC 1: KIỂM TRA CHỐNG RÁC DỮ LIỆU
        if (socialAccountOptional.isPresent()) {
            SocialAccount sa = socialAccountOptional.get();

            // Lấy User ra kiểm tra xem có thật không
            if (sa.getUser() != null && sa.getUser().getUserId() != null) {
                user = userRepository.findById(sa.getUser().getUserId()).orElse(null);
            }

            // Nếu có Social Account nhưng User đã "bốc hơi" khỏi DB -> Xóa rác
            if (user == null) {
                System.out.println(">> Phát hiện bản ghi mồ côi. Đang tiến hành dọn rác...");
                socialAccountRepository.delete(sa);
            }
        }

        // BƯỚC 2: XỬ LÝ (Nếu user vẫn null tức là chưa có hoặc rác vừa bị xóa)
        if (user == null) {
            String email = oAuth2UserInfo.getEmail();
            if (!StringUtils.hasText(email)) {
                email = oAuth2UserInfo.getId() + "@temp." + registrationId + ".com";
            }

            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isPresent()) {
                // Gộp tài khoản (Use-case 2)
                user = userOptional.get();
                linkSocialAccount(user, provider, oAuth2UserInfo);
                System.out.println(">> MERGE SUCCESSFUL for email: " + email);
            } else {
                // Tạo mới hoàn toàn (Use-case 1)
                user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo, email);
                linkSocialAccount(user, provider, oAuth2UserInfo);
                System.out.println(">> NEW USER REGISTERED: " + email);
            }
        }

        return CustomUserDetails.create(user, oAuth2User.getAttributes());
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