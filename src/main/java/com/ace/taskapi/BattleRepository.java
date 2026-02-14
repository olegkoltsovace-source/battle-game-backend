package com.ace.taskapi;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BattleRepository extends JpaRepository<Battle, Long> {

    // Find all battles for a specific user
    List<Battle> findByUserIdOrderByBattleDateDesc(Long userId);

    // Count total wins for a user
    long countByUserIdAndWinner(Long userId, String winner);
}