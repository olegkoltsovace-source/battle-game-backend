package com.ace.taskapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/battle")
public class BattleController {

    private BattleState battle = new BattleState();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BattleRepository battleRepository;

    // ── Constants ─────────────────────────────────────────────
    private static final int PUNCH_DAMAGE        = 10;
    private static final int PUNCH_RAGE_GAIN     = 5;
    private static final int KICK_RAGE_DRAIN     = 10;
    private static final int KICK_RAGE_GAIN      = 10;
    private static final int FIREBALL_DAMAGE     = 25;
    private static final int FIREBALL_RAGE_COST  = 30;
    private static final int HEAL_AMOUNT         = 15;
    private static final int HEAL_RAGE_COST      = 10;
    private static final int BLOCK_DAMAGE_REDUCE = 2;
    private static final int STUN_RAGE_COST      = 15;
    private static final int STUN_HP_COST        = 10;
    private static final int RAGE_MIN            = -20;
    private static final int RAGE_MAX            = 30;
    private static final int MAX_HP              = 100;

    @PostMapping("/start")
    public BattleState startBattle() {
        battle = new BattleState();
        battle.setOpponentIntent(determineOpponentIntent());
        return battle;
    }

    @PostMapping("/react")
    public BattleState react(@RequestBody ReactRequest request) {
        String slot1 = request.getSlot1() != null ? request.getSlot1() : "idle";
        battle.setSlot1(slot1);

        // Save stun state from previous round before resetting
        boolean opponentWasStunned = battle.isOpponentStunned();
        boolean playerWasStunned   = battle.isPlayerStunned();

        // Reset per-turn fields
        battle.setPlayerStunned(false);
        battle.setOpponentStunned(false);
        battle.setDamageDealtToOpponent(0);
        battle.setDamageDealtToPlayer(0);
        battle.setHealAmount(0);
        battle.setOpponentRageDrained(0);
        battle.setPlayerRageDrained(0);
        battle.setBlockedAmount(0);
        battle.setOpponentBlockedAmount(0);
        battle.setSlot1Result(null);

        handleOpponentAction(opponentWasStunned, playerWasStunned);

        if (battle.getOpponentHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("player");
        } else if (battle.getPlayerHP() <= 0) {
            battle.setBattleOver(true);
            battle.setWinner("opponent");
        }

        if (!battle.isBattleOver()) {
            if (battle.isOpponentStunned()) {
                battle.setOpponentIntent("stunned");
            } else {
                battle.setOpponentIntent(determineOpponentIntent());
            }
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
    private void handleOpponentAction(boolean opponentWasStunned, boolean playerWasStunned) {
        String  intent           = battle.getOpponentIntent();
        String  slot1            = battle.getSlot1();
        boolean playerBlocking   = "block".equals(slot1);
        boolean opponentBlocking = "block".equals(intent);

        // ── Execute player reaction ───────────────────────────
        // Skip if player was stunned last round — they had no action
        if (!playerWasStunned) {
            switch (slot1) {

                case "punch":
                    int punchDmg = opponentBlocking ? PUNCH_DAMAGE / BLOCK_DAMAGE_REDUCE : PUNCH_DAMAGE;
                    battle.setOpponentHP(battle.getOpponentHP() - punchDmg);
                    battle.setOpponentRage(clampRage(battle.getOpponentRage() + PUNCH_RAGE_GAIN));
                    battle.setDamageDealtToOpponent(punchDmg);
                    battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + punchDmg);
                    battle.setOpponentBlockedAmount(opponentBlocking ? PUNCH_DAMAGE - punchDmg : 0);
                    break;

                case "kick":
                    int previousOpponentRage = battle.getOpponentRage();
                    battle.setOpponentRage(clampRage(battle.getOpponentRage() - KICK_RAGE_DRAIN));
                    battle.setPlayerRage(clampRage(battle.getPlayerRage() + KICK_RAGE_GAIN));
                    int actualDrained = previousOpponentRage - battle.getOpponentRage();
                    battle.setOpponentRageDrained(actualDrained);
                    break;

                case "rageAction":
                    int fireballDmg = opponentBlocking ? FIREBALL_DAMAGE / BLOCK_DAMAGE_REDUCE : FIREBALL_DAMAGE;
                    battle.setOpponentHP(battle.getOpponentHP() - fireballDmg);
                    battle.setPlayerRage(0);
                    battle.setDamageDealtToOpponent(fireballDmg);
                    battle.setTotalDamageDealtByPlayer(battle.getTotalDamageDealtByPlayer() + fireballDmg);
                    battle.setOpponentBlockedAmount(opponentBlocking ? FIREBALL_DAMAGE - fireballDmg : 0);
                    break;

                case "heal":
                    battle.setPlayerHP(Math.min(MAX_HP, battle.getPlayerHP() + HEAL_AMOUNT));
                    battle.setPlayerRage(clampRage(battle.getPlayerRage() - HEAL_RAGE_COST));
                    battle.setHealAmount(HEAL_AMOUNT);
                    break;

                case "stun":
                    // Stun is blocked if opponent is blocking
                    if (!opponentBlocking) {
                        battle.setPlayerHP(battle.getPlayerHP() - STUN_HP_COST);
                        battle.setPlayerRage(clampRage(battle.getPlayerRage() - STUN_RAGE_COST));
                        battle.setOpponentStunned(true);
                    }
                    break;

                case "block":
                case "idle":
                default:
                    break;
            }
        }

        // ── Execute opponent intent ───────────────────────────
        // Skip if opponent was stunned last round, or intent is "stunned"
        if (!opponentWasStunned && !"stunned".equals(intent)) {
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
                    battle.setOpponentRage(0);
                    break;

                case "heal":
                    battle.setOpponentHP(Math.min(MAX_HP, battle.getOpponentHP() + HEAL_AMOUNT));
                    battle.setOpponentRage(clampRage(battle.getOpponentRage() - HEAL_RAGE_COST));
                    break;

                case "stun":
                    // Opponent stuns the player — blocked if player is blocking
                    if (!playerBlocking) {
                        battle.setPlayerStunned(true);
                    }
                    battle.setOpponentHP(battle.getOpponentHP() - STUN_HP_COST);
                    battle.setOpponentRage(clampRage(battle.getOpponentRage() - STUN_RAGE_COST));
                    break;

                case "block":
                    // Opponent is blocking — damage reduction already handled above
                    break;
            }
        }

        // Clear turn state
        battle.setOpponentIntent(null);
        battle.setSlot1(null);
    }

    // ── determineOpponentIntent ───────────────────────────────
    private String determineOpponentIntent() {
        double random          = Math.random();
        int    opponentHP      = battle.getOpponentHP();
        int    opponentRage    = battle.getOpponentRage();
        int    playerRage      = battle.getPlayerRage();

        boolean canFireball       = opponentRage >= FIREBALL_RAGE_COST;
        boolean canHeal           = opponentRage >= HEAL_RAGE_COST;
        boolean canStun           = opponentRage >= BattleState.STUN_THRESHOLD
                && opponentHP > STUN_HP_COST;
        boolean playerCanFireball = playerRage >= FIREBALL_RAGE_COST;
        boolean playerRageHigh    = playerRage >= 20;
        boolean playerRageLow  = playerRage <= -10;

        // Desperate — low HP
        if (opponentHP < 30) {
            if (canHeal && random < 0.65)    return "heal";
            if (canFireball && random < 0.8) return "fireball";
            if (canStun && random < 0.5)     return "stun";
            return random < 0.5 ? "punch" : "kick";
        }

        // Player is about to fireball — block or stun preemptively
        if (playerCanFireball) {
            if (canStun && random < 0.5) return "stun";
            if (random < 0.45)           return "block";
        }

        // Player has high rage — drain it
        if (playerRageHigh && random < 0.5) return "kick";

        // Full rage — use fireball
        if (canFireball && random < 0.6) return "fireball";

        // Stun — meaningful threat
        if (canStun && random < 0.5) return "stun";

        // Medium HP — consider healing
        if (opponentHP < 50 && canHeal && random < 0.25) return "heal";

        // Occasionally block unprompted — keeps player guessing
        if (random < 0.15) return "block";

        // Default — build rage
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