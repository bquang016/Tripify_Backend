package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.dto.request.auth.*;
import com.example.smart_booking_system.dto.response.auth.LoginResponse;
import com.example.smart_booking_system.entity.Role;
import com.example.smart_booking_system.entity.SocialAccount;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.exception.*;
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

    // ✅ 1. HÀM KIỂM TRA ĐỘ MẠNH MẬT KHẨU (PRIVATE HELPER)
    private boolean isStrongPassword(String password) {
        // Regex: 
        // (?=.*[0-9])       : Ít nhất 1 số
        // (?=.*[a-z])       : Ít nhất 1 chữ thường
        // (?=.*[A-Z])       : Ít nhất 1 chữ hoa
        // (?=.*[@#$%^&+=!]) : Ít nhất 1 ký tự đặc biệt
        // .{8,}             : Độ dài tối thiểu 8 ký tự
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!.*])(?=\\S+$).{8,}$";
        return password != null && password.matches(regex);
    }
    // --- CÁC HÀM MỚI CHO OWNER REGISTRATION (ĐÃ SỬA LỖI) ---

    @Override
    public void sendOwnerRegistrationOtp(String email) {
        // 1. Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email này đã được đăng ký.");
        }

        // 2. Tạo OTP 6 số (Tái sử dụng logic của hàm sendOtp có sẵn)
        String otpCode = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));

        // 3. Lưu vào Map otpStorage (dùng biến có sẵn của bạn)
        long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000); // 5 phút
        otpStorage.put(email, new OtpInfo(otpCode, expiryTime));

        // 4. Gửi Email
        org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
        context.setVariable("otpCode", otpCode);
        context.setVariable("title", "Xác thực đăng ký Đối tác");
        context.setVariable("message", "Sử dụng mã bên dưới để hoàn tất đăng ký đối tác Tripify.");

        emailService.sendHtmlEmail(email, "Mã xác thực đăng ký Đối tác", "email/otp-email", context);
    }

    @Override
    @Transactional // Nên thêm Transactional để đảm bảo toàn vẹn dữ liệu
    public LoginResponse verifyOwnerOtpAndRegister(OwnerRegisterRequest request) {
        // 1. Validate OTP
        OtpInfo info = otpStorage.get(request.getEmail());
        if (info == null || !info.getCode().equals(request.getOtp()) || System.currentTimeMillis() > info.getExpiryTime()) {
            throw new BadRequestException("Mã OTP không chính xác hoặc đã hết hạn.");
        }

        // Xóa OTP sau khi dùng
        otpStorage.remove(request.getEmail());

        // (Optional) Kiểm tra an toàn: Đảm bảo email chưa bị đăng ký bởi người khác trong lúc nhập OTP
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email này đã được đăng ký.");
        }

        // 2. Tạo User mới role OWNER
        User user = new User();
        user.setUserId(UUID.randomUUID().toString()); // Tạo ID
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName("Partner " + request.getEmail());
        user.setIsEmailVerified(true);
        user.setStatus("ACTIVE");

        // Dùng AuthProvider.local (chữ thường - khớp với Enum của bạn)
        user.setProvider(com.example.smart_booking_system.enums.AuthProvider.local);

        Role ownerRole = roleRepository.findByRoleName("OWNER")
                .orElseThrow(() -> new ResourceNotFoundException("Role OWNER not found"));
        user.addRole(ownerRole);

        User savedUser = userRepository.save(user);

        // 3. Generate Token
        // Tạo CustomUserDetails từ user vừa lưu để nạp vào Context
        com.example.smart_booking_system.security.CustomUserDetails userDetails =
                com.example.smart_booking_system.security.CustomUserDetails.create(savedUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        // --- ĐOẠN SỬA LỖI: CHỈ KHAI BÁO 1 LẦN ---
        String accessToken = tokenProvider.generateToken(authentication);
        long expiresIn = tokenProvider.getExpirationTime();
        // ----------------------------------------

        // 4. Trả về LoginResponse
        Set<String> roles = java.util.Set.of("OWNER");

        LoginResponse.UserResponse userResponse = new LoginResponse.UserResponse(
                savedUser.getUserId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getPhoneNumber(),
                savedUser.getIsEmailVerified(),
                savedUser.getStatus(),
                roles
        );

        return new LoginResponse(accessToken, expiresIn, userResponse);
    }

    // ✅ Tạo mật khẩu (cho user social chưa có pass)
    @Override
    public void createPassword(String userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getPasswordHash() != null && user.getPasswordHash().startsWith("$2a$")) {
            throw new BadRequestException("Tài khoản này đã có mật khẩu. Vui lòng dùng chức năng Đổi mật khẩu.");
        }

        // Check độ mạnh
        if (!isStrongPassword(newPassword)) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ✅ Ngắt kết nối MXH
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

    // ✅ Đăng ký người dùng mới
    @Override
    @Transactional
    public void register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase();
        if (!request.isPasswordMatching()) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        // Check độ mạnh mật khẩu
        if (!isStrongPassword(request.getPassword())) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
        }

        Optional<User> existingUser = userRepository.findByEmail(normalizedEmail);

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            // Nếu tài khoản tồn tại VÀ đang bị khóa -> Báo lỗi chặn đăng ký
            if ("SUSPENDED".equalsIgnoreCase(user.getStatus()) || "BANNED".equalsIgnoreCase(user.getStatus())) {
                throw new ForbiddenException("Tài khoản của bạn đã bị khóa. Không thể đăng ký lại với email này.");
            }

            // Nếu tài khoản tồn tại nhưng không bị khóa (đang Active/Inactive) -> Báo lỗi trùng email như cũ
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
        if (!verifyOtp(normalizedEmail, cleanOtp)) {
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

        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
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

    // ✅ Đăng nhập
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. Tìm user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Email hoặc mật khẩu không chính xác"));

        // 2. CHECK XÁC THỰC EMAIL
        // ❌ CŨ: throw new UnauthorizedException(...) -> Trả về 401 (Sai Logic)
        // ✅ MỚI: throw new DisabledException(...) -> Trả về 403 (Đúng Logic Frontend cần)
        if (Boolean.FALSE.equals(user.getIsEmailVerified())) {
            throw new DisabledException("Tài khoản chưa được xác thực. Vui lòng kiểm tra email!");
        }

        // 3. CHECK TRẠNG THÁI KHÓA
        // ✅ MỚI: Dùng LockedException hoặc DisabledException để trả về 403
        if ("SUSPENDED".equalsIgnoreCase(user.getStatus()) || "BANNED".equalsIgnoreCase(user.getStatus())) {
            throw new LockedException("Tài khoản của bạn đã bị khóa: " + user.getStatus());
        }

        // 4. Nếu qua được các bước trên thì mới check mật khẩu
        // Nếu sai mật khẩu ở đây, nó sẽ tự ném BadCredentialsException (401)
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

    // ✅ Xác minh email
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

    // ✅ Quên mật khẩu
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

    // ✅ Đặt lại mật khẩu (Reset Password)
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        // Check độ mạnh mật khẩu
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

    // ✅ Đổi mật khẩu (Change Password)
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

        // Check độ mạnh mật khẩu
        if (!isStrongPassword(request.getNewPassword())) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // ✅ Gửi lại email xác minh
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
        String normalizedEmail = email.toLowerCase();
        boolean userExists = userRepository.existsByEmail(normalizedEmail);

        // 1. Đối với ĐĂNG KÝ: Không được phép gửi OTP nếu email đã có trong hệ thống
        if (type == com.example.smart_booking_system.enums.OtpType.REGISTER && userExists) {
            throw new ConflictException("Email này đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập.");
        }

        // 2. Đối với QUÊN MẬT KHẨU hoặc 2FA: Email BUỘC PHẢI tồn tại thì mới gửi được
        if ((type == com.example.smart_booking_system.enums.OtpType.FORGOT_PASSWORD || 
             type == com.example.smart_booking_system.enums.OtpType.TWO_FACTOR_AUTH) && !userExists) {
            throw new ResourceNotFoundException("Không tìm thấy tài khoản với email này.");
        }

        // Tạo mã OTP 6 số
        String otpCode = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        System.out.println("DEBUG: OTP generated for " + normalizedEmail + " is: " + otpCode);

        // Lưu vào Map - Hết hạn sau 5 phút
        long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000);
        otpStorage.put(normalizedEmail, new OtpInfo(otpCode, expiryTime));

        // Gửi email qua EmailService
        emailService.sendOtpEmail(normalizedEmail, otpCode, type);
    }

    @Override
    public boolean verifyOtp(String email, String code) {
        if (email == null || code == null) return false;
        
        String normalizedEmail = email.trim().toLowerCase();
        String cleanCode = code.trim();
        
        OtpInfo info = otpStorage.get(normalizedEmail);
        
        if (info == null) {
            System.out.println("DEBUG: OTP Storage - NO DATA for email: " + normalizedEmail);
            return false;
        }

        // Kiểm tra hết hạn
        if (System.currentTimeMillis() > info.getExpiryTime()) {
            System.out.println("DEBUG: OTP Storage - EXPIRED for email: " + normalizedEmail);
            otpStorage.remove(normalizedEmail);
            return false;
        }

        // Kiểm tra khớp mã
        boolean isValid = info.getCode().equals(cleanCode);
        System.out.println("DEBUG: OTP Comparison for " + normalizedEmail + ": Expected=[" + info.getCode() + "], Received=[" + cleanCode + "], Match=" + isValid);
        
        // TẠM THỜI KHÔNG XÓA ĐỂ DEBUG - Sẽ xóa bằng scheduler sau
        // if (isValid) {
        //     otpStorage.remove(normalizedEmail); 
        // }
        
        return isValid;
    }

}