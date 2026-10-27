package com.murimblock.client.dev;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import com.murimblock.Murimblock;
import com.murimblock.anim.client.SkeletalPlayerRenderer;
import com.murimblock.anim.client.SkeletalPlayers;
import com.murimblock.client.dev.EngineTest.Step;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Step 3 of the engine roadmap in a real client: a lineup of players with default skins (wide and slim arms, outer
 * layers, items in hand) shot from six sides drawn by vanilla, then by our skinned body, compared pixel by pixel;
 * the same dressed up (armor of every kind, trims, glint, a pumpkin head, arrows, stingers, an elytra, a cape) and
 * crouching; the test poses and the skeleton view shot for a GIF; then twenty players timed.
 */
final class SkeletalRenderChecks {
    /** Feet of the lineup's middle, high in the sky so nothing else is in the shots. */
    private static final double X = 0.5, Y = 200, Z = 0.5;
    private static final float SPACING = 1.1f;
    /**
     * Default skins picked by UUID (index = hash % 18; slims first): slim Alex, wide Steve, slim Kai, wide Kai, wide
     * Zuri, slim Sunny.
     */
    private static final int[] SKINS = {0, 15, 3, 12, 17, 7};
    /** At most this share of the picture may differ between vanilla and us (vanilla's idle arm sway moves a few). */
    private static final double MAX_DIFFERENT = 0.004;
    /** Dressed up, a little more: the armor of the slim arms and the arrows follow vanilla's parts half a pixel off. */
    private static final double MAX_DIFFERENT_DRESSED = 0.006;

    /**
     * A camera place (feet, relative to the lineup), its look, which way the lineup faces, and whether a white floor
     * hides the far terrain (which loads while the test runs) behind the lineup.
     */
    private record View(String name, double dy, double dz, float pitch, float faces, boolean floor) {
    }

    private static final List<View> VIEWS = List.of(
            new View("front", -0.6, -4.5, 0, 180, false),
            new View("back", -0.6, -4.5, 0, 0, false),
            new View("left_side", -0.6, -4.5, 0, 90, false),
            new View("right_side", -0.6, -4.5, 0, -90, false),
            new View("below", -3.5, -1.8, -55, 180, false),
            new View("above", 4, -2.6, 55, 180, true));

    private static final List<RemotePlayer> LINEUP = new ArrayList<>();
    private static NativeImage vanilla;

    private SkeletalRenderChecks() {
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("lineup", SkeletalRenderChecks::stage));
        for (View view : VIEWS) {
            compareSteps(steps, "", view, MAX_DIFFERENT);
        }
        for (String pose : List.of("bend", "guard", "joints")) {
            for (View view : List.of(VIEWS.get(0), VIEWS.get(2))) {
                shotSteps(steps, pose + "_" + view.name(), view, () -> SkeletalPlayers.pose(pose));
            }
        }
        steps.add(new Step("dress", mc -> {
            SkeletalPlayers.pose("stand");
            dress(mc);
        }));
        for (View view : VIEWS.subList(0, 4)) {
            compareSteps(steps, "dressed_", view, MAX_DIFFERENT_DRESSED);
        }
        for (View view : List.of(VIEWS.get(0), VIEWS.get(2))) {
            shotSteps(steps, "dressed_bend_" + view.name(), view, () -> SkeletalPlayers.pose("bend"));
        }
        steps.add(new Step("crouch", mc -> {
            SkeletalPlayers.pose("stand");
            LINEUP.forEach(fake -> fake.setPose(net.minecraft.world.entity.Pose.CROUCHING));
        }));
        for (View view : List.of(VIEWS.get(0), VIEWS.get(2))) {
            compareSteps(steps, "crouch_", view, 1);
        }
        steps.add(new Step("stand up", mc -> LINEUP.forEach(fake -> fake.setPose(net.minecraft.world.entity.Pose.STANDING))));
        shotSteps(steps, "skeleton_view", VIEWS.get(2), () -> {
            SkeletalPlayers.pose("bend");
            SkeletalPlayers.showJoints(true);
        });
        steps.add(new Step("crowd", mc -> {
            SkeletalPlayers.showJoints(false);
            crowd(mc);
            SkeletalPlayerRenderer.resetTiming();
        }));
        steps.add(new Step("timing", mc -> {
            double micros = SkeletalPlayerRenderer.averageMicros();
            Murimblock.LOGGER.info("Engine test: skeletal player drawn in {} µs on average", String.format("%.1f", micros));
            EngineTest.check(LINEUP.size() + " armored and plain players take under 200 µs each to draw ("
                    + String.format("%.1f", micros) + ")", micros > 0 && micros < 200);
            save(mc, Screenshot.takeScreenshot(mc.getMainRenderTarget()), "crowd");
            SkeletalPlayers.enable(false);
            SkeletalPlayers.pose("stand");
        }));
        return steps;
    }

    /** Places the camera, shoots vanilla, then us, and checks how many pixels differ (any share passes when max is 1). */
    private static void compareSteps(List<Step> steps, String prefix, View view, double max) {
        steps.add(new Step(prefix + view.name() + " (place)", mc -> {
            place(mc, view);
            SkeletalPlayers.enable(false);
        }));
        steps.add(new Step(prefix + view.name() + " (vanilla)", mc -> {
            vanilla = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            SkeletalPlayers.enable(true);
        }));
        steps.add(new Step(prefix + view.name() + " (skeletal)", mc -> compare(mc, prefix + view.name(), max)));
    }

    /** Places the camera, sets things up, then saves a shot of our rendering. */
    private static void shotSteps(List<Step> steps, String name, View view, Runnable setup) {
        steps.add(new Step(name + " (place)", mc -> {
            place(mc, view);
            SkeletalPlayers.enable(true);
            setup.run();
        }));
        steps.add(new Step(name + " (shot)", mc -> save(mc, Screenshot.takeScreenshot(mc.getMainRenderTarget()), name)));
    }

    private static void stage(Minecraft mc) {
        // Compared with vanilla: vanilla's standing pose, not the animator.
        SkeletalPlayers.pose(SkeletalPlayers.STAND);
        mc.options.hideGui = true;
        mc.options.cloudStatus().set(CloudStatus.OFF);
        EngineTest.server(mc, player -> {
            player.serverLevel().setDayTime(6000);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        });
        for (int i = 0; i < SKINS.length; i++) {
            Dummy fake = new Dummy(mc, SKINS[i], i);
            if (i == 1) {
                fake.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                fake.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            } else if (i == 4) {
                fake.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                fake.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH));
            }
            mc.level.addEntity(fake);
            LINEUP.add(fake);
        }
        EngineTest.check("the lineup is in the world", LINEUP.stream().allMatch(fake -> mc.level.getEntity(fake.getId()) == fake));
    }

    /**
     * Armor of every kind (dyed leather, iron with a gold trim, enchanted diamond, netherite, chainmail, gold), a
     * pumpkin on a head, arrows and stingers stuck in a body, an elytra and a cape.
     */
    private static void dress(Minecraft mc) {
        ItemStack leather = new ItemStack(Items.LEATHER_CHESTPLATE);
        leather.set(DataComponents.DYED_COLOR, new DyedItemColor(0xB02E26, true));
        equip(0, leather, new ItemStack(Items.LEATHER_LEGGINGS), new ItemStack(Items.LEATHER_BOOTS),
                new ItemStack(Items.LEATHER_HELMET));
        ItemStack iron = new ItemStack(Items.IRON_CHESTPLATE);
        var registries = mc.level.registryAccess();
        iron.set(DataComponents.TRIM, new ArmorTrim(
                registries.registryOrThrow(Registries.TRIM_MATERIAL).getHolderOrThrow(TrimMaterials.GOLD),
                registries.registryOrThrow(Registries.TRIM_PATTERN).getHolderOrThrow(TrimPatterns.COAST)));
        equip(1, iron, ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.IRON_HELMET));
        ItemStack diamond = new ItemStack(Items.DIAMOND_CHESTPLATE);
        diamond.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        equip(2, diamond, new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS),
                new ItemStack(Items.DIAMOND_HELMET));
        equip(3, ItemStack.EMPTY, new ItemStack(Items.NETHERITE_LEGGINGS), new ItemStack(Items.NETHERITE_BOOTS),
                new ItemStack(Items.CARVED_PUMPKIN));
        equip(4, new ItemStack(Items.CHAINMAIL_CHESTPLATE), ItemStack.EMPTY, ItemStack.EMPTY,
                new ItemStack(Items.GOLDEN_HELMET));
        LINEUP.get(4).setArrowCount(4);
        LINEUP.get(4).setStingerCount(2);
        equip(5, new ItemStack(Items.ELYTRA), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
        // A cape needs a texture; the elytra one has the size of a cape, enough to see where the cape hangs.
        ((Dummy) LINEUP.get(3)).cape = ResourceLocation.withDefaultNamespace("textures/entity/elytra.png");
    }

    private static void equip(int index, ItemStack chest, ItemStack legs, ItemStack feet, ItemStack head) {
        RemotePlayer fake = LINEUP.get(index);
        fake.setItemSlot(EquipmentSlot.CHEST, chest);
        fake.setItemSlot(EquipmentSlot.LEGS, legs);
        fake.setItemSlot(EquipmentSlot.FEET, feet);
        fake.setItemSlot(EquipmentSlot.HEAD, head);
    }

    /** Fourteen more players behind the lineup, to time twenty at once. */
    private static void crowd(Minecraft mc) {
        for (int i = 0; i < 14; i++) {
            Dummy fake = new Dummy(mc, i % 18, 100 + i);
            fake.moveTo(X + (i % 7 - 3) * SPACING, Y, Z + 2 + i / 7 * 1.5, 180, 0);
            fake.setOldPosAndRot();
            fake.yBodyRot = fake.yBodyRotO = fake.yHeadRot = fake.yHeadRotO = 180;
            mc.level.addEntity(fake);
            LINEUP.add(fake);
        }
    }

    /**
     * A test player: frozen (the level counts ticks before calling tick, so tick puts the count back), with a default
     * skin picked by UUID and an optional cape.
     */
    static final class Dummy extends RemotePlayer {
        private ResourceLocation cape;
        /** Blocks the body seems to move each tick (the level sets the last position to the current one first). */
        double moveX, moveY;
        /** The hand using its item (drawing a bow, raising a shield...) for {@link #usedFor} ticks, or null. */
        InteractionHand using;
        int usedFor;
        boolean spectator;

        Dummy(Minecraft mc, int skin, int number) {
            this(mc, skin, number, "Test" + number);
        }

        Dummy(Minecraft mc, int skin, int number, String name) {
            // The default skin is picked by the UUID's hash; the number in both halves of the high bits cancels out of it,
            // so every test player has its own UUID (the level refuses a second one) and still the chosen skin.
            super(mc.level, new GameProfile(new UUID((long) number << 32 | number, skin), name));
            setId(-1000 - number);
        }

        @Override
        public void tick() {
            tickCount = 0;
            xo = getX() - moveX;
            yo = getY() - moveY;
        }

        @Override
        public PlayerSkin getSkin() {
            PlayerSkin skin = super.getSkin();
            return cape == null ? skin
                    : new PlayerSkin(skin.texture(), skin.textureUrl(), cape, null, skin.model(), skin.secure());
        }

        @Override
        public boolean isUsingItem() {
            return using != null || super.isUsingItem();
        }

        @Override
        public InteractionHand getUsedItemHand() {
            return using != null ? using : super.getUsedItemHand();
        }

        @Override
        public ItemStack getUseItem() {
            return using != null ? getItemInHand(using) : super.getUseItem();
        }

        @Override
        public int getUseItemRemainingTicks() {
            return using != null ? 1000 : super.getUseItemRemainingTicks();
        }

        @Override
        public int getTicksUsingItem() {
            return using != null ? usedFor : super.getTicksUsingItem();
        }

        @Override
        public boolean isSpectator() {
            return spectator;
        }

        /** Every outer part shown (hat, jacket, sleeves, trousers, cape), as players have them by default. */
        void showAllParts() {
            entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7F);
        }

        /** In elytra flight for a while (the flag and the flight ticks a real flight would have). */
        void glide() {
            setSharedFlag(7, true);
            try {
                Field ticks = LivingEntity.class.getDeclaredField("fallFlyTicks");
                ticks.setAccessible(true);
                ticks.setInt(this, 30);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        /** In a riptide spin. */
        void spin() {
            setLivingEntityFlag(4, true);
        }
    }

    /** Takes the lineup and the crowd out of the world. */
    static void clearLineup(Minecraft mc) {
        LINEUP.forEach(fake -> mc.level.removeEntity(fake.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED));
        LINEUP.clear();
    }

    private static void place(Minecraft mc, View view) {
        for (int i = 0; i < LINEUP.size(); i++) {
            RemotePlayer fake = LINEUP.get(i);
            fake.moveTo(X + (i - (LINEUP.size() - 1) / 2.0) * SPACING, Y, Z, view.faces(), 0);
            fake.setOldPosAndRot();
            fake.yBodyRot = fake.yBodyRotO = fake.yHeadRot = fake.yHeadRotO = view.faces();
        }
        EngineTest.server(mc, player -> {
            BlockState floor = view.floor() ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.AIR.defaultBlockState();
            for (BlockPos pos : BlockPos.betweenClosed((int) X - 12, (int) Y - 1, (int) Z - 4, (int) X + 12, (int) Y - 1, (int) Z + 12)) {
                player.serverLevel().setBlockAndUpdate(pos, floor);
            }
            player.connection.teleport(X, Y + view.dy(), Z + view.dz(), 0, view.pitch());
        });
    }

    private static void compare(Minecraft mc, String name, double max) {
        NativeImage ours = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        double share = difference(vanilla, ours);
        EngineTest.check(name + ": skeletal looks like vanilla (" + String.format("%.3f%%", share * 100)
                + " of pixels differ)", share < max);
        save(mc, vanilla, name + "_1_vanilla");
        save(mc, ours, name + "_2_skeletal");
        vanilla = null;
    }

    /** The share of pixels that differ clearly (one channel by more than 32) between two shots. */
    static double difference(NativeImage vanilla, NativeImage ours) {
        int different = 0;
        for (int y = 0; y < ours.getHeight(); y++) {
            for (int x = 0; x < ours.getWidth(); x++) {
                int a = vanilla.getPixelRGBA(x, y), b = ours.getPixelRGBA(x, y);
                int most = 0;
                for (int shift = 0; shift < 24; shift += 8) {
                    most = Math.max(most, Math.abs((a >> shift & 0xFF) - (b >> shift & 0xFF)));
                }
                if (most > 32) {
                    different++;
                }
            }
        }
        return (double) different / (ours.getWidth() * ours.getHeight());
    }

    private static void save(Minecraft mc, NativeImage image, String name) {
        saveTo(mc, image, "step3/" + name);
    }

    /** Saves (and frees) a shot as {@code run/engine-test/screenshots/<path>.png}. */
    static void saveTo(Minecraft mc, NativeImage image, String path) {
        File file = new File(mc.gameDirectory, "screenshots/" + path + ".png");
        file.getParentFile().mkdirs();
        try (image) {
            image.writeToFile(file);
        } catch (IOException e) {
            Murimblock.LOGGER.error("Engine test: could not save {}", file, e);
        }
    }
}
