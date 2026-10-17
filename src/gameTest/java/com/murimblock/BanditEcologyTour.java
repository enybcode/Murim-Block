package com.murimblock;

import com.murimblock.bloodsect.BloodSect;
import com.murimblock.mob.MurimEntities;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Genuine AI fights in a disposable client world, filmed at 20 fps; no forced targets, poses, hits or loot. */
@EventBusSubscriber(modid=Murimblock.MOD_ID, value=Dist.CLIENT)
public final class BanditEcologyTour {
    private static final String[] IDS={"bandit_alliance","bandit_culte","bandit_tigre"};
    private static final String[] TITLES={"Bandit vs Orthodox Alliance","Bandit vs Blood Soldier","Bandit vs Tiger - animals spared"};
    private static boolean creating, finished, building;
    private static volatile boolean ready;
    private static volatile String failure;
    private static int scene, age, frames;
    private static Marker camera;
    private static Mob bandit, defender;
    private static Cow cow;
    private static boolean targeted, hit;
    private static Path root;
    private static boolean enabled(){return Boolean.getBoolean("murimblock.banditEcologyTour");}

    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        if(!enabled()||finished)return;
        var mc=Minecraft.getInstance(); mc.options.pauseOnLostFocus=false;
        try{
            if(failure!=null)throw new IllegalStateException(failure);
            if(mc.player==null||mc.getSingleplayerServer()==null){
                if(mc.screen!=null&&mc.screen.getClass().getSimpleName().contains("Onboarding"))mc.setScreen(new TitleScreen(false));
                if(!creating&&mc.screen instanceof TitleScreen){
                    creating=true;String name="bandit-ecology-"+System.currentTimeMillis();
                    root=mc.gameDirectory.toPath().resolve("captures/"+name);Files.createDirectories(root);
                    mc.createWorldOpenFlows().createFreshLevel(name,
                            new LevelSettings(name,GameType.SPECTATOR,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                            new WorldOptions(1,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
                }
                return;
            }
            mc.options.hideGui=true;mc.options.guiScale().set(2);mc.options.fov().set(50);
            mc.options.renderDistance().set(5);mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getToasts().clear();
            var server=mc.getSingleplayerServer();
            if(!building){building=true;server.execute(()->prepare(mc));}
            if(!ready)return;
            if(camera==null){camera=new Marker(EntityType.MARKER,mc.level);mc.setCameraEntity(camera);}
            placeCamera();
            age++;
            if(age==81)server.execute(()->{bandit.setNoAi(false);defender.setNoAi(false);});
            if(age>=61&&age<221){
                Path file=root.resolve(IDS[scene]+String.format("/frame-%04d.png",frames++));Files.createDirectories(file.getParent());
                try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(file);}
                server.execute(()->{
                    targeted|=bandit.getTarget()==defender||defender.getTarget()==bandit;
                    hit|=bandit.getHealth()<bandit.getMaxHealth()||defender.getHealth()<defender.getMaxHealth();
                });
            }
            if(age==221){
                ready=false;int done=scene;int count=frames;
                server.execute(()->{
                    try{
                        if(!targeted||!hit||cow.getHealth()<cow.getMaxHealth())throw new IllegalStateException("Fight verification failed: "+IDS[done]+" target="+targeted+" hit="+hit+" cow="+cow.getHealth());
                        Files.writeString(root.resolve(IDS[done]+"-complete.txt"),"PASSED\nNative Minecraft framebuffer\nFrames: "+count+"\nFPS: 20\nAutomatic targets and real damage: true\nCow spared: true\nBandit health: "+bandit.getHealth()+"\nDefender health: "+defender.getHealth()+"\n");
                        bandit.discard();defender.discard();cow.discard();
                        mc.execute(()->{
                            scene++;frames=0;age=0;building=false;
                            if(scene==IDS.length){
                                try{Files.writeString(root.resolve("report.txt"),"PASSED\nThree real AI fights; 160 native frames each at 20 FPS.\n");Files.writeString(mc.gameDirectory.toPath().resolve("latest-capture.txt"),root.toAbsolutePath().toString());}
                                catch(Exception ex){failure=ex.toString();}
                                finished=true;mc.setCameraEntity(mc.player);mc.stop();
                            }
                        });
                    }catch(Exception ex){failure=ex.toString();}
                });
            }
        }catch(Exception ex){
            finished=true;com.mojang.logging.LogUtils.getLogger().error("Bandit ecology tour FAILED",ex);
            try{if(root!=null)Files.writeString(root.resolve("report.txt"),"FAILED\n"+ex);}catch(Exception ignored){}
            mc.stop();
        }
    }

    private static void prepare(Minecraft mc){
        try{
            var server=mc.getSingleplayerServer();var level=server.overworld();int ox=scene*40;
            level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-9;x<=9;x++)for(int z=-8;z<=8;z++)
                level.setBlock(new BlockPos(ox+x,-61,z),(Math.abs(x)==9||Math.abs(z)==8?Blocks.POLISHED_ANDESITE:Blocks.SMOOTH_SANDSTONE).defaultBlockState(),3);
            for(int x=-9;x<=9;x++)for(int y=0;y<3;y++)level.setBlock(new BlockPos(ox+x,-60+y,8),Blocks.SPRUCE_PLANKS.defaultBlockState(),3);
            for(int x:new int[]{-8,8}){
                for(int y=0;y<4;y++)level.setBlock(new BlockPos(ox+x,-60+y,7),Blocks.DARK_OAK_LOG.defaultBlockState(),3);
                level.setBlock(new BlockPos(ox+x,-56,7),Blocks.LANTERN.defaultBlockState(),3);
            }
            var player=server.getPlayerList().getPlayers().getFirst();player.teleportTo(level,ox+4,-57,-7,30,12);
            bandit=MurimEntities.MOUNTAIN_BANDIT.get().spawn(level,new BlockPos(ox-2,-60,0),MobSpawnType.COMMAND);
            EntityType<? extends Mob> opponent=scene==0?MurimEntities.ALLIANCE_SWORD_DISCIPLE.get():scene==1?BloodSect.BLOOD_SOLDIER.get():MurimEntities.TIGER.get();
            defender=opponent.spawn(level,new BlockPos(ox+2,-60,0),MobSpawnType.COMMAND);
            bandit.setNoAi(true);defender.setNoAi(true);bandit.setPersistenceRequired();defender.setPersistenceRequired();
            bandit.setYRot(-90);bandit.setYHeadRot(-90);bandit.yBodyRot=-90;
            defender.setYRot(90);defender.setYHeadRot(90);defender.yBodyRot=90;
            cow=EntityType.COW.spawn(level,new BlockPos(ox,-60,5),MobSpawnType.COMMAND);cow.setNoAi(true);cow.setPersistenceRequired();
            targeted=false;hit=false;ready=true;
        }catch(Exception ex){failure=ex.toString();}
    }
    private static void placeCamera(){
        Vec3 p=new Vec3(scene*40+5,-57.2,-7.8),aim=new Vec3(scene*40,-59.1,.5),d=aim.subtract(p);
        camera.setPos(p);camera.xo=camera.xOld=p.x;camera.yo=camera.yOld=p.y;camera.zo=camera.zOld=p.z;
        float yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z)),pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z)));
        camera.setYRot(yaw);camera.yRotO=yaw;camera.setXRot(pitch);camera.xRotO=pitch;
    }
    @SubscribeEvent public static void caption(RenderGuiEvent.Post e){
        if(!enabled()||finished||!ready)return;
        var g=e.getGuiGraphics();int w=g.guiWidth(),h=g.guiHeight();g.fill(0,0,w,18,0xff171416);g.fill(0,h-18,w,h,0xff171416);
        g.drawString(Minecraft.getInstance().font,TITLES[scene],8,5,0xffead9c6,false);
        g.drawString(Minecraft.getInstance().font,"Minecraft 1.21.1 / live AI combat",8,h-13,0xffead9c6,false);
    }
}
