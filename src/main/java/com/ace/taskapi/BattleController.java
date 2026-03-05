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

        battle.setSlot1(slot1);

        battle.setDamageDealtToOpponent(0);
        battle.setDamageDealtToPlayer(0);
        battle.setHealAmount(0);
        battle.setOpponentRageDrained(0);
        battle.setSlot1Result(null);

        handleOpponentAction();

        if (battle.getOpponentHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("player");
        } else if (battle.getPlayerHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("opponent");
        }

        String nextIntent = determineOpponentIntent();
        battle.setOpponentIntent(nextIntent);
        System.out.println("Opponent next intent: " + nextIntent);

        return battle;
    }

    // ── handlePlayerAction ───────────────────────────────────────────────────────
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
                damage = (int) (20 + Math.random() * 10);
                battle.setOpponentHP(battle.getOpponentHP() - damage);
                battle.setPlayerHP(battle.getPlayerHP() - 3);
                battle.setPlayerRage(0);
                battle.setDamageDealtToOpponent(battle.getDamageDealtToOpponent() + damage);
                battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + damage);
                break;

            case "heal":
                int healAmount = (int) (8 + Math.random() * 8);
                battle.setPlayerHP(Math.min(battle.getMaxHP(), battle.getPlayerHP() + healAmount));
                battle.setPlayerRage(battle.getPlayerRage() - 8);
                battle.setHealAmount(healAmount);
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

    // ── applyPlayerHealOrRage ────────────────────────────────────────────────────
    // Called inside handleOpponentAction when player chooses heal or rageAction
    // during the react phase. These execute independently of opponent intent —
    // the player heals or rage strikes while simultaneously taking whatever the
    // opponent does.
    private void applyPlayerHealOrRage(String action) {
        if ("heal".equals(action)) {
            int healAmount = (int) (8 + Math.random() * 8);
            battle.setPlayerHP(Math.min(battle.getMaxHP(), battle.getPlayerHP() + healAmount));
            battle.setPlayerRage(Math.max(0, battle.getPlayerRage() - 8));
            battle.setHealAmount(healAmount);
            battle.setSlot1Result("Player healed " + healAmount + " HP! (-8 rage)");
        } else if ("rageAction".equals(action)) {
            int damage = (int) (20 + Math.random() * 10);
            battle.setOpponentHP(battle.getOpponentHP() - damage);
            battle.setPlayerHP(battle.getPlayerHP() - 3);
            battle.setPlayerRage(0);
            battle.setDamageDealtToOpponent(battle.getDamageDealtToOpponent() + damage);
            battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + damage);
            battle.setSlot1Result("RAGE STRIKE for " + damage + " damage! Rage reset to 0.");
        }
    }

    // ── determineOpponentIntent ──────────────────────────────────────────────────
    private String determineOpponentIntent() {
        double random     = Math.random();
        int    opponentHP = battle.getOpponentHP();

        if (opponentHP < 30) {
            if (random < 0.5) return "heal";
            if (random < 0.8) return "attack";
            return "fireball";
        }

        if (random < 0.5) return "attack";
        if (random < 0.7) return "fireball";
        if (random < 0.9) return "heal";
        return "charge";
    }

    @PostMapping("/action")
    public BattleState performAction(@RequestBody String action) {
        action = action.replace("\"", "").trim();
        System.out.println("Received action: '" + action + "'");

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
        boolean playerHealing      = "heal".equals(battle.getSlot1());
        boolean playerRageStriking = "rageAction".equals(battle.getSlot1());
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
                    int counterDamage = (int) (3 + Math.random() * 4);
                    battle.setOpponentHP(battle.getOpponentHP() - counterDamage);
                    battle.setDamageDealtToOpponent(counterDamage);
                    rageChange += 3;
                    battle.setSlot1Result("Trade! Dealt " + counterDamage + " while taking hit! (+3 rage)");
                } else if (playerKicking) {
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
                } else if (playerHealing || playerRageStriking) {
                    // Player executes their action but still takes the hit
                    applyPlayerHealOrRage(battle.getSlot1());
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

                if (playerBlocking) {
                    int fireballBlocked = (int) (damage * 0.4);
                    damage          = damage - fireballBlocked;
                    rageChange     -= 4;
                    battle.setBlockedAmount(fireballBlocked);
                    battle.setSlot1Result("Block partially absorbed fireball! " + damage + " damage taken. (-4 rage)");
                } else if (playerDodging) {
                    damage      = 0;
                    rageChange += 2;
                    battle.setBlockedAmount(0);
                    battle.setSlot1Result("Dodge! Fireball avoided! (+2 rage)");
                } else if (playerHealing || playerRageStriking) {
                    // Fireball can't be interrupted but player still executes their action
                    applyPlayerHealOrRage(battle.getSlot1());
                }
                // Punching or kicking during fireball doesn't interrupt — opponent casting at range

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
                    int interruptDamage = (int) (3 + Math.random() * 4);
                    healAmount = healAmount / 2;
                    battle.setOpponentHP(battle.getOpponentHP() - interruptDamage);
                    battle.setDamageDealtToOpponent(interruptDamage);
                    rageChange += 3;
                    battle.setSlot1Result("Interrupted healing! (-" + interruptDamage + " opponent HP) (+3 rage)");
                } else if (playerKicking) {
                    int opponentRageBeforeKick = battle.getOpponentRage();
                    if (opponentRageBeforeKick > 0) {
                        int rageDrained = Math.min(battle.getKickRageDrain(), opponentRageBeforeKick);
                        battle.setOpponentRage(battle.getOpponentRage() - rageDrained);
                        rageChange += rageDrained;
                        battle.setOpponentRageDrained(battle.getOpponentRageDrained() + rageDrained);
                        healAmount = healAmount / 2;
                        battle.setSlot1Result("Kick drained " + rageDrained + " rage during heal! Heal halved. (+" + rageDrained + " rage)");
                    } else {
                        battle.setSlot1Result("Kick during heal — opponent has no rage to drain!");
                    }
                } else if (playerHealing || playerRageStriking) {
                    // Both act simultaneously
                    applyPlayerHealOrRage(battle.getSlot1());
                } else {
                    battle.setSlot1Result("Opponent healed fully.");
                }

                battle.setOpponentHP(Math.min(100, battle.getOpponentHP() + healAmount));
                battle.setHealAmount(healAmount);
                battle.setLastAction(separator + "Opponent heals for " + healAmount + " HP!");
                break;

            case "charge":
                battle.setLastAction(separator + "Opponent is charging power...");
                battle.setSlot1Result("Opponent charging — attack now!");

                if (playerInterrupting) {
                    rageChange += 2;
                } else if (playerHealing || playerRageStriking) {
                    // Free action during charge — no damage taken
                    applyPlayerHealOrRage(battle.getSlot1());
                }
                break;
        }

        // Apply net rage change from this exchange
        battle.setPlayerRage(Math.max(0, Math.min(battle.getMaxRage(), battle.getPlayerRage() + rageChange)));
        battle.setRageChange(rageChange);

        // Clear turn state
        battle.setOpponentIntent(null);
        battle.setSlot1(null);
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