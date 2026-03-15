package com.ace.taskapi;

import org.springframework.stereotype.Service;

@Service
public class BattleService {

    // ── Constants ─────────────────────────────────────────────
    static final int PUNCH_DAMAGE        = 10;
    static final int PUNCH_RAGE_GAIN     = 5;
    static final int KICK_RAGE_DRAIN     = 10;
    static final int KICK_RAGE_GAIN      = 10;
    static final int FIREBALL_DAMAGE     = 25;
    static final int FIREBALL_RAGE_COST  = 30;
    static final int HEAL_AMOUNT         = 15;
    static final int HEAL_RAGE_COST      = 10;
    static final int BLOCK_DAMAGE_REDUCE = 2;
    static final int STUN_RAGE_COST      = 15;
    static final int STUN_HP_COST        = 10;
    static final int RAGE_MIN            = -20;
    static final int RAGE_MAX            = 30;
    static final int MAX_HP              = 100;

    // ── Start ─────────────────────────────────────────────────
    public BattleState startBattle() {
        BattleState battle = new BattleState();
        battle.setOpponentIntent(determineOpponentIntent(battle));
        return battle;
    }

    // ── React ─────────────────────────────────────────────────
    public BattleState react(BattleState battle, String slot1) {
        battle.setSlot1(slot1);

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

        handleOpponentAction(battle, opponentWasStunned, playerWasStunned);

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
                battle.setOpponentIntent(determineOpponentIntent(battle));
            }
        }

        return battle;
    }

    // ── handlePlayerAction ────────────────────────────────────
    public void handlePlayerAction(BattleState battle, String action) {
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
    public void handleOpponentAction(BattleState battle, boolean opponentWasStunned, boolean playerWasStunned) {
        String  intent           = battle.getOpponentIntent();
        String  slot1            = battle.getSlot1();
        boolean playerBlocking   = "block".equals(slot1);
        boolean opponentBlocking = "block".equals(intent);

        // ── Execute player reaction ───────────────────────────
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
                    if (!playerBlocking) {
                        battle.setPlayerStunned(true);
                    }
                    battle.setOpponentHP(battle.getOpponentHP() - STUN_HP_COST);
                    battle.setOpponentRage(clampRage(battle.getOpponentRage() - STUN_RAGE_COST));
                    break;

                case "block":
                    break;
            }
        }

        battle.setOpponentIntent(null);
        battle.setSlot1(null);
    }

    // ── determineOpponentIntent ───────────────────────────────
    public String determineOpponentIntent(BattleState battle) {
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
        boolean canDrainPlayer    = playerRage > RAGE_MIN;

        // Desperate — low HP
        if (opponentHP < 30) {
            if (canHeal && random < 0.65)    return "heal";
            if (canFireball && random < 0.8) return "fireball";
            if (canStun && random < 0.5)     return "stun";
            return random < 0.5 ? "punch" : (canDrainPlayer ? "kick" : "punch");
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

        // Default — build rage, but don't kick if player is already at floor
        if (random < 0.6) return "punch";
        return canDrainPlayer ? "kick" : "punch";
    }

    // ── clampRage ─────────────────────────────────────────────
    public int clampRage(int rage) {
        return Math.max(RAGE_MIN, Math.min(RAGE_MAX, rage));
    }
}