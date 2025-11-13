
package com.example.iMeetBE.controller;

import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.example.iMeetBE.config.CognitoHostedUIConfig;
import com.example.iMeetBE.service.CognitoService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/oauth2")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class OAuth2Controller {

    @GetMapping("/hosted-ui/login-url-force")
    public Map<String, String> getHostedUILoginUrlForcePrompt() {
        String loginUrl = cognitoConfig.buildLoginUrlWithPrompt();
        return Map.of(
            "loginUrl", loginUrl,
            "message", "Use this URL for Cognito Hosted UI login (force account selection)"
        );
    }

    @Autowired
    private CognitoHostedUIConfig cognitoConfig;

    @Autowired
    private CognitoService cognitoService;

    @Value("${spring.security.oauth2.client.registration.cognito.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.cognito.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.cognito.issuer-uri}")
    private String issuerUri;

    @GetMapping("/user")
    public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
        try {
            if (principal == null) {
                return Map.of("authenticated", false, "error", "No principal found");
            }
            
            // Xử lý null values để tránh lỗi
            String username = principal.getAttribute("username");
            String email = principal.getAttribute("email");
            String name = principal.getAttribute("name");
            String picture = principal.getAttribute("picture");
            String sub = principal.getAttribute("sub");
            
            if (username == null) username = "";
            if (email == null) email = "";
            if (name == null) name = "";
            if (picture == null) picture = "";
            if (sub == null) sub = "";
            
            // Tự động lưu user vào database khi đăng nhập OAuth2
            try {
                if (email != null && !email.isEmpty()) {
                    cognitoService.createOrUpdateUserFromOAuth2(email, username, name, picture, sub);
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not save OAuth2 user to database: " + e.getMessage());
                // Không throw exception để không ảnh hưởng đến quá trình đăng nhập
            }
            
            return Map.of(
                "authenticated", true,
                "username", username,
                "email", email,
                "name", name,
                "fullName", name, // Trả về name từ Cognito
                "picture", picture, // Trả về ảnh avatar từ Cognito
                "sub", sub,
                "attributes", principal.getAttributes() != null ? principal.getAttributes() : Map.of()
            );
        } catch (Exception e) {
            return Map.of("authenticated", false, "error", e.getMessage());
        }
    }
    
    @PostMapping("/clear-session")
    public Map<String, Object> clearSession(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Invalidate session
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            
            // Clear any cookies
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
            }
            
            return Map.of("success", true, "message", "Session cleared");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    @PostMapping("/refresh")
    public Map<String, Object> refreshSession(@AuthenticationPrincipal OAuth2User principal) {
        if (principal != null) {
            return Map.of(
                "success", true,
                "authenticated", true,
                "username", principal.getAttribute("username"),
                "email", principal.getAttribute("email"),
                "name", principal.getAttribute("name"),
                "fullName", principal.getAttribute("name"),
                "picture", principal.getAttribute("picture"),
                "sub", principal.getAttribute("sub"),
                "attributes", principal.getAttributes(),
                "timestamp", System.currentTimeMillis()
            );
        } else {
            return Map.of(
                "success", false,
                "authenticated", false,
                "message", "No active session found"
            );
        }
    }

    @GetMapping("/login-url")
    public Map<String, String> getLoginUrl() {
        // Tạo login URL với prompt=select_account để luôn hiện màn hình chọn tài khoản
        String loginUrl = String.format(
            "/oauth2/authorization/cognito?prompt=select_account"
        );
        
        return Map.of(
            "loginUrl", loginUrl,
            "message", "Redirect to this URL to start Cognito authentication"
        );
    }
    
    
    @GetMapping("/logout-url")
    public Map<String, String> getLogoutUrl() {
        // Tạo Cognito logout URL để xóa session hoàn toàn
        // Sử dụng logout endpoint của Cognito User Pool
        String logoutUrl = String.format(
            "https://ap-southeast-2f7b4rq9tj.auth.ap-southeast-2.amazoncognito.com/logout?" +
            "client_id=%s&" +
            "logout_uri=%s",
            clientId,
            "https://imeettt.netlify.app/login,http://localhost:3000/login,http://localhost:3001/login"
        );
        
        return Map.of(
            "logoutUrl", logoutUrl,
            "message", "Use this URL to logout from Cognito completely"
        );
    }
    
    @GetMapping("/logout")
    public ResponseEntity<Void> cognitoLogout() {
        // Redirect trực tiếp đến Cognito logout URL với các parameter để clear session hoàn toàn
        String logoutUrl = String.format(
            "https://ap-southeast-2f7b4rq9tj.auth.ap-southeast-2.amazoncognito.com/logout?" +
            "client_id=%s&" +
            "logout_uri=%s&" +
            "response_mode=query",
            clientId,
            "http://localhost:3000/login,http://localhost:3001/login"
        );
        
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", logoutUrl)
                .build();
    }

    @GetMapping("/hosted-ui/login-url")
    public Map<String, String> getHostedUILoginUrl() {
        String loginUrl = cognitoConfig.buildLoginUrl();
        return Map.of(
            "loginUrl", loginUrl,
            "message", "Use this URL for Cognito Hosted UI login"
        );
    }

    @GetMapping("/hosted-ui/logout-url")
    public Map<String, String> getHostedUILogoutUrl() {
        String logoutUrl = cognitoConfig.buildLogoutUrl();
        return Map.of(
            "logoutUrl", logoutUrl,
            "message", "Use this URL for Cognito Hosted UI logout"
        );
    }

    @PostMapping("/exchange-code")
    public ResponseEntity<Map<String, Object>> exchangeCode(@RequestBody Map<String, String> request) {
        String authorizationCode = request.get("code");
        String state = request.get("state");

        if (authorizationCode == null || authorizationCode.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Authorization code is required"));
        }

        try {
            // Tạo token endpoint URL từ issuer URI
            String tokenEndpoint = issuerUri.replace("/ap-southeast-2_f7B4rq9tJ", "") + "/oauth2/token";

            // Chuẩn bị request body
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("client_id", clientId);
            body.add("code", authorizationCode);
            body.add("redirect_uri", "https://imeettt.netlify.app/oauth2/callback,http://localhost:3000/oauth2/callback,http://localhost:3001/oauth2/callback");

            // Chuẩn bị headers với Basic Authentication
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String auth = clientId + ":" + clientSecret;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            // Gọi Cognito token endpoint
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.exchange(
                tokenEndpoint,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();
                
                // Parse ID token để lấy user info
                String idToken = (String) tokenResponse.get("id_token");
                if (idToken != null) {
                    try {
                        // Decode JWT payload (simple decoding, không verify signature)
                        String[] chunks = idToken.split("\\.");
                        String payload = new String(Base64.getUrlDecoder().decode(chunks[1]));
                        
                        // Parse JSON (simple approach, có thể dùng ObjectMapper cho production)
                        // Ở đây trả về token response cùng với parsed user info
                        return ResponseEntity.ok(Map.of(
                            "accessToken", tokenResponse.get("access_token"),
                            "idToken", idToken,
                            "tokenType", tokenResponse.get("token_type"),
                            "expiresIn", tokenResponse.get("expires_in"),
                            "refreshToken", tokenResponse.getOrDefault("refresh_token", ""),
                            "tokenResponse", tokenResponse,
                            "message", "Tokens exchanged successfully"
                        ));
                    } catch (Exception e) {
                        return ResponseEntity.ok(tokenResponse);
                    }
                }
                
                return ResponseEntity.ok(tokenResponse);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to exchange authorization code"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error exchanging code: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra và đồng bộ user OAuth2 vào database
     */
    @PostMapping("/sync-user")
    public Map<String, Object> syncOAuth2User(@AuthenticationPrincipal OAuth2User principal) {
        try {
            if (principal == null) {
                return Map.of("success", false, "error", "No authenticated user found");
            }
            
            String email = principal.getAttribute("email");
            String username = principal.getAttribute("username");
            String name = principal.getAttribute("name");
            String picture = principal.getAttribute("picture");
            String sub = principal.getAttribute("sub");
            
            if (email == null || email.isEmpty()) {
                return Map.of("success", false, "error", "Email not found in OAuth2 user");
            }
            
            // Đồng bộ user vào database
            com.example.iMeetBE.model.User user = cognitoService.createOrUpdateUserFromOAuth2(
                email, username, name, picture, sub
            );
            
            return Map.of(
                "success", true,
                "message", "User synced to database successfully",
                "user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "username", user.getUsername(),
                    "fullName", user.getFullName(),
                    "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                    "googleId", user.getGoogleId() != null ? user.getGoogleId() : ""
                )
            );
            
        } catch (Exception e) {
            return Map.of("success", false, "error", "Failed to sync user: " + e.getMessage());
        }
    }
}