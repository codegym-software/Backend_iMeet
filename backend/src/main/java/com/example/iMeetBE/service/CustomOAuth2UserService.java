package com.example.iMeetBE.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.example.iMeetBE.model.User;
import com.example.iMeetBE.model.UserRole;
import com.example.iMeetBE.repository.UserRepository;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CognitoService cognitoService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        // Process the OAuth2 user and save/update in database
        processOAuth2User(oauth2User);
        
        return oauth2User;
    }

    private void processOAuth2User(OAuth2User oauth2User) {
        try {
            String email = oauth2User.getAttribute("email");
            String username = oauth2User.getAttribute("username");
            String fullName = oauth2User.getAttribute("name"); // Cognito name
            String pictureUrl = oauth2User.getAttribute("picture"); // Google picture URL
            String cognitoId = oauth2User.getAttribute("sub"); // Cognito user ID

            if (email == null) {
                throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
            }

            // Sử dụng CognitoService để tạo/cập nhật user
            cognitoService.createOrUpdateUserFromOAuth2(email, username, fullName, pictureUrl, cognitoId);
            
        } catch (Exception e) {
            System.err.println("Error processing OAuth2 user: " + e.getMessage());
            throw new OAuth2AuthenticationException("Error processing OAuth2 user: " + e.getMessage());
        }
    }
}