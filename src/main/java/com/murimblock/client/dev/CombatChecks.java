package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.combat.CombatClient;
import com.murimblock.client.combat.CombatEffects;
import com.murimblock.client.combat.LockOn;
import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.combat.CombatPayloads;
import com.murimblock.combat.CombatServer;
import com.murimblock.combat.ArcherFighting;
import com.murimblock.combat.DiscipleFighting;
import com.murimblock.combat.SpiderFighting;
import com.murimblock.combat.CombatState;
import com.murimblock.registry.MurimAttachments;
import com.murimblock.shenfa.ShenfaLibrary;
import com.murimblock.shenfa.client.SkeletalPlayers;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
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
            start(new Run(200, t -> {
                if (t == 0) {
                    hits.clear();
                }
                LivingEntity zombie = nearest(mc, EntityType.ZOMBIE);
                face(mc, zombie);
                // Vanilla's zombie strikes on contact, once a second: the guard goes down long enough for a new parry
                // window, then up, so some blow lands inside a window.
                CombatClient.holdGuard(t % 20 >= 11);
            }, () -> {
                CombatClient.holdGuard(false);
                List<String> results = hits.stream().map(hit -> hit.result().name()).toList();
                EngineTest.check("a guard raised as the blow comes parries it (" + results + ")",
                        results.contains("PARRY"));
                discard(mc);
            }));
        }));
        steps.add(new Step("lock-on through a combo", mc -> {
            at(mc, X, Z, 0);
            spawn(mc, EntityType.ZOMBIE, X, Z + 1.6, true, mob -> {
            });
            spawn(mc, EntityType.ZOMBIE, X + 4, Z + 3, true, mob -> {
            });
            LivingEntity[] locked = new LivingEntity[1];
            boolean[] kept = {true};
            boolean[] nudge = new boolean[1];
            start(new Run(90, t -> {
                if (t == 0) {
                    face(mc, nearest(mc, EntityType.ZOMBIE));
                    LockOn.toggle();
                    locked[0] = LockOn.target();
                }
                if (t % 6 == 1 && t <= 37) {
                    CombatClient.pressAttack();
                }
                if (t <= 60 && LockOn.target() != locked[0]) {
                    kept[0] = false;
                }
                if (t == 62) {
                    LockOn.mouse(3);
                    nudge[0] = LockOn.target() == locked[0];
                }
                if (t == 66) {
                    LockOn.mouse(15);
                }
                if (t == 78 && LockOn.target() == locked[0]) {
                    LockOn.mouse(-15);
                }
            }, () -> {
                EngineTest.check("the lock holds its foe through the combo and the uppercut's launch",
                        locked[0] != null && kept[0]);
                EngineTest.check("a small mouse move keeps the foe", nudge[0]);
                EngineTest.check("a flick of the mouse switches foe", LockOn.target() != null
                        && LockOn.target() != locked[0]);
                if (LockOn.target() != null) {
                    LockOn.toggle();
                }
                discard(mc);
            }));
        }));
        // The disciple's own fighting (DiscipleFighting): a move every two seconds or so, its blow landing at the
        // clip's impact, the double blow once its blows are blocked, the lunge when its prey backs away, only plain
        // strikes in a crowd.
        steps.add(new Step("disciple strikes on its impact", mc -> {
            at(mc, X, Z, 0);
            fighters(mc, 1, 2.0);
            start(new Run(150, t -> {
                if (t == 0) {
                    MOVES.clear();
                    WOUNDS.clear();
                    DiscipleFighting.probe = (mob, move) -> MOVES.add(new long[] {move.ordinal(), mob.level().getGameTime()});
                }
                face(mc, nearest(mc, EntityType.ZOMBIE));
                wounds(mc);
            }, () -> {
                LivingEntity zombie = nearest(mc, EntityType.ZOMBIE);
                Murimblock.LOGGER.info("Engine test disciple: moves {}, wounds {}, distance {}, mode {}", MOVES.stream()
                        .map(m -> m[0] + "@" + m[1]).toList(), WOUNDS, zombie == null ? -1 : zombie.distanceTo(mc.player),
                        serverPlayer(mc).gameMode.getGameModeForPlayer());
                List<String> names = moveNames();
                EngineTest.check("the disciple attacks every two seconds or so (" + names + ")",
                        names.size() >= 3 && names.size() <= 6);
                long first = MOVES.isEmpty() ? -1 : MOVES.get(0)[1];
                long wound = WOUNDS.stream().filter(at -> at >= first).findFirst().orElse(-100L);
                EngineTest.check("its blow lands at the clip's impact, not when it starts (" + (wound - first)
                        + " ticks after)", first >= 0 && wound - first >= 3 && wound - first <= 12);
                discard(mc);
            }));
        }));
        steps.add(new Step("disciple doubles on a guard", mc -> {
            at(mc, X, Z, 0);
            fighters(mc, 1, 2.0);
            start(new Run(260, t -> {
                if (t == 0) {
                    MOVES.clear();
                }
                face(mc, nearest(mc, EntityType.ZOMBIE));
                CombatClient.holdGuard(t > 2);
            }, () -> {
                CombatClient.holdGuard(false);
                List<String> names = moveNames();
                EngineTest.check("blocked, it switches to its double blow (" + names + ")", names.contains("DOUBLE"));
                discard(mc);
            }));
        }));
        steps.add(new Step("disciple lunges at a retreat", mc -> {
            at(mc, X, Z + 8, 0);
            fighters(mc, 1, 3.0, Z + 8);
            double[] lunge = {Double.NaN, 0};
            start(new Run(220, t -> {
                if (t == 0) {
                    MOVES.clear();
                }
                wounds(mc);
                LivingEntity zombie = nearest(mc, EntityType.ZOMBIE);
                face(mc, zombie);
                // The player backs away in bursts (two blocks in twelve ticks, a sprint), then stands, four times.
                if (t % 50 >= 10 && t % 50 < 22 && t < 200 && t % 3 == 0) {
                    EngineTest.server(mc, player -> player.connection.teleport(player.getX(), player.getY(),
                            player.getZ() - 0.5, player.getYRot(), player.getXRot()));
                }
                List<String> names = moveNames();
                if (zombie != null && names.contains("LUNGE")) {
                    LivingEntity body = server(zombie);
                    if (Double.isNaN(lunge[0])) {
                        lunge[0] = body.getZ();
                    }
                    lunge[1] = Math.max(lunge[1], lunge[0] - body.getZ());
                }
            }, () -> {
                List<String> names = moveNames();
                EngineTest.check("its prey backing away, it lunges (" + names + ")", names.contains("LUNGE"));
                EngineTest.check(String.format("and the lunge carries it forward (%.2f blocks)", lunge[1]),
                        lunge[1] > 1.2);
                discard(mc);
            }));
        }));
        steps.add(new Step("disciples in a crowd only strike", mc -> {
            at(mc, X, Z, 0);
            fighters(mc, 3, 2.0);
            start(new Run(200, t -> {
                if (t == 0) {
                    MOVES.clear();
                }
                face(mc, nearest(mc, EntityType.ZOMBIE));
                wounds(mc);
            }, () -> {
                List<String> names = moveNames();
                EngineTest.check("three around the player throw only plain strikes (" + names + ")",
                        !names.isEmpty() && names.stream().allMatch("STRIKE"::equals));
                DiscipleFighting.probe = null;
                discard(mc);
            }));
        }));
        // Armed, it fights its weapon's way: a disciple with a sword, an axe, then a shovel against a player who
        // guards, then backs away once (filmed from the side for the GIFs).
        for (var item : List.of(Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_SHOVEL)) {
            String weapon = DiscipleFighting.Weapon.of(new ItemStack(item)).name().toLowerCase(java.util.Locale.ROOT);
            steps.add(new Step("disciple with a " + weapon, mc -> {
                at(mc, X, Z + 6, 0);
                fighters(mc, 1, 2.6, Z + 6);
                EngineTest.server(mc, player -> player.serverLevel().getEntitiesOfClass(Mob.class,
                        player.getBoundingBox().inflate(6), mob -> mob.getType() == EntityType.ZOMBIE)
                        .forEach(mob -> mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(item))));
                List<String> played = new java.util.concurrent.CopyOnWriteArrayList<>();
                start(new Run(220, t -> {
                    if (t == 0) {
                        com.murimblock.shenfa.client.SkeletalMobs.moveProbe = (mob, move) -> played.add(move);
                        GifRecorder.start("disciple_" + weapon);
                        GifRecorder.camera(GifRecorder.side(mc.player));
                    }
                    face(mc, nearest(mc, EntityType.ZOMBIE));
                    wounds(mc);
                    CombatClient.holdGuard(t > 2 && t < 140);
                    if (t >= 140 && t < 152 && t % 3 == 0) {
                        EngineTest.server(mc, player -> player.connection.teleport(player.getX(), player.getY(),
                                player.getZ() - 0.5, player.getYRot(), player.getXRot()));
                    }
                }, () -> {
                    CombatClient.holdGuard(false);
                    com.murimblock.shenfa.client.SkeletalMobs.moveProbe = null;
                    EngineTest.check("a disciple with a " + weapon + " fights its way (" + played + ")",
                            played.stream().anyMatch(move -> move.startsWith(weapon + "_")));
                    discard(mc);
                }), false);
            }));
        }
        // Each weapon's moves one after the other on a disciple standing in its guard (the axe one left-handed: the
        // item must still be in its right hand), filmed from the side, then from the front three quarters.
        for (var item : List.of(Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_SHOVEL)) {
            DiscipleFighting.Weapon kind = DiscipleFighting.Weapon.of(new ItemStack(item));
            String weapon = kind.name().toLowerCase(java.util.Locale.ROOT);
            for (String view : List.of("side", "front")) {
                steps.add(new Step("disciple " + weapon + " moves, " + view, mc -> {
                    // At night, the player seeing in the dark: a bareheaded zombie would burn by day.
                    EngineTest.server(mc, player -> {
                        player.serverLevel().setDayTime(18000);
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.NIGHT_VISION, 400, 0, false, false));
                    });
                    at(mc, X, Z, 0);
                    bareheaded = true;
                    spawn(mc, EntityType.ZOMBIE, X, Z + 2.6, true, mob -> {
                        Mob zombie = (Mob) mob;
                        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(item));
                        zombie.setLeftHanded(item == Items.IRON_AXE);
                        zombie.setAggressive(true);
                    });
                    bareheaded = false;
                    List<String> played = new java.util.concurrent.CopyOnWriteArrayList<>();
                    String[] moves = kind.clips();
                    start(new Run(190, t -> {
                        LivingEntity zombie = nearest(mc, EntityType.ZOMBIE);
                        if (t == 0) {
                            com.murimblock.shenfa.client.SkeletalMobs.moveProbe = (mob, move) -> played.add(move);
                            mc.options.hideGui = true;
                            GifRecorder.start("disciple_" + weapon + "_" + view);
                        }
                        if (t == 2) {
                            // Facing the player (a zombie without AI keeps its body turned the way it spawned).
                            EngineTest.server(mc, player -> player.serverLevel().getEntitiesOfClass(Mob.class,
                                    player.getBoundingBox().inflate(6), m -> m.getType() == EntityType.ZOMBIE).forEach(m -> {
                                        m.setYRot(180);
                                        m.setYBodyRot(180);
                                        m.setYHeadRot(180);
                                    }));
                        }
                        // The camera once the client sees the zombie (it arrives a few ticks after it is spawned).
                        if (t < 15 && t % 3 == 0) {
                            if (zombie != null) {
                                Vec3 at = zombie.position();
                                GifRecorder.camera(view.equals("side")
                                        ? partial -> new GifRecorder.View(at.add(3.6, 1.1, -1.3), at.add(0, 0.9, -1.3))
                                        : partial -> new GifRecorder.View(at.add(2.7, 1.35, -1.4), at.add(0, 0.95, 0)));
                            }
                        }
                        mc.player.setYRot(0);
                        for (int i = 0; i < moves.length; i++) {
                            if (t == 20 + i * 55 && zombie != null) {
                                String clip = moves[i];
                                EngineTest.server(mc, player -> CombatServer.anim(server(zombie),
                                        com.murimblock.shenfa.Animator.Priority.MIDDLE,
                                        ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "disciple/" + clip), false));
                            }
                        }
                    }, () -> {
                        EngineTest.server(mc, player -> player.serverLevel().setDayTime(6000));
                        mc.options.hideGui = false;
                        com.murimblock.shenfa.client.SkeletalMobs.moveProbe = null;
                        EngineTest.check("the " + weapon + " disciple plays its three moves (" + played + ")",
                                played.containsAll(List.of(moves)));
                        LivingEntity zombie = nearest(mc, EntityType.ZOMBIE);
                        if (zombie instanceof Mob mob && item == Items.IRON_AXE) {
                            EngineTest.check("a left-handed disciple still holds its axe in the right hand",
                                    mob.isLeftHanded() && !mob.getMainHandItem().isEmpty());
                        }
                        discard(mc);
                    }), false);
                }));
            }
        }
        // The Archer impérial (ArcherFighting): an aimed shot at range, the retreat when its prey comes close, the
        // bash when touching, the volley over a wall. At night, filmed from the side.
        record ArcherScene(String name, double distance, boolean wall, String expect) {
        }
        for (ArcherScene scene : List.of(new ArcherScene("shot", 10, false, "SHOT"),
                new ArcherScene("retreat", 3.5, false, "retreat"), new ArcherScene("bash", 1.6, false, "BASH"),
                new ArcherScene("volley", 9, true, "VOLLEY"))) {
            steps.add(new Step("archer " + scene.name(), mc -> {
                at(mc, X, Z, 0);
                List<String> seen = new java.util.concurrent.CopyOnWriteArrayList<>();
                double[] from = new double[1];
                EngineTest.server(mc, player -> {
                    player.setGameMode(GameType.SURVIVAL);
                    player.setHealth(player.getMaxHealth());
                    player.serverLevel().setDayTime(18000);
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.NIGHT_VISION, 600, 0, false, false));
                    var skeleton = EntityType.SKELETON.create(player.serverLevel());
                    skeleton.moveTo(X, Y, Z + scene.distance(), 180, 0);
                    skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                    skeleton.setPersistenceRequired();
                    player.serverLevel().addFreshEntity(skeleton);
                    skeleton.setTarget(player);
                });
                ArcherFighting.probe = (mob, move) -> seen.add(move.name());
                com.murimblock.shenfa.client.SkeletalMobs.moveProbe = (mob, move) -> seen.add(move);
                start(new Run(scene.wall() ? 200 : 140, t -> {
                    LivingEntity skeleton = nearest(mc, EntityType.SKELETON);
                    if (t == 0) {
                        GifRecorder.start("archer_" + scene.name());
                        GifRecorder.camera(GifRecorder.side(mc.player));
                    }
                    if (t == 2 && skeleton != null) {
                        from[0] = server(skeleton).distanceTo(serverPlayer(mc));
                    }
                    if (!scene.wall() || t < 30) {
                        face(mc, skeleton);
                    }
                    if (scene.wall() && t == 30) {
                        // The player hides behind a wall the archer saw it go behind.
                        EngineTest.server(mc, player -> {
                            for (BlockPos pos : BlockPos.betweenClosed(AREA - 3, (int) Y, 2, AREA + 3, (int) Y + 3, 2)) {
                                player.serverLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                            }
                        });
                    }
                    wounds(mc);
                }, () -> {
                    LivingEntity skeleton = nearest(mc, EntityType.SKELETON);
                    double now = skeleton == null ? 0 : server(skeleton).distanceTo(serverPlayer(mc));
                    EngineTest.check(String.format("the archer %s (%s, %.1f to %.1f blocks)", switch (scene.name()) {
                        case "shot" -> "draws, holds and looses an aimed shot";
                        case "retreat" -> "backs away from a close prey";
                        case "bash" -> "bashes a prey touching it";
                        default -> "lobs a volley over the wall";
                    }, seen, from[0], now), seen.contains(scene.expect())
                            && (!scene.name().equals("retreat") || now > from[0] + 1.5));
                    ArcherFighting.probe = null;
                    com.murimblock.shenfa.client.SkeletalMobs.moveProbe = null;
                    EngineTest.server(mc, player -> {
                        player.serverLevel().setDayTime(6000);
                        for (BlockPos pos : BlockPos.betweenClosed(AREA - 3, (int) Y, 2, AREA + 3, (int) Y + 3, 2)) {
                            player.serverLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                        }
                        player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class,
                                player.getBoundingBox().inflate(30)).forEach(Entity::discard);
                    });
                    discard(mc);
                }), false);
            }));
        }
        // The Fileuse de linceuls (SpiderFighting): spits silk from afar, leaps on a slowed prey, bites at touch. At
        // night, filmed from the side.
        record SpiderScene(String name, double distance, boolean slowed, String expect) {
        }
        for (SpiderScene scene : List.of(new SpiderScene("spit", 7, false, "SPIT"),
                new SpiderScene("leap", 4.5, true, "LEAP"), new SpiderScene("bite", 1.4, false, "BITE"))) {
            steps.add(new Step("fileuse " + scene.name(), mc -> {
                at(mc, X, Z, 0);
                List<String> seen = new java.util.concurrent.CopyOnWriteArrayList<>();
                boolean[] slowedBy = new boolean[1];
                EngineTest.server(mc, player -> {
                    player.setGameMode(GameType.SURVIVAL);
                    player.setHealth(player.getMaxHealth());
                    player.removeAllEffects();
                    player.serverLevel().setDayTime(18000);
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.NIGHT_VISION, 600, 0, false, false));
                    if (scene.slowed()) {
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
                    }
                    var spider = EntityType.SPIDER.create(player.serverLevel());
                    spider.moveTo(X, Y, Z + scene.distance(), 180, 0);
                    spider.setPersistenceRequired();
                    player.serverLevel().addFreshEntity(spider);
                    spider.setTarget(player);
                });
                double[] slide = {-1};
                net.minecraft.world.phys.Vec3[] reared = new net.minecraft.world.phys.Vec3[1];
                net.minecraft.world.entity.Entity[] spitter = new net.minecraft.world.entity.Entity[1];
                SpiderFighting.probe = (mob, move) -> {
                    seen.add(move.name());
                    if (move == SpiderFighting.Move.SPIT && reared[0] == null) {
                        reared[0] = mob.position();
                        spitter[0] = mob;
                        slide[0] = -(mob.level().getGameTime() + 16);
                    }
                };
                start(new Run(120, t -> {
                    LivingEntity spider = nearest(mc, EntityType.SPIDER);
                    if (t == 0) {
                        GifRecorder.start("fileuse_" + scene.name());
                        GifRecorder.camera(GifRecorder.side(mc.player));
                    }
                    face(mc, spider);
                    wounds(mc);
                    EngineTest.server(mc, player -> {
                        // How far it slid while rearing (until just before the spit's small lunge).
                        if (reared[0] != null && slide[0] < 0 && player.level().getGameTime() >= -slide[0]) {
                            slide[0] = spitter[0].position().subtract(reared[0]).horizontalDistance();
                        }
                        if (!scene.slowed() && player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)) {
                            slowedBy[0] = true;
                        }
                    });
                }, () -> {
                    if (scene.name().equals("spit")) {
                        EngineTest.check(String.format("rearing, the Fileuse stands still (%.2f blocks)", slide[0]),
                                slide[0] >= 0 && slide[0] < 0.3);
                    }
                    EngineTest.check(String.format("the Fileuse %s (%s%s)", switch (scene.name()) {
                        case "spit" -> "rears and spits silk that slows its prey";
                        case "leap" -> "leaps on a slowed prey";
                        default -> "bites a prey at touch";
                    }, seen, scene.name().equals("spit") ? ", slowed " + slowedBy[0] : ""), seen.contains(scene.expect())
                            && (!scene.name().equals("spit") || slowedBy[0]));
                    SpiderFighting.probe = null;
                    EngineTest.server(mc, player -> {
                        player.serverLevel().setDayTime(6000);
                        player.removeAllEffects();
                    });
                    discard(mc);
                }), false);
            }));
        }
        if (GifRecorder.ON) {
            showcase(steps);
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

    /** Scenes only recorded for the GIFs (no checks): the other moves, the dodges, and first person. */
    private static void showcase(List<Step> steps) {
        steps.add(scene("fists dash strike", 4, 40, (mc, t) -> {
            boolean run = t >= 0 && t < 12;
            mc.options.keyUp.setDown(run);
            mc.options.keySprint.setDown(run);
            if (t == 9) {
                CombatClient.pressAttack();
            }
        }));
        steps.add(scene("fists air strike", 1.8, 40, (mc, t) -> {
            if (t == 0) {
                mc.player.jumpFromGround();
            }
            if (t == 5) {
                CombatClient.pressAttack();
            }
        }));
        steps.add(scene("dodges", 3.5, 80, (mc, t) -> {
            // Back, left, right, forward: the direction is the movement key held as the dodge key is pressed.
            int[] at = {0, 20, 40, 60};
            for (int i = 0; i < at.length; i++) {
                if (t == at[i]) {
                    mc.options.keyLeft.setDown(i == 1);
                    mc.options.keyRight.setDown(i == 2);
                    mc.options.keyUp.setDown(i == 3);
                    net.minecraft.client.KeyMapping.click(CombatClient.DODGE.getKey());
                }
                if (t == at[i] + 2) {
                    mc.options.keyLeft.setDown(false);
                    mc.options.keyRight.setDown(false);
                    mc.options.keyUp.setDown(false);
                }
            }
        }));
        steps.add(new Step("first person combo", mc -> {
            at(mc, X, Z, 0);
            spawn(mc, EntityType.ZOMBIE, X, Z + 1.6, true, mob -> {
            });
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            start(new Run(50, t -> {
                face(mc, nearest(mc, EntityType.ZOMBIE));
                mc.player.setXRot(15);
                if (t % 6 == 0 && t <= 18) {
                    CombatClient.pressAttack();
                }
            }, () -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                discard(mc);
            }), false);
        }));
    }

    private interface Script {
        void tick(Minecraft mc, int t);
    }

    /** A scene against a still zombie {@code distance} ahead. */
    private static Step scene(String name, double distance, int ticks, Script script) {
        return new Step(name, mc -> {
            at(mc, X, Z, 0);
            spawn(mc, EntityType.ZOMBIE, X, Z + distance, true, mob -> {
            });
            start(new Run(ticks, t -> {
                face(mc, nearest(mc, EntityType.ZOMBIE));
                script.tick(mc, t);
            }, () -> {
                mc.options.keyUp.setDown(false);
                mc.options.keySprint.setDown(false);
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
            if (GifRecorder.ON) {
                // An unseen roof: the recorded zombies must not burn in the sun.
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 10, (int) Y + 8, -10, AREA + 10, (int) Y + 8, 12)) {
                    level.setBlockAndUpdate(pos, Blocks.BARRIER.defaultBlockState());
                }
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

    /** Spawn zombies without the cap they get when filming (scenes shot at night). */
    private static boolean bareheaded;

    private static void spawn(Minecraft mc, EntityType<?> type, double x, double z, boolean still,
                              Consumer<Entity> setup) {
        boolean cap = GifRecorder.ON && !bareheaded;
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
                    if (cap) {
                        // A cap keeps the recorded zombies from burning in the sun.
                        mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                    }
                }
            }
            entity.moveTo(x, Y, z, 180, 0);
            level.addFreshEntity(entity);
            setup.accept(entity);
        });
    }

    /** Disciple moves the server started ({move ordinal, game tick}) and the ticks the player was wounded. */
    private static final List<long[]> MOVES = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final List<Long> WOUNDS = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static float lastHealth;

    private static List<String> moveNames() {
        return MOVES.stream().map(move -> DiscipleFighting.Move.values()[(int) move[0]].name()).toList();
    }

    /** {@code count} zombies fighting the player, the first {@code distance} ahead (helmets: no burning by day). */
    private static void fighters(Minecraft mc, int count, double distance) {
        fighters(mc, count, distance, Z);
    }

    private static void fighters(Minecraft mc, int count, double distance, double z) {
        EngineTest.server(mc, player -> {
            player.setGameMode(GameType.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            lastHealth = player.getHealth();
        });
        for (int i = 0; i < count; i++) {
            double angle = (i - (count - 1) / 2.0) * 0.9;
            spawn(mc, EntityType.ZOMBIE, X + Math.sin(angle) * distance, z + Math.cos(angle) * distance, false, mob -> {
                ((Mob) mob).setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                ((Mob) mob).setTarget(serverPlayer(mc));
            });
        }
    }

    /** Notes the tick the player loses health, then heals it so the fight goes on. */
    private static void wounds(Minecraft mc) {
        EngineTest.server(mc, player -> {
            if (player.getHealth() < lastHealth) {
                WOUNDS.add(player.level().getGameTime());
            }
            if (player.getHealth() < 8) {
                player.setHealth(player.getMaxHealth());
            }
            lastHealth = player.getHealth();
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
        start(run, true);
    }

    private static void start(Run run, boolean sideView) {
        at = -10;
        running = run;
        GifRecorder.start("combat_" + EngineTest.current);
        if (sideView) {
            GifRecorder.camera(GifRecorder.side(Minecraft.getInstance().player));
        }
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
            GifRecorder.stop();
            done.end().run();
        }
    }
}
