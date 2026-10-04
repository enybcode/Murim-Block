package com.murimblock.combat.attack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static com.murimblock.combat.attack.AttackContactResolver.*;
import static org.junit.jupiter.api.Assertions.*;

class AttackContactResolverTest {
    private static final UUID PLAYER = new UUID(0, 1);
    private static final UUID MOB = new UUID(0, 2);
    private static final UUID THIRD = new UUID(0, 3);
    private static final AttackId A = new AttackId(PLAYER, 1);
    private static final AttackId B = new AttackId(MOB, 1);
    private static final AttackId C = new AttackId(THIRD, 1);

    @Test
    void earlyBladeContactConsumesBothStrikesBeforeEitherBodyHit() {
        var clash = new BladeClash(100.25, B, A);
        var result = resolve(List.of(new BodyHit(100.5, A, MOB), new BodyHit(100.5, B, PLAYER), clash), Set.of());
        assertEquals(List.of(clash), result.accepted());
        assertEquals(Set.of(A, B), result.consumed());
        assertEquals(A, clash.attack());
    }

    @Test
    void lateClashCannotUndoAnEarlierHitOrConsumeTheOtherStrike() {
        var first = new BodyHit(100.1, A, MOB);
        var counter = new BodyHit(100.8, B, PLAYER);
        var result = resolve(List.of(counter, new BladeClash(100.5, A, B), first), Set.of());
        assertEquals(List.of(first, counter), result.accepted());
    }

    @Test
    void exactTimeClashWinsOverBodyHitsIndependentOfInputOrder() {
        var clash = new BladeClash(100.5, A, B);
        var result = resolve(List.of(new BodyHit(100.5, A, MOB), new BodyHit(100.5, B, PLAYER), clash), Set.of());
        assertEquals(List.of(clash), result.accepted());
    }

    @Test
    void wallWinsExactTimeTieAndDoesNotStopTheOtherActorsAttack() {
        var wall = new Obstruction(100.5, A, new BlockPos(1, 2, 3));
        var counter = new BodyHit(100.5, B, PLAYER);
        var result = resolve(List.of(new BodyHit(100.5, A, MOB), new BladeClash(100.5, A, B), counter, wall), Set.of());
        assertEquals(List.of(wall, counter), result.accepted());
    }

    @Test
    void chronologyTakesPriorityOverContactKind() {
        var hit = new BodyHit(100.1, A, MOB);
        var result = resolve(List.of(new Obstruction(100.2, A, BlockPos.ZERO), hit), Set.of());
        assertEquals(List.of(hit), result.accepted());
    }

    @Test
    void clashDoesNotMakeEitherParticipantImmuneToAnUnrelatedAttack() {
        var clash = new BladeClash(100, A, B);
        var thirdHit = new BodyHit(100.1, C, PLAYER);
        assertEquals(List.of(clash, thirdHit), resolve(List.of(thirdHit, clash), Set.of()).accepted());
    }

    @Test
    void duplicateContactsAndLaterUpdatesCannotApplyTheSameStrikeAgain() {
        var hit = new BodyHit(100, A, MOB);
        var first = resolve(List.of(hit, hit, new BodyHit(100.1, A, THIRD)), Set.of());
        assertEquals(List.of(hit), first.accepted());
        assertTrue(resolve(List.of(hit), first.consumed()).accepted().isEmpty());
        var nextAttack = new AttackId(PLAYER, 2);
        var nextHit = new BodyHit(110, nextAttack, MOB);
        assertEquals(List.of(nextHit), resolve(List.of(nextHit), first.consumed()).accepted());
    }

    @Test
    void cancelledStrikeCannotClashButDoesNotCancelTheOpponentsBodyHit() {
        var counter = new BodyHit(100.2, B, PLAYER);
        var result = resolve(List.of(new BladeClash(100, A, B), counter), Set.of(A));
        assertEquals(List.of(counter), result.accepted());
        assertEquals(Set.of(A, B), result.consumed());
    }

    @Test
    void ambiguousSameTimeContactsAreDeterministicAcrossPermutations() {
        List<Contact> contacts = new ArrayList<>(List.of(
                new BladeClash(100, B, C), new BladeClash(100, A, C), new BladeClash(100, B, A),
                new BodyHit(100, C, PLAYER), new BodyHit(100, C, MOB)));
        var expected = resolve(contacts, Set.of());
        assertEquals(List.of(new BladeClash(100, A, B), new BodyHit(100, C, PLAYER)), expected.accepted());
        var random = new Random(1234);
        for (int i = 0; i < 100; i++) {
            Collections.shuffle(contacts, random);
            assertEquals(expected, resolve(contacts, Set.of()));
        }
    }

    @Test
    void mutableInputsCannotChangeStoredContactsOrResolution() {
        var block = new BlockPos.MutableBlockPos(1, 2, 3);
        var obstruction = new Obstruction(100, A, block);
        block.set(4, 5, 6);
        assertEquals(new BlockPos(1, 2, 3), obstruction.block());
        var candidates = new ArrayList<Contact>(List.of(obstruction));
        var consumed = new java.util.HashSet<AttackId>();
        var result = resolve(candidates, consumed);
        assertTrue(consumed.isEmpty());
        candidates.clear();
        assertEquals(List.of(obstruction), result.accepted());
        assertThrows(UnsupportedOperationException.class, () -> result.accepted().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.consumed().clear());
    }

    @Test
    void invalidIdentityTimeAndSelfContactsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AttackId(PLAYER, -1));
        assertThrows(NullPointerException.class, () -> new AttackId(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new BodyHit(100, A, PLAYER));
        assertThrows(IllegalArgumentException.class, () -> new BladeClash(100, A, new AttackId(PLAYER, 2)));
        for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new BodyHit(invalid, A, MOB));
            assertThrows(IllegalArgumentException.class, () -> new BladeClash(invalid, A, B));
            assertThrows(IllegalArgumentException.class, () -> new Obstruction(invalid, A, BlockPos.ZERO));
        }
    }
}
