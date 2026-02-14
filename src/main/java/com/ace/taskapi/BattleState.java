package com.ace.taskapi;

public class BattleState {
    private int playerHP;
    private int opponentHP;
    private int playerRage;
    private int opponentMana;
    private boolean playerDefending;
    private boolean opponentDefending;
    private String lastAction;
    private int damageDealt;
    private boolean battleOver;
    private String winner;

    public BattleState() {
        // Initialize with starting values
        this.playerHP = 80;
        this.opponentHP = 80;
        this.playerRage = 0;
        this.opponentMana = 5;
        this.playerDefending = false;
        this.opponentDefending = false;
        this.battleOver = false;
    }

    // Getters and Setters
    private int totalDamageDealtByPlayer = 0;

    public int getTotalDamageDealtByPlayer() {
        return totalDamageDealtByPlayer;
    }

    public void setTotalDamageDealtByPlayer(int totalDamageDealtByPlayer) {
        this.totalDamageDealtByPlayer = totalDamageDealtByPlayer;
    }


    public int getPlayerHP() { return playerHP; }
    public void setPlayerHP(int playerHP) { this.playerHP = playerHP; }

    public int getOpponentHP() { return opponentHP; }
    public void setOpponentHP(int opponentHP) { this.opponentHP = opponentHP; }

    public int getPlayerRage() { return playerRage; }
    public void setPlayerRage(int playerRage) { this.playerRage = playerRage; }

    public int getOpponentMana() { return opponentMana; }
    public void setOpponentMana(int opponentMana) { this.opponentMana = opponentMana; }

    public boolean isPlayerDefending() { return playerDefending; }
    public void setPlayerDefending(boolean playerDefending) { this.playerDefending = playerDefending; }

    public boolean isOpponentDefending() { return opponentDefending; }
    public void setOpponentDefending(boolean opponentDefending) { this.opponentDefending = opponentDefending; }

    public String getLastAction() { return lastAction; }
    public void setLastAction(String lastAction) { this.lastAction = lastAction; }

    public int getDamageDealt() { return damageDealt; }
    public void setDamageDealt(int damageDealt) { this.damageDealt = damageDealt; }

    public boolean isBattleOver() { return battleOver; }
    public void setBattleOver(boolean battleOver) { this.battleOver = battleOver; }

    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }
}