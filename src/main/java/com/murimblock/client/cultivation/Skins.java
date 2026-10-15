package com.murimblock.client.cultivation;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;

/** A player's skin by their id, for what is drawn in their image (their body in meditation, their Heart Demon). */
public final class Skins {
    private Skins() {
    }

    public static PlayerSkin of(@Nullable UUID player) {
        if (player == null) return DefaultPlayerSkin.get(new UUID(0L, 0L));
        var connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(player);
        return info != null ? info.getSkin() : DefaultPlayerSkin.get(player);
    }
}
