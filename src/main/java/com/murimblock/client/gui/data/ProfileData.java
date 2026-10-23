package com.murimblock.client.gui.data;

import com.murimblock.client.cultivation.ClientCultivation;
import com.murimblock.cultivation.Cultivation;
import com.murimblock.cultivation.CultivationRules;
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

    /** The current player's profile. Title, sect and martial art are still placeholders. */
    public static ProfileData current() {
        Player player = Minecraft.getInstance().player;
        Component name = player != null ? player.getName() : Component.translatable("gui.murimblock.profile.player");
        float health = player != null ? player.getHealth() : 20;
        float maxHealth = player != null ? player.getMaxHealth() : 20;
        Component none = Component.translatable("gui.murimblock.none");
        Cultivation.Snapshot c = ClientCultivation.get();
        int realm = c.realm();
        float regen = (float) (c.meditating() ? CultivationRules.QI_REGEN_MEDITATING : CultivationRules.QI_REGEN);
        // Progress through the realm: whole stages done plus the share of the current gauge.
        float stageShare = (float) Math.min(1, c.stageQi() / CultivationRules.gauge(realm, c.stage()));
        int percent = (int) (100 * (c.stage() - 1 + stageShare) / CultivationRules.STAGES);
        Component nextRealm = CultivationData.revealed(realm + 1) && realm + 1 < CultivationData.REALMS.size()
                ? CultivationData.REALMS.get(realm + 1).rankName() : Component.translatable("gui.murimblock.unknown");
        return new ProfileData(
                name,
                CultivationData.REALMS.get(realm).rankName(),
                none,
                Component.translatable("gui.murimblock.profile.unaffiliated"),
                MartialArtsData.active().name(),
                health,
                maxHealth,
                (int) c.qi(),
                c.maxQi(),
                regen,
                percent,
                nextRealm);
    }
}
