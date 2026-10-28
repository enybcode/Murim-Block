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
    private static final java.util.Set<String> MOVES = new java.util.LinkedHashSet<>();

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
                zombie(level, X - 1.8, Z + 2.6, z -> z.setBaby(true));
                zombie(level, X + 2.2, Z + 4, z -> {
                    z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                });
                player.connection.teleport(X, Y, Z, 0, 10);
            });
        }));
        // The client hears of the new zombies a few ticks after they are spawned.
        steps.add(new Step("mob looks arriving", mc -> {
        }));
        steps.add(new Step("disciple front", mc -> {
            Zombie zombie = nearest(mc);
            EngineTest.check("the Disciple's model is loaded", SkeletalModels.get(SkeletalMobs.DISCIPLE.model()) != null);
            EngineTest.check("zombies are drawn by the Shenfa mob renderer", zombie != null
                    && mc.getEntityRenderDispatcher().getRenderer(zombie) instanceof SkeletalMobRenderer<?>);
            EngineTest.check("and wear the Disciple's look", zombie != null && SkeletalMobs.look(zombie) != null);
            EngineTest.check("standing, it plays its slumped idle (" + clip(zombie) + ")", "disciple/idle".equals(clip(zombie)));
            EngineTest.shoot(mc, "mob_disciple_front");
            EngineTest.server(mc, player -> {
                player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new AABB(AREA - 20, Y - 5, -20, AREA + 20, Y + 10, 20)).forEach(Entity::discard);
                player.connection.teleport(X, Y, Z + 1.3, 0, 38);
            });
        }));
        steps.add(new Step("disciple cloth close", mc -> {
            EngineTest.shoot(mc, "mob_disciple_cloth_front");
            EngineTest.server(mc, player -> player.connection.teleport(X - 1.9, Y, Z + 3, -90, 32));
        }));
        steps.add(new Step("disciple cloth side", mc -> {
            EngineTest.shoot(mc, "mob_disciple_cloth_side");
            EngineTest.server(mc, player -> player.connection.teleport(X + 3.2, Y + 0.6, Z + 1, 58, 15));
        }));
        steps.add(new Step("disciple three quarters", mc -> {
            Zombie dressed = nearest(mc);
            for (String piece : new String[] {"panel_front", "panel_back", "rope_1", "rope_2"}) {
                org.joml.Vector3f hang = dressed == null ? null : com.murimblock.shenfa.client.MobCloth.hang(dressed, piece);
                EngineTest.check("its " + piece + " hangs down as cloth (" + hang + ", bottom "
                        + com.murimblock.shenfa.client.MobCloth.ahead(dressed, piece) + " px ahead)",
                        hang != null && -hang.y > 0.6f * hang.length());
            }
            EngineTest.shoot(mc, "mob_disciple_three_quarter");
            SkeletalMobs.moveProbe = (mob, move) -> MOVES.add(move);
            GifRecorder.start("mob_disciple_moves");
            EngineTest.server(mc, player -> {
                player.connection.teleport(X, Y, Z + 1.2, 0, 15);
                MOVES.clear();
                for (Zombie zombie : zombies(player.serverLevel())) {
                    zombie.setAggressive(true);
                    // The move the server would pick (DiscipleFighting): every client plays it.
                    com.murimblock.combat.CombatServer.anim(zombie, com.murimblock.shenfa.Animator.Priority.MIDDLE,
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("murimblock", "disciple/strike"), false);
                }
            });
        }));
        steps.add(new Step("disciple strikes", mc -> {
            Zombie zombie = nearest(mc);
            EngineTest.check("told by the server, it strikes (" + MOVES + ")", MOVES.contains("strike"));
            EngineTest.check("aggressive, it stands in its half guard (" + clip(zombie) + ")",
                    "disciple/stance".equals(clip(zombie)));
            EngineTest.shoot(mc, "mob_disciple_stance");
            EngineTest.server(mc, player -> zombies(player.serverLevel()).forEach(z -> z.hurt(
                    player.damageSources().playerAttack(player), 1)));
        }));
        steps.add(new Step("disciple flinches", mc -> {
            EngineTest.check("hurt, it flinches (" + MOVES + ")", MOVES.contains("flinch"));
            EngineTest.server(mc, player -> zombies(player.serverLevel()).forEach(z -> z.hurt(
                    player.damageSources().playerAttack(player), 1000)));
        }));
        steps.add(new Step("disciple dies", mc -> {
            EngineTest.check("killed, it falls by its death clip (" + MOVES + ")", MOVES.contains("death"));
            GifRecorder.stop();
            SkeletalMobs.moveProbe = null;
            EngineTest.server(mc, player -> {
                zombie(player.serverLevel(), X, Z + 3, null);
                player.connection.teleport(X, Y, Z, 0, 10);
            });
        }));
        steps.add(new Step("disciple walking", mc -> {
            // One walks up to the player, its cloth swinging (filmed for the GIFs).
            GifRecorder.start("mob_disciple_walking");
            EngineTest.server(mc, player -> {
                zombies(player.serverLevel()).forEach(Entity::discard);
                zombie(player.serverLevel(), X, Z + 9, z -> z.setNoAi(false));
                player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new AABB(AREA - 20, Y - 5, -20, AREA + 20, Y + 10, 20)).forEach(Entity::discard);
                player.connection.teleport(X + 2.5, Y + 0.4, Z + 1, 32, 12);
            });
        }));
        steps.add(new Step("disciple walked", mc -> {
            EngineTest.server(mc, player -> zombies(player.serverLevel()).forEach(z -> z.getNavigation().moveTo(X, Y, Z + 1, 1)));
        }));
        steps.add(new Step("disciple walked up", mc -> {
            GifRecorder.stop();
            EngineTest.shoot(mc, "mob_disciple_walking");
            // The zombie family in a row behind it (docs/mobs/zombie-family.png), the piglin with its golden sword.
            EngineTest.server(mc, player -> {
                ServerLevel level = player.serverLevel();
                zombie(level, EntityType.HUSK, X - 3.3, Z + 4.5, null);
                zombie(level, EntityType.DROWNED, X - 1.1, Z + 4.5, null);
                zombie(level, EntityType.ZOMBIE_VILLAGER, X + 1.1, Z + 4.5, null);
                zombie(level, EntityType.ZOMBIFIED_PIGLIN, X + 3.3, Z + 4.5,
                        z -> z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD)));
                player.connection.teleport(X, Y + 0.4, Z - 1, 0, 8);
            });
        }));
        steps.add(new Step("zombie family arriving", mc -> {
        }));
        steps.add(new Step("zombie family", mc -> {
            for (EntityType<?> type : new EntityType<?>[] {EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
                    EntityType.ZOMBIFIED_PIGLIN}) {
                Zombie zombie = mc.level.getEntitiesOfClass(Zombie.class, mc.player.getBoundingBox().inflate(12),
                        z -> z.getType() == type).stream().findFirst().orElse(null);
                var look = zombie == null ? null : SkeletalMobs.look(zombie);
                String name = EntityType.getKey(type).getPath();
                EngineTest.check(name + " wears its look (" + (look == null ? null : look.model()) + ")", look != null
                        && look != SkeletalMobs.DISCIPLE && SkeletalModels.get(look.model()) != null
                        && mc.getEntityRenderDispatcher().getRenderer(zombie) instanceof SkeletalMobRenderer<?>);
                EngineTest.check(name + " plays the disciple's idle (" + clip(zombie) + ")", "disciple/idle".equals(clip(zombie)));
            }
            EngineTest.shoot(mc, "mob_zombie_family");
            EngineTest.server(mc, player -> player.connection.teleport(X + 5.5, Y + 0.8, Z + 1, 50, 12));
        }));
        steps.add(new Step("zombie family three quarters", mc -> {
            EngineTest.shoot(mc, "mob_zombie_family_three_quarter");
            EngineTest.server(mc, player -> {
                zombies(player.serverLevel()).stream().filter(z -> z.getType() != EntityType.ZOMBIE).forEach(Entity::discard);
                player.connection.teleport(X + 2.5, Y + 0.4, Z + 1, 32, 12);
            });
            SkeletalMobs.enable(false);
        }));
        steps.add(new Step("vanilla zombie look", mc -> {
            Zombie zombie = nearest(mc);
            EngineTest.check("with our looks off the zombie is vanilla's again", zombie != null
                    && SkeletalMobs.look(zombie) == null);
            EngineTest.shoot(mc, "mob_vanilla_zombie");
            SkeletalMobs.enable(null);
            EngineTest.server(mc, player -> {
                zombies(player.serverLevel()).forEach(Entity::discard);
                // The Archer impérial: one standing, one aiming its bow.
                for (double x : new double[] {X - 1.4, X + 1.4}) {
                    var skeleton = EntityType.SKELETON.create(player.serverLevel());
                    skeleton.moveTo(x, Y, Z + 3.4, 180, 0);
                    skeleton.setYHeadRot(180);
                    skeleton.setYBodyRot(180);
                    skeleton.setNoAi(true);
                    skeleton.addTag(TAG);
                    skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                    skeleton.setAggressive(x > X);
                    player.serverLevel().addFreshEntity(skeleton);
                }
                player.connection.teleport(X, Y, Z, 0, 10);
            });
        }));
        steps.add(new Step("archer look", mc -> {
            var skeleton = mc.level.getEntitiesOfClass(net.minecraft.world.entity.monster.Skeleton.class,
                    mc.player.getBoundingBox().inflate(8)).stream().findFirst().orElse(null);
            EngineTest.check("the Archer's model is loaded", SkeletalModels.get(SkeletalMobs.ARCHER.model()) != null);
            EngineTest.check("skeletons wear the Archer's look", skeleton != null && SkeletalMobs.look(skeleton) != null
                    && mc.getEntityRenderDispatcher().getRenderer(skeleton) instanceof SkeletalMobRenderer<?>);
            EngineTest.shoot(mc, "mob_archer_front");
            EngineTest.server(mc, player -> player.connection.teleport(X + 3.2, Y + 0.6, Z + 1, 58, 15));
        }));
        steps.add(new Step("archer three quarters", mc -> {
            EngineTest.shoot(mc, "mob_archer_three_quarter");
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            EngineTest.server(mc, player -> {
                player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Skeleton.class,
                        new AABB(AREA - 20, Y - 5, -20, AREA + 20, Y + 10, 20)).forEach(Entity::discard);
                player.removeEffect(MobEffects.NIGHT_VISION);
                player.serverLevel().setDayTime(6000);
                player.server.setDifficulty(Difficulty.PEACEFUL, true);
            });
        }));
        return steps;
    }

    private static void zombie(ServerLevel level, double x, double z, java.util.function.Consumer<Zombie> dress) {
        zombie(level, EntityType.ZOMBIE, x, z, dress);
    }

    private static void zombie(ServerLevel level, EntityType<? extends Zombie> type, double x, double z,
            java.util.function.Consumer<Zombie> dress) {
        Zombie zombie = type.create(level);
        if (zombie == null) {
            return;
        }
        zombie.moveTo(x, Y, z, 180, 0);
        zombie.setYHeadRot(180);
        zombie.setYBodyRot(180);
        zombie.setNoAi(true);
        zombie.setPersistenceRequired();
        zombie.setCanPickUpLoot(false);
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

    private static String clip(Zombie zombie) {
        var animation = zombie == null ? null : SkeletalMobs.baseAnimation(zombie);
        return animation == null ? null : animation.id();
    }

    private static Zombie nearest(Minecraft mc) {
        // Plain zombies only: the test world keeps husks and drowned left by other checks.
        return mc.level.getEntitiesOfClass(Zombie.class, mc.player.getBoundingBox().inflate(12),
                        zombie -> zombie.getType() == EntityType.ZOMBIE).stream()
                .min(java.util.Comparator.comparingDouble(zombie -> zombie.distanceToSqr(mc.player))).orElse(null);
    }
}
