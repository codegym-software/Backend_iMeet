package com.example.iMeetBE.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.AWSCognitoIdentityProviderException;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.example.iMeetBE.dto.UpdateProfileRequest;
import com.example.iMeetBE.dto.UserPreferencesRequest;
import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserService {

    @Autowired
    private AWSCognitoIdentityProvider cognitoClient;

    @Autowired
    private UserRepository userRepository;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    @Autowired
    private ObjectMapper objectMapper;

    public void updateUserProfile(String username, UpdateProfileRequest request) {
        try {
            // 1. Cập nhật database trước
            User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found in database: " + username));
            
            boolean updated = false;
            
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                user.setEmail(request.getEmail());
                updated = true;
            }

            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                user.setFullName(request.getName());
                updated = true;
            }
            
            if (updated) {
                userRepository.save(user);
            }

            // 2. Cập nhật Cognito (nếu cần)
            List<AttributeType> attributes = new ArrayList<>();

            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                attributes.add(new AttributeType()
                    .withName("email")
                    .withValue(request.getEmail()));
            }

            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                attributes.add(new AttributeType()
                    .withName("name")
                    .withValue(request.getName()));
            }

            if (!attributes.isEmpty()) {
                AdminUpdateUserAttributesRequest updateRequest = new AdminUpdateUserAttributesRequest()
                    .withUserPoolId(userPoolId)
                    .withUsername(username)
                    .withUserAttributes(attributes);

                cognitoClient.adminUpdateUserAttributes(updateRequest);
            }

        } catch (AWSCognitoIdentityProviderException e) {
            System.err.println("❌ Error updating user profile in Cognito: " + e.getMessage());
            throw new RuntimeException("Failed to update user profile: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error updating user profile: " + e.getMessage());
            throw new RuntimeException("Failed to update user profile: " + e.getMessage());
        }
    }

    public void updateUserProfile(String username, Map<String, Object> profileData) {
        try {
            List<AttributeType> attributes = new ArrayList<>();

            // Map profile data to Cognito attributes
            if (profileData.containsKey("email")) {
                attributes.add(new AttributeType()
                    .withName("email")
                    .withValue(profileData.get("email").toString()));
            }

            if (profileData.containsKey("name")) {
                attributes.add(new AttributeType()
                    .withName("name")
                    .withValue(profileData.get("name").toString()));
            }

            if (profileData.containsKey("given_name")) {
                attributes.add(new AttributeType()
                    .withName("given_name")
                    .withValue(profileData.get("given_name").toString()));
            }

            if (profileData.containsKey("family_name")) {
                attributes.add(new AttributeType()
                    .withName("family_name")
                    .withValue(profileData.get("family_name").toString()));
            }

            if (profileData.containsKey("phone_number")) {
                attributes.add(new AttributeType()
                    .withName("phone_number")
                    .withValue(profileData.get("phone_number").toString()));
            }

            if (profileData.containsKey("picture")) {
                attributes.add(new AttributeType()
                    .withName("picture")
                    .withValue(profileData.get("picture").toString()));
            }

            if (profileData.containsKey("locale")) {
                attributes.add(new AttributeType()
                    .withName("locale")
                    .withValue(profileData.get("locale").toString()));
            }

            if (profileData.containsKey("zoneinfo")) {
                attributes.add(new AttributeType()
                    .withName("zoneinfo")
                    .withValue(profileData.get("zoneinfo").toString()));
            }

            // Update user attributes in Cognito
            AdminUpdateUserAttributesRequest updateRequest = new AdminUpdateUserAttributesRequest()
                .withUserPoolId(userPoolId)
                .withUsername(username)
                .withUserAttributes(attributes);

            cognitoClient.adminUpdateUserAttributes(updateRequest);

        } catch (AWSCognitoIdentityProviderException e) {
            System.err.println("❌ Error updating user profile in Cognito: " + e.getMessage());
            throw new RuntimeException("Failed to update user profile: " + e.getMessage());
        }
    }

    public void updateUserProfile(String username, String email, String name, String givenName, String familyName) {
        try {
            List<AttributeType> attributes = new ArrayList<>();

            if (email != null && !email.isEmpty()) {
                attributes.add(new AttributeType().withName("email").withValue(email));
            }

            if (name != null && !name.isEmpty()) {
                attributes.add(new AttributeType().withName("name").withValue(name));
            }

            if (givenName != null && !givenName.isEmpty()) {
                attributes.add(new AttributeType().withName("given_name").withValue(givenName));
            }

            if (familyName != null && !familyName.isEmpty()) {
                attributes.add(new AttributeType().withName("family_name").withValue(familyName));
            }

            AdminUpdateUserAttributesRequest updateRequest = new AdminUpdateUserAttributesRequest()
                .withUserPoolId(userPoolId)
                .withUsername(username)
                .withUserAttributes(attributes);

            cognitoClient.adminUpdateUserAttributes(updateRequest);

        } catch (AWSCognitoIdentityProviderException e) {
            System.err.println("❌ Error updating user profile in Cognito: " + e.getMessage());
            throw new RuntimeException("Failed to update user profile: " + e.getMessage());
        }
    }

    @Transactional
    public void updateUserPreferences(String userId, UserPreferencesRequest preferences) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            
            // Convert preferences object to JSON string
            String preferencesJson = objectMapper.writeValueAsString(preferences);
            user.setPreferences(preferencesJson);
            userRepository.save(user);
            
            System.out.println("✅ Updated preferences for user: " + userId);
        } catch (Exception e) {
            System.err.println("❌ Error updating user preferences: " + e.getMessage());
            throw new RuntimeException("Failed to update user preferences: " + e.getMessage());
        }
    }

    public UserPreferencesRequest getUserPreferences(String userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            
            String preferencesJson = user.getPreferences();
            if (preferencesJson == null || preferencesJson.trim().isEmpty()) {
                // Return default preferences if none exist
                return new UserPreferencesRequest();
            }
            
            // Parse JSON string to preferences object
            return objectMapper.readValue(preferencesJson, UserPreferencesRequest.class);
        } catch (Exception e) {
            System.err.println("❌ Error getting user preferences: " + e.getMessage());
            throw new RuntimeException("Failed to get user preferences: " + e.getMessage());
        }
    }
}
