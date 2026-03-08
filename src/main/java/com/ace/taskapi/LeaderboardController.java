package com.ace.taskapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public List<Map<String, Object>> getLeaderboard() {
        List<User> topPlayers = userRepository.findTop10ByOrderByTotalWinsDesc();

        // Convert to simple map (don't send passwords!)
        return topPlayers.stream()
                .limit(10)
                .map(user -> {
                    Map<String, Object> playerData = new HashMap<>();
                    playerData.put("username", user.getUsername());
                    playerData.put("wins", user.getTotalWins());
                    playerData.put("losses", user.getTotalLosses());
                    playerData.put("totalDamage", user.getTotalDamageDealt());
                    return playerData;
                })
                .collect(Collectors.toList());
    }
}