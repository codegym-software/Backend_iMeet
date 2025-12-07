package com.example.iMeetBE.repository;

import com.example.iMeetBE.model.Group;
import com.example.iMeetBE.model.GroupInvite;
import com.example.iMeetBE.model.InviteStatusGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {
    
    Optional<GroupInvite> findByInviteToken(String inviteToken);
    
    List<GroupInvite> findByGroup(Group group);
    
    List<GroupInvite> findByInvitedEmail(String invitedEmail);
    
    List<GroupInvite> findByGroupAndStatus(Group group, InviteStatusGroup status);
    
    boolean existsByGroupAndInvitedEmailAndStatus(Group group, String invitedEmail, InviteStatusGroup status);
    
    @Query("SELECT i FROM GroupInvite i WHERE i.status = :status AND i.expiresAt < :now")
    List<GroupInvite> findExpiredInvites(@Param("status") InviteStatusGroup status, @Param("now") LocalDateTime now);
    
    Optional<GroupInvite> findByInviteTokenAndStatus(String inviteToken, InviteStatusGroup status);
}
