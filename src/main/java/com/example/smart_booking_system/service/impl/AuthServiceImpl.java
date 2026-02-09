package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.dto.request.auth.*;
import com.example.smart_booking_system.dto.response.auth.LoginResponse;
import com.example.smart_booking_system.dto.response.auth.VerifyOwnerOtpResponse;
import com.example.smart_booking_system.entity.Role;
import com.example.smart_booking_system.entity.SocialAccount;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.exception.*;
import com.example.smart_booking_system.repository.OwnerApplicationRepository;
import com.example.smart_booking_system.enums.ApplicationStatus;
import com.example.smart_booking_system.repository.RoleRepository;
import com.example.smart_booking_system.repository.SocialAccountRepository;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.security.JwtTokenProvider;
import com.example.smart_booking_system.service.AuthService;
import com.example.smart_booking_system.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import java.util.Random;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final OwnerApplicationRepository ownerApplicationRepository;

    // Thay thế Redis bằng Map trong RAM
    private final Map<String, OtpInfo> otpStorage = new ConcurrentHashMap<>();

    // Class nội bộ lưu thông tin OTP
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class OtpInfo {
        private String code;
        private long expiryTime;
    }

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    private boolean isStrongPassword(String password) {
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!.*])(?=\\S+$).{8,}$";
        return password != null && password.matches(regex);
    }

    @Override
    @Transactional
    public void sendOwnerOtp(String email) {
        checkOwnerEmail(email); // Reuse the existing check

        String otpCode = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        LocalDateTime otpExpiry = LocalDateTime.now().plusMinutes(5);

        com.example.smart_booking_system.entity.OwnerApplication application =
                ownerApplicationRepository.findByEmail(email).orElseGet(() -> {
                    com.example.smart_booking_system.entity.OwnerApplication newApp = new com.example.smart_booking_system.entity.OwnerApplication();
                    newApp.setEmail(email);
                    newApp.setStatus(ApplicationStatus.PENDING); // Initial status
                    return newApp;
                });

        application.setOtp(otpCode);
        application.setOtpExpiry(otpExpiry);
        application.setEmailVerified(false); // Reset verification status on new OTP request
        ownerApplicationRepository.save(application);

        Context context = new Context();
        context.setVariable("otpCode", otpCode);
        context.setVariable("title", "Xác thực đăng ký Đối tác");
        context.setVariable("message", "Sử dụng mã bên dưới để hoàn tất đăng ký đối tác Tripify.");

        emailService.sendHtmlEmail(email, "Mã xác thực đăng ký Đối tác", "email/otp-email", context);
    }

    @Override
    @Transactional
    public VerifyOwnerOtpResponse verifyOwnerOtp(VerifyOtpRequest request) {
        com.example.smart_booking_system.entity.OwnerApplication application = ownerApplicationRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Yêu cầu không hợp lệ. Vui lòng thử lại từ đầu."));

        if (application.getOtp() == null ||
            !application.getOtp().equals(request.getOtpCode()) ||
            application.getOtpExpiry() == null ||
            application.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Mã OTP không chính xác hoặc đã hết hạn.");
        }

        // OTP is valid, update application
        application.setEmailVerified(true);
        application.setOtp(null);
        application.setOtpExpiry(null);
        ownerApplicationRepository.save(application);

        // Generate and return the temporary token for the next steps
        String temporaryToken = tokenProvider.generateTemporaryToken(request.getEmail());
        return new VerifyOwnerOtpResponse(temporaryToken);
    }

    @Override
    public void checkOwnerEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email đã được sử dụng bởi một tài khoản khác.");
        }
        if (ownerApplicationRepository.existsByEmailAndStatus(email, ApplicationStatus.PENDING)) {
            throw new ConflictException("Một đơn đăng ký với email này đang được chờ duyệt.");
        }
    }

    @Override
    public void createPassword(String userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getPasswordHash() != null && user.getPasswordHash().startsWith("$2a$")) {
            throw new BadRequestException("Tài khoản này đã có mật khẩu. Vui lòng dùng chức năng Đổi mật khẩu.");
        }

        if (!isStrongPassword(newPassword)) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void unlinkSocialAccount(String userId, String providerName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional<SocialAccount> accountToDelete = user.getSocialAccounts().stream()
                .filter(acc -> acc.getProvider().toString().equalsIgnoreCase(providerName))
                .findFirst();

        if (accountToDelete.isEmpty()) {
            throw new BadRequestException("Tài khoản " + providerName + " chưa được liên kết.");
        }

        boolean hasPassword = user.getPasswordHash() != null && user.getPasswordHash().startsWith("$2a$");
        boolean hasOtherSocial = user.getSocialAccounts().size() > 1;

        if (!hasPassword && !hasOtherSocial) {
            throw new BadRequestException("Bạn không thể ngắt kết nối " + providerName +
                    " vì đây là phương thức đăng nhập duy nhất. Hãy đặt mật khẩu trước.");
        }

        SocialAccount account = accountToDelete.get();
        user.getSocialAccounts().remove(account);
        socialAccountRepository.delete(account);
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (!request.isPasswordMatching()) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        if (!isStrongPassword(request.getPassword())) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
        }

        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            if ("SUSPENDED".equalsIgnoreCase(user.getStatus()) || "BANNED".equalsIgnoreCase(user.getStatus())) {
                throw new ForbiddenException("Tài khoản của bạn đã bị khóa. Không thể đăng ký lại với email này.");
            }

            throw new ConflictException("Email đã được đăng ký");
        }

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus("INACTIVE");
        user.setIsEmailVerified(false);

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusMinutes(10));

        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Role 'CUSTOMER' not found"));
        user.addRole(customerRole);

        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationToken);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Email hoặc mật khẩu không chính xác"));

        if (Boolean.FALSE.equals(user.getIsEmailVerified())) {
            throw new DisabledException("Tài khoản chưa được xác thực. Vui lòng kiểm tra email!");
        }

        if ("SUSPENDED".equalsIgnoreCase(user.getStatus()) || "BANNED".equalsIgnoreCase(user.getStatus())) {
            throw new LockedException("Tài khoản của bạn đã bị khóa: " + user.getStatus());
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.generateToken(authentication);
        long expiresIn = tokenProvider.getExpirationTime();

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Set<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());

        LoginResponse.UserResponse userResponse = new LoginResponse.UserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getIsEmailVerified(),
                user.getStatus(),
                roles
        );

        return new LoginResponse(token, expiresIn, userResponse);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Mã xác minh không hợp lệ"));

        if (user.getVerificationTokenExpiry() == null || user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Mã xác minh đã hết hạn");
        }

        user.setIsEmailVerified(true);
        user.setStatus("ACTIVE");
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email: " + request.getEmail()));

        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(30));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        emailService.sendResetPasswordEmail(user.getEmail(), user.getFullName(), resetToken);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        if (!isStrongPassword(request.getNewPassword())) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
        }

        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Mã đặt lại mật khẩu không hợp lệ"));

        if (user.getResetPasswordTokenExpiry() == null || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Mã đặt lại mật khẩu đã hết hạn");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu cũ không chính xác");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu mới không khớp");
        }

        if (!isStrongPassword(request.getNewPassword())) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new BadRequestException("Email đã được xác minh trước đó");
        }

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationToken);
    }
    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn.");
        }

        String email = authentication.getName(); // Lấy email từ token
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại."));
    }

    @Override
    public void sendOtp(String email, com.example.smart_booking_system.enums.OtpType type) {
        String otpCode = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));

        long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000);
        otpStorage.put(email, new OtpInfo(otpCode, expiryTime));

        emailService.sendOtpEmail(email, otpCode, type);
    }

    @Override
    public boolean verifyOtp(String email, String code) {
        OtpInfo info = otpStorage.get(email);
        
        if (info == null) {
            return false;
        }

        if (System.currentTimeMillis() > info.getExpiryTime()) {
            otpStorage.remove(email);
            return false;
        }

        boolean isValid = info.getCode().equals(code);
        
        if (isValid) {
            otpStorage.remove(email);
        }
        
        return isValid;
    }

}