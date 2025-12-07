package com.example.iMeetBE.repository;

import com.example.iMeetBE.model.Group;
import com.example.iMeetBE.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    
    List<Group> findByOwner(User owner);
    
    Optional<Group> findByIdAndOwner(Long id, User owner);
    
    @Query("SELECT g FROM Group g JOIN g.members m WHERE m.user.id = :userId")
    List<Group> findGroupsByUserId(@Param("userId") String userId);
    
    boolean existsByIdAndOwner(Long id, User owner);
}
