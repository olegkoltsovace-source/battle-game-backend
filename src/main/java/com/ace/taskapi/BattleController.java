package com.ace.taskapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/battle")
@CrossOrigin(origins = "http://localhost:5173")
public class BattleController {

    private BattleState battle = new BattleState();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BattleRepository battleRepository;

    @PostMapping("/start")
    public BattleState startBattle() {
        battle = new BattleState();
        return battle;
    }

    @PostMapping("/telegraph")
    public BattleState telegraph() {
        String intent = determineOpponentIntent();
        battle.setOpponentIntent(intent);
        System.out.println("Opponent intends to: " + intent);
        return battle;
    }

    @PostMapping("/react")
    public BattleState react(@RequestBody ReactRequest request) {

        String slot1 = request.getSlot1() != null ? request.getSlot1() : "idle";
        String slot2 = request.getSlot2() != null ? request.getSlot2() : "idle";

        // Sanitize invalid combinations server-side
        boolean slot1Defensive = slot1.equals("block") || slot1.equals("dodge");
        boolean slot2Defensive = slot2.equals("block") || slot2.equals("dodge");

        boolean slot1IsOffensive = slot1.equals("punch") || slot1.equals("kick");

        if (slot1Defensive && slot2Defensive) slot2 = "idle";
        if (slot1IsOffensive && slot2Defensive) slot2 = "idle";
        if (slot1.equals("heal") && slot2.equals("heal")) slot2 = "idle";

        if (slot1Defensive && slot2Defensive) {
            slot2 = "idle"; // silently convert to idle
        }
        if (slot1.equals("heal") && slot2.equals("heal")) {
            slot2 = "idle";
        }

        System.out.println("Slot1: " + slot1 + " | Slot2: " + slot2);

        battle.setSlot1(slot1);
        battle.setSlot2(slot2);

        System.out.println("Slot1: " + request.getSlot1() + " | Slot2: " + request.getSlot2());

        // Reset turn results
        battle.setDamageDealtToOpponent(0);
        battle.setDamageDealtToPlayer(0);
        battle.setHealAmount(0);
        battle.setSlot1Result(null);
        battle.setSlot2Result(null);

        // Resolve slot1 against opponent action
        handleOpponentAction();

        // Resolve slot2 as player's follow-up action
        handleSlot2Action(request.getSlot2());

        // Check battle over
        if (battle.getOpponentHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("player");
        } else if (battle.getPlayerHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("opponent");
        }

        // Opponent immediately telegraphs reaction to slot2
        // This keeps the pressure on the player continuously
        String nextIntent = determineOpponentIntent();
        battle.setOpponentIntent(nextIntent);
        System.out.println("Opponent next intent: " + nextIntent);

        return battle;
    }

    // ── handleSlot2Action ────────────────────────────────────────────────────────
    private void handleSlot2Action(String action) {
        if (action == null || action.equals("idle")) {
            battle.setSlot2Result("Player did nothing.");
            return;
        }

        int damage = 0;
        int rageChange = battle.getRageChange(); // carry forward from slot1
        String result = "";

        switch (action) {
            case "punch":
                damage = (int) (5 + Math.random() * 6);
                battle.setOpponentHP(battle.getOpponentHP() - damage);
                rageChange += 3;
                battle.setDamageDealtToOpponent(battle.getDamageDealtToOpponent() + damage);
                battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + damage);
                result = "Player punched for " + damage + " damage! (+3 rage)";
                break;

            case "kick":
                damage = (int) (5 + Math.random() * 6);
                battle.setOpponentHP(battle.getOpponentHP() - damage);
                rageChange += 3;
                battle.setDamageDealtToOpponent(battle.getDamageDealtToOpponent() + damage);
                battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + damage);
                result = "Player kicked for " + damage + " damage! (+3 rage)";
                break;

            case "rageAction":
                if (!battle.canRageStrike()) {
                    result = "Not enough rage!";
                    break;
                }
                damage = (int) (20 + Math.random() * 10);
                int rageBefore = battle.getPlayerRage() + rageChange;
                battle.setOpponentHP(battle.getOpponentHP() - damage);
                battle.setPlayerHP(battle.getPlayerHP() - 3);
                rageChange = -rageBefore; // rage resets to 0
                battle.setPlayerRage(0);
                battle.setDamageDealtToOpponent(battle.getDamageDealtToOpponent() + damage);
                battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + damage);
                result = "RAGE STRIKE! " + damage + " damage! Rage reset to 0.";
                break;

            case "heal":
                int rageCost = 8;
                if (battle.getPlayerRage() < rageCost) {
                    result = "Not enough rage to heal!";
                    break;
                }
                int healAmount = (int) (8 + Math.random() * 8);
                battle.setPlayerHP(Math.min(battle.getMaxHP(), battle.getPlayerHP() + healAmount));
                rageChange -= rageCost;
                battle.setHealAmount(healAmount);
                result = "Player heals " + healAmount + " HP! (-" + rageCost + " rage)";
                break;

            case "block":
                rageChange -= 2;
                result = "Player braces. (-2 rage)";
                break;

            case "dodge":
                rageChange -= 6;
                result = "Player dodges. (-6 rage)";
                break;

            default:
                result = "Unknown slot2 action: " + action;
                break;
        }

        // Apply final rage change
        battle.setPlayerRage(Math.max(0, Math.min(battle.getMaxRage(), battle.getPlayerRage() + rageChange)));
        battle.setRageChange(rageChange);
        battle.setSlot2Result(result);
        battle.setLastAction((battle.getLastAction() != null ? battle.getLastAction() + " | " : "") + result);
    }

    private String determineOpponentIntent() {
        double random = Math.random();
        int opponentHP = battle.getOpponentHP();

        // Opponent is desperate — heals more often at low HP
        if (opponentHP < 30) {
            if (random < 0.5) return "heal";
            if (random < 0.8) return "attack";
            return "fireball";
        }

        // Opponent is healthy — more aggressive
        if (random < 0.5) return "attack";
        if (random < 0.7) return "fireball";
        if (random < 0.9) return "heal";
        return "charge"; // builds up for next turn
    }

    @PostMapping("/action")
    public BattleState performAction(@RequestBody String action) {
        action = action.replace("\"", "").trim();
        System.out.println("Received action: '" + action + "'");

        handlePlayerAction(action);

        if (battle.getOpponentHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("player");
            return battle;
        }

        // handleOpponentAction();

        if (battle.getPlayerHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("opponent");
        }

        return battle;
    }

    @PostMapping("/finish")
    public BattleState finishBattle(@RequestParam Long userId) {
        if (battle.isBattleOver()) {
            Battle battleRecord = new Battle(
                    userId,
                    battle.getPlayerHP(),
                    battle.getOpponentHP(),
                    battle.getWinner(),
                    battle.getTotalDamageDealtByPlayer()
            );
            battleRepository.save(battleRecord);

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

    // ── handlePlayerAction ───────────────────────────────────────────────────────
    private void handlePlayerAction(String action) {
        int damage = 0;
        battle.setRageChange(0); // reset each action

        switch (action) {
            case "punch":
                damage = (int) (5 + Math.random() * 6);
                battle.setOpponentHP(battle.getOpponentHP() - damage);
                battle.setPlayerRage(battle.getPlayerRage() + 3);
                battle.setRageChange(3);
                battle.setDamageDealtToOpponent(damage);
                battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + damage);
                battle.setLastAction("Player punched for " + damage + " damage!");
                break;

            case "kick":
                damage = (int) (5 + Math.random() * 6);
                battle.setOpponentHP(battle.getOpponentHP() - damage);
                battle.setPlayerRage(battle.getPlayerRage() + 3);
                battle.setRageChange(3);
                battle.setDamageDealtToOpponent(damage);
                battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + damage);
                battle.setLastAction("Player kicked for " + damage + " damage!");
                break;

            case "rageAction":
                if (!battle.canRageStrike()) {
                    battle.setLastAction("Not enough rage!");
                    break;
                }
                damage = (int) (20 + Math.random() * 10);
                int rageBefore = battle.getPlayerRage();
                battle.setOpponentHP(battle.getOpponentHP() - damage);
                battle.setPlayerHP(battle.getPlayerHP() - 3);
                battle.setRageChange(-rageBefore); // rage resets to 0
                battle.setPlayerRage(0);
                battle.setDamageDealtToOpponent(damage);
                battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + damage);
                battle.setLastAction("Player unleashes rage strike for " + damage + " damage!");
                break;

            case "block":
                battle.setSlot1("block");
                battle.setRageChange(0);
                battle.setLastAction("Player is defending!");
                break;

            default:
                battle.setLastAction("Unknown action: " + action);
                break;
        }
    }

    // ── handleOpponentAction ─────────────────────────────────────────────────────
    private void handleOpponentAction() {
        String intent = battle.getOpponentIntent();
        String currentAction = battle.getLastAction() != null ? battle.getLastAction() : "";
        String separator = currentAction.isEmpty() ? "" : " | ";
        boolean playerBlocking = "block".equals(battle.getSlot1());
        boolean playerDodging = "dodge".equals(battle.getSlot1());
        boolean playerInterrupting = "punch".equals(battle.getSlot1()) || "kick".equals(battle.getSlot1());

        int damage = 0;
        int rageChange = 0; // track net rage change for this resolution

        switch (intent != null ? intent : "attack") {
            case "attack":
                damage = (int) (5 + Math.random() * 6);
                int blockedAmount = 0;

                if (playerBlocking) {
                    blockedAmount = damage / 2;
                    damage = damage - blockedAmount;
                    rageChange -= 4; // block costs rage
                    battle.setBlockedAmount(blockedAmount);
                    battle.setSlot1Result("Block reduced damage to " + damage + "! (-4 rage)");
                } else if (playerDodging) {
                    damage = 0;
                    rageChange += 2; // dodge gives momentum
                    battle.setBlockedAmount(0);
                    battle.setSlot1Result("Dodge! Attack avoided completely! (+2 rage)");
                } else if (playerInterrupting) {
                    int counterDamage = (int) (3 + Math.random() * 4);
                    battle.setOpponentHP(battle.getOpponentHP() - counterDamage);
                    battle.setDamageDealtToOpponent(counterDamage);
                    rageChange += 3;
                    battle.setSlot1Result("Trade! Dealt " + counterDamage + " while taking hit! (+3 rage)");
                } else {
                    battle.setSlot1Result("Player took the hit!");
                }

                if (damage > 0) {
                    battle.setPlayerHP(battle.getPlayerHP() - damage);
                    battle.setDamageDealtToPlayer(damage);
                    rageChange += 5; // pain fuels rage
                }

                battle.setLastAction(separator + "Opponent attacked for " + damage + " damage!");
                break;

            case "fireball":
                damage = (int) (12 + Math.random() * 8);
                int fireballBlocked = 0;

                if (playerBlocking) {
                    fireballBlocked = (int) (damage * 0.4);
                    damage = damage - fireballBlocked;
                    rageChange -= 4;
                    battle.setBlockedAmount(fireballBlocked);
                    battle.setSlot1Result("Block partially absorbed fireball! " + damage + " damage taken. (-4 rage)");
                } else if (playerDodging) {
                    damage = 0;
                    rageChange += 2;
                    battle.setBlockedAmount(0);
                    battle.setSlot1Result("Dodge! Fireball avoided! (+2 rage)");
                }

                if (damage > 0) {
                    battle.setPlayerHP(battle.getPlayerHP() - damage);
                    battle.setDamageDealtToPlayer(damage);
                    rageChange += 5;
                }

                battle.setLastAction(separator + "Opponent casts fireball for " + damage + " damage!");
                break;

            case "heal":
                int healAmount = (int) (8 + Math.random() * 8);

                if ("punch".equals(battle.getSlot1()) || "kick".equals(battle.getSlot1())) {
                    int interruptDamage = (int) (3 + Math.random() * 4);
                    healAmount = healAmount / 2;
                    battle.setOpponentHP(battle.getOpponentHP() - interruptDamage);
                    battle.setDamageDealtToOpponent(interruptDamage);
                    rageChange += 3;
                    battle.setSlot1Result("Interrupted healing! (-" + interruptDamage + " opponent HP) (+3 rage)");
                } else {
                    battle.setSlot1Result("Opponent healed fully.");
                }

                battle.setOpponentHP(battle.getOpponentHP() + healAmount);
                battle.setHealAmount(healAmount);
                battle.setLastAction(separator + "Opponent heals for " + healAmount + " HP!");
                break;

            case "charge":
                battle.setLastAction(separator + "Opponent is charging power...");
                battle.setSlot1Result("Opponent charging — attack now!");

                if ("punch".equals(battle.getSlot1()) || "kick".equals(battle.getSlot1())) {
                    rageChange += 2;
                }
                break;
        }

        // Apply net rage change
        battle.setPlayerRage(Math.max(0, Math.min(battle.getMaxRage(), battle.getPlayerRage() + rageChange)));
        battle.setRageChange(rageChange);

        // Reset for next turn
        battle.setOpponentIntent(null);
        battle.setSlot1(null);
        battle.setSlot2(null);
    }

    @GetMapping("/state")
    public BattleState getBattleState() {
        return battle;
    }
}