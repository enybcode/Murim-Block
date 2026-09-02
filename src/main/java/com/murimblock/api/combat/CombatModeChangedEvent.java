package com.murimblock.api.combat;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fired on the NeoForge event bus when a player's Murimblock combat mode changes.
 *
 * <p>This event is emitted only on actual state transitions, never every tick.</p>
 */
public class CombatModeChangedEvent extends PlayerEvent {
    private final boolean enabled;

    public CombatModeChangedEvent(ServerPlayer player, boolean enabled) {
        super(player);
        this.enabled = enabled;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    public boolean enabled() {
        return enabled;
    }
}
