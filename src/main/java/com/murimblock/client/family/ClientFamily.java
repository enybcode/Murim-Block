package com.murimblock.client.family;

import com.murimblock.Murimblock;
import com.murimblock.family.Family;
import com.murimblock.family.FamilyEvents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** The local player's house, as the server sent it, and the Destiny window when the server asks for it. */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class ClientFamily {
    private static Family family;
    private static boolean freeReroll;
    /** The server asked for the Destiny window: opened as soon as no other screen is up (the world may still load). */
    private static boolean pendingOpen;
    /** Increases with each draw received, so an open window replays its spin. */
    private static int draws;
    /** The dev runs that play the game by script: the window would pause them at the first join. */
    private static final boolean SCRIPTED = Boolean.getBoolean("murimblock.engineTest")
            || Boolean.getBoolean("murimblock.mapTour") || Boolean.getBoolean("murimblock.innerWorldTour")
            || Boolean.getBoolean("murimblock.riseFilm");

    private ClientFamily() {
    }

    public static void receive(String id, boolean reroll, boolean open) {
        Family before = family;
        family = Family.byId(id);
        freeReroll = reroll;
        if (open) {
            pendingOpen = true;
            if (family != before || Minecraft.getInstance().screen instanceof DestinyScreen) {
                draws++;
            }
        } else if (Minecraft.getInstance().screen instanceof DestinyScreen screen) {
            screen.onClose();
        }
    }

    /** The house, or null before the server has drawn it. */
    public static Family family() {
        return family;
    }

    public static boolean freeReroll() {
        return freeReroll;
    }

    static int draws() {
        return draws;
    }

    /** Keep the house, or spend the free reroll. */
    static void choose(boolean reroll) {
        PacketDistributor.sendToServer(new FamilyEvents.Choice(reroll));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (SCRIPTED) {
            pendingOpen = false;
        }
        if (pendingOpen && mc.player != null && (mc.screen == null || mc.screen instanceof DestinyScreen)) {
            pendingOpen = false;
            if (!(mc.screen instanceof DestinyScreen)) {
                mc.setScreen(new DestinyScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        family = null;
        freeReroll = false;
        pendingOpen = false;
    }

    /** What hovering the house shows: its name, rarity, seat and inheritance (every inheritance is still coming soon). */
    public static List<Component> tooltip(Family house) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(house.nameKey()).withStyle(s -> s.withColor(house.rarity.text).withBold(true)));
        lines.add(Component.translatable("family.murimblock.tooltip.rarity", Component.translatable(house.rarity.labelKey()))
                .withStyle(s -> s.withColor(house.rarity.line)));
        if (house.seat != null) {
            lines.add(Component.translatable("family.murimblock.tooltip.seat",
                    Component.translatable("family.murimblock.seat." + house.seat)).withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable(house.rarity.inheritanceKey()).copy().withStyle(ChatFormatting.AQUA));
        if (house.great()) {
            lines.add(Component.translatable("family.murimblock.coming_soon").withStyle(ChatFormatting.YELLOW));
        }
        return lines;
    }
}
