package com.example.iMeetBE.repository;

import com.example.iMeetBE.model.Group;
import com.example.iMeetBE.model.GroupMember;
import com.example.iMeetBE.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    
    List<GroupMember> findByGroup(Group group);
    
    List<GroupMember> findByUser(User user);
    
    Optional<GroupMember> findByGroupAndUser(Group group, User user);
    
    boolean existsByGroupAndUser(Group group, User user);
    
    void deleteByGroupAndUser(Group group, User user);
    
    long countByGroup(Group group);
}
