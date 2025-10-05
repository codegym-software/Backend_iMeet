package com.example.iMeetBE.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.iMeetBE.dto.ApiResponse;
import com.example.iMeetBE.dto.ForgotPasswordRequest;
import com.example.iMeetBE.dto.ResetPasswordRequest;
import com.example.iMeetBE.dto.VerifyCodeRequest;
import com.example.iMeetBE.service.ForgotPasswordService;

@RestController
@RequestMapping("/api/forgot-password")
@CrossOrigin(origins = "http://localhost:3000")
public class ForgotPasswordController {
    
    @Autowired
    private ForgotPasswordService forgotPasswordService;
    
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Object>> sendVerificationCode(@RequestBody ForgotPasswordRequest request) {
        try {
            ApiResponse<Object> response = forgotPasswordService.sendVerificationCode(request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage()));
        }
    }
    
    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<Object>> verifyCode(@RequestBody VerifyCodeRequest request) {
        try {
            ApiResponse<Object> response = forgotPasswordService.verifyCode(request.getEmail(), request.getCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage()));
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            ApiResponse<Object> response = forgotPasswordService.resetPassword(
                request.getEmail(), 
                request.getCode(), 
                request.getNewPassword(), 
                request.getConfirmPassword()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage()));
        }
    }
    
}
//