package com.example.iMeetBE.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.iMeetBE.dto.ChangePasswordResponse;
import com.example.iMeetBE.dto.LoginResponse;
import com.example.iMeetBE.dto.SignupResponse;
import com.example.iMeetBE.model.User;
import com.example.iMeetBE.model.UserRole;
import com.example.iMeetBE.repository.UserRepository;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CognitoService cognitoService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public LoginResponse login(String usernameOrEmail, String password) {
        User user = null;
        
        // Thử tìm user bằng email trước
        if (usernameOrEmail.contains("@")) {
            user = userRepository.findByEmail(usernameOrEmail).orElse(null);
        }
        
        // Nếu không tìm thấy bằng email, thử tìm bằng username
        if (user == null) {
            user = userRepository.findByUsername(usernameOrEmail).orElse(null);
        }
        
        if (user == null) {
            throw new RuntimeException("Tên đăng nhập hoặc email không tồn tại");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu không đúng");
        }

        // Tạo JWT token
        String token = jwtService.generateToken(user.getEmail(), user.getUsername(), user.getId());
        
        // Lưu token vào database
        user.setAccessToken(token);
        userRepository.save(user);
        

        return new LoginResponse(true, "Đăng nhập thành công", token, user.getId(), user.getUsername(), user.getFullName(), user.getEmail(), user.getAvatarUrl(), user.getRole().name());
    }

    public SignupResponse signup(String username, String email, String password, String fullName) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        User user = new User();
        // Tạo ID duy nhất cho traditional user (có thể sử dụng UUID hoặc email-based)
        String userId = "traditional_" + email.replace("@", "_").replace(".", "_");
        user.setId(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(UserRole.USER);

        userRepository.save(user);

        // Tự động đồng bộ user lên Cognito (optional)
        try {
            if (!cognitoService.userExistsInCognito(email)) {
                cognitoService.createUserInCognito(user);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Không thể đồng bộ user lên Cognito: " + e.getMessage());
            // Không throw exception để không ảnh hưởng đến signup process
        }

        return new SignupResponse(true, "Đăng ký thành công", user.getId(), user.getUsername(), user.getEmail());
    }

    public ChangePasswordResponse changePassword(String email, String currentPassword, String newPassword, String confirmPassword) {
        // Validation
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("Mật khẩu mới không được để trống");
        }
        
        if (newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }
        
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }
        
        // Tìm user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        
        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }
        
        // Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        return new ChangePasswordResponse(true, "Đổi mật khẩu thành công");
    }

    // Validate JWT token từ database
    public boolean validateTokenFromDatabase(String token) {
        try {
            // Lấy email từ token
            String email = jwtService.getEmailFromToken(token);
            
            // Tìm user trong database
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            
            // Kiểm tra token có khớp với token trong database không
            if (token.equals(user.getAccessToken())) {
                // Kiểm tra token có hợp lệ không (chưa hết hạn)
                return jwtService.validateToken(token);
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // Lấy user từ token
    public User getUserFromToken(String token) {
        try {
            String email = jwtService.getEmailFromToken(token);
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } catch (Exception e) {
            throw new RuntimeException("Invalid token");
        }
    }

    // Upload avatar - lưu vào database
    public String uploadAvatar(User user, MultipartFile file) throws IOException {
        try {
            // Kiểm tra file
            if (file.isEmpty()) {
                throw new IOException("File không được để trống");
            }
            
            // Lấy dữ liệu file
            byte[] fileData = file.getBytes();
            String contentType = file.getContentType();
            
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IOException("File phải là ảnh");
            }
            
            // Tạo base64 data URL
            String base64Data = "data:" + contentType + ";base64," + 
                java.util.Base64.getEncoder().encodeToString(fileData);
            
            // Cập nhật user với base64 data URL
            user.setAvatarUrl(base64Data);
            userRepository.save(user);
            
            return base64Data;
        } catch (IOException e) {
            throw new IOException("Không thể lưu ảnh: " + e.getMessage());
        }
    }

    // Xóa avatar
    public boolean removeAvatar(User user) {
        try {
            if (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty()) {
                return false; // User không có avatar
            }
            
            // Xóa avatar từ database
            user.setAvatarUrl(null);
            userRepository.save(user);
            
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Không thể xóa avatar: " + e.getMessage());
        }
    }

}