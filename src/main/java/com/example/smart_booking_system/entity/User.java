package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.AuthProvider;
import com.example.smart_booking_system.enums.MembershipRank;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString; // ✅ Import mới
import lombok.EqualsAndHashCode; // ✅ Import mới

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String userId = UUID.randomUUID().toString();

    @Column(length = 255)
    private String fullName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 32)
    private String phoneNumber;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_account_id")
    private String stripeAccountId;

    private Boolean isEmailVerified = false;
    private Boolean twoFactorEnabled = false;

    private String twoFactorSecret;
    private String status = "ACTIVE";

    @Column(length = 500)
    private String resetPasswordToken;

    @Column(length = 255)
    private String notificationEmail;

    private LocalDateTime resetPasswordTokenExpiry;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    //membership - rank
    private int points = 0;
    @Enumerated(EnumType.STRING)
    private MembershipRank membershipRank = MembershipRank.BRONZE; // Mặc định là hạng Đồng


    // Many-to-Many with Role
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ✅ SỬA: Ngắt vòng lặp toString với UserDetail
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserDetail userDetail;

    // helper methods
    public void addRole(Role role) {
        this.roles.add(role);
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.getName().equalsIgnoreCase(roleName));
    }

    public boolean hasPermission(String permissionCode) {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(p -> p.getCode().equalsIgnoreCase(permissionCode));
    }

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String providerId;

    // Getter và Setter cho 2 trường mới này
    public AuthProvider getProvider() { return provider; }
    public void setProvider(AuthProvider provider) { this.provider = provider; }
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    // ✅ SỬA: Ngắt vòng lặp toString với SocialAccount
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    // Helper method
    public void addSocialAccount(SocialAccount socialAccount) {
        socialAccounts.add(socialAccount);
        socialAccount.setUser(this);
    }
}