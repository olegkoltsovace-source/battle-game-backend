-- V2__create_battles.sql
-- Initial battles table matching the Battle entity

CREATE TABLE IF NOT EXISTS battles (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT REFERENCES users(id),
    player_hp_final    INTEGER,
    opponent_hp_final  INTEGER,
    winner             VARCHAR(20),
    total_damage_dealt INTEGER,
    battle_date        TIMESTAMP
);
