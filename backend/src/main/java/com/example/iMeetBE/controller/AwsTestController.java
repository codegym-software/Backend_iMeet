package com.example.iMeetBE.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.ListUsersRequest;
import com.amazonaws.services.cognitoidp.model.ListUsersResult;

@RestController
@RequestMapping("/api/aws")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://imeeet.netlify.app"}, allowCredentials = "true")
public class AwsTestController {

    @Autowired
    private AWSCognitoIdentityProvider cognitoClient;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    @Value("${aws.region:ap-southeast-2}")
    private String region;

    @GetMapping("/test-connection")
    public ResponseEntity<?> testAwsConnection() {
        try {
            // Test AWS Cognito connection by listing users
            ListUsersRequest request = new ListUsersRequest()
                .withUserPoolId(userPoolId)
                .withLimit(5);

            ListUsersResult response = cognitoClient.listUsers(request);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "AWS Cognito connection successful",
                "userCount", response.getUsers().size(),
                "userPoolId", userPoolId
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "AWS Cognito connection failed: " + e.getMessage(),
                "error", e.getClass().getSimpleName()
            ));
        }
    }

    @GetMapping("/config")
    public ResponseEntity<?> getAwsConfig() {
        try {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "userPoolId", userPoolId,
                "region", region,
                "message", "AWS configuration retrieved successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to get AWS configuration: " + e.getMessage()
            ));
        }
    }
}
