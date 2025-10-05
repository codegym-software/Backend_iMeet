package com.example.iMeetBE.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.DeliveryMediumType;
import com.amazonaws.services.cognitoidp.model.ListUsersRequest;
import com.amazonaws.services.cognitoidp.model.ListUsersResult;
import com.amazonaws.services.cognitoidp.model.MessageActionType;
import com.amazonaws.services.cognitoidp.model.UserNotFoundException;
import com.amazonaws.services.cognitoidp.model.UserType;
import com.example.iMeetBE.model.User;
import com.example.iMeetBE.model.UserRole;
import com.example.iMeetBE.repository.UserRepository;

@Service
public class CognitoService {

    @Autowired
    private UserRepository userRepository;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    @Value("${aws.cognito.region:ap-southeast-2}")
    private String region;

    @Value("${aws.access-key}")
    private String accessKeyId;

    @Value("${aws.secret-key}")
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
            
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                attributes.add(new AttributeType().withName("picture").withValue(user.getAvatarUrl()));
            }

            AdminCreateUserRequest createUserRequest = new AdminCreateUserRequest()
                    .withUserPoolId(userPoolId)
                    .withUsername(user.getEmail()) // Sử dụng email làm username
                    .withUserAttributes(attributes)
                    .withTemporaryPassword("TempPass123!") // Password tạm thời
                    .withMessageAction(MessageActionType.SUPPRESS) // Không gửi email
                    .withDesiredDeliveryMediums(DeliveryMediumType.EMAIL);

            AdminCreateUserResult result = cognitoClient.adminCreateUser(createUserRequest);
            
            
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
            
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                attributes.add(new AttributeType().withName("picture").withValue(user.getAvatarUrl()));
            }

            if (!attributes.isEmpty()) {
                AdminUpdateUserAttributesRequest updateRequest = new AdminUpdateUserAttributesRequest()
                        .withUserPoolId(userPoolId)
                        .withUsername(user.getEmail())
                        .withUserAttributes(attributes);

                cognitoClient.adminUpdateUserAttributes(updateRequest);
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
            
        } catch (AWSCognitoIdentityProviderException e) {
            System.err.println("❌ Error deleting user from Cognito: " + e.getMessage());
            throw new RuntimeException("Failed to delete user from Cognito: " + e.getMessage());
        }
    }

    /**
     * Đồng bộ tất cả users từ database lên Cognito
     */
    public void syncAllUsersToCognito(List<User> users) {
        
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
        
    }

    /**
     * Lấy danh sách tất cả users từ Cognito User Pool
     */
    public List<UserType> getAllUsersFromCognito() {
        try {
            AWSCognitoIdentityProvider cognitoClient = getCognitoClient();
            List<UserType> allUsers = new ArrayList<>();
            
            ListUsersRequest listUsersRequest = new ListUsersRequest()
                    .withUserPoolId(userPoolId)
                    .withLimit(60); // Tối đa 60 users mỗi lần
            
            ListUsersResult result = cognitoClient.listUsers(listUsersRequest);
            allUsers.addAll(result.getUsers());
            
            // Xử lý pagination nếu có
            while (result.getPaginationToken() != null && !result.getPaginationToken().isEmpty()) {
                listUsersRequest.setPaginationToken(result.getPaginationToken());
                result = cognitoClient.listUsers(listUsersRequest);
                allUsers.addAll(result.getUsers());
            }
            
            return allUsers;
            
        } catch (AWSCognitoIdentityProviderException e) {
            System.err.println("❌ Error getting users from Cognito: " + e.getMessage());
            throw new RuntimeException("Failed to get users from Cognito: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin chi tiết của một user từ Cognito
     */
    public UserType getUserFromCognito(String email) {
        try {
            AWSCognitoIdentityProvider cognitoClient = getCognitoClient();
            
            AdminGetUserRequest getUserRequest = new AdminGetUserRequest()
                    .withUserPoolId(userPoolId)
                    .withUsername(email);
            
            AdminGetUserResult result = cognitoClient.adminGetUser(getUserRequest);
            // Tạo UserType từ AdminGetUserResult
            UserType userType = new UserType();
            userType.setUsername(result.getUsername());
            userType.setUserStatus(result.getUserStatus());
            userType.setEnabled(result.getEnabled());
            userType.setUserCreateDate(result.getUserCreateDate());
            userType.setUserLastModifiedDate(result.getUserLastModifiedDate());
            userType.setAttributes(result.getUserAttributes());
            return userType;
            
        } catch (UserNotFoundException e) {
            return null;
        } catch (AWSCognitoIdentityProviderException e) {
            System.err.println("❌ Error getting user from Cognito: " + e.getMessage());
            throw new RuntimeException("Failed to get user from Cognito: " + e.getMessage());
        }
    }

    /**
     * Chuyển đổi UserType từ Cognito thành User entity cho database
     */
    private User convertCognitoUserToEntity(UserType cognitoUser) {
        User user = new User();
        
        // Lấy thông tin từ attributes
        String email = null;
        String fullName = null;
        String avatarUrl = null;
        String googleId = null;
        
        for (AttributeType attribute : cognitoUser.getAttributes()) {
            switch (attribute.getName()) {
                case "email":
                    email = attribute.getValue();
                    break;
                case "name":
                    fullName = attribute.getValue();
                    break;
                case "picture":
                    avatarUrl = attribute.getValue();
                    break;
                case "sub":
                    googleId = attribute.getValue();
                    break;
            }
        }
        
        // Xác định loại user và tạo ID phù hợp
        String userId;
        if (cognitoUser.getUsername().startsWith("google_")) {
            // User từ Google OAuth2
            userId = cognitoUser.getUsername();
            user.setGoogleId(googleId);
        } else {
            // Traditional user
            userId = "traditional_" + email.replace("@", "_").replace(".", "_");
        }
        
        user.setId(userId);
        user.setEmail(email);
        user.setUsername(cognitoUser.getUsername());
        user.setFullName(fullName);
        user.setAvatarUrl(avatarUrl);
        user.setRole(UserRole.USER); // Mặc định là USER role
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        return user;
    }

    /**
     * Lấy danh sách email của tất cả users trong Cognito
     */
    public Set<String> getCognitoUserEmails() {
        try {
            List<UserType> cognitoUsers = getAllUsersFromCognito();
            Set<String> emails = new HashSet<>();
            
            for (UserType user : cognitoUsers) {
                for (AttributeType attribute : user.getAttributes()) {
                    if ("email".equals(attribute.getName())) {
                        emails.add(attribute.getValue());
                        break;
                    }
                }
            }
            
            return emails;
            
        } catch (Exception e) {
            System.err.println("❌ Error getting Cognito user emails: " + e.getMessage());
            return new HashSet<>();
        }
    }

    /**
     * Lấy danh sách email của tất cả users trong database
     */
    public Set<String> getDatabaseUserEmails() {
        try {
            List<User> dbUsers = userRepository.findAll();
            Set<String> emails = new HashSet<>();
            
            for (User user : dbUsers) {
                if (user.getEmail() != null) {
                    emails.add(user.getEmail());
                }
            }
            
            return emails;
            
        } catch (Exception e) {
            System.err.println("❌ Error getting database user emails: " + e.getMessage());
            return new HashSet<>();
        }
    }

    /**
     * Xóa user khỏi database nếu không còn tồn tại trong Cognito
     */
    public void syncDeleteUsersFromCognito() {
        try {
            Set<String> cognitoEmails = getCognitoUserEmails();
            Set<String> databaseEmails = getDatabaseUserEmails();
            
            // Tìm users trong database nhưng không có trong Cognito
            Set<String> emailsToDelete = new HashSet<>(databaseEmails);
            emailsToDelete.removeAll(cognitoEmails);
            
            int deletedCount = 0;
            int errorCount = 0;
            int skippedCount = 0;
            
            for (String email : emailsToDelete) {
                try {
                    Optional<User> userToDelete = userRepository.findByEmail(email);
                    if (userToDelete.isPresent()) {
                        User user = userToDelete.get();
                        
                        // Chỉ xóa users OAuth2 (có googleId) hoặc users traditional không có password
                        // Không xóa users traditional có password (có thể là users cũ quan trọng)
                        if (user.getGoogleId() != null && !user.getGoogleId().isEmpty()) {
                            // OAuth2 user - có thể xóa an toàn
                            userRepository.delete(user);
                            deletedCount++;
                        } else if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
                            // Traditional user không có password - có thể xóa
                            userRepository.delete(user);
                            deletedCount++;
                        } else {
                            // Traditional user có password - KHÔNG xóa để tránh mất dữ liệu quan trọng
                            skippedCount++;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error deleting user " + email + ": " + e.getMessage());
                    errorCount++;
                }
            }
            
            if (deletedCount > 0 || skippedCount > 0) {
                System.out.println("Delete sync: " + deletedCount + " deleted, " + skippedCount + " skipped, " + errorCount + " errors");
            }
            
        } catch (Exception e) {
            System.err.println("Error syncing delete users from Cognito: " + e.getMessage());
            throw new RuntimeException("Failed to sync delete users from Cognito: " + e.getMessage());
        }
    }

    /**
     * Đồng bộ hoàn toàn: Thêm users mới từ Cognito và xóa users không còn tồn tại
     */
    public void fullSyncFromCognito() {
        try {
            // 1. Lấy danh sách users từ Cognito
            List<UserType> cognitoUsers = getAllUsersFromCognito();
            Set<String> cognitoEmails = getCognitoUserEmails();
            
            // 2. Lấy danh sách users từ database
            List<User> dbUsers = userRepository.findAll();
            Set<String> databaseEmails = getDatabaseUserEmails();
            
            int createCount = 0;
            int updateCount = 0;
            int deleteCount = 0;
            int errorCount = 0;
            
            // 3. Xử lý từng user trong Cognito
            for (UserType cognitoUser : cognitoUsers) {
                try {
                    // Lấy email từ attributes
                    String email = null;
                    for (AttributeType attribute : cognitoUser.getAttributes()) {
                        if ("email".equals(attribute.getName())) {
                            email = attribute.getValue();
                            break;
                        }
                    }
                    
                    if (email == null) {
                        continue;
                    }
                    
                    User userEntity = convertCognitoUserToEntity(cognitoUser);
                    
                    // Kiểm tra user đã tồn tại trong database chưa
                    Optional<User> existingUser = userRepository.findByEmail(email);
                    if (existingUser.isPresent()) {
                        // Cập nhật user hiện có
                        User existing = existingUser.get();
                        existing.setFullName(userEntity.getFullName());
                        existing.setAvatarUrl(userEntity.getAvatarUrl());
                        existing.setUpdatedAt(LocalDateTime.now());
                        userRepository.save(existing);
                        updateCount++;
                    } else {
                        // Tạo user mới
                        userRepository.save(userEntity);
                        createCount++;
                    }
                    
                } catch (Exception e) {
                    System.err.println("Failed to sync user " + cognitoUser.getUsername() + ": " + e.getMessage());
                    errorCount++;
                }
            }
            
            // 4. Xóa users không còn tồn tại trong Cognito (chỉ OAuth2 users)
            Set<String> emailsToDelete = new HashSet<>(databaseEmails);
            emailsToDelete.removeAll(cognitoEmails);
            
            for (String email : emailsToDelete) {
                try {
                    Optional<User> userToDelete = userRepository.findByEmail(email);
                    if (userToDelete.isPresent()) {
                        User user = userToDelete.get();
                        
                        // Chỉ xóa OAuth2 users để tránh xóa nhầm traditional users quan trọng
                        if (user.getGoogleId() != null && !user.getGoogleId().isEmpty()) {
                            userRepository.delete(user);
                            deleteCount++;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error deleting user " + email + ": " + e.getMessage());
                    errorCount++;
                }
            }
            
            if (createCount > 0 || updateCount > 0 || deleteCount > 0) {
                System.out.println("Full sync: " + createCount + " created, " + updateCount + " updated, " + deleteCount + " deleted, " + errorCount + " errors");
            }
            
        } catch (Exception e) {
            System.err.println("Error in full sync from Cognito: " + e.getMessage());
            throw new RuntimeException("Failed to full sync from Cognito: " + e.getMessage());
        }
    }

    /**
     * Đồng bộ một user từ Cognito về database
     */
    public User syncUserFromCognitoToDatabase(String email) {
        try {
            UserType cognitoUser = getUserFromCognito(email);
            if (cognitoUser == null) {
                throw new RuntimeException("User không tồn tại trong Cognito: " + email);
            }
            
            User userEntity = convertCognitoUserToEntity(cognitoUser);
            
            // Kiểm tra user đã tồn tại trong database chưa
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent()) {
                // Cập nhật user hiện có
                User existing = existingUser.get();
                existing.setFullName(userEntity.getFullName());
                existing.setAvatarUrl(userEntity.getAvatarUrl());
                existing.setUpdatedAt(LocalDateTime.now());
                userRepository.save(existing);
                return existing;
            } else {
                // Tạo user mới
                userRepository.save(userEntity);
                return userEntity;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error syncing user from Cognito to database: " + e.getMessage());
            throw new RuntimeException("Failed to sync user from Cognito: " + e.getMessage());
        }
    }

    /**
     * Đồng bộ tất cả users từ Cognito về database (chỉ thêm/cập nhật, không xóa)
     */
    public void syncAllUsersFromCognitoToDatabase() {
        try {
            List<UserType> cognitoUsers = getAllUsersFromCognito();
            
            int successCount = 0;
            int errorCount = 0;
            int updateCount = 0;
            int createCount = 0;
            
            for (UserType cognitoUser : cognitoUsers) {
                try {
                    // Lấy email từ attributes
                    String email = null;
                    for (AttributeType attribute : cognitoUser.getAttributes()) {
                        if ("email".equals(attribute.getName())) {
                            email = attribute.getValue();
                            break;
                        }
                    }
                    
                    if (email == null) {
                        continue;
                    }
                    
                    User userEntity = convertCognitoUserToEntity(cognitoUser);
                    
                    // Kiểm tra user đã tồn tại trong database chưa
                    Optional<User> existingUser = userRepository.findByEmail(email);
                    if (existingUser.isPresent()) {
                        // Cập nhật user hiện có
                        User existing = existingUser.get();
                        existing.setFullName(userEntity.getFullName());
                        existing.setAvatarUrl(userEntity.getAvatarUrl());
                        existing.setUpdatedAt(LocalDateTime.now());
                        userRepository.save(existing);
                        updateCount++;
                    } else {
                        // Tạo user mới
                        userRepository.save(userEntity);
                        createCount++;
                    }
                    
                    successCount++;
                    
                } catch (Exception e) {
                    System.err.println("Failed to sync user " + cognitoUser.getUsername() + ": " + e.getMessage());
                    errorCount++;
                }
            }
            
            if (successCount > 0 || createCount > 0 || updateCount > 0) {
                System.out.println("Sync: " + successCount + " successful, " + createCount + " created, " + updateCount + " updated, " + errorCount + " errors");
            }
            
        } catch (Exception e) {
            System.err.println("Error syncing all users from Cognito: " + e.getMessage());
            throw new RuntimeException("Failed to sync all users from Cognito: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra và khôi phục users traditional bị xóa nhầm
     * Chỉ khôi phục users có password (traditional users quan trọng)
     */
    public void restoreTraditionalUsers() {
        try {
            Set<String> cognitoEmails = getCognitoUserEmails();
            List<User> allDbUsers = userRepository.findAll();
            
            int restoredCount = 0;
            
            for (User user : allDbUsers) {
                // Chỉ kiểm tra traditional users có password
                if ((user.getGoogleId() == null || user.getGoogleId().isEmpty()) && 
                    user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()) {
                    
                    // Nếu user có trong Cognito nhưng không có googleId, cập nhật
                    if (cognitoEmails.contains(user.getEmail()) && 
                        (user.getGoogleId() == null || user.getGoogleId().isEmpty())) {
                        
                        // Tìm user trong Cognito để lấy thông tin
                        UserType cognitoUser = getUserFromCognito(user.getEmail());
                        if (cognitoUser != null) {
                            // Cập nhật thông tin từ Cognito
                            for (AttributeType attribute : cognitoUser.getAttributes()) {
                                if ("sub".equals(attribute.getName())) {
                                    user.setGoogleId(attribute.getValue());
                                    break;
                                }
                            }
                            userRepository.save(user);
                            restoredCount++;
                        }
                    }
                }
            }
            
            if (restoredCount > 0) {
                System.out.println("Restore: " + restoredCount + " users restored");
            }
            
        } catch (Exception e) {
            System.err.println("Error restoring traditional users: " + e.getMessage());
        }
    }

    /**
     * Xóa user khỏi Cognito khi xóa khỏi database
     */
    public void deleteUserFromCognitoByEmail(String email) {
        try {
            if (email == null || email.isEmpty()) {
                System.err.println("Email is required to delete user from Cognito");
                return;
            }

            // Kiểm tra user có tồn tại trong Cognito không
            if (userExistsInCognito(email)) {
                deleteUserFromCognito(email);
            }
        } catch (Exception e) {
            System.err.println("Error deleting user from Cognito: " + e.getMessage());
            // Không throw exception để không ảnh hưởng đến việc xóa trong database
        }
    }

    /**
     * Tạo user trong Cognito khi tạo trong database
     */
    public void createUserInCognitoFromDatabase(User user) {
        try {
            if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
                System.err.println("User or email is required to create user in Cognito");
                return;
            }

            // Chỉ tạo cho traditional users (không phải OAuth2 users)
            if (user.getGoogleId() == null || user.getGoogleId().isEmpty()) {
                // Kiểm tra user đã tồn tại trong Cognito chưa
                if (!userExistsInCognito(user.getEmail())) {
                    createUserInCognito(user);
                }
            }
        } catch (Exception e) {
            System.err.println("Error creating user in Cognito: " + e.getMessage());
            // Không throw exception để không ảnh hưởng đến việc tạo trong database
        }
    }

    /**
     * Cập nhật user trong Cognito khi cập nhật trong database
     */
    public void updateUserInCognitoFromDatabase(User user) {
        try {
            if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
                System.err.println("User or email is required to update user in Cognito");
                return;
            }

            // Chỉ cập nhật cho traditional users (không phải OAuth2 users)
            if (user.getGoogleId() == null || user.getGoogleId().isEmpty()) {
                // Kiểm tra user có tồn tại trong Cognito không
                if (userExistsInCognito(user.getEmail())) {
                    updateUserInCognito(user);
                } else {
                    // Nếu không tồn tại, tạo mới
                    createUserInCognito(user);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating user in Cognito: " + e.getMessage());
            // Không throw exception để không ảnh hưởng đến việc cập nhật trong database
        }
    }

    /**
     * Tạo hoặc cập nhật user từ OAuth2 attributes (được gọi khi user đăng nhập OAuth2)
     */
    public User createOrUpdateUserFromOAuth2(String email, String username, String fullName, String pictureUrl, String cognitoId) {
        try {
            if (email == null || email.isEmpty()) {
                throw new RuntimeException("Email is required for OAuth2 user");
            }

            // Tạo ID phù hợp cho OAuth2 user
            String userId = "google_" + (username != null ? username : email.replace("@", "_").replace(".", "_"));
            
            Optional<User> existingUser = userRepository.findByEmail(email);
            
            if (existingUser.isPresent()) {
                // Update existing user with OAuth2 information
                User user = existingUser.get();
                
                // Cập nhật thông tin từ OAuth2
                if (cognitoId != null && !cognitoId.isEmpty()) {
                    user.setGoogleId(cognitoId);
                }
                if (fullName != null && !fullName.isEmpty()) {
                    user.setFullName(fullName);
                }
                if (pictureUrl != null && !pictureUrl.isEmpty()) {
                    user.setAvatarUrl(pictureUrl);
                }
                if (username != null && !username.isEmpty()) {
                    user.setUsername(username);
                }
                user.setUpdatedAt(LocalDateTime.now());
                
                userRepository.save(user);
                return user;
            } else {
                // Create new user from OAuth2 information
                User newUser = new User();
                newUser.setId(userId);
                newUser.setEmail(email);
                newUser.setUsername(username != null ? username : email);
                newUser.setFullName(fullName != null ? fullName : (username != null ? username : email));
                newUser.setAvatarUrl(pictureUrl);
                newUser.setGoogleId(cognitoId);
                newUser.setRole(UserRole.USER);
                newUser.setPasswordHash(""); // OAuth2 users don't need password
                newUser.setCreatedAt(LocalDateTime.now());
                newUser.setUpdatedAt(LocalDateTime.now());
                
                userRepository.save(newUser);
                return newUser;
            }
        } catch (Exception e) {
            System.err.println("Error creating/updating OAuth2 user: " + e.getMessage());
            throw new RuntimeException("Failed to create/update OAuth2 user: " + e.getMessage());
        }
    }
}
