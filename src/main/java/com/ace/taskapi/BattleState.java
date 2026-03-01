package com.ace.taskapi;

public class BattleState {

    // ── Core stats ─────────────────────────────────────────────
    private int playerHP     = 100;
    private int opponentHP   = 100;
    private int playerRage   = 0;
    private int opponentRage = MAX_RAGE;  // opponent starts at full rage — the player can drain it
    private int blockedAmount;
    private int maxHP        = 100;
    private int maxRage      = 30;
    private static final int MAX_HP              = 100;
    private static final int MAX_RAGE            = 30;
    private static final int RAGE_STRIKE_THRESHOLD = 20;
    private static final int KICK_RAGE_DRAIN     = 7;   // how much rage kick steals from opponent

    // ── Turn state ──────────────────────────────────────────────
    private String opponentIntent = null; // "attack" | "fireball" | "heal" | "charge"
    private String slot1          = null; // player's reaction during opponent action
    private String slot2          = null; // player's action after opponent action
    private int    timeWindow     = 4;    // seconds player has to queue actions

    // ── Resolution results (sent to frontend each turn) ─────────
    private int    damageDealtToOpponent = 0;
    private int    damageDealtToPlayer   = 0;
    private int    rageChange            = 0;  // net player rage change this turn
    private int    opponentRageDrained   = 0;  // how much rage the kick stole this turn
    private int    healAmount            = 0;
    private String slot1Result           = null;
    private String slot2Result           = null;
    private String lastAction            = null;

    // ── Battle status ───────────────────────────────────────────
    private boolean battleOver              = false;
    private String  winner                  = null;
    private int     totalDamageDealtByPlayer = 0;

    // ── Getters and setters ─────────────────────────────────────
    public int getPlayerHP() { return playerHP; }
    public void setPlayerHP(int playerHP) { this.playerHP = Math.max(0, Math.min(MAX_HP, playerHP)); }

    public int getOpponentHP() { return opponentHP; }
    public void setOpponentHP(int opponentHP) { this.opponentHP = Math.max(0, Math.min(MAX_HP, opponentHP)); }

    public int getBlockedAmount() { return blockedAmount; }
    public void setBlockedAmount(int blockedAmount) { this.blockedAmount = blockedAmount; }

    public int getPlayerRage() { return playerRage; }
    public void setPlayerRage(int playerRage) { this.playerRage = Math.max(0, Math.min(MAX_RAGE, playerRage)); }

    public int getOpponentRage() { return opponentRage; }
    public void setOpponentRage(int opponentRage) { this.opponentRage = Math.max(0, Math.min(MAX_RAGE, opponentRage)); }

    public int getOpponentRageDrained() { return opponentRageDrained; }
    public void setOpponentRageDrained(int opponentRageDrained) { this.opponentRageDrained = opponentRageDrained; }

    public int getMaxRage() { return MAX_RAGE; }
    public int getRageStrikeThreshold() { return RAGE_STRIKE_THRESHOLD; }
    public int getKickRageDrain() { return KICK_RAGE_DRAIN; }
    public int getMaxHP() { return MAX_HP; }

    public String getOpponentIntent() { return opponentIntent; }
    public void setOpponentIntent(String opponentIntent) { this.opponentIntent = opponentIntent; }

    public String getSlot1() { return slot1; }
    public void setSlot1(String slot1) { this.slot1 = slot1; }

    public String getSlot2() { return slot2; }
    public void setSlot2(String slot2) { this.slot2 = slot2; }

    public int getTimeWindow() { return timeWindow; }
    public void setTimeWindow(int timeWindow) { this.timeWindow = timeWindow; }

    public int getDamageDealtToOpponent() { return damageDealtToOpponent; }
    public void setDamageDealtToOpponent(int d) { this.damageDealtToOpponent = d; }

    public int getDamageDealtToPlayer() { return damageDealtToPlayer; }
    public void setDamageDealtToPlayer(int d) { this.damageDealtToPlayer = d; }

    public int getRageChange() { return rageChange; }
    public void setRageChange(int rageChange) { this.rageChange = rageChange; }

    public int getHealAmount() { return healAmount; }
    public void setHealAmount(int healAmount) { this.healAmount = healAmount; }

    public String getSlot1Result() { return slot1Result; }
    public void setSlot1Result(String slot1Result) { this.slot1Result = slot1Result; }

    public String getSlot2Result() { return slot2Result; }
    public void setSlot2Result(String slot2Result) { this.slot2Result = slot2Result; }

    public String getLastAction() { return lastAction; }
    public void setLastAction(String lastAction) { this.lastAction = lastAction; }

    public boolean isBattleOver() { return battleOver; }
    public void setBattleOver(boolean battleOver) { this.battleOver = battleOver; }

    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }

    public int getTotalDamageDealtByPlayer() { return totalDamageDealtByPlayer; }
    public void setTotalDamageDealtByPlayer(int t) { this.totalDamageDealtByPlayer = t; }

    public boolean canRageStrike() { return playerRage >= RAGE_STRIKE_THRESHOLD; }
}