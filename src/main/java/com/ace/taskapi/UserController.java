package com.ace.taskapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{userId}/stats")
    public ResponseEntity<?> getStats(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(Map.of(
                        "totalWins",        user.getTotalWins(),
                        "totalLosses",      user.getTotalLosses(),
                        "totalDamageDealt", user.getTotalDamageDealt()
                )))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}