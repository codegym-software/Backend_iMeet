package com.example.iMeetBE.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.iMeetBE.model.Room;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {
    
    List<Room> findByCapacityGreaterThanEqual(Integer minCapacity);
    
    @Query("SELECT r FROM Room r WHERE r.name LIKE %:search% OR r.location LIKE %:search%")
    List<Room> findByNameContainingIgnoreCaseOrLocationContainingIgnoreCase(@Param("search") String search);
    
    // Removed status-based search
    
    @Query("SELECT r FROM Room r WHERE r.capacity >= :minCapacity AND (r.name LIKE %:search% OR r.location LIKE %:search%)")
    List<Room> findByCapacityGreaterThanEqualAndNameContainingIgnoreCaseOrLocationContainingIgnoreCase(
        @Param("minCapacity") Integer minCapacity, @Param("search") String search);
    
    @Query("SELECT r FROM Room r ORDER BY r.name ASC")
    List<Room> findAllOrderByName();
    
    // Removed status-based ordering
    
    boolean existsByName(String name);
    
    Optional<Room> findByName(String name);
}
