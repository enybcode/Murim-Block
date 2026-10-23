package com.murimblock.client.gui.data;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Everything the Profile page shows. */
public record ProfileData(
        Component name,
        Component realm,
        Component title,
        Component sect,
        Component martialArt,
        float health,
        float maxHealth,
        int qi,
        int maxQi,
        float qiPerSecond,
        int breakthroughPercent,
        Component nextRealm) {

    /**
     * The current player's profile. Name and health are real; the Qi side is placeholder data
     * until the Qi system exists.
     */
    public static ProfileData current() {
        Player player = Minecraft.getInstance().player;
        Component name = player != null ? player.getName() : Component.translatable("gui.murimblock.profile.player");
        float health = player != null ? player.getHealth() : 20;
        float maxHealth = player != null ? player.getMaxHealth() : 20;
        Component none = Component.translatable("gui.murimblock.none");
        return new ProfileData(
                name,
                Component.translatable("murimblock.realm.third_rate_warrior"),
                none,
                Component.translatable("gui.murimblock.profile.unaffiliated"),
                MartialArtsData.active().name(),
                health,
                maxHealth,
                62,
                100,
                1,
                62,
                Component.translatable("gui.murimblock.unknown"));
    }
}
