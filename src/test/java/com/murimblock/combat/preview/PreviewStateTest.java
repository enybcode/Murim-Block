package com.murimblock.combat.preview;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.HumanoidArm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreviewStateTest {
    @Test
    void initialStateIsInactiveAndStartHasExclusiveExpiry() {
        assertFalse(PreviewState.initial().isActive(0));
        PreviewState state = PreviewState.initial().begin(100);
        assertEquals(1, state.generation());
        assertFalse(state.isActive(99));
        assertTrue(state.isActive(100));
        assertTrue(state.isActive(123));
        assertFalse(state.isActive(124));
        assertEquals(PreviewDefinition.TIMELINE.durationTicks(), state.durationTicks());
    }

    @Test
    void samplesDoNotRestartAndStopCannotMoveTimeBackwards() {
        PreviewState state = PreviewState.initial().begin(100).sample(109);
        assertEquals(9, state.sampledAge());
        assertEquals(100, state.startedAt());
        PreviewState stopped = state.stop(1);
        assertEquals(109, stopped.sampledAt());
        assertFalse(stopped.isActive(110));
        assertEquals(2, stopped.begin(3).generation());
        assertEquals(24, state.sample(Long.MAX_VALUE).sampledAge());
    }

    @Test
    void malformedSnapshotsAndGenerationOverflowAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PreviewState(-1, 0, 0, 24, true));
        assertThrows(IllegalArgumentException.class, () -> new PreviewState(1, -1, 0, 24, true));
        assertThrows(IllegalArgumentException.class, () -> new PreviewState(1, 5, 4, 24, true));
        assertThrows(IllegalArgumentException.class, () -> new PreviewState(1, 0, 0, 25, true));
        assertThrows(IllegalArgumentException.class, () -> new PreviewState(0, 0, 0, 24, true));
        assertThrows(IllegalArgumentException.class, () -> new PreviewState(1, 0, 0, 0, true));
        assertThrows(ArithmeticException.class, () -> new PreviewState(Long.MAX_VALUE, 0, 0, 24, false).begin(0));
    }

    @Test
    void allSnapshotsRoundTripWithNoLostTiming() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            PreviewState start = PreviewState.initial().begin(345);
            for (PreviewState state : new PreviewState[]{PreviewState.initial(), start, start.sample(354), start.stop(360)}) {
                PreviewState.STREAM_CODEC.encode(buffer, state);
                assertEquals(state, PreviewState.STREAM_CODEC.decode(buffer));
            }
        } finally { buffer.release(); }
    }

    @Test
    void socketSelectionDoesNotPretendToBeAWorldSpaceCollider() {
        assertEquals("right_item", PreviewDefinition.weaponSocket(HumanoidArm.RIGHT));
        assertEquals("left_item", PreviewDefinition.weaponSocket(HumanoidArm.LEFT));
    }
}
