package com.murimblock;

import com.murimblock.client.EpicFightControls;
import com.murimblock.client.MurimblockKeyMappings;
import com.murimblock.client.gui.MurimProfileScreen;
import com.murimblock.combat.CombatService;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.EpicFightContentPolicy;
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
import yesman.epicfight.config.ClientConfig;

/** Opt-in real-client rendering checks. Never loads into a published jar or the normal IntelliJ run. */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class ClientVisualSmoke {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static int ticks;
    private static int bootTicks;
    private static boolean initialized;
    private static boolean failed;
    private static final AtomicBoolean ready = new AtomicBoolean();
    private static volatile java.util.UUID opponentId;

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
                    EpicFightCombatDefaults.enforce(player);
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
                    require(mc.screen.children().size() == 5, "Native technique library widgets remained");
                    var patch = EpicFightBridge.patch(mc.player);
                    require(patch.getSkill(yesman.epicfight.skill.SkillSlots.GUARD).getSkill() == EpicFightSkills.GUARD.get(), "Default guard not synchronized");
                    require(patch.getSkill(yesman.epicfight.skill.SkillSlots.DODGE).getSkill() == EpicFightSkills.ROLL.get(), "Default dodge not synchronized");
                }
                case 115 -> capture("02-techniques.png");
                case 125 -> mc.setScreen(new SkillBookScreen(mc.player, EpicFightSkills.GUARD.get(), null, null));
                case 127 -> require(mc.screen instanceof MurimProfileScreen, "Native skill book GUI remained open");
                case 140 -> { mc.setScreen(new MurimProfileScreen()); clickButton("Cultivation"); }
                case 155 -> capture("03-cultivation.png");
                case 165 -> { mc.setScreen(new MurimProfileScreen()); clickButton("Info"); }
                case 180 -> capture("04-info.png");
                case 190 -> clickButton("Combat Settings");
                case 192 -> {
                    checkToggle("Blood effects:", () -> ClientConfig.bloodEffects);
                    checkToggle("Camera motion:", () -> ClientConfig.enableFirstPersonCameraMove);
                    checkToggle("Auto perspective:", () -> ClientConfig.autoPerspectiveSwithing);
                    checkToggle("First-person body:", () -> ClientConfig.enableAnimatedFirstPersonModel);
                    checkToggle("Lock-on snapping:", () -> ClientConfig.lockOnSnapping);
                    var before = ClientConfig.tpsType;
                    clickButton("Camera:");
                    require(ClientConfig.tpsType != before, "Camera mode did not change");
                    for (int count = 0; ClientConfig.tpsType != before && count < 10; count++) clickButton("Camera:");
                    require(ClientConfig.tpsType == before, "Camera mode did not restore");
                    mc.screen.setFocused(null);
                }
                case 205 -> capture("05-settings.png");
                case 210 -> {
                    clickButton("Keybinds");
                    require(mc.screen instanceof net.minecraft.client.gui.screens.options.controls.ControlsScreen,
                            "The GUI Keybinds action did not open controls");
                }
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
                case 265 -> {
                    var patch = EpicFightBridge.patch(mc.player);
                    patch.setStamina(0);
                    require(patch.getStamina() == patch.getMaxStamina(), "Client still reads depleted stamina");
                    require(patch.hasStamina(patch.getMaxStamina() + 100), "Client still limits actions by stamina");
                    capture("07-hud.png");
                }
                case 280 -> {
                    mc.getWindow().setWindowed(960, 720);
                    mc.options.guiScale().set(3);
                    mc.resizeDisplay();
                    mc.setScreen(MurimProfileScreen.techniques());
                }
                case 295 -> capture("08-small-scale.png");
                case 300 -> {
                    mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().getPlayerList()
                            .getPlayer(mc.player.getUUID()).setGameMode(GameType.CREATIVE));
                }
                case 305 -> {
                    require(mc.gameMode.hasInfiniteItems(), "Creative mode switch was not synchronized");
                    net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(mc.level.enabledFeatures(), true, mc.level.registryAccess());
                    for (var tab : net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB) {
                        require(tab.getDisplayItems().stream().noneMatch(EpicFightContentPolicy::blocked), "Native creative item remained");
                        require(tab.getSearchTabDisplayItems().stream().noneMatch(EpicFightContentPolicy::blocked), "Native creative search item remained");
                    }
                    mc.setScreen(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player,
                            mc.level.enabledFeatures(), true));
                }
                case 315 -> {
                    require(mc.screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen,
                            "Creative inventory was replaced by survival inventory");
                    capture("09-creative.png");
                }
                case 320 -> {
                    mc.setScreen(new MurimProfileScreen());
                    clickButton("Close");
                    require(mc.screen == null, "Close button did not close the GUI");
                    mc.setScreen(new MurimProfileScreen());
                    mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_K, 0, 0);
                    require(mc.screen == null, "Profile key did not close the GUI");
                    mc.setScreen(new MurimProfileScreen());
                    mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_E, 0, 0);
                    require(mc.screen == null, "Inventory key did not close the GUI");
                    mc.setScreen(new MurimProfileScreen());
                    mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE, 0, 0);
                    require(mc.screen == null, "Escape did not close the GUI");
                    mc.options.guiScale().set(2);
                    mc.getWindow().setWindowed(1280, 720);
                    mc.resizeDisplay();
                    mc.setScreen(new MurimProfileScreen());
                }
                case 335 -> capture("10-profile-large.png");
                case 340 -> clickButton("Cultivation");
                case 355 -> capture("11-cultivation-large.png");
                case 360 -> clickButton("Info");
                case 365 -> clickButton("Combat Settings");
                case 380 -> capture("12-settings-large.png");
                case 390 -> mc.getSingleplayerServer().execute(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    var mob = com.murimblock.mob.MurimEntities.TRAINING_OPPONENT.get().create(level);
                    mob.setNoGravity(true);
                    mob.moveTo(1.5, 80, 0.5, 0, 0);
                    require(level.addFreshEntity(mob), "Custom mob spawn failed");
                    mob.setNoAi(true);
                    opponentId = mob.getUUID();
                });
                case 410 -> {
                    var mob = mc.level.entitiesForRendering().iterator();
                    net.minecraft.world.entity.LivingEntity opponent = null;
                    while (mob.hasNext()) {
                        var candidate = mob.next();
                        if (candidate.getUUID().equals(opponentId)) opponent = (net.minecraft.world.entity.LivingEntity) candidate;
                    }
                    require(opponent != null, "Custom mob was not tracked on the real client");
                    var patch = yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(opponent,
                            yesman.epicfight.world.capabilities.entitypatch.mob.ZombiePatch.class);
                    require(patch != null, "Custom client mob patch missing");
                    require(yesman.epicfight.client.events.engine.RenderEngine.getInstance().hasRendererFor(opponent),
                            "Custom mob animated renderer missing");
                    mc.setScreen(new MobPreviewScreen(opponent));
                }
                case 415 -> capture("13-mob-idle.png");
                case 420 -> mc.getSingleplayerServer().execute(() -> {
                    var mob = mc.getSingleplayerServer().overworld().getEntity(opponentId);
                    var patch = yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mob,
                            yesman.epicfight.world.capabilities.entitypatch.mob.ZombiePatch.class);
                    patch.playAnimationSynchronized(yesman.epicfight.gameasset.Animations.BIPED_MOB_ONEHAND1, 0);
                });
                case 430 -> capture("14-mob-windup.png");
                case 433 -> capture("15-mob-contact.png");
                case 455 -> {
                    ClientCaptureChecks.verify(mc.gameDirectory.toPath().resolve("screenshots"));
                    Files.writeString(mc.gameDirectory.toPath().resolve("visual-smoke-passed.txt"),
                            "Clear Manuscript: four pages and settings; button/K/E/Escape close; all six settings toggled and restored; keybind action; compact/large screenshots. Custom mob tracking, Epic Fight patch/renderer and synchronized attack captures. Techniques empty; native screens replaced; guard/roll synchronized; one key category; native items absent; stamina disabled.\n");
                    mc.getSingleplayerServer().execute(() -> {
                        var mob = mc.getSingleplayerServer().overworld().getEntity(opponentId);
                        if (mob != null) mob.discard();
                    });
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

    private static void clickButton(String message) {
        Screen screen = Minecraft.getInstance().screen;
        for (var child : screen.children()) {
            if (child instanceof Button button && button.getMessage().getString().startsWith(message)) {
                require(screen.mouseClicked(button.getX() + button.getWidth() / 2.0,
                        button.getY() + button.getHeight() / 2.0, 0), "Button did not handle click: " + message);
                return;
            }
        }
        throw new IllegalStateException("Missing button: " + message);
    }

    private static void checkToggle(String label, java.util.function.BooleanSupplier value) {
        boolean before = value.getAsBoolean();
        clickButton(label);
        require(value.getAsBoolean() != before, "Setting did not toggle: " + label);
        clickButton(label);
        require(value.getAsBoolean() == before, "Setting did not restore: " + label);
    }

    private static final class MobPreviewScreen extends Screen {
        private final net.minecraft.world.entity.LivingEntity mob;
        MobPreviewScreen(net.minecraft.world.entity.LivingEntity mob) { super(mob.getName()); this.mob = mob; }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void renderBackground(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float tick) {
            g.fill(0, 0, width, height, 0xFFF0E7CE);
            g.drawCenteredString(font, title, width / 2, 16, 0xFF202522);
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(g,
                    width / 2 - 90, 40, width / 2 + 90, height - 20, 80, 0.0625F, width / 2.0F, height / 2.0F, mob);
        }
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
        require(visible == 10, "Expected 10 useful mappings in a single Murim category, got " + visible);
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
