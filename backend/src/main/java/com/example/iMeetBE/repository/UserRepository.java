package com.example.iMeetBE.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.iMeetBE.model.User;
import com.example.iMeetBE.model.UserRole;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByGoogleId(String googleId);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByGoogleId(String googleId);
    
    // Count methods
    long countByRole(UserRole role);
    
    // Search methods
    @Query("SELECT u FROM User u WHERE u.email LIKE %:search% OR u.fullName LIKE %:search%")
    org.springframework.data.domain.Page<User> findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
        @Param("search") String search, 
        @Param("search") String search2, 
        org.springframework.data.domain.Pageable pageable);
    
    // Find users with Google Calendar sync enabled
    List<User> findByGoogleCalendarSyncEnabledTrue();
    
    // Find user by Google Channel ID
    Optional<User> findByGoogleChannelId(String googleChannelId);
}