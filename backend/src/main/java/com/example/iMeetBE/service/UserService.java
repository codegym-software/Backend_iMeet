package com.example.iMeetBE.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.AWSCognitoIdentityProviderException;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.example.iMeetBE.dto.UpdateProfileRequest;
import com.example.iMeetBE.model.User;
import com.example.iMeetBE.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private AWSCognitoIdentityProvider cognitoClient;

    @Autowired
    private UserRepository userRepository;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

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
}
