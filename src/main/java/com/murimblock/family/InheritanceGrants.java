package com.murimblock.family;

import com.murimblock.Murimblock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Grants a house's {@link Inheritance} to a player and takes its bonuses back when the house changes. Stats are
 * permanent attribute modifiers named {@code murimblock:family}, one per attribute, removed and put back on every
 * change; items are given once per house; the art goes to {@link #teachArt} (no martial art system on the server yet).
 */
public final class InheritanceGrants {
    public static final ResourceLocation MODIFIER = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "family");

    /** Teaches the art to the player: logs it until the art system exists on the server. */
    public static BiConsumer<ServerPlayer, String> teachArt = (player, art) ->
            Murimblock.LOGGER.info("Family: {} would learn the art {} (the art system is not built yet)",
                    player.getGameProfile().getName(), art);

    private InheritanceGrants() {
    }

    /** Puts the player's house's inheritance in force: old bonuses off, new ones on, items and art once. */
    public static void apply(ServerPlayer player, FamilyState state) {
        removeStats(player, state);
        Family house = state.family();
        if (house == null) {
            return;
        }
        Inheritance inheritance = Inheritance.of(house);
        if (inheritance.comingSoon()) {
            return;
        }
        List<String> applied = new ArrayList<>();
        for (Inheritance.Stat stat : inheritance.stats()) {
            AttributeInstance instance = attribute(player, stat.attribute());
            if (instance == null) {
                Murimblock.LOGGER.warn("Family: no attribute {}", stat.attribute());
                continue;
            }
            instance.removeModifier(MODIFIER);
            instance.addPermanentModifier(new AttributeModifier(MODIFIER, stat.amount(), stat.multiply()
                    ? AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL : AttributeModifier.Operation.ADD_VALUE));
            applied.add(stat.attribute());
        }
        state.statsApplied(applied);
        if (!house.id().equals(state.giftsFor())) {
            for (Inheritance.Gift gift : inheritance.items()) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(gift.item()));
                ItemStack stack = new ItemStack(item, gift.count());
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
            if (inheritance.art() != null) {
                teachArt.accept(player, inheritance.art());
            }
            state.giftsGiven(house.id());
        }
    }

    /** Takes back every attribute bonus the house gave. */
    public static void removeStats(ServerPlayer player, FamilyState state) {
        for (String id : state.statsApplied()) {
            AttributeInstance instance = attribute(player, id);
            if (instance != null) {
                instance.removeModifier(MODIFIER);
            }
        }
        state.statsApplied(List.of());
    }

    private static AttributeInstance attribute(ServerPlayer player, String id) {
        Holder<Attribute> holder = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(id)).orElse(null);
        return holder == null ? null : player.getAttribute(holder);
    }
}
