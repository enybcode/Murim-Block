package com.murimblock.client.dev;

import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.shenfa.client.SkeletalMobRenderer;
import com.murimblock.shenfa.client.SkeletalMobs;
import com.murimblock.shenfa.client.SkeletalModels;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * The mobs' looks in game: the zombie drawn as the Disciple déchu (model, glowing eyes), one wearing a helmet and
 * holding a sword, one reaching for the player and swinging; then vanilla's zombie again with our looks off. At night
 * (the zombies would burn by day), the player seeing in the dark.
 */
final class MobLookChecks {
    private static final int AREA = 680;
    private static final double X = AREA + 0.5, Y = 200, Z = 0.5;
    private static final String TAG = "murimblock_look_check";

    private MobLookChecks() {
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("mob looks setup", mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            SkeletalMobs.enable(true);
            EngineTest.server(mc, player -> {
                ServerLevel level = player.serverLevel();
                player.server.setDifficulty(Difficulty.NORMAL, true);
                level.setDayTime(18000);
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 60, 0, false, false));
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 6, (int) Y - 1, -6, AREA + 6, (int) Y - 1, 12)) {
                    level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
                }
                zombie(level, X, Z + 3, null);
                zombie(level, X + 2.2, Z + 4, z -> {
                    z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                });
                player.connection.teleport(X, Y, Z, 0, 10);
            });
        }));
        steps.add(new Step("disciple front", mc -> {
            Zombie zombie = nearest(mc);
            EngineTest.check("the Disciple's model is loaded", SkeletalModels.get(SkeletalMobs.DISCIPLE.model()) != null);
            EngineTest.check("zombies are drawn by the Shenfa mob renderer", zombie != null
                    && mc.getEntityRenderDispatcher().getRenderer(zombie) instanceof SkeletalMobRenderer<?>);
            EngineTest.check("and wear the Disciple's look", zombie != null && SkeletalMobs.look(zombie) != null);
            EngineTest.shoot(mc, "mob_disciple_front");
            EngineTest.server(mc, player -> player.connection.teleport(X + 3.2, Y + 0.6, Z + 1, 58, 15));
        }));
        steps.add(new Step("disciple three quarters", mc -> {
            EngineTest.shoot(mc, "mob_disciple_three_quarter");
            EngineTest.server(mc, player -> {
                player.connection.teleport(X, Y, Z + 1.2, 0, 15);
                for (Zombie zombie : zombies(player.serverLevel())) {
                    zombie.setAggressive(true);
                    zombie.swing(InteractionHand.MAIN_HAND);
                }
            });
        }));
        steps.add(new Step("disciple reaching", mc -> {
            Zombie zombie = nearest(mc);
            EngineTest.check("an aggressive zombie shows it (arms raised)", zombie != null && zombie.isAggressive());
            EngineTest.shoot(mc, "mob_disciple_reaching");
            SkeletalMobs.enable(false);
        }));
        steps.add(new Step("vanilla zombie look", mc -> {
            Zombie zombie = nearest(mc);
            EngineTest.check("with our looks off the zombie is vanilla's again", zombie != null
                    && SkeletalMobs.look(zombie) == null);
            EngineTest.shoot(mc, "mob_vanilla_zombie");
            SkeletalMobs.enable(null);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            EngineTest.server(mc, player -> {
                zombies(player.serverLevel()).forEach(Entity::discard);
                player.removeEffect(MobEffects.NIGHT_VISION);
                player.serverLevel().setDayTime(6000);
                player.server.setDifficulty(Difficulty.PEACEFUL, true);
            });
        }));
        return steps;
    }

    private static void zombie(ServerLevel level, double x, double z, java.util.function.Consumer<Zombie> dress) {
        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            return;
        }
        zombie.moveTo(x, Y, z, 180, 0);
        zombie.setYHeadRot(180);
        zombie.setYBodyRot(180);
        zombie.setNoAi(true);
        zombie.setPersistenceRequired();
        zombie.addTag(TAG);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            zombie.setItemSlot(slot, ItemStack.EMPTY);
        }
        if (dress != null) {
            dress.accept(zombie);
        }
        level.addFreshEntity(zombie);
    }

    private static List<Zombie> zombies(ServerLevel level) {
        return level.getEntitiesOfClass(Zombie.class, new AABB(AREA - 20, Y - 5, -20, AREA + 20, Y + 10, 20),
                zombie -> zombie.getTags().contains(TAG));
    }

    private static Zombie nearest(Minecraft mc) {
        return mc.level.getEntitiesOfClass(Zombie.class, mc.player.getBoundingBox().inflate(12)).stream()
                .min(java.util.Comparator.comparingDouble(zombie -> zombie.distanceToSqr(mc.player))).orElse(null);
    }
}
