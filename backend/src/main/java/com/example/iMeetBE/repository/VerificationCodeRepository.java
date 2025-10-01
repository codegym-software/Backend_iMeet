package com.example.iMeetBE.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.iMeetBE.model.VerificationCode;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    
    Optional<VerificationCode> findByEmailAndCodeAndUsedFalse(String email, String code);
    
    @Query("SELECT v FROM VerificationCode v WHERE v.email = :email AND v.code = :code AND v.used = false AND v.expiresAt > :now")
    Optional<VerificationCode> findValidCode(@Param("email") String email, @Param("code") String code, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE VerificationCode v SET v.used = true WHERE v.email = :email AND v.code = :code")
    int markAsUsed(@Param("email") String email, @Param("code") String code);
    
    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.email = :email")
    void deleteByEmail(@Param("email") String email);
    
    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.expiresAt < :now")
    void deleteExpiredCodes(@Param("now") LocalDateTime now);
}