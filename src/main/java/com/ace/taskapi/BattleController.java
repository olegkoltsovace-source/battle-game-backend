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

    // ── Constants ─────────────────────────────────────────────
    private static final int PUNCH_DAMAGE        = 10;
    private static final int PUNCH_RAGE_GAIN     = 5;   // rage gained by the one being hit
    private static final int KICK_RAGE_DRAIN     = 10;
    private static final int KICK_RAGE_GAIN      = 10;  // rage gained by the kicker
    private static final int FIREBALL_DAMAGE     = 25;
    private static final int FIREBALL_RAGE_COST  = 30;
    private static final int HEAL_AMOUNT         = 15;
    private static final int HEAL_RAGE_COST      = 10;
    private static final int BLOCK_DAMAGE_REDUCE = 2;   // divides damage by 2
    private static final int RAGE_MIN            = -20;
    private static final int RAGE_MAX            = 30;
    private static final int MAX_HP              = 100;

    @PostMapping("/start")
    public BattleState startBattle() {
        battle = new BattleState();
        return battle;
    }

    @PostMapping("/telegraph")
    public BattleState telegraph() {
        String intent = determineOpponentIntent();
        battle.setOpponentIntent(intent);
        return battle;
    }

    @PostMapping("/react")
    public BattleState react(@RequestBody ReactRequest request) {
        String slot1 = request.getSlot1() != null ? request.getSlot1() : "idle";
        battle.setSlot1(slot1);

        // Reset per-turn fields
        battle.setDamageDealtToOpponent(0);
        battle.setDamageDealtToPlayer(0);
        battle.setHealAmount(0);
        battle.setOpponentRageDrained(0);
        battle.setPlayerRageDrained(0);
        battle.setSlot1Result(null);

        handleOpponentAction();

        if (battle.getOpponentHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("player");
        } else if (battle.getPlayerHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("opponent");
        }

        if (!battle.isBattleOver()) {
            battle.setOpponentIntent(determineOpponentIntent());
        }

        return battle;
    }

    @PostMapping("/action")
    public BattleState performAction(@RequestBody String action) {
        action = action.replace("\"", "").trim();

        battle.setDamageDealtToOpponent(0);
        battle.setDamageDealtToPlayer(0);
        battle.setOpponentRageDrained(0);
        battle.setHealAmount(0);

        handlePlayerAction(action);

        if (battle.getOpponentHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("player");
        } else if (battle.getPlayerHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("opponent");
        }

        return battle;
    }

    // ── handlePlayerAction ────────────────────────────────────
    // Handles the player's free action before the telegraph phase.
    private void handlePlayerAction(String action) {
        switch (action) {

            case "punch":
                battle.setOpponentHP(battle.getOpponentHP() - PUNCH_DAMAGE);
                battle.setOpponentRage(clampRage(battle.getOpponentRage() + PUNCH_RAGE_GAIN));
                battle.setDamageDealtToOpponent(PUNCH_DAMAGE);
                battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + PUNCH_DAMAGE);
                break;

            case "kick":
                int kickDrain = KICK_RAGE_DRAIN;
                battle.setOpponentRage(clampRage(battle.getOpponentRage() - kickDrain));
                battle.setPlayerRage(clampRage(battle.getPlayerRage() + KICK_RAGE_GAIN));
                battle.setOpponentRageDrained(kickDrain);
                break;

            case "rageAction":
                battle.setOpponentHP(battle.getOpponentHP() - FIREBALL_DAMAGE);
                battle.setPlayerRage(0);
                battle.setDamageDealtToOpponent(FIREBALL_DAMAGE);
                battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + FIREBALL_DAMAGE);
                break;

            case "heal":
                battle.setPlayerHP(Math.min(MAX_HP, battle.getPlayerHP() + HEAL_AMOUNT));
                battle.setPlayerRage(clampRage(battle.getPlayerRage() - HEAL_RAGE_COST));
                battle.setHealAmount(HEAL_AMOUNT);
                break;
        }
    }

    // ── handleOpponentAction ──────────────────────────────────
    // Resolves the opponent's telegraphed intent against the player's chosen reaction.
    // Both sides act simultaneously — the player's action always executes regardless
    // of opponent intent, and the opponent's action always executes regardless of
    // player reaction, except where block explicitly reduces damage.
    private void handleOpponentAction() {
        String intent  = battle.getOpponentIntent();
        String slot1   = battle.getSlot1();

        boolean playerBlocking     = "block".equals(slot1);

        // ── Execute player reaction ───────────────────────────
        switch (slot1) {

            case "punch":
                // Player punches — deals damage, opponent gains rage
                battle.setOpponentHP(battle.getOpponentHP() - PUNCH_DAMAGE);
                battle.setOpponentRage(clampRage(battle.getOpponentRage() + PUNCH_RAGE_GAIN));
                battle.setDamageDealtToOpponent(PUNCH_DAMAGE);
                battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + PUNCH_DAMAGE);
                break;

            case "kick":
                int previousOpponentRage = battle.getOpponentRage();
                battle.setOpponentRage(clampRage(battle.getOpponentRage() - KICK_RAGE_DRAIN));
                battle.setPlayerRage(clampRage(battle.getPlayerRage() + KICK_RAGE_GAIN));
                int actualDrained = previousOpponentRage - battle.getOpponentRage();
                battle.setOpponentRageDrained(actualDrained);
                break;

            case "rageAction":
                // Player fireballs — deals damage, resets own rage to 0
                battle.setOpponentHP(battle.getOpponentHP() - FIREBALL_DAMAGE);
                battle.setPlayerRage(0);
                battle.setDamageDealtToOpponent(FIREBALL_DAMAGE);
                battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + FIREBALL_DAMAGE);
                break;

            case "heal":
                // Player heals — gains HP, loses rage
                battle.setPlayerHP(Math.min(MAX_HP, battle.getPlayerHP() + HEAL_AMOUNT));
                battle.setPlayerRage(clampRage(battle.getPlayerRage() - HEAL_RAGE_COST));
                battle.setHealAmount(HEAL_AMOUNT);
                break;

            case "block":
            case "idle":
            default:
                // Player blocks or does nothing — handled below when opponent acts
                break;
        }

        // ── Execute opponent intent ───────────────────────────
        switch (intent != null ? intent : "punch") {

            case "punch":
                int punchDamage = PUNCH_DAMAGE;
                if (playerBlocking) punchDamage /= BLOCK_DAMAGE_REDUCE;
                battle.setPlayerHP(battle.getPlayerHP() - punchDamage);
                battle.setPlayerRage(clampRage(battle.getPlayerRage() + PUNCH_RAGE_GAIN));
                battle.setDamageDealtToPlayer(punchDamage);
                battle.setBlockedAmount(playerBlocking ? PUNCH_DAMAGE - punchDamage : 0);
                break;

            case "kick":
                int previousPlayerRage = battle.getPlayerRage();
                battle.setPlayerRage(clampRage(battle.getPlayerRage() - KICK_RAGE_DRAIN));
                battle.setOpponentRage(clampRage(battle.getOpponentRage() + KICK_RAGE_GAIN));
                int actualPlayerRageDrained = previousPlayerRage - battle.getPlayerRage();
                battle.setPlayerRageDrained(actualPlayerRageDrained);
                break;

            case "fireball":
                int fireballDamage = FIREBALL_DAMAGE;
                if (playerBlocking) fireballDamage /= BLOCK_DAMAGE_REDUCE;
                battle.setPlayerHP(battle.getPlayerHP() - fireballDamage);
                battle.setPlayerRage(clampRage(battle.getPlayerRage() + PUNCH_RAGE_GAIN));
                battle.setDamageDealtToPlayer(fireballDamage);
                battle.setBlockedAmount(playerBlocking ? FIREBALL_DAMAGE - fireballDamage : 0);
                battle.setOpponentRage(0); // opponent rage resets after firing
                break;

            case "heal":
                battle.setOpponentHP(Math.min(MAX_HP, battle.getOpponentHP() + HEAL_AMOUNT));
                battle.setOpponentRage(clampRage(battle.getOpponentRage() - HEAL_RAGE_COST));
                break;
        }

        // Clear turn state
        battle.setOpponentIntent(null);
        battle.setSlot1(null);
    }

    // ── determineOpponentIntent ───────────────────────────────
    private String determineOpponentIntent() {
        double random       = Math.random();
        int    opponentHP   = battle.getOpponentHP();
        int    opponentRage = battle.getOpponentRage();

        boolean canFireball = opponentRage >= FIREBALL_RAGE_COST;
        boolean canHeal     = opponentRage >= HEAL_RAGE_COST;

        // Desperate — low HP, prioritize healing if affordable
        if (opponentHP < 30) {
            if (canHeal && random < 0.6)     return "heal";
            if (canFireball && random < 0.8) return "fireball";
            return random < 0.5 ? "punch" : "kick";
        }

        // Full rage — use fireball
        if (canFireball && random < 0.6) return "fireball";

        // Can heal — occasionally heal
        if (canHeal && random < 0.3) return "heal";

        // Default — build rage through combat
        return random < 0.6 ? "punch" : "kick";
    }

    // ── clampRage ─────────────────────────────────────────────
    private int clampRage(int rage) {
        return Math.max(RAGE_MIN, Math.min(RAGE_MAX, rage));
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