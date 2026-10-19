package com.murimblock.world.plant;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

/**
 * A harvested herb (docs/cultivation/plants.md): its maturity (young, mature, spirit maturity) and its purity, kept on
 * the stack (custom data), shown under its name. What the alchemy cauldron will work with.
 */
public class HerbItem extends Item {
    private final PlantCatalog.Species species;

    public HerbItem(PlantCatalog.Species species, Properties properties) {
        super(properties);
        this.species = species;
    }

    public PlantCatalog.Species species() { return species; }

    /** A stack of this herb with its maturity ("young", "mature", "spirit") and purity (0 to 100). */
    public static ItemStack of(Item herb, int count, String maturity, int purity) {
        ItemStack stack = new ItemStack(herb, count);
        CompoundTag tag = new CompoundTag();
        tag.putString("maturity", maturity);
        tag.putInt("purity", purity);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static String maturity(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null || !data.contains("maturity") ? "mature" : data.copyTag().getString("maturity");
    }

    public static int purity(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null || !data.contains("purity") ? 60 : data.copyTag().getInt("purity");
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String maturity = maturity(stack);
        tooltip.add(Component.translatable("tooltip.murimblock.herb.maturity." + maturity)
                .withStyle(maturity.equals("spirit") ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.murimblock.herb.purity", purity(stack)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.murimblock.herb.rarity." + species.rarity().name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override public boolean isFoil(ItemStack stack) {
        return maturity(stack).equals("spirit");
    }
}
