package com.ace.taskapi;

public class BattleState {

    // ── Core stats ────────────────────────────────────────────
    private int playerHP     = 100;
    private int opponentHP   = 100;
    private int playerRage   = 0;
    private int opponentRage = 0;   // both start at 0 — rage is built through combat

    // ── Constants ─────────────────────────────────────────────
    private static final int MAX_HP             = 100;
    private static final int MAX_RAGE           = 30;
    private static final int MIN_RAGE           = -20;
    private static final int FIREBALL_THRESHOLD = 30;  // rage required to fireball
    private static final int HEAL_THRESHOLD     = 10;  // rage required to heal
    public static final int STUN_THRESHOLD = 15;

    // ── Turn state ────────────────────────────────────────────
    private String opponentIntent = null;  // "punch" | "kick" | "fireball" | "heal"
    private String slot1          = null;  // player's chosen reaction

    // ── Resolution results (sent to frontend each turn) ───────
    private int    damageDealtToOpponent = 0;
    private int    damageDealtToPlayer   = 0;
    private int    playerRageDrained     = 0;
    private int    opponentRageDrained   = 0;
    private int    healAmount            = 0;
    private int    blockedAmount         = 0;
    private int    opponentBlockedAmount = 0;
    private String slot1Result           = null;
    private boolean playerStunned = false;
    private boolean opponentStunned = false;

    // ── Battle status ─────────────────────────────────────────
    private boolean battleOver               = false;
    private String  winner                   = null;
    private int     totalDamageDealtByPlayer = 0;

    // ── Getters and setters ───────────────────────────────────

    public int getPlayerHP() { return playerHP; }
    public void setPlayerHP(int hp) { this.playerHP = Math.max(0, Math.min(MAX_HP, hp)); }

    public int getOpponentHP() { return opponentHP; }
    public void setOpponentHP(int hp) { this.opponentHP = Math.max(0, Math.min(MAX_HP, hp)); }

    public int getPlayerRage() { return playerRage; }
    public void setPlayerRage(int rage) { this.playerRage = Math.max(MIN_RAGE, Math.min(MAX_RAGE, rage)); }

    public int getOpponentRage() { return opponentRage; }
    public void setOpponentRage(int rage) { this.opponentRage = Math.max(MIN_RAGE, Math.min(MAX_RAGE, rage)); }

    public int getBlockedAmount() { return blockedAmount; }
    public void setBlockedAmount(int blockedAmount) { this.blockedAmount = blockedAmount; }

    public int getOpponentRageDrained() { return opponentRageDrained; }
    public void setOpponentRageDrained(int drained) { this.opponentRageDrained = drained; }

    public int getPlayerRageDrained() { return playerRageDrained; }
    public void setPlayerRageDrained(int drained) { this.playerRageDrained = drained; }

    public int getMaxHP() { return MAX_HP; }
    public int getMaxRage() { return MAX_RAGE; }
    public int getMinRage() { return MIN_RAGE; }

    public int getFireballThreshold() { return FIREBALL_THRESHOLD; }
    public int getHealThreshold() { return HEAL_THRESHOLD; }

    public int getStunThreshold() { return STUN_THRESHOLD; }

    public String getOpponentIntent() { return opponentIntent; }
    public void setOpponentIntent(String intent) { this.opponentIntent = intent; }

    public String getSlot1() { return slot1; }
    public void setSlot1(String slot1) { this.slot1 = slot1; }

    public int getDamageDealtToOpponent() { return damageDealtToOpponent; }
    public void setDamageDealtToOpponent(int d) { this.damageDealtToOpponent = d; }

    public int getDamageDealtToPlayer() { return damageDealtToPlayer; }
    public void setDamageDealtToPlayer(int d) { this.damageDealtToPlayer = d; }

    public int getHealAmount() { return healAmount; }
    public void setHealAmount(int healAmount) { this.healAmount = healAmount; }

    public int getOpponentBlockedAmount() { return opponentBlockedAmount; }
    public void setOpponentBlockedAmount(int opponentBlockedAmount) { this.opponentBlockedAmount = opponentBlockedAmount; }

    public String getSlot1Result() { return slot1Result; }
    public void setSlot1Result(String result) { this.slot1Result = result; }

    public boolean isPlayerStunned() { return playerStunned; }
    public void setPlayerStunned(boolean playerStunned) { this.playerStunned = playerStunned; }

    public boolean isOpponentStunned() { return opponentStunned; }
    public void setOpponentStunned(boolean opponentStunned) { this.opponentStunned = opponentStunned; }

    public boolean isBattleOver() { return battleOver; }
    public void setBattleOver(boolean battleOver) { this.battleOver = battleOver; }

    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }

    public int getTotalDamageDealtByPlayer() { return totalDamageDealtByPlayer; }
    public void setTotalDamageDealtByPlayer(int t) { this.totalDamageDealtByPlayer = t; }

    // ── Convenience checks ────────────────────────────────────
    public boolean canFireball() { return playerRage >= FIREBALL_THRESHOLD; }
    public boolean canHeal()     { return playerRage >= HEAL_THRESHOLD; }
}