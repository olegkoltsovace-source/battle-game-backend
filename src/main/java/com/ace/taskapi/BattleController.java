package com.ace.taskapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    @RestController
    public class HealthController {
        @GetMapping("/api/health")
        public ResponseEntity<String> health() {
            return ResponseEntity.ok("ok");
        }
    }

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
        boolean slot1Defensive   = slot1.equals("block") || slot1.equals("dodge");
        boolean slot2Defensive   = slot2.equals("block") || slot2.equals("dodge");
        boolean slot1IsOffensive = slot1.equals("punch") || slot1.equals("kick");

        if (slot1Defensive && slot2Defensive)   slot2 = "idle";
        if (slot1IsOffensive && slot2Defensive) slot2 = "idle";
        if (slot1.equals("heal") && slot2.equals("heal")) slot2 = "idle";

        System.out.println("Slot1: " + slot1 + " | Slot2: " + slot2);

        battle.setSlot1(slot1);
        battle.setSlot2(slot2);

        // Reset per-turn result fields
        battle.setDamageDealtToOpponent(0);
        battle.setDamageDealtToPlayer(0);
        battle.setHealAmount(0);
        battle.setOpponentRageDrained(0);
        battle.setSlot1Result(null);
        battle.setSlot2Result(null);

        // Resolve slot1 — player reacts to the opponent's telegraphed action
        handleOpponentAction();

        // Resolve slot2 — player's follow-up after the exchange
        handleSlot2Action(slot2);

        // Check battle over after both slots resolve
        if (battle.getOpponentHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("player");
        } else if (battle.getPlayerHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("opponent");
        }

        // Opponent immediately telegraphs next intent so the player feels constant pressure
        String nextIntent = determineOpponentIntent();
        battle.setOpponentIntent(nextIntent);
        System.out.println("Opponent next intent: " + nextIntent);

        return battle;
    }

    // ── handlePlayerAction ───────────────────────────────────────────────────────
    // Handles the player's main turn action (the action button row, before telegraph)
    private void handlePlayerAction(String action) {
        int damage = 0;
        battle.setRageChange(0);
        battle.setOpponentRageDrained(0);

        switch (action) {

            case "punch":
                damage = (int) (5 + Math.random() * 6);
                battle.setOpponentHP(battle.getOpponentHP() - damage);
                battle.setPlayerRage(battle.getPlayerRage() + 3);
                battle.setRageChange(3);
                battle.setDamageDealtToOpponent(damage);
                battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + damage);
                battle.setLastAction("Player punched for " + damage + " damage! (+3 rage)");
                break;

            case "kick":
                // Kick drains rage from the opponent and transfers it to the player.
                // If the opponent has no rage, nothing is transferred — player must pay attention.
                int opponentRageBeforeKick = battle.getOpponentRage();

                if (opponentRageBeforeKick <= 0) {
                    battle.setLastAction("Kick connects but opponent has no rage to drain!");
                    break;
                }

                int rageDrained = Math.min(battle.getKickRageDrain(), opponentRageBeforeKick);
                battle.setOpponentRage(battle.getOpponentRage() - rageDrained);
                battle.setPlayerRage(battle.getPlayerRage() + rageDrained);
                battle.setRageChange(rageDrained);
                battle.setOpponentRageDrained(rageDrained);
                battle.setLastAction("Kick drains " + rageDrained + " rage from opponent! (+" + rageDrained + " player rage)");
                break;

            case "rageAction":
                if (!battle.canRageStrike()) {
                    battle.setLastAction("Not enough rage!");
                    break;
                }
                damage = (int) (20 + Math.random() * 10);
                int rageBeforeStrike = battle.getPlayerRage();
                battle.setOpponentHP(battle.getOpponentHP() - damage);
                battle.setPlayerHP(battle.getPlayerHP() - 3);
                battle.setRageChange(-rageBeforeStrike); // rage resets to 0
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

    // ── handleSlot2Action ────────────────────────────────────────────────────────
    // Handles the player's follow-up action in the react phase (slot2)
    private void handleSlot2Action(String action) {
        if (action == null || action.equals("idle")) {
            battle.setSlot2Result("Player did nothing.");
            return;
        }

        int damage    = 0;
        int rageChange = battle.getRageChange(); // carry forward from slot1 resolution
        String result  = "";

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
                // Same logic as the main turn kick — drain rage, transfer to player, deal no damage
                int opponentRageBeforeKick = battle.getOpponentRage();

                if (opponentRageBeforeKick <= 0) {
                    result = "Kick connects but opponent has no rage to drain!";
                    break;
                }

                int rageDrained = Math.min(battle.getKickRageDrain(), opponentRageBeforeKick);
                battle.setOpponentRage(battle.getOpponentRage() - rageDrained);
                rageChange += rageDrained;
                battle.setOpponentRageDrained(battle.getOpponentRageDrained() + rageDrained);
                result = "Kick drains " + rageDrained + " rage from opponent! (+" + rageDrained + " player rage)";
                break;

            case "rageAction":
                if (!battle.canRageStrike()) {
                    result = "Not enough rage!";
                    break;
                }
                damage = (int) (20 + Math.random() * 10);
                int rageBeforeStrike = battle.getPlayerRage() + rageChange;
                battle.setOpponentHP(battle.getOpponentHP() - damage);
                battle.setPlayerHP(battle.getPlayerHP() - 3);
                rageChange = -rageBeforeStrike; // rage resets to 0
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

        // Apply final rage change for this slot
        battle.setPlayerRage(Math.max(0, Math.min(battle.getMaxRage(), battle.getPlayerRage() + rageChange)));
        battle.setRageChange(rageChange);
        battle.setSlot2Result(result);
        battle.setLastAction((battle.getLastAction() != null ? battle.getLastAction() + " | " : "") + result);
    }

    // ── determineOpponentIntent ──────────────────────────────────────────────────
    private String determineOpponentIntent() {
        double random      = Math.random();
        int    opponentHP  = battle.getOpponentHP();

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
        return "charge";
    }

    @PostMapping("/action")
    public BattleState performAction(@RequestBody String action) {
        action = action.replace("\"", "").trim();
        System.out.println("Received action: '" + action + "'");

        // Reset per-turn fields before resolving
        battle.setDamageDealtToOpponent(0);
        battle.setDamageDealtToPlayer(0);
        battle.setOpponentRageDrained(0);

        handlePlayerAction(action);

        if (battle.getOpponentHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("player");
            return battle;
        }

        if (battle.getPlayerHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("opponent");
        }

        return battle;
    }

    // ── handleOpponentAction ─────────────────────────────────────────────────────
    private void handleOpponentAction() {
        String  intent             = battle.getOpponentIntent();
        String  currentAction      = battle.getLastAction() != null ? battle.getLastAction() : "";
        String  separator          = currentAction.isEmpty() ? "" : " | ";
        boolean playerBlocking     = "block".equals(battle.getSlot1());
        boolean playerDodging      = "dodge".equals(battle.getSlot1());
        boolean playerPunching     = "punch".equals(battle.getSlot1());
        boolean playerKicking      = "kick".equals(battle.getSlot1());
        // punch interrupts with counter-damage; kick interrupts with rage drain
        boolean playerInterrupting = playerPunching || playerKicking;

        int damage     = 0;
        int rageChange = 0;

        switch (intent != null ? intent : "attack") {

            case "attack":
                damage = (int) (5 + Math.random() * 6);
                int blockedAmount = 0;

                if (playerBlocking) {
                    blockedAmount = damage / 2;
                    damage        = damage - blockedAmount;
                    rageChange   -= 4;
                    battle.setBlockedAmount(blockedAmount);
                    battle.setSlot1Result("Block reduced damage to " + damage + "! (-4 rage)");
                } else if (playerDodging) {
                    damage      = 0;
                    rageChange += 2;
                    battle.setBlockedAmount(0);
                    battle.setSlot1Result("Dodge! Attack avoided completely! (+2 rage)");
                } else if (playerPunching) {
                    // Punch during opponent attack = trade — player takes hit but deals counter-damage
                    int counterDamage = (int) (3 + Math.random() * 4);
                    battle.setOpponentHP(battle.getOpponentHP() - counterDamage);
                    battle.setDamageDealtToOpponent(counterDamage);
                    rageChange += 3;
                    battle.setSlot1Result("Trade! Dealt " + counterDamage + " while taking hit! (+3 rage)");
                } else if (playerKicking) {
                    // Kick during opponent attack = rage drain — player still takes the hit
                    int opponentRageBeforeKick = battle.getOpponentRage();
                    if (opponentRageBeforeKick > 0) {
                        int rageDrained = Math.min(battle.getKickRageDrain(), opponentRageBeforeKick);
                        battle.setOpponentRage(battle.getOpponentRage() - rageDrained);
                        rageChange += rageDrained;
                        battle.setOpponentRageDrained(battle.getOpponentRageDrained() + rageDrained);
                        battle.setSlot1Result("Kick drained " + rageDrained + " rage while taking hit! (+" + rageDrained + " rage)");
                    } else {
                        battle.setSlot1Result("Kick connects but opponent has no rage to drain!");
                    }
                } else {
                    battle.setSlot1Result("Player took the hit!");
                }

                if (damage > 0) {
                    battle.setPlayerHP(battle.getPlayerHP() - damage);
                    battle.setDamageDealtToPlayer(damage);
                    rageChange += 5;
                }

                battle.setLastAction(separator + "Opponent attacked for " + damage + " damage!");
                break;

            case "fireball":
                damage = (int) (12 + Math.random() * 8);
                int fireballBlocked = 0;

                if (playerBlocking) {
                    fireballBlocked = (int) (damage * 0.4);
                    damage          = damage - fireballBlocked;
                    rageChange     -= 4;
                    battle.setBlockedAmount(fireballBlocked);
                    battle.setSlot1Result("Block partially absorbed fireball! " + damage + " damage taken. (-4 rage)");
                } else if (playerDodging) {
                    damage      = 0;
                    rageChange += 2;
                    battle.setBlockedAmount(0);
                    battle.setSlot1Result("Dodge! Fireball avoided! (+2 rage)");
                }
                // Punching or kicking during a fireball doesn't interrupt it —
                // the opponent is casting at range, not in melee

                if (damage > 0) {
                    battle.setPlayerHP(battle.getPlayerHP() - damage);
                    battle.setDamageDealtToPlayer(damage);
                    rageChange += 5;
                }

                battle.setLastAction(separator + "Opponent casts fireball for " + damage + " damage!");
                break;

            case "heal":
                int healAmount = (int) (8 + Math.random() * 8);

                if (playerPunching) {
                    // Punch interrupts healing — deals damage, halves the heal
                    int interruptDamage = (int) (3 + Math.random() * 4);
                    healAmount = healAmount / 2;
                    battle.setOpponentHP(battle.getOpponentHP() - interruptDamage);
                    battle.setDamageDealtToOpponent(interruptDamage);
                    rageChange += 3;
                    battle.setSlot1Result("Interrupted healing! (-" + interruptDamage + " opponent HP) (+3 rage)");
                } else if (playerKicking) {
                    // Kick during heal = drain rage instead of dealing damage
                    int opponentRageBeforeKick = battle.getOpponentRage();
                    if (opponentRageBeforeKick > 0) {
                        int rageDrained = Math.min(battle.getKickRageDrain(), opponentRageBeforeKick);
                        battle.setOpponentRage(battle.getOpponentRage() - rageDrained);
                        rageChange += rageDrained;
                        battle.setOpponentRageDrained(battle.getOpponentRageDrained() + rageDrained);
                        healAmount = healAmount / 2; // also disrupts the heal somewhat
                        battle.setSlot1Result("Kick drained " + rageDrained + " rage during heal! Heal halved. (+" + rageDrained + " rage)");
                    } else {
                        battle.setSlot1Result("Kick during heal — opponent has no rage to drain!");
                    }
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

                if (playerInterrupting) {
                    rageChange += 2;
                }
                break;
        }

        // Apply net rage change from this exchange
        battle.setPlayerRage(Math.max(0, Math.min(battle.getMaxRage(), battle.getPlayerRage() + rageChange)));
        battle.setRageChange(rageChange);

        // Clear turn state ready for next round
        battle.setOpponentIntent(null);
        battle.setSlot1(null);
        battle.setSlot2(null);
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
                user.setTotalDamageDealt(user.getTotalDamageDealt() + battle.getTotalDamageDealtByPlayer());
                userRepository.save(user);
            }
        }
        return battle;
    }

    @GetMapping("/state")
    public BattleState getBattleState() {
        return battle;
    }
}