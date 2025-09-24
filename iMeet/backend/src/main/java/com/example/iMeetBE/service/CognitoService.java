package com.example.iMeetBE.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AWSCognitoIdentityProviderException;
import com.amazonaws.services.cognitoidp.model.AdminCreateUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminCreateUserResult;
import com.amazonaws.services.cognitoidp.model.AdminDeleteUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.DeliveryMediumType;
import com.amazonaws.services.cognitoidp.model.MessageActionType;
import com.amazonaws.services.cognitoidp.model.UserNotFoundException;
import com.example.iMeetBE.model.User;

@Service
public class CognitoService {

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    @Value("${aws.cognito.region:ap-southeast-2}")
    private String region;

    @Value("${aws.access-key-id}")
    private String accessKeyId;

    @Value("${aws.secret-access-key}")
    private String secretAccessKey;

    private AWSCognitoIdentityProvider getCognitoClient() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        return AWSCognitoIdentityProviderClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(Regions.fromName(region))
                .build();
    }

    /**
     * Tạo user trong Cognito User Pool từ database user
     */
    public String createUserInCognito(User user) {
        try {
            AWSCognitoIdentityProvider cognitoClient = getCognitoClient();

            // Tạo attributes cho user
            List<AttributeType> attributes = new ArrayList<>();
            attributes.add(new AttributeType().withName("email").withValue(user.getEmail()));
            attributes.add(new AttributeType().withName("email_verified").withValue("true"));
            
            if (user.getFullName() != null && !user.getFullName().isEmpty()) {
                attributes.add(new AttributeType().withName("name").withValue(user.getFullName()));
            }
            
            if (user.getPictureUrl() != null && !user.getPictureUrl().isEmpty()) {
                attributes.add(new AttributeType().withName("picture").withValue(user.getPictureUrl()));
            }

            AdminCreateUserRequest createUserRequest = new AdminCreateUserRequest()
                    .withUserPoolId(userPoolId)
                    .withUsername(user.getEmail()) // Sử dụng email làm username
                    .withUserAttributes(attributes)
                    .withTemporaryPassword("TempPass123!") // Password tạm thời
                    .withMessageAction(MessageActionType.SUPPRESS) // Không gửi email
                    .withDesiredDeliveryMediums(DeliveryMediumType.EMAIL);

            AdminCreateUserResult result = cognitoClient.adminCreateUser(createUserRequest);
            
            System.out.println("✅ User created in Cognito: " + user.getEmail());
            System.out.println("   Cognito User ID: " + result.getUser().getUsername());
            
            return result.getUser().getUsername();
            
        } catch (AWSCognitoIdentityProviderException e) {
            System.err.println("❌ Error creating user in Cognito: " + e.getMessage());
            throw new RuntimeException("Failed to create user in Cognito: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra user có tồn tại trong Cognito không
     */
    public boolean userExistsInCognito(String email) {
        try {
            AWSCognitoIdentityProvider cognitoClient = getCognitoClient();
            
            AdminGetUserRequest getUserRequest = new AdminGetUserRequest()
                    .withUserPoolId(userPoolId)
                    .withUsername(email);
            
            cognitoClient.adminGetUser(getUserRequest);
            return true;
            
        } catch (UserNotFoundException e) {
            return false;
        } catch (AWSCognitoIdentityProviderException e) {
            System.err.println("❌ Error checking user in Cognito: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật user attributes trong Cognito
     */
    public void updateUserInCognito(User user) {
        try {
            AWSCognitoIdentityProvider cognitoClient = getCognitoClient();

            List<AttributeType> attributes = new ArrayList<>();
            
            if (user.getFullName() != null && !user.getFullName().isEmpty()) {
                attributes.add(new AttributeType().withName("name").withValue(user.getFullName()));
            }
            
            if (user.getPictureUrl() != null && !user.getPictureUrl().isEmpty()) {
                attributes.add(new AttributeType().withName("picture").withValue(user.getPictureUrl()));
            }

            if (!attributes.isEmpty()) {
                AdminUpdateUserAttributesRequest updateRequest = new AdminUpdateUserAttributesRequest()
                        .withUserPoolId(userPoolId)
                        .withUsername(user.getEmail())
                        .withUserAttributes(attributes);

                cognitoClient.adminUpdateUserAttributes(updateRequest);
                System.out.println("✅ User updated in Cognito: " + user.getEmail());
            }
            
        } catch (AWSCognitoIdentityProviderException e) {
            System.err.println("❌ Error updating user in Cognito: " + e.getMessage());
            throw new RuntimeException("Failed to update user in Cognito: " + e.getMessage());
        }
    }

    /**
     * Xóa user khỏi Cognito User Pool
     */
    public void deleteUserFromCognito(String email) {
        try {
            AWSCognitoIdentityProvider cognitoClient = getCognitoClient();

            AdminDeleteUserRequest deleteUserRequest = new AdminDeleteUserRequest()
                    .withUserPoolId(userPoolId)
                    .withUsername(email);

            cognitoClient.adminDeleteUser(deleteUserRequest);
            System.out.println("✅ User deleted from Cognito: " + email);
            
        } catch (AWSCognitoIdentityProviderException e) {
            System.err.println("❌ Error deleting user from Cognito: " + e.getMessage());
            throw new RuntimeException("Failed to delete user from Cognito: " + e.getMessage());
        }
    }

    /**
     * Đồng bộ tất cả users từ database lên Cognito
     */
    public void syncAllUsersToCognito(List<User> users) {
        System.out.println("🔄 Starting sync of " + users.size() + " users to Cognito...");
        
        int successCount = 0;
        int errorCount = 0;
        
        for (User user : users) {
            try {
                if (!userExistsInCognito(user.getEmail())) {
                    createUserInCognito(user);
                    successCount++;
                } else {
                    updateUserInCognito(user);
                    successCount++;
                }
            } catch (Exception e) {
                System.err.println("❌ Failed to sync user " + user.getEmail() + ": " + e.getMessage());
                errorCount++;
            }
        }
        
        System.out.println("✅ Sync completed: " + successCount + " success, " + errorCount + " errors");
    }
}
