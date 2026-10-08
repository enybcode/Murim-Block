package com.murimblock.integration.epicfight.mixin;

import com.google.gson.JsonElement;
import com.murimblock.integration.epicfight.EpicFightContentPolicy;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RecipeManager.class)
public abstract class EpicFightRecipesMixin {
    @ModifyVariable(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Map<ResourceLocation, JsonElement> murimblock$filterRecipes(Map<ResourceLocation, JsonElement> recipes) {
        Map<ResourceLocation, JsonElement> kept = new LinkedHashMap<>(recipes);
        kept.entrySet().removeIf(entry -> EpicFightContentPolicy.blockedRecipe(entry.getKey(), entry.getValue()));
        return kept;
    }
}
