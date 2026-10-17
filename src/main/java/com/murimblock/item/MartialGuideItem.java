package com.murimblock.item;

import com.murimblock.Murimblock;
import com.murimblock.world.structure.StarPeakPlacement;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * A guide to a martial art, sold by the Golden Dragon in human form (mob/GoldenDragonShop): read, it opens as a book
 * that tells of the art, who keeps it and how it is won; and it writes in the chat the way to its keeper, the
 * direction and the distance to the nearest place of its kind (the Star-Plucking Peak's summit for the Seven Stars).
 * Unlike a manual, it teaches nothing and is never used up.
 */
public class MartialGuideItem extends Item {
    /** How many pages each guide's book has (guide.murimblock.<art>.page.<n>). */
    public static final int PAGES = 3;

    private final ResourceLocation art;
    /** The place it leads to; null: the Star-Plucking Peak's summit. */
    @Nullable private final ResourceKey<Structure> place;

    public MartialGuideItem(Properties properties, ResourceLocation art, @Nullable String place) {
        super(properties);
        this.art = art;
        this.place = place == null ? null : ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, place));
    }

    public ResourceLocation art() { return art; }

    private String key() {
        return "guide.murimblock." + art.getPath();
    }

    /** The book's pages, in order. */
    public List<Component> pages() {
        return java.util.stream.IntStream.range(0, PAGES).mapToObj(i -> (Component) Component.translatable(key() + ".page." + i)).toList();
    }

    /** What it leads to, for its messages. */
    public Component target() {
        return Component.translatable(key() + ".target");
    }

    /** Where it leads from here, or null when nothing is within reach (or outside the Overworld). */
    @Nullable public BlockPos lead(ServerLevel level, BlockPos from) {
        if (level.dimension() != Level.OVERWORLD) return null;
        if (place != null) return LocatorPendantItem.locate(level, place, from);
        return StarPeakPlacement.get(level).summit();
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.murimblock.martial_guide.hint").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            com.murimblock.client.GuideBookClient.open(pages());
            return InteractionResultHolder.success(stack);
        }
        if (level.dimension() != Level.OVERWORLD) {
            player.sendSystemMessage(Component.translatable("item.murimblock.martial_guide.other_world"));
        } else {
            BlockPos to = lead((ServerLevel) level, player.blockPosition());
            if (to == null) {
                player.sendSystemMessage(Component.translatable("item.murimblock.locator_pendant.none", target()));
            } else {
                double dx = to.getX() + 0.5 - player.getX(), dz = to.getZ() + 0.5 - player.getZ();
                int distance = (int) Math.round(Math.hypot(dx, dz));
                player.sendSystemMessage(distance < 32
                        ? Component.translatable("item.murimblock.locator_pendant.here", target())
                        : Component.translatable("item.murimblock.locator_pendant.direction", target(), distance,
                                Component.translatable("direction.murimblock." + SectTokenItem.compass(dx, dz))));
            }
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        // A search walks the world's structure grid: not on every click.
        player.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
