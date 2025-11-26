package com.example.iMeetBE.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Xử lý lỗi authentication cho API requests
 * Trả về 401 JSON thay vì redirect đến login page
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        
        // Chỉ trả về 401 cho API requests, không redirect
        if (request.getRequestURI().startsWith("/api/")) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(
                "{\"error\":\"Unauthorized\",\"message\":\"Authentication required\",\"status\":401}"
            );
        } else {
            // Cho non-API requests, có thể redirect (nhưng thường không cần)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        }
    }
}


