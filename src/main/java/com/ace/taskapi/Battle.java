package com.ace.taskapi;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "battles")
public class Battle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "player_hp_final")
    private Integer playerHpFinal;

    @Column(name = "opponent_hp_final")
    private Integer opponentHpFinal;

    @Column(name = "winner")
    private String winner; // "player" or "opponent"

    @Column(name = "total_damage_dealt")
    private Integer totalDamageDealt;

    @Column(name = "battle_date")
    private LocalDateTime battleDate;

    // Constructors
    public Battle() {
        this.battleDate = LocalDateTime.now();
    }

    public Battle(Long userId, Integer playerHpFinal, Integer opponentHpFinal,
                  String winner, Integer totalDamageDealt) {
        this.userId = userId;
        this.playerHpFinal = playerHpFinal;
        this.opponentHpFinal = opponentHpFinal;
        this.winner = winner;
        this.totalDamageDealt = totalDamageDealt;
        this.battleDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getPlayerHpFinal() {
        return playerHpFinal;
    }

    public void setPlayerHpFinal(Integer playerHpFinal) {
        this.playerHpFinal = playerHpFinal;
    }

    public Integer getOpponentHpFinal() {
        return opponentHpFinal;
    }

    public void setOpponentHpFinal(Integer opponentHpFinal) {
        this.opponentHpFinal = opponentHpFinal;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public Integer getTotalDamageDealt() {
        return totalDamageDealt;
    }

    public void setTotalDamageDealt(Integer totalDamageDealt) {
        this.totalDamageDealt = totalDamageDealt;
    }

    public LocalDateTime getBattleDate() {
        return battleDate;
    }

    public void setBattleDate(LocalDateTime battleDate) {
        this.battleDate = battleDate;
    }
}