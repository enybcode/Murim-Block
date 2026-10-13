package com.murimblock;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The hit boxes of every animation of the Azure Cloud Sword Art, checked on Epic Fight's real hit detection: for each,
 * a zombie is put where the move must hit (in front, at the sides of a cut, at the end of a dash, all around a slam) and
 * where it must not (behind, too far, too wide), one place at a time, and the move is played. A table of every case is
 * written to the log ("Azure hitbox"). Player facing south (+Z): dz is ahead, dx to the side.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AzureHitboxGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    /**
     * Ticks given to each case: the move plays with the zombie there (long enough for Thousand Drizzle's last thrust),
     * then without it until every animation of the art has ended on its own (at most 1.5 s).
     */
    private static final int PLAY_TICKS = 30, CASE_TICKS = 40;

    private AzureHitboxGameTests() { }

    private record Case(String move, Supplier<AnimationAccessor<? extends StaticAnimation>> animation, double dx, double dz, boolean hits) { }

    private static Case hit(String move, Supplier<AnimationAccessor<? extends StaticAnimation>> a, double dx, double dz) {
        return new Case(move, a, dx, dz, true);
    }

    private static Case miss(String move, Supplier<AnimationAccessor<? extends StaticAnimation>> a, double dx, double dz) {
        return new Case(move, a, dx, dz, false);
    }

    /**
     * Where each move must hit and must not, from its box (MurimAnimations; a zombie is 0.6 wide, so it is touched up to
     * 0.3 beyond a box's edge). Every place is well inside the test's own 11-block arena. The far tip of a reach is left
     * out: there a hit depends on the tick the swing is sampled at.
     */
    private static List<Case> cases() {
        List<Case> c = new ArrayList<>();
        // M1. The cuts: 2.6 wide, from 0.1 to 2.3 ahead.
        for (var cut : List.<Supplier<AnimationAccessor<? extends StaticAnimation>>>of(() -> MurimAnimations.DRIFTING_CUT, () -> MurimAnimations.RETURNING_CUT)) {
            String name = cut.get() == MurimAnimations.DRIFTING_CUT ? "drifting_cut" : "returning_cut";
            c.add(hit(name, cut, 0, 1.5));
            c.add(hit(name, cut, 1.1, 1.2));
            c.add(hit(name, cut, -1.1, 1.2));
            c.add(miss(name, cut, 0, 2.9));
            c.add(miss(name, cut, 0, -0.8));
            c.add(miss(name, cut, 1.9, 1.0));
        }
        // The thrust: narrow (1.0), to 3.6 ahead.
        c.add(hit("piercing_mist", () -> MurimAnimations.PIERCING_MIST, 0, 1.2));
        c.add(hit("piercing_mist", () -> MurimAnimations.PIERCING_MIST, 0, 3.2));
        c.add(miss("piercing_mist", () -> MurimAnimations.PIERCING_MIST, 1.0, 1.5));
        c.add(miss("piercing_mist", () -> MurimAnimations.PIERCING_MIST, 0, 4.2));
        // Rising Cloud: 2.0 wide, to 2.2 ahead. Falling Rain: 1.8 wide, to 2.4.
        c.add(hit("rising_cloud", () -> MurimAnimations.RISING_CLOUD, 0, 1.5));
        c.add(miss("rising_cloud", () -> MurimAnimations.RISING_CLOUD, 0, 2.8));
        c.add(miss("rising_cloud", () -> MurimAnimations.RISING_CLOUD, 1.6, 1.0));
        c.add(hit("falling_rain", () -> MurimAnimations.FALLING_RAIN, 0, 1.5));
        c.add(miss("falling_rain", () -> MurimAnimations.FALLING_RAIN, 0, 3.0));
        c.add(miss("falling_rain", () -> MurimAnimations.FALLING_RAIN, 1.5, 1.0));
        c.add(hit("falling_rain_air", () -> MurimAnimations.FALLING_RAIN_AIR, 0, 1.5));
        // Wind Step slides two blocks: its box reaches 3.2 ahead.
        c.add(hit("wind_step", () -> MurimAnimations.WIND_STEP, 0, 2.8));
        c.add(miss("wind_step", () -> MurimAnimations.WIND_STEP, 0, -0.8));
        c.add(miss("wind_step", () -> MurimAnimations.WIND_STEP, 1.6, 1.5));
        // Cloud Step Slash: the 5-block path, 1.6 wide. Its riposte: 2.5 blocks. Its dive: 4 ahead (and down).
        c.add(hit("cloud_step_slash", () -> MurimAnimations.CLOUD_STEP_SLASH, 0, 1.0));
        c.add(hit("cloud_step_slash", () -> MurimAnimations.CLOUD_STEP_SLASH, 0, 4.2));
        c.add(miss("cloud_step_slash", () -> MurimAnimations.CLOUD_STEP_SLASH, 1.4, 2.0));
        c.add(miss("cloud_step_slash", () -> MurimAnimations.CLOUD_STEP_SLASH, 0, -0.8));
        c.add(hit("cloud_step_riposte", () -> MurimAnimations.CLOUD_STEP_RIPOSTE, 0, 2.5));
        c.add(miss("cloud_step_riposte", () -> MurimAnimations.CLOUD_STEP_RIPOSTE, 0, 4.0));
        c.add(miss("cloud_step_riposte", () -> MurimAnimations.CLOUD_STEP_RIPOSTE, 1.5, 1.5));
        c.add(hit("cloud_step_dive", () -> MurimAnimations.CLOUD_STEP_DIVE, 0, 3.0));
        c.add(hit("cloud_step_dive", () -> MurimAnimations.CLOUD_STEP_DIVE, 0, 4.2));
        c.add(miss("cloud_step_dive", () -> MurimAnimations.CLOUD_STEP_DIVE, 0, -0.9));
        c.add(miss("cloud_step_dive", () -> MurimAnimations.CLOUD_STEP_DIVE, 1.5, 1.5));
        // Swallow Returns spins: all around the player, but not far.
        c.add(hit("swallow_returns", () -> MurimAnimations.SWALLOW_RETURNS, 1.2, 0.3));
        c.add(hit("swallow_returns", () -> MurimAnimations.SWALLOW_RETURNS, 0, 1.5));
        c.add(hit("swallow_returns", () -> MurimAnimations.SWALLOW_RETURNS, 0, -0.8));
        c.add(miss("swallow_returns", () -> MurimAnimations.SWALLOW_RETURNS, 0, 3.6));
        // Thousand Drizzle: 1.4 wide, to 3.0 ahead; Storm of Needles 2.8 wide, to 3.5.
        c.add(hit("thousand_drizzle", () -> MurimAnimations.THOUSAND_DRIZZLE, 0, 2.0));
        c.add(miss("thousand_drizzle", () -> MurimAnimations.THOUSAND_DRIZZLE, 1.3, 1.5));
        c.add(miss("thousand_drizzle", () -> MurimAnimations.THOUSAND_DRIZZLE, 0, 3.6));
        c.add(hit("thousand_drizzle_storm", () -> MurimAnimations.THOUSAND_DRIZZLE_STORM, 1.2, 2.0));
        // The crescent: 2.4 wide, to 2.4 ahead. The descent's slam: three blocks all around.
        c.add(hit("heaven_splitting_crescent", () -> MurimAnimations.HEAVEN_SPLITTING_CRESCENT, 0, 1.5));
        c.add(miss("heaven_splitting_crescent", () -> MurimAnimations.HEAVEN_SPLITTING_CRESCENT, 0, 3.0));
        c.add(miss("heaven_splitting_crescent", () -> MurimAnimations.HEAVEN_SPLITTING_CRESCENT, 1.8, 1.0));
        c.add(hit("cloud_splitting_descent", () -> MurimAnimations.CLOUD_SPLITTING_DESCENT, 2.0, 0));
        c.add(hit("cloud_splitting_descent", () -> MurimAnimations.CLOUD_SPLITTING_DESCENT, 0, -0.9));
        c.add(hit("cloud_splitting_descent", () -> MurimAnimations.CLOUD_SPLITTING_DESCENT, 0, 2.5));
        c.add(miss("cloud_splitting_descent", () -> MurimAnimations.CLOUD_SPLITTING_DESCENT, 0, 4.0));
        return c;
    }

    @GameTest(template = "foundation_arena", batch = "azure_hitboxes", timeoutTicks = 80 * CASE_TICKS)
    public static void everyMoveHitsWhereItIsDrawnAndNowhereElse(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        List<Case> cases = cases();
        List<String> table = new ArrayList<>(), wrong = new ArrayList<>();
        Zombie[] zombie = new Zombie[1];
        boolean[] hitThisCase = new boolean[1];
        // Near the world's origin, in the open sky above the spawn chunks (always loaded): the test arenas lie ten
        // million blocks out, where the floats of Epic Fight's hit maths are only good to about a block.
        var level = helper.getLevel();
        level.getChunk(0, 0);
        Vec3 origin = new Vec3(8.5, 120, 8.5);
        for (int i = 0; i < cases.size(); i++) {
            Case k = cases.get(i);
            int start = 2 + i * CASE_TICKS;
            helper.runAfterDelay(start, () -> {
                // The last case's animation has played out on its own: nothing of it carries over.
                MartialCombat.clearForTests();
                MartialService.clearCooldownsForTests(player);
                player.moveTo(origin.x, origin.y, origin.z, 0, 0);
                player.setOnGround(true);
                // Facing south in every way Epic Fight may read it: the body turns toward the head over a few ticks in
                // Minecraft, and Epic Fight turns the model (and its hit boxes) with its own yaw.
                player.setYHeadRot(0);
                player.yHeadRotO = 0;
                player.setYBodyRot(0);
                player.yBodyRotO = 0;
                player.yRotO = 0;
                patch.setYRot(0);
                patch.setYRotO(0);
                patch.setModelYRot(0, false);
                // A fresh zombie for each case: nothing carries over (a knock-down, its invulnerable getting up).
                zombie[0] = EntityType.ZOMBIE.create(level);
                zombie[0].setNoAi(true);
                // A helmet: no burning in the sun (it would count as hits).
                zombie[0].setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                zombie[0].getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
                zombie[0].setHealth(1000);
                zombie[0].moveTo(origin.x + k.dx, origin.y, origin.z + k.dz, 180, 0);
                level.addFreshEntity(zombie[0]);
                patch.playAnimationSynchronized(k.animation.get(), 0.0F);
            });
            for (int t = start + 1; t < start + CASE_TICKS; t++) {
                int at = t;
                helper.runAfterDelay(t, () -> {
                    // Held in place: a client moves its own player, and this mock one would drift with what is left of
                    // its speed. The boxes are measured from where the player stands.
                    player.setPos(origin.x, origin.y, origin.z);
                    player.setDeltaMovement(Vec3.ZERO);
                    player.doTick();
                    if (zombie[0] != null) zombie[0].invulnerableTime = 0;
                    // The move is over: the zombie leaves, the animation plays out for a few more ticks.
                    if (at == start + PLAY_TICKS) {
                        hitThisCase[0] = zombie[0].getHealth() < 1000;
                        zombie[0].discard();
                        zombie[0] = null;
                    }
                });
            }
            helper.runAfterDelay(start + CASE_TICKS - 1, () -> {
                boolean hit = hitThisCase[0];
                String line = String.format("%-26s dx %5.1f dz %5.1f  %s%s", k.move, k.dx, k.dz, hit ? "hit " : "miss",
                        hit == k.hits ? "" : "   <-- expected " + (k.hits ? "a hit" : "a miss"));
                table.add(line);
                if (hit != k.hits) wrong.add(line.trim());
            });
        }
        helper.runAfterDelay(2 + cases.size() * CASE_TICKS + 2, () -> {
            LOGGER.info("Azure hitbox survey:\n{}", String.join("\n", table));
            MartialCombat.clearForTests();
            cleanup(player);
            helper.assertTrue(wrong.isEmpty(), "Hit boxes off: " + String.join(" | ", wrong));
            helper.succeed();
        });
    }
}
