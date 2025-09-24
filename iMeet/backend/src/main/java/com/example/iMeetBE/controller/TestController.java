package com.example.iMeetBE.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.iMeetBE.service.CognitoService;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private CognitoService cognitoService;

    /**
     * Test endpoint để kiểm tra kết nối Cognito
     */
    @GetMapping("/cognito-connection")
    public ResponseEntity<?> testCognitoConnection() {
        try {
            // Test với một email giả
            boolean exists = cognitoService.userExistsInCognito("test@example.com");
            
            return ResponseEntity.ok().body("✅ Cognito connection successful. Test result: " + exists);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("❌ Cognito connection failed: " + e.getMessage());
        }
    }
}
