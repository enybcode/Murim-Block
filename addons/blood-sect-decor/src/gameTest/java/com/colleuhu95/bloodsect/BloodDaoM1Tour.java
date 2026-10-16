package com.colleuhu95.bloodsect;

import com.colleuhu95.bloodsect.ninja.BloodNinja;
import java.nio.file.Files;
import java.util.HashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in graphical regression tour. Never included in the player JAR. */
@EventBusSubscriber(modid=BloodSectDecor.ID,value=Dist.CLIENT)
public final class BloodDaoM1Tour {
    private static boolean creating,finished;
    private static int ticks;
    private static volatile BloodNinja ninja;
    private static Cow cow;
    private static Marker camera;
    private static final HashSet<Integer> captured=new HashSet<>();
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("bloodsect.daoM1Tour") || finished) return;
        var mc=Minecraft.getInstance();
        mc.options.pauseOnLostFocus=false;
        try {
            if(mc.player==null || mc.getSingleplayerServer()==null) {
                if(mc.screen!=null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if(!creating && mc.screen instanceof TitleScreen) {
                    creating=true;
                    var settings=new LevelSettings("Blood Dao M1 Render Check",GameType.CREATIVE,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("blood-dao-m1-check-"+System.currentTimeMillis(),settings,new WorldOptions(1L,false,false),
                        r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
                }
                return;
            }
            ticks++;
            mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.options.hideGui=true;mc.getToasts().clear();
            var server=mc.getSingleplayerServer();
            if(ticks==15) server.execute(()->{
                var level=server.overworld();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
                for(int x=-5;x<12;x++) for(int z=-6;z<8;z++) level.setBlockAndUpdate(new BlockPos(x,-61,z),Blocks.SMOOTH_SANDSTONE.defaultBlockState());
                var self=server.getPlayerList().getPlayer(mc.player.getUUID());
                self.teleportTo(level,2.5,-60,-5,0,0);
                ninja=BloodSectDecor.BLOOD_NINJA.get().create(level);
                ninja.moveTo(.5,-60,.5,-90,0);ninja.setNoAi(true);ninja.setHealth(10);ninja.setGuardPost(ninja.blockPosition());level.addFreshEntity(ninja);
                cow=EntityType.COW.create(level);cow.moveTo(4.5,-60,.5);cow.setNoAi(true);
                cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);level.addFreshEntity(cow);
            });
            if(ticks==40) {
                camera=new Marker(EntityType.MARKER,mc.level);
                camera.setPos(.2,-58.5,-4.5);camera.xo=camera.xOld=camera.getX();camera.yo=camera.yOld=camera.getY();camera.zo=camera.zOld=camera.getZ();
                camera.setYRot(-12F);camera.yRotO=camera.getYRot();camera.setXRot(7);camera.xRotO=7;
                mc.options.fov().set(55);mc.setCameraEntity(camera);
            }
            if(camera!=null && mc.getCameraEntity()!=camera) mc.setCameraEntity(camera);
            if(ticks==65) server.execute(()->{
                ninja.setOnGround(true);ninja.setTarget(cow);
                var goal=(MeleeAttackGoal)ninja.goalSelector.getAvailableGoals().stream().map(w->w.getGoal()).filter(g->g instanceof MeleeAttackGoal).findFirst().orElseThrow();
                goal.start();goal.tick();
            });
            if(ticks==110) server.execute(()->{
                ninja.moveTo(.5,-60,.5,-90,0);ninja.setHealth(10);cow.moveTo(2.2,-60,.5);cow.setHealth(10);
            });
            if(ticks==125) server.execute(()->{
                ninja.setOnGround(true);ninja.setTarget(cow);
                var goal=(MeleeAttackGoal)ninja.goalSelector.getAvailableGoals().stream().map(w->w.getGoal()).filter(g->g instanceof MeleeAttackGoal).findFirst().orElseThrow();
                goal.start();goal.tick();
            });
            if(ticks>=125 && ticks<=160 && ninja!=null) {
                var client=mc.level.getEntity(ninja.getId());
                if(client instanceof BloodNinja n) {
                    int age=(int)n.getDaoAttackAge(0);
                    if(n.getDaoAttackVariant()==1 && age>=0 && age<19 && captured.add(age)) capture(mc,"m1-"+String.format("%02d",age)+".png");
                }
            }
            if(ticks==175) {
                boolean ok=ninja!=null && Math.abs(ninja.getX()-.5)<.1 && ninja.getHealth()==10 && cow.getHealth()==6 && captured.size()>=10;
                Files.writeString(mc.gameDirectory.toPath().resolve("dao-m1-tour-report.txt"),
                    (ok?"PASSED":"FAILED")+"; distance="+(ninja==null?-1:ninja.getX()-.5)+"; health="+(ninja==null?-1:ninja.getHealth())+"; victimHealth="+cow.getHealth()+"; captured="+captured+"\n");
                finished=true;mc.stop();
            }
        } catch(Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Blood Dao M1 tour failed",e);finished=true;mc.stop();
        }
    }
    private static void capture(Minecraft mc,String name) throws Exception {
        var path=mc.gameDirectory.toPath().resolve("screenshots-1.10.1").resolve(name);Files.createDirectories(path.getParent());
        var image=Screenshot.takeScreenshot(mc.getMainRenderTarget());
        net.minecraft.Util.ioPool().execute(()->{try(image){image.writeToFile(path);}catch(Exception e){throw new RuntimeException(e);}});
    }
}
