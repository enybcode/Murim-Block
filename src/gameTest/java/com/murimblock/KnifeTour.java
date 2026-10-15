package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.combat.CombatService;
import com.murimblock.item.MurimItems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Development-only real-client check of the Murim knives (gradlew -PgameTests runKnifeTour): the Bishou in hand, three
 * real M1 (Epic Fight's dagger strikes), then a Flying Knife in the other hand (its dual daggers), filmed from the side,
 * then the Flying Knife from the player's eyes. Report in knife-tour-report.txt, pictures in build/knife-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class KnifeTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static Marker camera;
    private static Vec3 start;
    private static final List<Zombie> zombies = new ArrayList<>();
    private static final List<String> report = new ArrayList<>();
    private static volatile float health;
    private static volatile double zombieZ;

    private KnifeTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.knifeTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-knife", GameType.SURVIVAL, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-knife", settings, new WorldOptions(1L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.screen);
                }
                return;
            }
            ticks++;
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            step(mc, mc.getSingleplayerServer());
        } catch (Exception e) {
            check(false, "tour crashed: " + e);
            LOGGER.error("Knife tour crashed", e);
            finish(mc);
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        if (camera != null && mc.getCameraEntity() == camera && mc.getConnection() != null) {
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround()));
        }
        if (camera != null && ticks >= 56 && ticks < 200 && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
        switch (ticks) {
            case 20 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                ServerPlayer self = self(server);
                self.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.BISHOU.get()));
                CombatService.setCombatMode(self, true);
                self.teleportTo(level, Math.floor(self.getX()) + 0.5, self.getY(), Math.floor(self.getZ()) + 0.5, 0, 0);
            });
            case 40 -> capture(mc, "00-hotbar.png");
            case 48 -> server.execute(() -> zombie(server, 0, 1.6));
            case 56 -> {
                mc.options.hideGui = false;
                yesman.epicfight.config.ClientConfig.showTargetIndicator = false;
                yesman.epicfight.config.ClientConfig.enableTargetEntityGuide = false;
                start = mc.player.position();
                placeSideCamera(mc);
            }
            case 58 -> capture(mc, "01-bishou-held.png");
            // The Bishou alone: Epic Fight's three dagger strikes, clicked one after the other.
            case 60, 70, 80 -> KeyMapping.click(mc.options.keyAttack.getKey());
            case 63 -> check(clientPlaying(mc, Animations.DAGGER_AUTO1), "the first M1 is Epic Fight's dagger strike (playing: " + clientAnimation(mc) + ")");
            case 73 -> check(clientPlaying(mc, Animations.DAGGER_AUTO2), "the second M1 is the dagger's second strike (playing: " + clientAnimation(mc) + ")");
            case 83 -> check(clientPlaying(mc, Animations.DAGGER_AUTO3), "the third M1 is the dagger's third strike (playing: " + clientAnimation(mc) + ")");
            case 100 -> server.execute(() -> check(zombies.getFirst().getHealth() < zombies.getFirst().getMaxHealth(), "the Bishou's strikes land"));
            // A Flying Knife in the other hand: Epic Fight's dual daggers.
            case 110 -> server.execute(() -> { reset(server, 0, 1.6); self(server).setItemInHand(InteractionHand.OFF_HAND, new ItemStack(MurimItems.FLYING_KNIFE.get())); });
            case 118 -> capture(mc, "02-dual-held.png");
            case 120, 130, 140, 150 -> KeyMapping.click(mc.options.keyAttack.getKey());
            case 123 -> check(clientPlaying(mc, Animations.DAGGER_DUAL_AUTO1), "two knives strike as Epic Fight's dual daggers (playing: " + clientAnimation(mc) + ")");
            case 170 -> server.execute(() -> check(zombies.getFirst().getHealth() < zombies.getFirst().getMaxHealth(), "the dual strikes land"));
            // From the player's eyes, the Flying Knife alone.
            case 200 -> {
                mc.setCameraEntity(mc.player);
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                server.execute(() -> { reset(server, 0, 1.8); ServerPlayer self = self(server);
                    self.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.FLYING_KNIFE.get()));
                    self.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY); });
            }
            case 212 -> capture(mc, "04-first-person-held.png");
            case 214, 224, 234 -> KeyMapping.click(mc.options.keyAttack.getKey());
            case 217 -> check(clientPlaying(mc, Animations.DAGGER_AUTO1), "the Flying Knife strikes as a dagger too (playing: " + clientAnimation(mc) + ")");
            case 260 -> {
                server.execute(() -> zombies.forEach(Zombie::discard));
                finish(mc);
            }
            default -> { }
        }
        record(mc, "03-bishou-m1", 58, 100, 1);
        record(mc, "05-dual-m1", 118, 170, 1);
        record(mc, "06-first-person-m1", 212, 256, 1);
    }

    /** A sturdy zombie without AI, `dx` to the side and `dz` in front of the player. */
    private static void zombie(MinecraftServer server, double dx, double dz) {
        ServerPlayer self = self(server);
        Zombie z = EntityType.ZOMBIE.create(server.overworld());
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        z.setHealth(200);
        z.moveTo(self.getX() + dx, self.getY(), self.getZ() + dz, dz >= 0 ? 180 : 0, 0);
        z.setNoAi(true);
        z.setPersistenceRequired();
        // A helmet: no burning in the sun.
        z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        server.overworld().addFreshEntity(z);
        zombies.add(z);
    }

    /** The player back at the start, facing south, techniques ready; one fresh zombie `dz` ahead. */
    private static void reset(MinecraftServer server, double dx, double dz) {
        ServerPlayer self = self(server);
        zombies.forEach(Zombie::discard);
        zombies.clear();
        self.teleportTo(server.overworld(), start.x, start.y, start.z, 0, 0);
        self.setDeltaMovement(Vec3.ZERO);
        zombie(server, dx, dz);
        health = 200;
    }

    /** Presses a combat bar key (its release, two ticks later, casts). */
    private static void press(Minecraft mc, int slot) {
        mc.options.keyHotbarSlots[slot].setDown(true);
        KeyMapping.click(mc.options.keyHotbarSlots[slot].getKey());
    }

    /** The side camera, beside where the player stands. */
    private static void placeSideCamera(Minecraft mc) {
        Vec3 at = mc.player.position();
        Vec3 wanted = at.add(6.5, 1.8, 1.0);
        Vec3 d = at.add(0, 0.9, 1.0).subtract(wanted);
        Vec3 eye = wanted.add(d.normalize().scale(4));
        if (camera == null) camera = new Marker(EntityType.MARKER, mc.level);
        camera.setPos(eye);
        camera.xo = camera.xOld = eye.x;
        camera.yo = camera.yOld = eye.y;
        camera.zo = camera.zOld = eye.z;
        camera.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        camera.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
        camera.yRotO = camera.getYRot();
        camera.xRotO = camera.getXRot();
        mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        mc.setCameraEntity(camera);
    }

    private static String clientAnimation(Minecraft mc) {
        var patch = EpicFightCapabilities.getEntityPatch(mc.player, PlayerPatch.class);
        var main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
        return main == null || main.getRealAnimation() == null ? "nothing" : String.valueOf(main.getRealAnimation().get());
    }

    private static boolean clientPlaying(Minecraft mc, AnimationAccessor<?> animation) {
        var patch = EpicFightCapabilities.getEntityPatch(mc.player, PlayerPatch.class);
        if (patch == null) return false;
        var main = patch.getAnimator().getPlayerFor(null);
        return main != null && main.getRealAnimation() != null && main.getRealAnimation().get() == animation.get();
    }

    /** No first-person hand while the side camera films. */
    @SubscribeEvent
    public static void onRenderHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        if (Boolean.getBoolean("murimblock.knifeTour") && camera != null && Minecraft.getInstance().getCameraEntity() == camera) event.setCanceled(true);
    }

    private static ServerPlayer self(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    private static void record(Minecraft mc, String name, int from, int to, int every) throws Exception {
        if (ticks >= from && ticks <= to && (ticks - from) % every == 0) capture(mc, String.format("%s/frame-%03d.png", name, (ticks - from) / every));
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(file.getParent());
        var image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        net.minecraft.Util.ioPool().execute(() -> {
            try (image) { image.writeToFile(file); } catch (Exception e) { LOGGER.error("Frame write failed", e); }
        });
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("Knife tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("knife-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the Knife tour report", e);
        }
        LOGGER.info("Knife tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
