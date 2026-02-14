package com.ace.taskapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/battle")
@CrossOrigin(origins = "http://localhost:5173")
public class BattleController {

    private BattleState battle = new BattleState();

    @PostMapping("/start")
    public BattleState startBattle() {
        battle = new BattleState();
        return battle;
    }

    @PostMapping("/action")
    public BattleState performAction(@RequestBody String action) {
        action = action.replace("\"", "").trim();
        System.out.println("Received action: '" + action + "'");

        // Player's turn
        handlePlayerAction(action);

        // Check if battle is over
        if (battle.getOpponentHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("player");
            return battle;
        }

        // Opponent's turn (simple AI)
        handleOpponentAction();

        // Check if battle is over
        if (battle.getPlayerHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("opponent");
        }

        return battle;
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BattleRepository battleRepository;

    // Add this new endpoint to finish battle and save results
    @PostMapping("/finish")
    public BattleState finishBattle(@RequestParam Long userId) {
        if (battle.isBattleOver()) {
            // Save battle to database
            Battle battleRecord = new Battle(
                    userId,
                    battle.getPlayerHP(),
                    battle.getOpponentHP(),
                    battle.getWinner(),
                    battle.getTotalDamageDealtByPlayer()
            );
            battleRepository.save(battleRecord);

            // Update user stats
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if ("player".equals(battle.getWinner())) {
                    user.setTotalWins(user.getTotalWins() + 1);
                } else {
                    user.setTotalLosses(user.getTotalLosses() + 1);
                }

                user.setTotalDamageDealt(
                        user.getTotalDamageDealt() + battle.getTotalDamageDealtByPlayer()
                );

                userRepository.save(user);
            }
        }

        return battle;
    }

    private void handlePlayerAction(String action) {
        int damage = 0;
        int rageMultiplier = 2;
        int rageHealthCost = 3;

        switch (action) {
            case "punch":
                damage = (int) (5 + Math.random() * 6); // Random 5-10
                battle.setOpponentHP(battle.getOpponentHP() - damage);
                battle.setPlayerRage(battle.getPlayerRage() + 3);
                battle.setLastAction("Player punched for " + damage + " damage!");
                // ADD THIS LINE:
                battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + damage);
                break;

            case "kick":
                damage = (int) (5 + Math.random() * 6); // Random 5-10
                battle.setOpponentHP(battle.getOpponentHP() - damage);
                battle.setPlayerRage(battle.getPlayerRage() + 3);
                battle.setLastAction("Player kicked for " + damage + " damage!");
                battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + damage);
                break;

            case "rageAction":
                damage = (int) ((12 * rageMultiplier) * (0.8 + Math.random() * 0.2));
                battle.setOpponentHP(battle.getOpponentHP() - damage);
                battle.setPlayerRage(0);
                battle.setPlayerHP(battle.getPlayerHP() - rageHealthCost);
                battle.setLastAction("Player frenzies and attacks for " + damage + " damage and loses " + rageHealthCost + " health! Player rage reset to 0.");
                battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + damage);
                break;

            case "block":
                battle.setPlayerDefending(true);
                battle.setLastAction("Player is defending!");
                damage = 0;
                break;

            default:
                battle.setLastAction("Player action unknown: " + action);
                System.out.println("Unknown action received: '" + action + "'");
                break;
        }

        battle.setDamageDealt(damage);
    }

    private void handleOpponentAction() {
        double random = Math.random();
        int damage = 0;
        String currentAction = battle.getLastAction();

        if (battle.getOpponentMana() >= 5) {
            // Fireball attack (ignores defense in original code, but let's apply reduction)
            damage = 15;

            // Apply player defense reduction
            if (battle.isPlayerDefending()) {
                damage = damage / 2; // 50% reduction
            }

            battle.setPlayerHP(battle.getPlayerHP() - damage);
            battle.setOpponentMana(battle.getOpponentMana() - 5);
            battle.setLastAction(currentAction + " | Opponent casts fireball for " + damage + " damage!");
        } else if (random < 0.6) {
            // Regular attack (60% chance)
            damage = (int) (5 + Math.random() * 6); // Random 5-10

            // Apply player defense reduction
            if (battle.isPlayerDefending()) {
                damage = damage / 2; // 50% reduction
            }

            battle.setPlayerHP(battle.getPlayerHP() - damage);
            battle.setLastAction(currentAction + " | Opponent attacked for " + damage + " damage!");
        } else {
            // Heal (40% chance)
            int healAmount = (int) (5 + Math.random() * 6); // Random 5-10
            battle.setOpponentHP(Math.min(battle.getOpponentHP() + healAmount, 80)); // Cap at 80
            battle.setOpponentDefending(true); // Use this flag to trigger heal animation
            battle.setLastAction(currentAction + " | Opponent heals for " + healAmount + " HP!");
        }

        // Reset defending state after turn
        battle.setPlayerDefending(false);
        battle.setOpponentDefending(false);
    }

    @GetMapping("/state")
    public BattleState getBattleState() {
        return battle;
    }
}