package com.ace.taskapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// No Spring context needed — BattleService is plain Java logic.
// Tests run fast with no database or HTTP layer involved.
class BattleServiceTest {

    private BattleService service;
    private BattleState   battle;

    // Runs before every single test — fresh service and fresh battle state
    @BeforeEach
    void setUp() {
        service = new BattleService();
        battle  = new BattleState();
    }

    // ─────────────────────────────────────────────────────────
    // PLAYER ACTIONS
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Player punch deals correct damage to opponent")
    void playerPunch_dealsDamage() {
        service.handlePlayerAction(battle, "punch");

        assertEquals(90, battle.getOpponentHP());
        assertEquals(BattleService.PUNCH_DAMAGE, battle.getDamageDealtToOpponent());
    }

    @Test
    @DisplayName("Player punch gives opponent rage")
    void playerPunch_givesOpponentRage() {
        service.handlePlayerAction(battle, "punch");

        assertEquals(BattleService.PUNCH_RAGE_GAIN, battle.getOpponentRage());
    }

    @Test
    @DisplayName("Player kick drains opponent rage")
    void playerKick_drainsOpponentRage() {
        battle.setOpponentRage(20);
        service.handlePlayerAction(battle, "kick");

        assertEquals(10, battle.getOpponentRage());
        assertEquals(BattleService.KICK_RAGE_DRAIN, battle.getOpponentRageDrained());
    }

    @Test
    @DisplayName("Player kick gives player rage")
    void playerKick_givesPlayerRage() {
        battle.setOpponentRage(20);
        service.handlePlayerAction(battle, "kick");

        assertEquals(BattleService.KICK_RAGE_GAIN, battle.getPlayerRage());
    }

    @Test
    @DisplayName("Player kick does not drain below rage minimum")
    void playerKick_doesNotGoBelowRageMin() {
        battle.setOpponentRage(BattleService.RAGE_MIN); // already at floor
        service.handlePlayerAction(battle, "kick");

        assertEquals(BattleService.RAGE_MIN, battle.getOpponentRage());
    }

    @Test
    @DisplayName("Player rageAction deals fireball damage and resets rage")
    void playerRageAction_dealsFireballDamageAndResetsRage() {
        battle.setPlayerRage(30);
        service.handlePlayerAction(battle, "rageAction");

        assertEquals(75, battle.getOpponentHP());
        assertEquals(0, battle.getPlayerRage());
        assertEquals(BattleService.FIREBALL_DAMAGE, battle.getDamageDealtToOpponent());
    }

    @Test
    @DisplayName("Player heal restores HP and costs rage")
    void playerHeal_restoresHP() {
        battle.setPlayerHP(60);
        battle.setPlayerRage(20);
        service.handlePlayerAction(battle, "heal");

        assertEquals(75, battle.getPlayerHP());
        assertEquals(10, battle.getPlayerRage());
        assertEquals(BattleService.HEAL_AMOUNT, battle.getHealAmount());
    }

    @Test
    @DisplayName("Player heal does not exceed max HP")
    void playerHeal_doesNotExceedMaxHP() {
        battle.setPlayerHP(95);
        battle.setPlayerRage(20);
        service.handlePlayerAction(battle, "heal");

        assertEquals(100, battle.getPlayerHP());
    }

    // ─────────────────────────────────────────────────────────
    // OPPONENT ACTIONS — normal round
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Opponent punch deals damage to player")
    void opponentPunch_dealsDamageToPlayer() {
        battle.setOpponentIntent("punch");
        battle.setSlot1("idle");
        service.handleOpponentAction(battle, false, false);

        assertEquals(90, battle.getPlayerHP());
        assertEquals(BattleService.PUNCH_DAMAGE, battle.getDamageDealtToPlayer());
    }

    @Test
    @DisplayName("Opponent kick drains player rage")
    void opponentKick_drainsPlayerRage() {
        battle.setPlayerRage(20);
        battle.setOpponentIntent("kick");
        battle.setSlot1("idle");
        service.handleOpponentAction(battle, false, false);

        assertEquals(10, battle.getPlayerRage());
        assertEquals(10, battle.getPlayerRageDrained());
    }

    @Test
    @DisplayName("Opponent kick does not drain player below rage minimum")
    void opponentKick_doesNotGoBelowRageMin() {
        battle.setPlayerRage(BattleService.RAGE_MIN);
        battle.setOpponentIntent("kick");
        battle.setSlot1("idle");
        service.handleOpponentAction(battle, false, false);

        assertEquals(BattleService.RAGE_MIN, battle.getPlayerRage());
        assertEquals(0, battle.getPlayerRageDrained());
    }

    @Test
    @DisplayName("Opponent fireball deals correct damage and resets opponent rage")
    void opponentFireball_dealsDamageAndResetsRage() {
        battle.setOpponentRage(30);
        battle.setOpponentIntent("fireball");
        battle.setSlot1("idle");
        service.handleOpponentAction(battle, false, false);

        assertEquals(75, battle.getPlayerHP());
        assertEquals(0, battle.getOpponentRage());
        assertEquals(BattleService.FIREBALL_DAMAGE, battle.getDamageDealtToPlayer());
    }

    @Test
    @DisplayName("Opponent heal restores opponent HP")
    void opponentHeal_restoresHP() {
        battle.setOpponentHP(60);
        battle.setOpponentRage(20);
        battle.setOpponentIntent("heal");
        battle.setSlot1("idle");
        service.handleOpponentAction(battle, false, false);

        assertEquals(75, battle.getOpponentHP());
    }

    @Test
    @DisplayName("Opponent stun marks player as stunned")
    void opponentStun_stunsPlayer() {
        battle.setOpponentIntent("stun");
        battle.setSlot1("idle");
        service.handleOpponentAction(battle, false, false);

        assertTrue(battle.isPlayerStunned());
    }

    // ─────────────────────────────────────────────────────────
    // PLAYER BLOCKING
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Player block halves opponent punch damage")
    void playerBlock_halvesPunchDamage() {
        battle.setOpponentIntent("punch");
        battle.setSlot1("block");
        service.handleOpponentAction(battle, false, false);

        assertEquals(95, battle.getPlayerHP()); // 10 / 2 = 5 damage
        assertEquals(5, battle.getBlockedAmount());
    }

    @Test
    @DisplayName("Player block halves opponent fireball damage")
    void playerBlock_halvesFireballDamage() {
        battle.setOpponentRage(30);
        battle.setOpponentIntent("fireball");
        battle.setSlot1("block");
        service.handleOpponentAction(battle, false, false);

        assertEquals(88, battle.getPlayerHP()); // 25 / 2 = 12 damage (integer division)
        assertEquals(13, battle.getBlockedAmount());
    }

    @Test
    @DisplayName("Player block prevents opponent stun")
    void playerBlock_preventsOpponentStun() {
        battle.setOpponentIntent("stun");
        battle.setSlot1("block");
        service.handleOpponentAction(battle, false, false);

        assertFalse(battle.isPlayerStunned());
    }

    @Test
    @DisplayName("Player block does not prevent opponent kick")
    void playerBlock_doesNotPreventKick() {
        battle.setPlayerRage(20);
        battle.setOpponentIntent("kick");
        battle.setSlot1("block");
        service.handleOpponentAction(battle, false, false);

        // Rage should still be drained — block doesn't protect rage
        assertEquals(10, battle.getPlayerRage());
    }

    // ─────────────────────────────────────────────────────────
    // OPPONENT BLOCKING
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Opponent block halves player punch damage")
    void opponentBlock_halvesPunchDamage() {
        battle.setOpponentIntent("block");
        battle.setSlot1("punch");
        service.handleOpponentAction(battle, false, false);

        assertEquals(95, battle.getOpponentHP()); // 10 / 2 = 5 damage
        assertEquals(5, battle.getOpponentBlockedAmount());
    }

    @Test
    @DisplayName("Opponent block halves player rageAction damage")
    void opponentBlock_halvesFireballDamage() {
        battle.setOpponentIntent("block");
        battle.setSlot1("rageAction");
        service.handleOpponentAction(battle, false, false);

        assertEquals(88, battle.getOpponentHP()); // 25 / 2 = 12 damage
        assertEquals(13, battle.getOpponentBlockedAmount());
    }

    @Test
    @DisplayName("Opponent block prevents player stun")
    void opponentBlock_preventsPlayerStun() {
        battle.setOpponentIntent("block");
        battle.setSlot1("stun");
        service.handleOpponentAction(battle, false, false);

        assertFalse(battle.isOpponentStunned());
    }

    // ─────────────────────────────────────────────────────────
    // STUN STATE
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Player stun stuns the opponent")
    void playerStun_stunsOpponent() {
        battle.setOpponentIntent("punch"); // opponent is attacking
        battle.setPlayerRage(15);
        battle.setSlot1("stun");
        service.handleOpponentAction(battle, false, false);

        assertTrue(battle.isOpponentStunned());
    }

    @Test
    @DisplayName("Stunned opponent does not act")
    void stunnedOpponent_doesNotAct() {
        // opponentWasStunned = true means they were stunned last round
        battle.setOpponentIntent("punch");
        battle.setSlot1("idle");
        service.handleOpponentAction(battle, true, false); // opponent was stunned

        // Player should take no damage
        assertEquals(100, battle.getPlayerHP());
        assertEquals(0, battle.getDamageDealtToPlayer());
    }

    @Test
    @DisplayName("Stunned player does not act")
    void stunnedPlayer_doesNotAct() {
        // playerWasStunned = true means they were stunned last round
        battle.setOpponentIntent("punch");
        battle.setSlot1("punch"); // player chose punch but they're stunned
        service.handleOpponentAction(battle, false, true); // player was stunned

        // Opponent should take no damage from player
        assertEquals(100, battle.getOpponentHP());
        assertEquals(0, battle.getDamageDealtToOpponent());
        // But opponent still attacks
        assertEquals(90, battle.getPlayerHP());
    }

    // ─────────────────────────────────────────────────────────
    // RAGE CLAMPING
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Rage does not exceed maximum")
    void rage_doesNotExceedMax() {
        battle.setPlayerRage(28);
        // punch gives 5 rage — would push to 33, should clamp to 30
        battle.setOpponentIntent("punch");
        battle.setSlot1("idle");
        service.handleOpponentAction(battle, false, false);

        assertEquals(BattleService.RAGE_MAX, battle.getPlayerRage());
    }

    @Test
    @DisplayName("Rage does not go below minimum")
    void rage_doesNotGoBelowMin() {
        battle.setPlayerRage(-18);
        // kick drains 10 — would push to -28, should clamp to -20
        battle.setOpponentIntent("kick");
        battle.setSlot1("idle");
        service.handleOpponentAction(battle, false, false);

        assertEquals(BattleService.RAGE_MIN, battle.getPlayerRage());
    }

    // ─────────────────────────────────────────────────────────
    // BATTLE OVER
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("React sets battle over when opponent HP reaches zero")
    void react_battleOverWhenOpponentDies() {
        battle.setOpponentHP(5);
        battle.setOpponentIntent("punch"); // opponent punches while player also punches
        battle = service.react(battle, "punch");

        assertTrue(battle.isBattleOver());
        assertEquals("player", battle.getWinner());
    }

    @Test
    @DisplayName("React sets battle over when player HP reaches zero")
    void react_battleOverWhenPlayerDies() {
        battle.setPlayerHP(5);
        battle.setOpponentIntent("punch");
        battle = service.react(battle, "idle");

        assertTrue(battle.isBattleOver());
        assertEquals("opponent", battle.getWinner());
    }

    // ─────────────────────────────────────────────────────────
    // AI — determineOpponentIntent
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("AI never kicks when player rage is at minimum")
    void ai_neverKicksWhenPlayerRageAtMin() {
        battle.setPlayerRage(BattleService.RAGE_MIN);

        // Run 200 times — kick should never be returned
        for (int i = 0; i < 200; i++) {
            String intent = service.determineOpponentIntent(battle);
            assertNotEquals("kick", intent,
                    "AI should not kick when player rage is already at minimum");
        }
    }

    @Test
    @DisplayName("AI returns valid intent strings only")
    void ai_returnsValidIntents() {
        java.util.Set<String> valid = java.util.Set.of(
                "punch", "kick", "fireball", "heal", "stun", "block"
        );

        for (int i = 0; i < 500; i++) {
            String intent = service.determineOpponentIntent(battle);
            assertTrue(valid.contains(intent),
                    "Unexpected intent returned: " + intent);
        }
    }

    @Test
    @DisplayName("AI does not use fireball without enough rage")
    void ai_doesNotFireballWithoutRage() {
        battle.setOpponentRage(0); // no rage

        for (int i = 0; i < 200; i++) {
            String intent = service.determineOpponentIntent(battle);
            assertNotEquals("fireball", intent,
                    "AI should not fireball without enough rage");
        }
    }

    @Test
    @DisplayName("AI does not stun without enough rage")
    void ai_doesNotStunWithoutRage() {
        battle.setOpponentRage(0);

        for (int i = 0; i < 200; i++) {
            String intent = service.determineOpponentIntent(battle);
            assertNotEquals("stun", intent,
                    "AI should not stun without enough rage");
        }
    }

    @Test
    @DisplayName("clampRage returns value within bounds")
    void clampRage_staysInBounds() {
        assertEquals(BattleService.RAGE_MAX, service.clampRage(999));
        assertEquals(BattleService.RAGE_MIN, service.clampRage(-999));
        assertEquals(15, service.clampRage(15));
    }
}