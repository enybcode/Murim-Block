package com.murimblock;

import com.murimblock.mob.*;
import com.murimblock.mob.ScarletGuardianEntity.Art;
import com.murimblock.martial.BloodCrystalEntity;
import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Real registered guardian, server AI, existing Sanguine projectiles and native framebuffer. No mesh storyboard. */
@EventBusSubscriber(modid=Murimblock.MOD_ID,value=Dist.CLIENT)
public final class ScarletGuardianTour {
    private static final boolean HEALTH_BAR=Boolean.getBoolean("murimblock.scarletGuardianHealthTour");
    private static final String[] TAKES=HEALTH_BAR?new String[]{"05-health-bar"}:new String[]{"01-crystal","02-scarlet-tear","03-form-barrage","04-blood-temple"};
    private static final String[] TITLES={"BLOOD CRYSTAL / charge and impact","SCARLET TEAR / summon, cuts, throw","CRYSTAL FORM / dorsal barrage","BLOOD ICEBERG / temporary temple"};
    private static boolean creating,built,finished,capture;
    private static volatile boolean ready;
    private static volatile String error;
    private static int warmup,take,frame;
    private static long started,lastCapture;
    private static Path root;
    private static Marker camera;
    private static ScarletGuardianEntity guardian;
    private static Zombie opponent;
    private static final StringBuilder REPORT=new StringBuilder(HEALTH_BAR
        ?"Native Minecraft framebuffer. Vanilla boss HUD. Live combat against a 1000 HP test zombie with 18 attack damage.\n"
        :"Native Minecraft framebuffer. Registered guardian and server AI. Target is a stationary test dummy.\n");
    private static boolean enabled(){return Boolean.getBoolean("murimblock.scarletGuardianTour");}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!enabled()||finished)return;var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        try {
            if(error!=null)throw new IllegalStateException(error);
            if(mc.player==null||mc.getSingleplayerServer()==null){
                if(mc.screen!=null&&mc.screen.getClass().getSimpleName().contains("Onboarding"))mc.setScreen(new TitleScreen(false));
                if(!creating&&mc.screen instanceof TitleScreen){
                    creating=true;String name="guardian-review-"+System.currentTimeMillis();root=mc.gameDirectory.toPath().resolve("captures/"+name);Files.createDirectories(root);
                    mc.createWorldOpenFlows().createFreshLevel(name,new LevelSettings(name,GameType.SPECTATOR,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                        new WorldOptions(1,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
                }return;
            }
            mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getToasts().clear();
            yesman.epicfight.config.ClientConfig.enableTargetEntityGuide=false;
            yesman.epicfight.config.ClientConfig.showTargetIndicator=false;
            if(!built){built=true;mc.options.hideGui=!HEALTH_BAR;mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.gamma().set(.7);mc.options.fov().set(58);
                mc.getSingleplayerServer().execute(()->build(mc.getSingleplayerServer()));}
            if(!ready)return;
            if(camera==null){camera=new Marker(EntityType.MARKER,mc.level);mc.setCameraEntity(camera);}
            if(++warmup<80)return;
            if(warmup==80){started=System.nanoTime();lastCapture=0;int t=take;mc.getSingleplayerServer().execute(()->begin(mc.getSingleplayerServer(),t));}
            capture=true;
        }catch(Exception e){fail(mc,e);}
    }
    private static void build(MinecraftServer server){
        try{
            var l=server.overworld();l.setDayTime(6000);l.setWeatherParameters(6000,0,false,false);
            l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
            for(int x=-26;x<=26;x++)for(int z=-26;z<=26;z++){
                boolean ring=Math.abs(x)==23||Math.abs(z)==23;
                l.setBlock(new BlockPos(x,-61,z),(ring?Blocks.RED_NETHER_BRICKS:(x%6==0||z%6==0)?Blocks.POLISHED_BLACKSTONE:Blocks.DEEPSLATE_TILES).defaultBlockState(),3);
            }
            for(int x=-25;x<=25;x++)for(int y=0;y<8;y++)l.setBlock(new BlockPos(x,-60+y,-25),
                (x%6==0?Blocks.CHISELED_POLISHED_BLACKSTONE:Blocks.DEEPSLATE_BRICKS).defaultBlockState(),3);
            for(int x:new int[]{-18,-12,-6,6,12,18}){
                for(int y=0;y<10;y++)l.setBlock(new BlockPos(x,-60+y,-22),Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),3);
                for(int y=3;y<8;y++)l.setBlock(new BlockPos(x,-60+y,-21),Blocks.RED_WOOL.defaultBlockState(),3);
                l.setBlock(new BlockPos(x,-50,-22),Blocks.GOLD_BLOCK.defaultBlockState(),3);
            }
            for(int x:new int[]{-10,10})for(int z:new int[]{-10,12}){
                l.setBlock(new BlockPos(x,-60,z),Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(),3);
                l.setBlock(new BlockPos(x,-59,z),Blocks.LANTERN.defaultBlockState(),3);
            }
            server.getPlayerList().getPlayers().getFirst().teleportTo(l,4,-54,6,180,0);ready=true;
        }catch(Exception e){error=e.toString();}
    }
    private static void begin(MinecraftServer server,int shot){
        try{
            var l=server.overworld();
            guardian=MurimEntities.SCARLET_GUARDIAN.get().create(l);guardian.moveTo(0,-60,0,0,0);l.addFreshEntity(guardian);
            // The camera chooses the starting distance/health; the real AI chooses and executes the technique.
            opponent=EntityType.ZOMBIE.create(l);opponent.setNoAi(!HEALTH_BAR);opponent.setPersistenceRequired();
            opponent.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);opponent.setHealth(1000);
            opponent.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);opponent.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));
            opponent.moveTo(0,-60,HEALTH_BAR||shot==1?3:9,180,0);l.addFreshEntity(opponent);
            if(HEALTH_BAR){opponent.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(18);opponent.setTarget(guardian);}
            if(shot==2)guardian.setHealth(guardian.getMaxHealth()*.58F);
            if(shot==3)guardian.setHealth(guardian.getMaxHealth()*.29F);
            guardian.setTarget(opponent);
            REPORT.append(TAKES[shot]).append(" starts: health=").append(guardian.getHealth()).append(", distance=").append(guardian.distanceTo(opponent)).append('\n');
        }catch(Exception e){error=e.toString();}
    }
    @SubscribeEvent public static void pre(RenderFrameEvent.Pre e){
        if(!enabled()||camera==null||finished)return;
        Vec3 eye=take==3?new Vec3(29,-34,35):new Vec3(8,-54.5,11);
        Vec3 aim=take==3?new Vec3(0,-56,0):new Vec3(0,-58,3);
        Vec3 d=aim.subtract(eye);camera.setPos(eye);camera.xo=camera.xOld=eye.x;camera.yo=camera.yOld=eye.y;camera.zo=camera.zOld=eye.z;
        float yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z)),pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z)));
        camera.setYRot(yaw);camera.yRotO=yaw;camera.setXRot(pitch);camera.xRotO=pitch;
    }
    @SubscribeEvent public static void caption(RenderGuiEvent.Post event){
        if(!enabled()||camera==null||finished)return;var g=event.getGuiGraphics();int w=g.guiWidth(),h=g.guiHeight();
        g.fill(0,h-20,w,h,0xff161217);
        if(HEALTH_BAR){g.drawString(Minecraft.getInstance().font,"BOSS HEALTH / live test duel",10,h-14,0xffe6c9bf,false);return;}
        g.fill(0,0,w,21,0xff161217);
        g.drawString(Minecraft.getInstance().font,"GUARDIAN OF THE SCARLET THRESHOLD",10,7,0xffe6c9bf,false);
        g.drawString(Minecraft.getInstance().font,TITLES[take]+" / in-game AI",10,h-14,0xffe6c9bf,false);
    }
    @SubscribeEvent public static void capture(RenderFrameEvent.Post e){
        if(!enabled()||!capture||finished)return;var mc=Minecraft.getInstance();
        long now=System.nanoTime();if(now-lastCapture<100_000_000L)return;lastCapture=now;
        try{
            var path=root.resolve(TAKES[take]+String.format("/frame-%04d.png",frame++));Files.createDirectories(path.getParent());
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(path);}
            Files.writeString(root.resolve(TAKES[take]+"/timing.csv"),(frame-1)+","+(now-started)/1_000_000L+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            if(frame%20==0){int t=take,f=frame;mc.getSingleplayerServer().execute(()->{
                REPORT.append(TAKES[t]).append(" frame ").append(f).append(": ").append(guardian.currentArt()).append(" boss health=").append(guardian.getHealth()).append('/').append(guardian.getMaxHealth()).append(" target health=").append(opponent.getHealth()).append(" crystals=").append(guardian.crystals()).append('\n');
            });}
            if(now-started>=12_000_000_000L){
                int shot=take;mc.getSingleplayerServer().execute(()->{
                    REPORT.append(TAKES[shot]).append(" final damage=").append(1000-opponent.getHealth()).append('\n');
                    guardian.discard();opponent.discard();
                    for(var entity:mc.getSingleplayerServer().overworld().getEntitiesOfClass(BloodCrystalEntity.class,guardian.getBoundingBox().inflate(60)))entity.discard();
                });
                frame=0;warmup=0;capture=false;
                if(++take==TAKES.length){finished=true;mc.getSingleplayerServer().execute(()->{
                    try{Files.writeString(root.resolve("report.txt"),REPORT.toString());Files.writeString(mc.gameDirectory.toPath().resolve("latest-capture.txt"),root.toAbsolutePath().toString());}
                    catch(Exception ex){com.mojang.logging.LogUtils.getLogger().error("Guardian report failed",ex);}
                    mc.execute(mc::stop);
                });}
            }
        }catch(Exception exception){fail(mc,exception);}
    }
    private static void fail(Minecraft mc,Exception e){finished=true;com.mojang.logging.LogUtils.getLogger().error("Guardian tour failed",e);mc.stop();}
}
