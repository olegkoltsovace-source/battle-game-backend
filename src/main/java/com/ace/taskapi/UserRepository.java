package com.ace.taskapi;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by username (for login)
    Optional<User> findByUsername(String username);

    // Check if username already exists (for registration)
    boolean existsByUsername(String username);

    // Get leaderboard - top 10 players by wins
    @Query("SELECT u FROM User u ORDER BY u.totalWins DESC, u.totalDamageDealt DESC")
    List<User> findTop10ByOrderByTotalWinsDesc();
}