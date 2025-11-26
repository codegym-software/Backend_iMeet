package com.example.iMeetBE.security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Xử lý lỗi access denied (403) cho API requests
 * Trả về 403 JSON thay vì redirect
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        // Chỉ trả về 403 cho API requests
        if (request.getRequestURI().startsWith("/api/")) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(
                "{\"error\":\"Forbidden\",\"message\":\"Access denied. Insufficient permissions.\",\"status\":403}"
            );
        } else {
            // Cho non-API requests
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
        }
    }
}


