package com.murimblock;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.murimblock.combat.CombatService;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimSkills;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.AzureSenseiEntity;
import com.murimblock.mob.MurimEntities;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.slf4j.Logger;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Development-only filming of a duel with Sensei Yun (gradlew -PgameTests runSenseiDuel): in a fresh flat world a
 * disciple (a server player on a mock connection, iron sword, the Azure Cloud Sword Art learned) cuts at the sensei
 * and raises the Murim guard against his answers; halfway the sensei falls below half his health and fights in
 * earnest. The camera circles them slowly. One frame per client tick in build/sensei-duel-run/frames, for
 * scripts/showcase/make-clip.cjs.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SenseiDuel {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** When filming starts, and how long it lasts (ticks). */
    static final int START = 60, LENGTH = 320;
    /** When the sensei falls below half his health. */
    static final int SECOND_PHASE = START + 170;
    private static boolean creating, finished;
    private static int ticks, frame;
    private static volatile ServerPlayer disciple;
    private static volatile AzureSenseiEntity sensei;
    private static double bx, by, bz;
    /** The disciple's round: three cuts, then the guard raised a while. */
    private static final int ROUND = 64;

    private SenseiDuel() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.senseiDuel") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        yesman.epicfight.config.ClientConfig.showTargetIndicator = false;
        yesman.epicfight.config.ClientConfig.enableTargetEntityGuide = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-sensei-duel", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-sensei-duel", settings, new WorldOptions(1L, false, false),
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
            LOGGER.error("Sensei duel crashed", e);
            finished = true;
            mc.stop();
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        if (ticks == 20) {
            mc.options.hideGui = true;
            server.execute(() -> setup(server));
            return;
        }
        if (ticks < 30 || disciple == null || sensei == null) return;
        int t = ticks - START;
        server.execute(() -> fight(t));
        camera(mc, t);
        if (t >= 0 && t < LENGTH) capture(mc);
        if (t == LENGTH + 10) {
            finished = true;
            LOGGER.info("Sensei duel filmed: {} frames", frame);
            mc.stop();
        }
    }

    private static void setup(MinecraftServer server) {
        ServerLevel level = server.overworld();
        level.setDayTime(5000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        ServerPlayer self = server.getPlayerList().getPlayers().get(0);
        bx = Math.floor(self.getX()) + 0.5;
        by = self.getY();
        bz = Math.floor(self.getZ()) + 0.5;
        self.getAbilities().flying = true;
        self.getAbilities().invulnerable = true;
        self.onUpdateAbilities();
        disciple = spawnDisciple(server, level);
        disciple.moveTo(bx - 1.6, by, bz, -90, 0);
        sensei = MurimEntities.AZURE_SENSEI.get().spawn(level, BlockPos.containing(bx + 1.6, by, bz), MobSpawnType.COMMAND);
        sensei.moveTo(bx + 1.6, by, bz, 90, 0);
        sensei.setTarget(disciple);
    }

    /** One tick of the fight, server side. */
    private static void fight(int t) {
        if (disciple.isRemoved() || sensei.isRemoved()) return;
        disciple.doTick();
        sensei.setTarget(disciple);
        // The disciple keeps facing the sensei and does not fall.
        double dx = sensei.getX() - disciple.getX(), dz = sensei.getZ() - disciple.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        disciple.setYRot(yaw);
        disciple.setYHeadRot(yaw);
        disciple.yBodyRot = yaw;
        if (disciple.getHealth() < disciple.getMaxHealth() * 0.3F) disciple.setHealth(disciple.getMaxHealth());
        if (t < 0) return;
        PlayerPatch<?> patch = EpicFightBridge.patch(disciple);
        int r = t % ROUND;
        AnimationAccessor<? extends StaticAnimation> cut = switch (r) {
            case 0 -> MurimAnimations.DRIFTING_CUT;
            case 12 -> MurimAnimations.RETURNING_CUT;
            case 24 -> MurimAnimations.PIERCING_MIST;
            default -> null;
        };
        if (cut != null && patch != null) patch.playAnimationSynchronized(cut, 0.0F);
        if (r == 38 && patch != null) {
            patch.startSkillHolding(MurimSkills.MURIM_GUARD.get());
            patch.getSkill(SkillSlots.GUARD).getDataManager().setDataSync(EpicFightSkillDataKeys.LAST_ACTIVE, disciple.tickCount);
        }
        if (r == 60 && patch != null) patch.resetHolding();
        if (t == SECOND_PHASE - START) sensei.setHealth(sensei.getMaxHealth() * 0.46F);
        // Below the ground he stands on: which animation takes him there.
        if (sensei.getY() < by - 0.2) {
            var sp = yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(sensei, yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch.class);
            var main = sp == null ? null : sp.getAnimator().getPlayerFor(null);
            LOGGER.warn("Sensei duel: the sensei is {} below the ground at t={} in {}", String.format("%.2f", by - sensei.getY()), t, main == null ? null : main.getRealAnimation());
        }
    }

    /** The camera's bearing from the two (radians), eased toward the side of the line between them. */
    private static double bearing = Double.NaN;

    /**
     * The camera beside the two, square to the line between them so neither hides the other, drifting a little to and
     * fro, a little above their heads; eased so a dash past the disciple does not jerk it; client side so it glides.
     */
    private static void camera(Minecraft mc, int t) {
        double mx = (disciple.getX() + sensei.getX()) / 2, mz = (disciple.getZ() + sensei.getZ()) / 2;
        double lx = sensei.getX() - disciple.getX(), lz = sensei.getZ() - disciple.getZ();
        // The side of the line facing the camera now: of its two normals, the nearer to where the camera is.
        double side = Math.atan2(lx, -lz) + Math.PI / 2;
        if (!Double.isNaN(bearing) && Math.cos(side - bearing) < 0) side += Math.PI;
        double want = side + 0.35 * Math.sin(t / 60.0);
        if (Double.isNaN(bearing)) bearing = want;
        else bearing += Math.atan2(Math.sin(want - bearing), Math.cos(want - bearing)) * 0.06;
        double angle = bearing;
        double radius = 4.6;
        Vec3 eye = new Vec3(mx + radius * Math.sin(angle), by + 2.0, mz - radius * Math.cos(angle));
        Vec3 target = new Vec3(mx, by + 1.0, mz);
        Vec3 d = target.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        var p = mc.player;
        p.getAbilities().flying = true;
        p.absMoveTo(eye.x, eye.y - p.getEyeHeight(), eye.z, yaw, pitch);
        p.yRotO = yaw; p.xRotO = pitch; p.setYHeadRot(yaw); p.yHeadRotO = yaw; p.setYBodyRot(yaw); p.yBodyRotO = yaw;
        p.setDeltaMovement(Vec3.ZERO);
    }

    private static ServerPlayer spawnDisciple(MinecraftServer server, ServerLevel level) {
        var cookie = new CommonListenerCookie(new GameProfile(UUID.randomUUID(), "Disciple"), 0,
                ClientInformation.createDefault(), false, ConnectionType.NEOFORGE);
        ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        for (int i = 0; i <= 60; i++) player.tick();
        ItemStack manual = new ItemStack(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get());
        manual.use(level, player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60);
        player.setHealth(60);
        CombatService.setCombatMode(player, true);
        EpicFightCombatDefaults.enforce(player);
        return player;
    }

    private static void capture(Minecraft mc) {
        Path file = mc.gameDirectory.toPath().resolve("frames").resolve(String.format("%05d.png", frame++));
        try {
            Files.createDirectories(file.getParent());
            var image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            Util.ioPool().execute(() -> {
                try (image) { image.writeToFile(file); } catch (Exception e) { LOGGER.error("Frame write failed", e); }
            });
        } catch (Exception e) {
            LOGGER.error("Frame capture failed", e);
        }
    }
}
