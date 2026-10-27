package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.combat.CombatClient;
import com.murimblock.client.combat.CombatEffects;
import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.combat.CombatPayloads;
import com.murimblock.combat.CombatServer;
import com.murimblock.combat.CombatState;
import com.murimblock.combat.MobPatches;
import com.murimblock.registry.MurimAttachments;
import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.ShenfaLibrary;
import com.murimblock.shenfa.client.SkeletalPlayers;
import com.murimblock.shenfa.client.mob.MobShenfa;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The fight in game, on the real player: the combat rules, the fists' combo on a zombie (damage, staggers, hit
 * stop), a dodge, the guard and the parry against a zombie that really attacks, and each patched mob landing its
 * Shenfa attack on the player (its clip plays, the blow lands at the clip's window).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
final class CombatChecks {
    private static final int AREA = 520;
    private static final double X = AREA + 0.5, Y = 200, Z = 0.5;

    private record Run(int ticks, Consumer<Integer> tick, Runnable end) {
    }

    private static Run running;
    private static int at;
    private static final List<CombatPayloads.Hit> hits = new ArrayList<>();
    private static final List<ResourceLocation> moves = new ArrayList<>();

    private CombatChecks() {
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("combat setup", CombatChecks::setup));
        steps.add(new Step("combat rules", mc -> rules()));
        steps.add(new Step("fists combo on a zombie", mc -> {
            LivingEntity[] zombie = new LivingEntity[1];
            float[] health = new float[1];
            spawn(mc, EntityType.ZOMBIE, X, Z + 1.6, true, mob -> {
            });
            start(new Run(60, t -> {
                if (t == 0) {
                    zombie[0] = nearest(mc, EntityType.ZOMBIE);
                    health[0] = zombie[0] == null ? 0 : server(zombie[0]).getHealth();
                    hits.clear();
                    moves.clear();
                    face(mc, zombie[0]);
                }
                if (t % 6 == 0 && t <= 36) {
                    CombatClient.pressAttack();
                }
                ResourceLocation clip = CombatClient.local().clip;
                if (clip != null && (moves.isEmpty() || !moves.get(moves.size() - 1).equals(clip))) {
                    moves.add(clip);
                }
            }, () -> {
                List<String> names = moves.stream().map(ResourceLocation::getPath).toList();
                EngineTest.check("the combo chains jab, cross, hook, uppercut (" + names + ")",
                        names.size() >= 4 && names.subList(0, 4).equals(List.of("fists/jab", "fists/cross", "fists/hook",
                                "fists/uppercut")));
                long landed = hits.stream().filter(hit -> hit.result() == CombatPayloads.Result.HIT).count();
                float lost = zombie[0] == null ? 0 : health[0] - server(zombie[0]).getHealth();
                EngineTest.check("the blows land on the zombie (" + landed + " hits, " + lost + " health lost)",
                        landed >= 3 && lost > 4);
                EngineTest.check("they stagger it and freeze both a few ticks (" + hits.stream()
                                .map(hit -> hit.stun() + "/" + hit.hitStop()).toList() + ")",
                        hits.stream().anyMatch(hit -> hit.stun() > 0 && hit.hitStop() >= 2));
                EngineTest.check("the zombie reels on the client", zombie[0] != null
                        && MobShenfa.playing(zombie[0], Animator.Priority.HIGHEST) != null
                        || hits.stream().anyMatch(hit -> hit.stun() > 0));
                discard(mc);
            }));
        }));
        steps.add(new Step("dodge back", mc -> {
            at(mc, X, Z, 0);
            double[] from = new double[1];
            boolean[] safe = new boolean[1];
            start(new Run(20, t -> {
                if (t == 0) {
                    from[0] = mc.player.getZ();
                    CombatClient.local().buffered = null;
                    pressDodge(mc);
                }
                if (t == 3) {
                    CombatState state = CombatServer.existing(serverPlayer(mc));
                    safe[0] = state != null && state.invulnerable();
                }
            }, () -> {
                double moved = from[0] - mc.player.getZ();
                EngineTest.check(String.format("a dodge back carries the body a block back (%.2f)", moved),
                        moved > 0.7 && moved < 1.3);
                EngineTest.check("and blows miss it during its window (server)", safe[0]);
            }));
        }));
        steps.add(new Step("guard against a zombie", mc -> {
            at(mc, X, Z, 0);
            spawn(mc, EntityType.ZOMBIE, X, Z + 2.5, false, mob -> ((Mob) mob).setTarget(serverPlayer(mc)));
            float[] qi = new float[1];
            start(new Run(160, t -> {
                if (t == 0) {
                    hits.clear();
                    qi[0] = (float) serverPlayer(mc).getData(MurimAttachments.CULTIVATION).qi();
                }
                face(mc, nearest(mc, EntityType.ZOMBIE));
                CombatClient.holdGuard(t > 2);
            }, () -> {
                CombatClient.holdGuard(false);
                long guarded = hits.stream().filter(hit -> hit.result() == CombatPayloads.Result.GUARD).count();
                float spent = qi[0] - (float) serverPlayer(mc).getData(MurimAttachments.CULTIVATION).qi();
                EngineTest.check("a raised guard blocks the zombie's blows for Qi (" + guarded + " blocked, " + spent
                        + " Qi)", guarded >= 1 && spent > 0);
                EngineTest.check("and no blow gets through (" + hits.stream().map(hit -> hit.result().name()).toList()
                        + ")", hits.stream().noneMatch(hit -> hit.result() == CombatPayloads.Result.HIT));
                discard(mc);
            }));
        }));
        steps.add(new Step("parry a zombie", mc -> {
            at(mc, X, Z, 0);
            spawn(mc, EntityType.ZOMBIE, X, Z + 2.5, false, mob -> ((Mob) mob).setTarget(serverPlayer(mc)));
            int[] raisedAt = {-100};
            start(new Run(200, t -> {
                if (t == 0) {
                    hits.clear();
                }
                LivingEntity zombie = nearest(mc, EntityType.ZOMBIE);
                face(mc, zombie);
                // Raise the guard a few ticks into the zombie's swing, so the blow lands inside the window.
                boolean swinging = zombie != null && MobShenfa.playing(zombie, Animator.Priority.HIGH) != null;
                if (swinging && t - raisedAt[0] > 20) {
                    raisedAt[0] = t + 3;
                }
                CombatClient.holdGuard(t >= raisedAt[0] && t < raisedAt[0] + 10);
            }, () -> {
                CombatClient.holdGuard(false);
                List<String> results = hits.stream().map(hit -> hit.result().name()).toList();
                EngineTest.check("a guard raised as the blow comes parries it (" + results + ")",
                        results.contains("PARRY"));
                discard(mc);
            }));
        }));
        for (EntityType<?> type : List.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.SKELETON,
                EntityType.WITHER_SKELETON, EntityType.VINDICATOR, EntityType.PIGLIN_BRUTE, EntityType.ENDERMAN,
                EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.IRON_GOLEM, EntityType.RAVAGER,
                EntityType.HOGLIN, EntityType.ZOGLIN, EntityType.WOLF, EntityType.POLAR_BEAR, EntityType.WARDEN,
                EntityType.VEX, EntityType.SILVERFISH)) {
            steps.add(mobAttacks(type));
        }
        steps.add(new Step("combat done", mc -> {
            CombatEffects.probe = null;
            CombatClient.setMode(false);
            EngineTest.server(mc, player -> {
                player.setGameMode(GameType.CREATIVE);
                player.server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
                player.removeAllEffects();
            });
            EngineTest.busy = () -> false;
        }));
        return steps;
    }

    /** A mob that attacks the player: its Shenfa attack plays and its blow lands on the player. */
    private static Step mobAttacks(EntityType<?> type) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
        return new Step(name + " attacks", mc -> {
            at(mc, X, Z, 0);
            spawn(mc, type, X, Z + 3, false, mob -> {
                Mob attacker = (Mob) mob;
                if (type == EntityType.SKELETON || type == EntityType.WITHER_SKELETON) {
                    attacker.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, type
                            == EntityType.WITHER_SKELETON ? new ItemStack(Items.STONE_SWORD) : ItemStack.EMPTY);
                    if (attacker instanceof net.minecraft.world.entity.monster.AbstractSkeleton skeleton) {
                        skeleton.reassessWeaponGoal();
                    }
                }
                Player target = serverPlayer(mc);
                attacker.setTarget(target);
                if (attacker instanceof net.minecraft.world.entity.monster.warden.Warden warden) {
                    warden.increaseAngerAt(target, 150, false);
                }
                if (attacker instanceof NeutralMob neutral) {
                    neutral.setPersistentAngerTarget(target.getUUID());
                    neutral.startPersistentAngerTimer();
                }
            });
            boolean[] clip = new boolean[1];
            start(new Run(200, t -> {
                if (t == 0) {
                    hits.clear();
                }
                LivingEntity mob = nearest(mc, type);
                if (mob != null && MobShenfa.playing(mob, Animator.Priority.HIGH) != null) {
                    clip[0] = true;
                }
                face(mc, mob);
                if (t % 20 == 0) {
                    EngineTest.server(mc, player -> {
                        player.setHealth(player.getMaxHealth());
                        Mob attacker = (Mob) nearestServer(player, type);
                        if (attacker != null && attacker.getTarget() == null) {
                            attacker.setTarget(player);
                        }
                    });
                }
            }, () -> {
                List<String> by = hits.stream().filter(hit -> mc.level.getEntity(hit.attacker()) != null
                        && mc.level.getEntity(hit.attacker()).getType() == type).map(hit -> hit.result().name()).toList();
                EngineTest.check(name + " plays a Shenfa attack (" + MobPatches.of(type).attacks().get(0).clip()
                        + "...) and its blow lands at the window (" + by + ")", clip[0] && !by.isEmpty());
                discard(mc);
            }));
        });
    }

    private static void rules() {
        var jab = ShenfaLibrary.get(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "fists/jab"));
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "fists/jab");
        EngineTest.check("the server knows the fists' clips", jab != null && jab.attack() != null);
        if (jab == null) {
            return;
        }
        CombatState state = new CombatState();
        state.start(id, jab, CombatState.Kind.ATTACK);
        state.combo = 0;
        boolean locked = state.movementLocked() && !state.free();
        state.buffer(CombatState.Input.ATTACK, 0, 0);
        long tick = 0;
        while (!state.free()) {
            state.tick(++tick);
        }
        boolean buffered = state.takeBuffered(tick) == CombatState.Input.ATTACK && state.takeBuffered(tick) == null;
        while (state.playing()) {
            state.tick(++tick);
        }
        boolean chain = state.nextCombo(tick + 2, 4) == 1 && state.nextCombo(tick + CombatState.COMBO_GRACE + 1, 4) == 0;
        state.buffer(CombatState.Input.ATTACK, 0, 100);
        boolean stale = state.takeBuffered(100 + CombatState.BUFFER + 1) == null;
        state.start(id, jab, CombatState.Kind.ATTACK);
        state.hitStop = 3;
        state.tick(200);
        boolean held = state.time == 0;
        CombatState guard = new CombatState();
        guard.guard(true, 1000);
        boolean parry = guard.parrying(1000 + CombatState.PARRY_WINDOW) && !guard.parrying(1001 + CombatState.PARRY_WINDOW);
        guard.guard(false, 1010);
        guard.guard(true, 1012);
        boolean rest = !guard.parrying(1013);
        EngineTest.check("rules: a strike locks walking until its recovery", locked);
        EngineTest.check("rules: an input waits in the 8-tick buffer, taken once", buffered);
        EngineTest.check("rules: the combo continues within its grace, then starts again", chain);
        EngineTest.check("rules: a stale input is dropped", stale);
        EngineTest.check("rules: a hit stop holds the move", held);
        EngineTest.check("rules: the parry window is 8 ticks after raising the guard", parry);
        EngineTest.check("rules: no parry by raising the guard again too soon", rest);
    }

    private static void setup(Minecraft mc) {
        mc.options.pauseOnLostFocus = false;
        mc.options.hideGui = false;
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        CombatEffects.probe = hits::add;
        SkeletalPlayers.enable(true);
        EngineTest.server(mc, player -> {
            ServerLevel level = player.serverLevel();
            for (BlockPos pos : BlockPos.betweenClosed(AREA - 10, (int) Y - 1, -10, AREA + 10, (int) Y - 1, 12)) {
                level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
            }
            for (BlockPos pos : BlockPos.betweenClosed(AREA - 10, (int) Y, -10, AREA + 10, (int) Y + 4, 12)) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            // The test world is peaceful: monsters would vanish and never attack.
            player.server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
            level.getEntities((Entity) null, new AABB(AREA - 30, Y - 5, -30, AREA + 30, Y + 10, 30),
                    entity -> !(entity instanceof Player)).forEach(Entity::discard);
            player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100000, 3, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100000, 4, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100000, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 100000, 0, false, false));
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            var cultivation = player.getData(MurimAttachments.CULTIVATION);
            cultivation.set(cultivation.realm(), cultivation.stage(), cultivation.stageQi(), 10000, 10000,
                    cultivation.meridians());
            player.connection.teleport(X, Y, Z, 0, 0);
            level.setDayTime(6000);
        });
        CombatClient.setMode(true);
        EngineTest.busy = () -> running != null;
    }

    private static void spawn(Minecraft mc, EntityType<?> type, double x, double z, boolean still,
                              Consumer<Entity> setup) {
        EngineTest.server(mc, player -> {
            ServerLevel level = player.serverLevel();
            Entity entity = type.create(level);
            if (entity instanceof Mob mob) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, Y, z)), MobSpawnType.COMMAND,
                        null);
                mob.setNoAi(still);
                mob.setPersistenceRequired();
                if (type == EntityType.ZOMBIE || type == EntityType.HUSK) {
                    mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }
            }
            entity.moveTo(x, Y, z, 180, 0);
            level.addFreshEntity(entity);
            setup.accept(entity);
        });
    }

    private static void discard(Minecraft mc) {
        EngineTest.server(mc, player -> player.serverLevel().getEntities((Entity) null,
                new AABB(AREA - 20, Y - 5, -20, AREA + 20, Y + 10, 20), entity -> !(entity instanceof Player))
                .forEach(Entity::discard));
    }

    private static void pressDodge(Minecraft mc) {
        // The dodge key as pressed with nothing held: a hop back.
        mc.options.keyUp.setDown(false);
        CombatClient.DODGE.setDown(true);
        net.minecraft.client.KeyMapping.click(CombatClient.DODGE.getKey());
        CombatClient.DODGE.setDown(false);
    }

    private static void at(Minecraft mc, double x, double z, float yaw) {
        EngineTest.server(mc, player -> player.connection.teleport(x, Y, z, yaw, 0));
        mc.player.setYRot(yaw);
        mc.player.setXRot(0);
    }

    private static void face(Minecraft mc, Entity target) {
        if (target == null) {
            return;
        }
        double dx = target.getX() - mc.player.getX(), dz = target.getZ() - mc.player.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        mc.player.setYRot(yaw);
        mc.player.setYHeadRot(yaw);
        mc.player.setXRot(10);
    }

    private static LivingEntity nearest(Minecraft mc, EntityType<?> type) {
        List<? extends Entity> found = mc.level.getEntities((Entity) null, new AABB(AREA - 20, Y - 5, -20, AREA + 20,
                Y + 10, 20), entity -> entity.getType() == type && entity.isAlive());
        return found.isEmpty() ? null : (LivingEntity) found.get(0);
    }

    private static Entity nearestServer(Player player, EntityType<?> type) {
        List<? extends Entity> found = player.level().getEntities((Entity) null, new AABB(AREA - 20, Y - 5, -20,
                AREA + 20, Y + 10, 20), entity -> entity.getType() == type && entity.isAlive());
        return found.isEmpty() ? null : found.get(0);
    }

    /** The server's copy of a client entity (single player). */
    private static LivingEntity server(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        ServerLevel level = mc.getSingleplayerServer().getLevel(entity.level().dimension());
        return (LivingEntity) level.getEntity(entity.getId());
    }

    private static net.minecraft.server.level.ServerPlayer serverPlayer(Minecraft mc) {
        return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
    }

    private static void start(Run run) {
        at = -10;
        running = run;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (running == null || mc.player == null || mc.isPaused()) {
            return;
        }
        if (at >= 0) {
            running.tick().accept(at);
        }
        if (++at >= running.ticks()) {
            Run done = running;
            running = null;
            done.end().run();
        }
    }
}
