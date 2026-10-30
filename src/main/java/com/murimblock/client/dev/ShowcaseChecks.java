package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.combat.CombatClient;
import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.innerworld.TrialZombie;
import com.murimblock.registry.MurimEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Showcase shots for the site and the mod's pages ({@code -PengineGroups=showcase -PengineGifs=true}, the scene camera needs the latter): each mob look fighting the
 * player at night on open grass, lit by night vision (the undead do not burn), the player hitting back in combat mode, filmed over the player's shoulder at three
 * quarters so both are seen in action. Shots {@code show_<mob>_<tick>} every 6 ticks and a GIF {@code show_<mob>}; no checks beyond the mob being
 * there.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
final class ShowcaseChecks {
    private static final int AREA = 1100;
    private static final double X = AREA + 0.5, Y = 200, Z = 0.5;
    private static Consumer<Integer> running;
    private static int at, length;
    private static Runnable after;

    /** One mob to film: its name in the shots, its type (looked up once the registries are bound), how far off it starts, and how it is dressed. */
    private record Cast(String name, Supplier<EntityType<? extends Mob>> type, double distance, Consumer<Mob> dress) {
    }

    private ShowcaseChecks() {
    }

    private static List<Cast> cast() {
        Consumer<Mob> plain = mob -> {
        };
        return List.of(
                new Cast("disciple", () -> EntityType.ZOMBIE, 3.5, plain),
                new Cast("disciple_sword", () -> EntityType.ZOMBIE, 3.5, mob -> hold(mob, Items.IRON_SWORD)),
                new Cast("sables", () -> EntityType.HUSK, 3.5, mob -> hold(mob, Items.IRON_AXE)),
                new Cast("paysan", () -> EntityType.ZOMBIE_VILLAGER, 3.5, plain),
                new Cast("sanglier", () -> EntityType.ZOMBIFIED_PIGLIN, 3.5, mob -> hold(mob, Items.GOLDEN_SWORD)),
                new Cast("noye", () -> EntityType.DROWNED, 3.5, plain),
                new Cast("archer", () -> EntityType.SKELETON, 8, mob -> hold(mob, Items.BOW)),
                new Cast("esprit", () -> EntityType.STRAY, 8, mob -> hold(mob, Items.BOW)),
                new Cast("fileuse", () -> EntityType.SPIDER, 5, plain),
                new Cast("venins", () -> EntityType.CAVE_SPIDER, 4, plain),
                new Cast("fourneau", () -> EntityType.CREEPER, 4, plain),
                new Cast("sceau", () -> EntityType.SLIME, 4.5, mob -> ((Slime) mob).setSize(4, true)),
                new Cast("empoisonneuse", () -> EntityType.WITCH, 6, plain),
                new Cast("moine", () -> EntityType.ENDERMAN, 3.5, plain),
                new Cast("golem", () -> EntityType.IRON_GOLEM, 4, plain),
                new Cast("warden", MurimEntities.TRIAL_ZOMBIE::get, 4.5, mob -> ((TrialZombie) mob).makeWarden()));
    }

    private static void hold(Mob mob, net.minecraft.world.item.Item item) {
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
        mob.setDropChance(EquipmentSlot.MAINHAND, 0);
    }

    private static AABB area() {
        return new AABB(AREA - 30, Y - 5, -30, AREA + 30, Y + 20, 30);
    }

    /** Over the player's shoulder at three quarters: the foe's front and the player's side, both in frame. */
    private static GifRecorder.View shoulder(Minecraft mc, Entity foe, float partialTick) {
        Vec3 me = mc.player.getPosition(partialTick), it = foe.getPosition(partialTick);
        Vec3 along = it.subtract(me).multiply(1, 0, 1);
        double span = Math.max(1, along.length());
        along = along.scale(1 / span);
        Vec3 left = new Vec3(along.z, 0, -along.x);
        double height = Math.max(1.8, foe.getBbHeight());
        Vec3 middle = me.add(it).scale(0.5).add(0, height * 0.42, 0);
        double distance = 2.2 + 0.42 * Math.max(span, height);
        Vec3 eye = middle.add(left.scale(distance)).subtract(along.scale(distance * 0.35)).add(0, 0.6, 0);
        return new GifRecorder.View(eye, middle);
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("showcase setup", mc -> {
            mc.options.hideGui = true;
            CombatClient.setMode(true);
            // Third person so the player is drawn; the scene camera (GifRecorder, on with -PengineGifs=true) moves it.
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            EngineTest.server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, player.server);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, player.server);
                level.setDayTime(18000);
                level.setWeatherParameters(20 * 600, 0, false, false);
                player.server.setDifficulty(Difficulty.NORMAL, true);
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 30, (int) Y - 2, -30, AREA + 30, (int) Y - 1, 30)) {
                    level.setBlockAndUpdate(pos, pos.getY() == (int) Y - 1 ? Blocks.GRASS_BLOCK.defaultBlockState()
                            : Blocks.DIRT.defaultBlockState());
                }
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 30, (int) Y, -30, AREA + 30, (int) Y + 8, 30)) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 30, (int) Y + 14, -30, AREA + 30, (int) Y + 14, 30)) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
                level.getEntitiesOfClass(Entity.class, area(), entity -> !(entity instanceof Player)).forEach(Entity::discard);
                player.setGameMode(GameType.SURVIVAL);
                player.connection.teleport(X, Y, Z, 0, 0);
            });
        }));
        steps.add(new Step("showcase settle", mc -> run(40, t -> {
        }, () -> {
        })));
        for (Cast cast : cast()) {
            int[] id = {-1};
            steps.add(new Step("showcase " + cast.name(), mc -> {
                EngineTest.server(mc, player -> {
                    ServerLevel level = player.serverLevel();
                    level.getEntitiesOfClass(Entity.class, area(), entity -> !(entity instanceof Player)).forEach(Entity::discard);
                    player.setHealth(player.getMaxHealth());
                    player.removeAllEffects();
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 30, 4, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 30, 3, false, false));
                    // A night fight under the stars, lit as by day.
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 30, 0, false, false));
                    player.connection.teleport(X, Y, Z, 0, 0);
                    Mob mob = cast.type().get().create(level);
                    mob.moveTo(X, Y, Z + cast.distance(), 180, 0);
                    mob.setYHeadRot(180);
                    mob.setYBodyRot(180);
                    cast.dress().accept(mob);
                    level.addFreshEntity(mob);
                    mob.setTarget(player);
                    if (cast.type().get() == EntityType.IRON_GOLEM) {
                        // The Gardien-talisman wakes when a villager is harmed.
                        Villager villager = EntityType.VILLAGER.create(level);
                        villager.moveTo(X + 1.5, Y, Z + 1, 0, 0);
                        villager.setNoAi(true);
                        level.addFreshEntity(villager);
                        player.attack(villager);
                    }
                    id[0] = mob.getId();
                });
                run(110, t -> {
                    if (!(mc.level.getEntity(id[0]) instanceof LivingEntity foe)) {
                        return;
                    }
                    if (t == 0) {
                        GifRecorder.start("show_" + cast.name());
                    }
                    GifRecorder.camera(partial -> shoulder(mc, foe, partial));
                    // Face the foe and hit back now and then.
                    Vec3 to = foe.position().subtract(mc.player.position());
                    float yaw = (float) (Math.toDegrees(Math.atan2(-to.x, to.z)));
                    mc.player.setYRot(yaw);
                    mc.player.setYHeadRot(yaw);
                    if (t % 22 == 6 && to.length() < 3.5) {
                        CombatClient.pressAttack();
                    }
                    if (t >= 12 && t % 6 == 0) {
                        EngineTest.shoot(mc, "show_" + cast.name() + "_" + t);
                    }
                }, () -> {
                    EngineTest.check("the " + cast.name() + " was filmed", id[0] >= 0);
                    GifRecorder.stop();
                });
            }));
        }
        steps.add(new Step("showcase done", mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            CombatClient.setMode(false);
            EngineTest.server(mc, player -> {
                player.serverLevel().getEntitiesOfClass(Entity.class, area(), entity -> !(entity instanceof Player))
                        .forEach(Entity::discard);
                player.serverLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true, player.server);
                player.serverLevel().setDayTime(6000);
                player.setGameMode(GameType.CREATIVE);
                player.removeAllEffects();
                player.server.setDifficulty(Difficulty.PEACEFUL, true);
            });
        }));
        return steps;
    }

    private static void run(int ticks, Consumer<Integer> tick, Runnable end) {
        at = -10;
        length = ticks;
        running = tick;
        after = end;
        EngineTest.busy = () -> running != null;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (running == null || mc.player == null || mc.isPaused()) {
            return;
        }
        if (at >= 0) {
            running.accept(at);
        }
        if (++at >= length) {
            running = null;
            after.run();
        }
    }
}
