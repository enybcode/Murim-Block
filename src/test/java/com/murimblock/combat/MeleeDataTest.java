package com.murimblock.combat;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MeleeDataTest {
    @Test
    void initialStateDoesNotGuardLockOrAnimate() {
        assertFalse(MeleeData.initial().isGuarding());
        assertFalse(MeleeData.initial().locksAttack(100));
        assertFalse(MeleeData.initial().isActive(100));
    }

    @Test
    void heldGuardPersistsButAllowsPlayerToBeginAnAttack() {
        MeleeData guard = MeleeData.begin(MeleeData.Action.GUARD, 100, 0);
        assertTrue(guard.isGuarding());
        assertTrue(guard.isActive(1000));
        assertFalse(guard.locksAttack(1000));
    }

    @Test
    void recoveryAndStunHaveExclusiveExpiryAndBlockAttacksUntilThen() {
        for (MeleeData.Action action : new MeleeData.Action[]{MeleeData.Action.SWING,
                MeleeData.Action.CLASH, MeleeData.Action.GUARD_IMPACT, MeleeData.Action.GUARD_BREAK}) {
            MeleeData data = MeleeData.begin(action, 100, 6);
            assertTrue(data.locksAttack(105), action.toString());
            assertTrue(data.isActive(105), action.toString());
            assertFalse(data.locksAttack(106), action.toString());
            assertFalse(data.isActive(106), action.toString());
        }
    }

    @Test
    void successfulGuardImpactRemainsProtectiveButClashAndBreakDoNot() {
        assertTrue(MeleeData.begin(MeleeData.Action.GUARD_IMPACT, 100, 4).isGuarding());
        assertFalse(MeleeData.begin(MeleeData.Action.CLASH, 100, 6).isGuarding());
        assertFalse(MeleeData.begin(MeleeData.Action.GUARD_BREAK, 100, 14).isGuarding());
    }

    @Test
    void allActionStatesRoundTripWithoutLosingTheirTimings() {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            for (MeleeData.Action action : MeleeData.Action.values()) {
                MeleeData expected = MeleeData.begin(action, 900, 14);
                MeleeData.STREAM_CODEC.encode(buffer, expected);
                assertEquals(expected, MeleeData.STREAM_CODEC.decode(buffer));
            }
        } finally {
            buffer.release();
        }
    }

    @Test
    void invalidActionsAndTimesCannotEnterSynchronizedState() {
        assertThrows(IllegalArgumentException.class, () -> MeleeData.Action.decode(-1));
        assertThrows(IllegalArgumentException.class, () -> MeleeData.Action.decode(6));
        assertThrows(IllegalArgumentException.class, () -> new MeleeData(MeleeData.Action.SWING, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> new MeleeData(MeleeData.Action.SWING, 20, 10));
        assertThrows(IllegalArgumentException.class, () -> MeleeData.begin(MeleeData.Action.GUARD, 10, -1));
        assertThrows(ArithmeticException.class, () -> MeleeData.begin(MeleeData.Action.SWING, Long.MAX_VALUE, 10));
        assertFalse(MeleeData.begin(MeleeData.Action.SWING, 100, 10).isActive(99));
    }
}
