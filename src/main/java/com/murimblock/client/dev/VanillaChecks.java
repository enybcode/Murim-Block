package com.murimblock.client.dev;

import com.mojang.blaze3d.platform.NativeImage;
import com.murimblock.shenfa.client.SkeletalPlayerRenderer;
import com.murimblock.shenfa.client.SkeletalPlayers;
import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.client.dev.SkeletalRenderChecks.Dummy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * Everything vanilla draws on a player, drawn by us and compared with vanilla pixel by pixel (in the {@code stand}
 * pose, vanilla's own, so only placement and the vanilla arm work are compared): the arm poses of items in use (bow,
 * crossbow charging and charged, shield, trident, spyglass at the eye, goat horn, brush), an attack swing, a slim
 * player's grip, a left-handed archer; then the bodies vanilla turns: asleep in a bed, in elytra flight, in a riptide
 * spin, upside down (Dinnerbone), frozen and shaking, seated in a boat, and armor with a model of its own. The same
 * lineups are then shot with the animator.
 */
final class VanillaChecks {
    private static final double X = 0.5, Y = 200, Z = 0.5;
    private static final float SPACING = 1.1f;
    /** Most pixels may differ only where the body's outline lands half a pixel apart. */
    private static final double MAX_DIFFERENT = 0.006;
    /** One spinning player shot twice at angles up to 75° apart. */
    private static final double MAX_DIFFERENT_SPIN = 0.02;

    private record View(String name, float faces) {
    }

    private static final List<View> VIEWS = List.of(new View("front", 180), new View("side", -90));

    private static final List<Entity> SPAWNED = new ArrayList<>();
    private static final List<Dummy> LINEUP = new ArrayList<>();
    private static NativeImage vanilla;

    private VanillaChecks() {
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("vanilla arms", VanillaChecks::arms));
        for (View view : VIEWS) {
            compare(steps, "arms_" + view.name(), view, MAX_DIFFERENT);
        }
        steps.add(new Step("vanilla bodies", VanillaChecks::bodies));
        steps.add(new Step("vanilla bodies (check)", mc -> {
            Dummy armored = LINEUP.get(LINEUP.size() - 1);
            PlayerRenderer renderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(armored);
            EngineTest.check("a helmet with a model of its own is drawn by vanilla's armor layer",
                    SkeletalPlayerRenderer.drawsVanillaArmor(renderer, armored, EquipmentSlot.HEAD));
            EngineTest.check("the sleeper lies in its bed", LINEUP.get(0).getBedOrientation() == Direction.NORTH);
        }));
        for (View view : VIEWS) {
            compare(steps, "bodies_" + view.name(), view, MAX_DIFFERENT);
        }
        // A riptide spin turns 75° a tick, partial ticks included, so two shots never catch it at the same angle: it is
        // compared alone, allowing for the turn.
        steps.add(new Step("vanilla spin", mc -> {
            clear(mc);
            SkeletalPlayers.pose(SkeletalPlayers.STAND);
            add(mc, 15, new ItemStack(Items.TRIDENT), Dummy::spin);
        }));
        compare(steps, "spin_front", VIEWS.get(0), MAX_DIFFERENT_SPIN);
        steps.add(new Step("vanilla end", VanillaChecks::clear));
        return steps;
    }

    /** Arms: one player per vanilla arm pose. */
    private static void arms(Minecraft mc) {
        SkeletalRenderChecks.clearLineup(mc);
        SkeletalPlayers.pose(SkeletalPlayers.STAND);
        ItemStack charged = new ItemStack(Items.CROSSBOW);
        charged.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(new ItemStack(Items.ARROW)));
        add(mc, 15, new ItemStack(Items.BOW), dummy -> use(dummy, InteractionHand.MAIN_HAND, 20));
        add(mc, 15, new ItemStack(Items.CROSSBOW), dummy -> use(dummy, InteractionHand.MAIN_HAND, 10));
        add(mc, 15, ItemStack.EMPTY, dummy -> dummy.setItemSlot(EquipmentSlot.MAINHAND, charged));
        add(mc, 15, new ItemStack(Items.IRON_SWORD), dummy -> {
            dummy.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            use(dummy, InteractionHand.OFF_HAND, 10);
        });
        add(mc, 15, new ItemStack(Items.TRIDENT), dummy -> use(dummy, InteractionHand.MAIN_HAND, 10));
        add(mc, 15, new ItemStack(Items.SPYGLASS), dummy -> use(dummy, InteractionHand.MAIN_HAND, 10));
        add(mc, 15, new ItemStack(Items.GOAT_HORN), dummy -> use(dummy, InteractionHand.MAIN_HAND, 10));
        add(mc, 15, new ItemStack(Items.BRUSH), dummy -> use(dummy, InteractionHand.MAIN_HAND, 10));
        add(mc, 15, new ItemStack(Items.DIAMOND_SWORD), dummy -> {
            // Frozen a third of the way through a swing.
            dummy.swingingArm = InteractionHand.MAIN_HAND;
            dummy.attackAnim = dummy.oAttackAnim = 0.35f;
        });
        add(mc, 0, new ItemStack(Items.IRON_SWORD), dummy -> {
        });
        add(mc, 3, new ItemStack(Items.BOW), dummy -> {
            dummy.setMainArm(HumanoidArm.LEFT);
            use(dummy, InteractionHand.MAIN_HAND, 20);
        });
        EngineTest.check("eleven players show vanilla's arm poses", LINEUP.size() == 11);
    }

    /** Bodies vanilla turns or lays down. */
    private static void bodies(Minecraft mc) {
        clear(mc);
        SkeletalPlayers.pose(SkeletalPlayers.STAND);
        add(mc, 15, ItemStack.EMPTY, dummy -> {
            BlockPos bed = BlockPos.containing(slotX(0), Y, Z);
            EngineTest.server(mc, player -> {
                BlockState head = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH)
                        .setValue(BedBlock.PART, BedPart.HEAD);
                player.serverLevel().setBlockAndUpdate(bed, head);
                player.serverLevel().setBlockAndUpdate(bed.south(), head.setValue(BedBlock.PART, BedPart.FOOT));
            });
            dummy.setPose(Pose.SLEEPING);
            dummy.setSleepingPos(bed);
        });
        add(mc, 15, ItemStack.EMPTY, Dummy::glide);
        // Upside down needs the name and the cape shown, as vanilla checks.
        Dummy dinnerbone = new Dummy(mc, 12, 300, "Dinnerbone");
        dinnerbone.showAllParts();
        LINEUP.add(dinnerbone);
        mc.level.addEntity(dinnerbone);
        SPAWNED.add(dinnerbone);
        add(mc, 15, ItemStack.EMPTY, dummy -> dummy.setTicksFrozen(dummy.getTicksRequiredToFreeze()));
        add(mc, 15, ItemStack.EMPTY, dummy -> {
            Boat boat = new Boat(EntityType.BOAT, mc.level);
            boat.moveTo(slotX(5), Y, Z, 180, 0);
            mc.level.addEntity(boat);
            SPAWNED.add(boat);
            dummy.startRiding(boat, true);
        });
        add(mc, 15, ItemStack.EMPTY, dummy -> {
            dummy.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.TURTLE_HELMET));
            dummy.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        });
    }

    private static void add(Minecraft mc, int skin, ItemStack held, Consumer<Dummy> setup) {
        Dummy dummy = new Dummy(mc, skin, 200 + LINEUP.size());
        dummy.setItemSlot(EquipmentSlot.MAINHAND, held);
        LINEUP.add(dummy);
        mc.level.addEntity(dummy);
        SPAWNED.add(dummy);
        setup.accept(dummy);
    }

    private static void use(Dummy dummy, InteractionHand hand, int ticks) {
        dummy.using = hand;
        dummy.usedFor = ticks;
    }

    private static double slotX(int index) {
        return X + (index - 5) * SPACING;
    }

    private static void place(Minecraft mc, View view) {
        for (int i = 0; i < LINEUP.size(); i++) {
            Dummy dummy = LINEUP.get(i);
            if (dummy.isPassenger()) {
                dummy.getVehicle().moveTo(slotX(i), Y, Z, view.faces(), 0);
                dummy.getVehicle().setOldPosAndRot();
                dummy.setYRot(view.faces());
            } else if (dummy.hasPose(Pose.SLEEPING)) {
                BlockPos bed = dummy.getSleepingPos().orElseThrow();
                dummy.moveTo(bed.getX() + 0.5, bed.getY() + 0.6875, bed.getZ() + 0.5, view.faces(), 0);
            } else {
                dummy.moveTo(slotX(i), Y, Z, view.faces(), 0);
            }
            dummy.setOldPosAndRot();
            dummy.yBodyRot = dummy.yBodyRotO = dummy.yHeadRot = dummy.yHeadRotO = view.faces();
        }
        EngineTest.server(mc, player -> player.connection.teleport(X, Y - 0.2, Z - 7.5, 0, 0));
    }

    private static void compare(List<Step> steps, String name, View view, double max) {
        steps.add(new Step(name + " (place)", mc -> {
            place(mc, view);
            SkeletalPlayers.enable(false);
        }));
        steps.add(new Step(name + " (vanilla)", mc -> {
            vanilla = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            SkeletalPlayers.enable(true);
        }));
        steps.add(new Step(name + " (skeletal)", mc -> {
            NativeImage ours = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            double share = SkeletalRenderChecks.difference(vanilla, ours);
            EngineTest.check(name + ": skeletal looks like vanilla (" + String.format("%.3f%%", share * 100)
                    + " of pixels differ)", share < max);
            SkeletalRenderChecks.saveTo(mc, vanilla, "vanilla/" + name + "_1_vanilla");
            SkeletalRenderChecks.saveTo(mc, ours, "vanilla/" + name + "_2_skeletal");
            vanilla = null;
            SkeletalPlayers.pose(SkeletalPlayers.ANIMATED);
        }));
        steps.add(new Step(name + " (animated)", mc -> {
            SkeletalRenderChecks.saveTo(mc, Screenshot.takeScreenshot(mc.getMainRenderTarget()),
                    "vanilla/" + name + "_3_animated");
            SkeletalPlayers.pose(SkeletalPlayers.STAND);
        }));
    }

    private static void clear(Minecraft mc) {
        BlockPos bed = BlockPos.containing(slotX(0), Y, Z);
        EngineTest.server(mc, player -> {
            player.serverLevel().setBlockAndUpdate(bed.south(), Blocks.AIR.defaultBlockState());
            player.serverLevel().setBlockAndUpdate(bed, Blocks.AIR.defaultBlockState());
        });
        SPAWNED.forEach(entity -> {
            entity.stopRiding();
            mc.level.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
        });
        SPAWNED.clear();
        LINEUP.clear();
        SkeletalPlayers.enable(false);
    }
}
