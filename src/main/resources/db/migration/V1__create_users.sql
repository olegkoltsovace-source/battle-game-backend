-- V1__create_users.sql
-- Initial users table matching the User entity

CREATE TABLE IF NOT EXISTS users (
    id                 BIGSERIAL PRIMARY KEY,
    username           VARCHAR(255) NOT NULL UNIQUE,
    password           VARCHAR(255) NOT NULL,
    created_at         TIMESTAMP,
    total_wins         INTEGER DEFAULT 0,
    total_losses       INTEGER DEFAULT 0,
    total_damage_dealt INTEGER DEFAULT 0
);
