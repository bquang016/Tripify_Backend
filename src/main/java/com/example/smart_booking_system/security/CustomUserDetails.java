package com.example.smart_booking_system.security;

import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.entity.SocialAccount; // Import entity
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class CustomUserDetails implements UserDetails, OAuth2User {

    private String userId;
    private String email;
    private String password;
    private String fullName;
    private Boolean isEmailVerified;
    private String status;
    private Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;
    private List<SocialAccount> socialAccounts;
    private boolean twoFactorEnabled;

    // ✅ THÊM TRƯỜNG MỚI
    private boolean hasPassword;

    /**
     * Create UserDetails from User entity (Dùng cho Login thường)
     */
    public static CustomUserDetails create(User user) {
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleName()))
                .collect(Collectors.toSet());

        // Add permissions as authorities
        user.getRoles().forEach(role -> {
            role.getPermissions().forEach(permission -> {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            });
        });

        // ✅ LOGIC KIỂM TRA MẬT KHẨU
        // Mật khẩu thật (do BCrypt mã hóa) luôn bắt đầu bằng "$2a$".
        // Mật khẩu ngẫu nhiên (UUID do hệ thống tạo khi login Google/FB) sẽ không có tiền tố này.
        boolean hasPasswordSet = user.getPasswordHash() != null && user.getPasswordHash().startsWith("$2a$");

        return new CustomUserDetails(
                user.getUserId().toString(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getIsEmailVerified(),
                user.getStatus(),
                authorities,
                null, // Attributes là null khi login thường
                user.getSocialAccounts(),
                user.getTwoFactorEnabled() != null && user.getTwoFactorEnabled(), // ✅ populated from user entity
                hasPasswordSet // ✅ Truyền giá trị vào constructor
        );
    }

    /**
     * Create UserDetails from User entity & Attributes (Dùng cho OAuth2)
     */
    public static CustomUserDetails create(User user, Map<String, Object> attributes) {
        CustomUserDetails userDetails = CustomUserDetails.create(user);
        userDetails.setAttributes(attributes);
        return userDetails;
    }

    // --- UserDetails Methods ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"SUSPENDED".equals(status) && !"BANNED".equals(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(status);
    }

    // --- OAuth2User Methods ---
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return String.valueOf(userId); // Trả về userId làm định danh chính
    }
}