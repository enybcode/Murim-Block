package com.murimblock;

import com.murimblock.client.EpicFightControls;
import com.murimblock.client.MurimblockKeyMappings;
import com.murimblock.client.gui.MurimProfileScreen;
import com.murimblock.combat.CombatService;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightSkillService;
import com.murimblock.qi.QiService;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.client.gui.screen.SkillEditScreen;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.skill.SkillSlots;

/** Opt-in real-client rendering checks. Never loads into a published jar or the normal IntelliJ run. */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class ClientVisualSmoke {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static int ticks;
    private static int bootTicks;
    private static boolean initialized;
    private static boolean failed;
    private static final AtomicBoolean ready = new AtomicBoolean();

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.visualSmoke")) return;
        Minecraft mc = Minecraft.getInstance();
        if (failed) { mc.stop(); return; }
        try {
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
                if (++bootTicks == 40) {
                    LOGGER.info("Visual smoke bootstrap screen: {}", mc.screen == null ? "none" : mc.screen.getClass().getName());
                    capture("00-bootstrap.png");
                }
                if (mc.screen instanceof BackupConfirmScreen) {
                    // Only the disposable copied world is ever opened by this test run.
                    for (var child : mc.screen.children()) {
                        if (child instanceof Button button && button.getMessage().getString().equals("Proceed")) {
                            button.onPress();
                            break;
                        }
                    }
                }
                if (bootTicks > 1200) throw new IllegalStateException("Visual client did not enter the disposable world");
                return;
            }
            if (!initialized) {
                initialized = true;
                mc.options.guiScale().set(3);
                mc.options.renderDistance().set(4);
                mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                mc.resizeDisplay();
                mc.getSingleplayerServer().execute(() -> {
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    player.setGameMode(GameType.CREATIVE);
                    player.setNoGravity(true);
                    player.teleportTo(0.5, 80, 0.5);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
                    CombatService.setCombatMode(player, true);
                    QiService.setQiMax(player, 120);
                    QiService.setQi(player, 87);
                    EpicFightSkillService.change(player, SkillSlots.GUARD.universalOrdinal(), "epicfight:guard", -1);
                    EpicFightBridge.patch(player).setStamina(5);
                    ready.set(true);
                });
            }
            if (!ready.get()) return;
            mc.getToasts().clear();
            ticks++;
            switch (ticks) {
                case 50 -> mc.setScreen(new MurimProfileScreen());
                case 70 -> capture("01-profile.png");
                case 80 -> mc.setScreen(new SkillEditScreen(mc.player, EpicFightBridge.patch(mc.player).getPlayerSkills()));
                case 82 -> require(mc.screen instanceof MurimProfileScreen, "Native skills GUI remained open");
                case 100 -> {
                    // Exercise the real library row button, not a separate mock layout.
                    click(65, 90);
                }
                case 115 -> capture("02-techniques.png");
                case 125 -> mc.setScreen(new SkillBookScreen(mc.player, EpicFightSkills.GUARD.get(), null, null));
                case 127 -> require(mc.screen instanceof MurimProfileScreen, "Native skill book GUI remained open");
                case 140 -> { mc.setScreen(new MurimProfileScreen()); click(201, 194); }
                case 155 -> capture("03-cultivation.png");
                case 165 -> { mc.setScreen(new MurimProfileScreen()); click(278, 194); }
                case 180 -> capture("04-info.png");
                case 190 -> mc.setScreen(MurimProfileScreen.combatSettings());
                case 205 -> capture("05-settings.png");
                case 215 -> mc.setScreen(new KeyBindsScreen(mc.screen, mc.options));
                case 220 -> checkControls();
                case 235 -> capture("06-keybinds.png");
                case 245 -> {
                    mc.setScreen(null);
                    mc.getSingleplayerServer().execute(() -> {
                        var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                        player.setGameMode(GameType.SURVIVAL);
                        EpicFightBridge.patch(player).setStamina(5);
                    });
                }
                case 265 -> capture("07-hud.png");
                case 280 -> {
                    mc.getWindow().setWindowed(960, 720);
                    mc.options.guiScale().set(3);
                    mc.resizeDisplay();
                    mc.setScreen(MurimProfileScreen.techniques(null, null));
                }
                case 295 -> capture("08-small-scale.png");
                case 320 -> {
                    Files.writeString(mc.gameDirectory.toPath().resolve("visual-smoke-passed.txt"),
                            "Native skill/book screens replaced; one Murim key category; hidden rows absent; gameplay mappings retained.\n");
                    LOGGER.info("Murim client visual smoke checks passed");
                    mc.stop();
                }
                default -> { }
            }
        } catch (Exception exception) {
            failed = true;
            LOGGER.error("Murim client visual smoke failed", exception);
        }
    }

    private static void click(int x, int y) {
        Screen screen = Minecraft.getInstance().screen;
        screen.mouseClicked((screen.width - 320) / 2.0 + x, (screen.height - 214) / 2.0 + y, 0);
    }

    private static void checkControls() {
        Minecraft mc = Minecraft.getInstance();
        long headers = 0;
        int visible = 0;
        for (var listener : mc.screen.children()) {
            if (!(listener instanceof KeyBindsList list)) continue;
            for (var entry : list.children()) {
                if (!(entry instanceof KeyBindsList.KeyEntry key)) continue;
                require(!EpicFightControls.hidden(key.key), "Hidden native key row remained");
                if (key.key.getCategory().equals(MurimblockKeyMappings.CATEGORY)) visible++;
            }
            headers = list.children().stream().filter(entry -> entry instanceof KeyBindsList.CategoryEntry).count();
            list.setScrollAmount(Math.max(0, list.getMaxScroll() - 80));
        }
        require(visible == 12, "Expected 12 useful mappings in a single Murim category, got " + visible);
        long categories = java.util.Arrays.stream(mc.options.keyMappings).filter(mapping -> !EpicFightControls.hidden(mapping))
                .map(net.minecraft.client.KeyMapping::getCategory).distinct().count();
        require(headers == categories, "Unexpected empty or duplicated keybind sections: " + headers);
        require(!EpicFightKeyMappings.ATTACK.isUnbound() && !EpicFightKeyMappings.GUARD.isUnbound(), "Combat inputs were disabled");
        require(EpicFightKeyMappings.SWITCH_MODE.isUnbound() && EpicFightKeyMappings.SKILL_EDIT.isUnbound(), "Duplicate shortcuts active");
    }

    private static void capture(String name) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        var folder = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(folder);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(folder.resolve(name));
        }
        LOGGER.info("Visual capture: {}", name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
