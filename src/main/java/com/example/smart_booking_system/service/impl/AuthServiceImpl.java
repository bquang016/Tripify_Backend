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
    private final Map<String, RegisterRequest> pendingRegistrations = new ConcurrentHashMap<>();

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
        checkOwnerEmail(email); // Giữ nguyên check cũ

        // 1. Tạo OTP
        String otpCode = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        LocalDateTime otpExpiry = LocalDateTime.now().plusMinutes(5);

        // 2. Lưu vào DB
        com.example.smart_booking_system.entity.OwnerApplication application =
                ownerApplicationRepository.findByEmail(email).orElseGet(() -> {
                    com.example.smart_booking_system.entity.OwnerApplication newApp = new com.example.smart_booking_system.entity.OwnerApplication();
                    newApp.setEmail(email);
                    newApp.setStatus(ApplicationStatus.PENDING);
                    return newApp;
                });

        application.setOtp(otpCode);
        application.setOtpExpiry(otpExpiry);
        application.setEmailVerified(false);
        ownerApplicationRepository.save(application);

        // ============================================================
        // 3. DEBUG TRÊN CONSOLE (Quan trọng: Lấy mã ở đây để test ngay)
        System.out.println("=============================================");
        System.out.println(">>> [DEBUG] OTP SAVED FOR: " + email);
        System.out.println(">>> [DEBUG] YOUR OTP IS: " + otpCode);
        System.out.println("=============================================");
        // ============================================================

        // 4. CHUẨN BỊ CONTEXT (Phải làm trước khi gửi mail)
        Context context = new Context();
        context.setVariable("otpCode", otpCode);
        // Các biến khác nếu template otp-2fa.html cần (dựa vào file bạn gửi thì chỉ cần otpCode là đủ)
        // context.setVariable("title", "Xác thực đăng ký Đối tác");

        // 5. GỬI MAIL (Sử dụng đúng template otp-2fa)
        try {
            System.out.println(">>> [DEBUG] PREPARING TO SEND EMAIL TO " + email + "...");

            // SỬA TÊN TEMPLATE Ở ĐÂY: "email/otp-2fa"
            emailService.sendHtmlEmail(email, "Mã xác thực đăng ký Đối tác - Tripify", "email/otp-email", context);

            System.out.println(">>> [DEBUG] EMAIL SERVICE CALLED SUCCESSFULLY.");
        } catch (Exception e) {
            // Log lỗi nhưng không chặn luồng chính để bạn vẫn nhập được OTP từ console
            System.err.println(">>> [ERROR] EMAIL SENDING FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    @Transactional
    public VerifyOwnerOtpResponse verifyOwnerOtp(VerifyOtpRequest request) {
        com.example.smart_booking_system.entity.OwnerApplication application = ownerApplicationRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Yêu cầu không hợp lệ. Vui lòng thử lại từ đầu."));

        if (application.getOtp() == null ||
            !application.getOtp().equals(request.getOtp()) ||
            application.getOtpExpiry() == null ||
            application.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Mã OTP không chính xác hoặc đã hết hạn.");
        }

        // Xóa OTP sau khi dùng
        otpStorage.remove(request.getEmail());

        // (Optional) Kiểm tra an toàn: Đảm bảo email chưa bị đăng ký bởi người khác trong lúc nhập OTP
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email này đã được đăng ký.");
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
        String normalizedEmail = request.getEmail().toLowerCase();
        if (!request.isPasswordMatching()) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        if (!isStrongPassword(request.getPassword())) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
        }

        Optional<User> existingUser = userRepository.findByEmail(normalizedEmail);

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            if ("SUSPENDED".equalsIgnoreCase(user.getStatus()) || "BANNED".equalsIgnoreCase(user.getStatus())) {
                throw new ForbiddenException("Tài khoản của bạn đã bị khóa. Không thể đăng ký lại với email này.");
            }

            throw new ConflictException("Email đã được đăng ký");
        }

        // Thay vì lưu vào DB, lưu vào Map tạm thời
        pendingRegistrations.put(normalizedEmail, request);
        
        // Gửi OTP qua email
        sendOtp(normalizedEmail, com.example.smart_booking_system.enums.OtpType.REGISTER);
    }

    @Override
    @Transactional
    public LoginResponse verifyRegisterOtp(VerifyOtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String cleanOtp = request.getOtp().trim();

        System.out.println("DEBUG: VerifyRegister attempt for: " + normalizedEmail + " with OTP: [" + cleanOtp + "]");

        // 1. Kiểm tra OTP
        if (verifyOtp(normalizedEmail, cleanOtp, com.example.smart_booking_system.enums.OtpType.REGISTER) == null) {
            throw new BadRequestException("Mã OTP không chính xác hoặc đã hết hạn.");
        }

        // 2. Lấy thông tin đăng ký tạm thời
        RegisterRequest registerRequest = pendingRegistrations.get(normalizedEmail);
        if (registerRequest == null) {
            System.out.println("DEBUG: Pending registration NOT FOUND for: " + normalizedEmail);
            throw new BadRequestException("Thông tin đăng ký không tồn tại hoặc đã hết hạn.");
        }

        // 3. Tạo User mới
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setFullName(registerRequest.getFullName());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setStatus("ACTIVE"); // Kích hoạt luôn
        user.setIsEmailVerified(true); // Đã xác thực qua OTP
        user.setProvider(com.example.smart_booking_system.enums.AuthProvider.local);

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Role 'CUSTOMER' not found"));
        user.addRole(customerRole);

        User savedUser = userRepository.save(user);
        
        // Xóa thông tin đăng ký tạm thời
        pendingRegistrations.remove(request.getEmail());

        // 4. Đăng nhập luôn và trả về token
        CustomUserDetails userDetails = CustomUserDetails.create(savedUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        String token = tokenProvider.generateToken(authentication);
        long expiresIn = tokenProvider.getExpirationTime();

        Set<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());

        LoginResponse.UserResponse userResponse = new LoginResponse.UserResponse(
                savedUser.getUserId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getPhoneNumber(),
                savedUser.getIsEmailVerified(),
                savedUser.getStatus(),
                roles
        );

        return new LoginResponse(token, expiresIn, userResponse);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. Chuẩn hóa email để tránh lỗi khoảng trắng hoặc chữ hoa/thường
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        
        // 2. Tìm user
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Email hoặc mật khẩu không chính xác"));

        // 3. CHECK XÁC THỰC EMAIL
        if (Boolean.FALSE.equals(user.getIsEmailVerified())) {
            throw new DisabledException("Tài khoản chưa được xác thực. Vui lòng kiểm tra email!");
        }

        // 4. CHECK TRẠNG THÁI KHÓA (SUSPENDED hoặc BANNED)
        if ("SUSPENDED".equalsIgnoreCase(user.getStatus()) || "BANNED".equalsIgnoreCase(user.getStatus())) {
            throw new LockedException("Tài khoản của bạn đã bị khóa: " + user.getStatus());
        }

        // 5. Kiểm tra mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Email hoặc mật khẩu không chính xác");
        }

        // 6. KIỂM TRA 2FA
        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            // Gửi OTP LOGIN_2FA
            sendOtp(normalizedEmail, com.example.smart_booking_system.enums.OtpType.LOGIN_2FA);
            return LoginResponse.twoFaRequired();
        }

        // 7. Nếu không bật 2FA hoặc mọi thứ hợp lệ, thực hiện authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
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
    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email: " + request.getEmail()));

        // Tạo mã OTP 6 số
        String otpCode = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        
        // 1. Lưu vào Database (để dùng cho resetPassword nếu không có email)
        user.setResetPasswordToken(otpCode);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(5));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // 2. Lưu vào Map (để đồng bộ với logic OTP chung)
        long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000);
        otpStorage.put(normalizedEmail, new OtpInfo(otpCode, expiryTime));

        // 3. Gửi OTP qua email (sử dụng template OTP)
        emailService.sendOtpEmail(normalizedEmail, otpCode, com.example.smart_booking_system.enums.OtpType.FORGOT_PASSWORD);
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

        // Tìm user bằng Token (UUID đã được sinh ra ở bước verify-otp)
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Mã xác thực không hợp lệ hoặc đã được sử dụng."));

        if (user.getResetPasswordTokenExpiry() == null || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Mã xác thực đã hết hạn.");
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
        // 1. Chuẩn hóa email
        String normalizedEmail = email.trim().toLowerCase();
        boolean userExists = userRepository.existsByEmail(normalizedEmail);

        // 2. Logic kiểm tra theo từng loại OTP
        // Đối với ĐĂNG KÝ: Không gửi OTP nếu email đã tồn tại
        if (type == com.example.smart_booking_system.enums.OtpType.REGISTER && userExists) {
            throw new ConflictException("Email này đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập.");
        }

        // Đối với QUÊN MẬT KHẨU hoặc 2FA: Email BUỘC PHẢI tồn tại
        if ((type == com.example.smart_booking_system.enums.OtpType.FORGOT_PASSWORD || 
             type == com.example.smart_booking_system.enums.OtpType.LOGIN_2FA) && !userExists) {
            throw new ResourceNotFoundException("Không tìm thấy tài khoản với email này.");
        }

        // 3. Tạo mã OTP 6 số
        String otpCode = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        System.out.println("DEBUG: OTP generated for " + normalizedEmail + " is: " + otpCode);

        // 4. Lưu vào bộ nhớ tạm (otpStorage) - Hết hạn sau 5 phút
        long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000);
        otpStorage.put(normalizedEmail, new OtpInfo(otpCode, expiryTime));

        // 5. Gửi email qua EmailService - Sử dụng email đã chuẩn hóa
        emailService.sendOtpEmail(normalizedEmail, otpCode, type);
    }

    @Override
    @Transactional
    public String verifyOtp(String email, String code, com.example.smart_booking_system.enums.OtpType type) {
        if (email == null || code == null) return null;
        
        String normalizedEmail = email.trim().toLowerCase();
        String cleanCode = code.trim();
        
        // 1. Kiểm tra OTP trong Map (otpStorage)
        OtpInfo info = otpStorage.get(normalizedEmail);
        if (info == null) return null;

        // Kiểm tra hết hạn
        if (System.currentTimeMillis() > info.getExpiryTime()) {
            otpStorage.remove(normalizedEmail);
            return null;
        }

        // Kiểm tra khớp mã
        if (!info.getCode().equals(cleanCode)) return null;

        // 2. Nếu OTP đúng và loại là FORGOT_PASSWORD, sinh Secure Token (UUID)
        if (type == com.example.smart_booking_system.enums.OtpType.FORGOT_PASSWORD) {
            User user = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
            String secureToken = UUID.randomUUID().toString();
            user.setResetPasswordToken(secureToken);
            user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user); // Sửa: Dùng userRepository để lưu
            otpStorage.remove(normalizedEmail);
            return secureToken;
        }

        // 3. Xóa OTP sau khi verify thành công cho các trường hợp còn lại (Register, 2FA, Login)
        otpStorage.remove(normalizedEmail);
        return "SUCCESS";
    }

    // --- 2FA IMPLEMENTATION (Giữ nguyên từ develop) ---

    @Override
    @Transactional
    public void request2faToggle(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Gửi OTP để xác nhận bật/tắt
        sendOtp(user.getEmail(), com.example.smart_booking_system.enums.OtpType.TWO_FACTOR_AUTH);
    }

    @Override
    @Transactional
    public void verify2faToggle(String userId, String otp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Gọi lại verifyOtp để kiểm tra mã
        if (verifyOtp(user.getEmail(), otp, com.example.smart_booking_system.enums.OtpType.TWO_FACTOR_AUTH) == null) {
            throw new BadRequestException("Mã OTP không chính xác hoặc đã hết hạn.");
        }

        // Đảo ngược trạng thái 2FA
        user.setTwoFactorEnabled(!Boolean.TRUE.equals(user.getTwoFactorEnabled()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public LoginResponse verify2faLogin(VerifyOtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        
        // 1. Kiểm tra OTP
        if (verifyOtp(normalizedEmail, request.getOtp(), com.example.smart_booking_system.enums.OtpType.LOGIN_2FA) == null) {
            throw new BadRequestException("Mã OTP không chính xác hoặc đã hết hạn.");
        }

        // 2. Nếu OTP đúng, tiến hành lấy thông tin user và tạo token
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CustomUserDetails userDetails = CustomUserDetails.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.generateToken(authentication);
        long expiresIn = tokenProvider.getExpirationTime();

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
    // --------------------------

}