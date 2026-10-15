package com.murimblock;

import net.minecraft.client.Screenshot;

import com.murimblock.combat.CombatService;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.*;
import com.murimblock.qi.QiService;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.client.input.EpicFightKeyMappings;

/** Development-only native client: actual key input, skin overlay, first person and Photon recording. */
@EventBusSubscriber(modid=Murimblock.MOD_ID,value=Dist.CLIENT)
public final class SanguineTour {
    private static boolean creating,finished,m1Seen,throwSeen,barrageSeen;
    private static int shardsBefore;
    private static int ticks;
    private static int attackReleaseAt;
    private static Marker camera;
    private static Zombie zombie;
    private static Vec3 start;
    private static float beforeSalvo;
    private static final List<String> report=new CopyOnWriteArrayList<>();
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("murimblock.sanguineTour")||finished)return;
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(mc.screen instanceof PauseScreen)mc.setScreen(null);
        try {
            if(mc.player==null||mc.getSingleplayerServer()==null) {
                if(mc.screen!=null&&mc.screen.getClass().getSimpleName().contains("Onboarding"))mc.setScreen(new TitleScreen(false));
                if(!creating&&mc.screen instanceof TitleScreen) {
                    creating=true;var settings=new LevelSettings("crystal-preview",GameType.SURVIVAL,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("crystal-preview-"+System.currentTimeMillis(),settings,new WorldOptions(1L,false,false),
                        registries->registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
                }
                return;
            }
            ticks++;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getToasts().clear();
            var server=mc.getSingleplayerServer();
            if(ticks==40&&(!CombatService.isInCombatMode(mc.player)||!SanguineArt.guardsBareHanded(mc.player))) {ticks--;return;}
            if(attackReleaseAt>0&&ticks>=attackReleaseAt) {
                EpicFightKeyMappings.ATTACK.setDown(false);mc.options.keyAttack.setDown(false);attackReleaseAt=0;
            }
            if(camera!=null) {
                mc.setCameraEntity(camera);
                mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(mc.player.getX(),mc.player.getY(),mc.player.getZ(),mc.player.getYRot(),mc.player.getXRot(),mc.player.onGround()));
            }
            if(ticks>=65&&ticks<86)EpicFightKeyMappings.GUARD.setDown(true);
            switch(ticks) {
                case 20 -> server.execute(()->{
                    var level=server.overworld();level.setDayTime(6000);
                    level.getGameRules().getRule(yesman.epicfight.world.gamerule.EpicFightGameRules.STIFF_COMBO_ATTACKS.getRuleKey()).set(false,server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
                    var p=self(server);new ItemStack(MurimItems.SANGUINE_ART_SCROLL.get()).use(level,p,InteractionHand.MAIN_HAND);
                    p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                    var ts=List.of(MartialArts.SCARLET_TEAR,MartialArts.BLOOD_CRYSTAL_FORM,MartialArts.CRYSTAL_BARRAGE,MartialArts.BLOOD_ICEBERG);
                    for(int i=0;i<ts.size();i++){MartialService.unlock(p,ts.get(i));MartialService.assign(p,i+1,ts.get(i).id());}
                    CombatService.setCombatMode(p,true);QiService.setQiMax(p,100);QiService.setQi(p,100);
                    p.teleportTo(level,Math.floor(p.getX())+.5,p.getY(),Math.floor(p.getZ())+.5,0,0);p.getFoodData().setFoodLevel(6);
                    check(MartialService.data(p).knowsArt(MartialArts.SANGUINE_ART),"scroll teaches Sanguine Art");
                });
                case 40 -> {
                    yesman.epicfight.config.ClientConfig.showTargetIndicator=false;yesman.epicfight.config.ClientConfig.enableTargetEntityGuide=false;
                    // The recording camera can point at terrain; Adaptive would treat empty-hand input as mining.
                    yesman.epicfight.config.ClientConfig.playerBehaviorStrategy=yesman.epicfight.config.ClientConfig.PlayerBehaviorStrategy.SWITCHING_MODE;
                    start=mc.player.position();view(mc,3.2,1.8,1.1);server.execute(()->target(server,1.3));
                }
                case 46,200,225,250,323,348,373,398,423,570 -> {
                    server.execute(()->{var p=self(server);zombie.moveTo(p.getX(),p.getY(),p.getZ()+1.3,180,0);zombie.setDeltaMovement(Vec3.ZERO);});
                    EpicFightKeyMappings.ATTACK.setDown(true);mc.options.keyAttack.setDown(true);
                    KeyMapping.click(EpicFightKeyMappings.ATTACK.getKey());attackReleaseAt=ticks+2;
                }
                case 61 -> {check(m1Seen,"attack key uses Sanguine M1");}
                case 78 -> {check(EpicFightBridge.patch(mc.player).currentCompositeMotion==MurimAnimations.Motions.SANGUINE_GUARD,"guard key uses Sanguine guard");capture(mc,"guard.png");}
                case 86 -> EpicFightKeyMappings.GUARD.setDown(false);
                case 92,150 -> server.execute(()->{MartialService.resetCooldowns(self(server));QiService.setQi(self(server),100);});
                case 94,154 -> press(mc,1);
                case 96,156 -> mc.options.keyHotbarSlots[1].setDown(false);
                case 195 -> server.execute(()->check(SanguineArt.state(self(server)).gauge()==4,"one M1 and a landed sword throw build four gauge points: "+SanguineArt.state(self(server))));
                // The gauge as drops over the weapon slot: four lit, then all twenty bright.
                case 197 -> capture(mc,"gauge-four.png");
                // Three more M1 make seven; the rest of the gauge is topped up for the form.
                case 262 -> server.execute(()->{var p=self(server);p.setData(com.murimblock.martial.BloodCrystalAttachments.STATE,new com.murimblock.martial.BloodCrystalState(com.murimblock.martial.BloodCrystalState.MAX_GAUGE,false,0));});
                case 268 -> server.execute(()->{check(SanguineArt.state(self(server)).gauge()==com.murimblock.martial.BloodCrystalState.MAX_GAUGE&&!SanguineArt.state(self(server)).active(),"full gauge waits for manual activation: "+SanguineArt.state(self(server)));QiService.setQi(self(server),100);});
                case 270 -> capture(mc,"gauge-full.png");
                case 274 -> press(mc,2);
                case 276 -> mc.options.keyHotbarSlots[2].setDown(false);
                case 303 -> {check(SanguineArt.state(mc.player).active(),"key 3 activates synchronized form");capture(mc,"form-front.png");}
                case 308 -> {camera=null;mc.setCameraEntity(mc.player);mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);}
                case 313 -> capture(mc,"form-first-person.png");
                case 319 -> view(mc,1.6,1.7,-3.4);
                case 445 -> {check(SanguineArt.state(mc.player).crystals()==5,"five landed M1 earn five synchronized dorsal crystals");capture(mc,"form-back-five-crystals.png");server.execute(()->{target(server,7);QiService.setQi(self(server),100);});}
                case 450 -> view(mc,3.3,1.5,0);
                case 454 -> press(mc,0);
                case 466 -> {check(SanguineArt.charging(mc.player),"key 1 holds the crystal charge");capture(mc,"hand-charge.png");}
                case 468 -> {camera=null;mc.setCameraEntity(mc.player);mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);}
                case 469 -> {check(com.murimblock.client.BloodCrystalRenderer.firstPersonCharges()>0,"first-person charge binds to the visible wrist pose");
                    var backs=java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(),false).filter(e->e instanceof com.murimblock.martial.BloodCrystalEntity c&&c.dorsal()&&c.visualOwner()==mc.player).map(e->(com.murimblock.martial.BloodCrystalEntity)e).toList();
                    check(!backs.isEmpty()&&backs.stream().noneMatch(c->((com.murimblock.client.BloodCrystalRenderer)mc.getEntityRenderDispatcher().getRenderer(c)).shouldRender(c,null,0,0,0)),"own back crystals stay out of the first-person view ("+backs.size()+")");capture(mc,"hand-charge-first-person.png");}
                case 470 -> mc.options.keyHotbarSlots[0].setDown(false);
                case 473 -> view(mc,3.3,1.5,0);
                case 498 -> server.execute(()->{check(zombie.getHealth()<500&&self(server).getHealth()==10,"thrown hand crystal hits without healing");beforeSalvo=zombie.getHealth();QiService.setQi(self(server),100);});
                case 504 -> press(mc,3);
                case 506 -> mc.options.keyHotbarSlots[3].setDown(false);
                case 550 -> {check(throwSeen,"release plays the throw animation");check(barrageSeen,"key 4 plays barrage animation");check(SanguineArt.state(mc.player).active()&&SanguineArt.state(mc.player).crystals()==0,"salvo consumes crystals and keeps form");capture(mc,"form-after-salvo.png");}
                case 558 -> server.execute(()->{check(zombie.getHealth()<beforeSalvo-1,"crystal salvo deals real damage");target(server,1.3);});
                case 592 -> {check(SanguineArt.state(mc.player).crystals()==1,"M1 regrows a crystal after the salvo");capture(mc,"form-regrown.png");}
                case 600 -> {com.murimblock.client.MurimblockKeyMappings.COMBAT_MODE.setDown(true);KeyMapping.click(com.murimblock.client.MurimblockKeyMappings.COMBAT_MODE.getKey());}
                case 602 -> com.murimblock.client.MurimblockKeyMappings.COMBAT_MODE.setDown(false);
                case 616 -> {check(SanguineArt.state(mc.player).active(),"V exits combat but the form holds");capture(mc,"combat-exit.png");}
                // The great crystal: a full gauge drawn into it while key 1 is held, then its explosion, filmed from the side.
                case 632 -> server.execute(()->{var p=self(server);MartialCombat.forget(p);CombatService.setCombatMode(p,true);target(server,9);});
                case 635 -> server.execute(()->{var p=self(server);
                    p.setData(com.murimblock.martial.BloodCrystalAttachments.STATE,new com.murimblock.martial.BloodCrystalState(com.murimblock.martial.BloodCrystalState.MAX_GAUGE,false,0));});
                case 636 -> viewAlong(mc,7.5,3.2,4.5);
                case 640 -> press(mc,0);
                // Twenty points at one every tenth of a second, past the first 0.6 s and the key reaching the server.
                case 694 -> server.execute(()->{var c=server.overworld().getEntitiesOfClass(com.murimblock.martial.BloodCrystalEntity.class,self(server).getBoundingBox().inflate(5)).stream()
                        .filter(e->e.state()==com.murimblock.martial.BloodCrystalEntity.CHARGING).findFirst().orElse(null);
                    check(c!=null&&c.great()&&SanguineArt.state(self(server)).gauge()==0,"holding key 1 draws the whole gauge into a great crystal: "+(c==null?"none":c.points()+" points, scale "+c.crystalScale()));});
                case 703 -> mc.options.keyHotbarSlots[0].setDown(false);
                case 745 -> server.execute(()->{check(zombie.getHealth()<500,"the great crystal strikes: "+zombie.getHealth());
                    check(com.murimblock.client.BloodCrystalEffects.shardsThrown()>0,"its explosion throws Photon crystal shards");});
                // Scarlet Tear's blood sword: summoned, fought with (two cuts), then thrown like a javelin on the second press.
                case 765 -> server.execute(()->{target(server,2.0);var p=self(server);MartialService.resetCooldowns(p);QiService.setQi(p,100);
                    p.setData(com.murimblock.martial.BloodCrystalAttachments.STATE,com.murimblock.martial.BloodCrystalState.EMPTY);});
                case 770 -> {view(mc,4.6,1.3,1.0);shardsBefore=com.murimblock.client.BloodCrystalEffects.shardsThrown();}
                case 774 -> press(mc,1);
                case 776 -> mc.options.keyHotbarSlots[1].setDown(false);
                case 796 -> {check(SanguineArt.holdsSword(mc.player),"Scarlet Tear summons a blood sword in the hand");
                    server.execute(()->check(MartialService.castSlot(self(server),2)==MartialService.CastResult.SWORD_OUT,"no other technique while the sword is out"));}
                case 801,814 -> {EpicFightKeyMappings.ATTACK.setDown(true);mc.options.keyAttack.setDown(true);KeyMapping.click(EpicFightKeyMappings.ATTACK.getKey());attackReleaseAt=ticks+2;}
                case 826 -> server.execute(()->check(zombie.getHealth()<500,"the sword cuts as a sword (M1): "+zombie.getHealth()));
                case 832 -> {server.execute(()->{target(server,7);QiService.setQi(self(server),100);});}
                case 836 -> press(mc,1);
                case 838 -> mc.options.keyHotbarSlots[1].setDown(false);
                case 870 -> {check(com.murimblock.client.BloodCrystalEffects.shardsThrown()>shardsBefore,"the second press throws the sword, which explodes");
                    server.execute(()->{check(zombie.getHealth()<500,"the thrown sword strikes: "+zombie.getHealth());MartialService.resetCooldowns(self(server));QiService.setQi(self(server),100);});}
                case 878 -> {camera=null;mc.setCameraEntity(mc.player);mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);}
                case 882 -> press(mc,1);
                case 884 -> mc.options.keyHotbarSlots[1].setDown(false);
                case 892 -> capture(mc,"sword-first-person-forming.png");
                case 912 -> capture(mc,"sword-first-person.png");
                case 914 -> press(mc,1);
                case 916 -> mc.options.keyHotbarSlots[1].setDown(false);
                case 919 -> capture(mc,"sword-first-person-throw.png");
                // The form again, then its end: the crystals come away from the body and fall.
                case 922 -> view(mc,3.4,1.5,2.2);
                case 940 -> server.execute(()->{var p=self(server);MartialService.resetCooldowns(p);QiService.setQi(p,100);
                    p.setData(com.murimblock.martial.BloodCrystalAttachments.STATE,new com.murimblock.martial.BloodCrystalState(com.murimblock.martial.BloodCrystalState.MAX_GAUGE,false,0));});
                case 945 -> press(mc,2);
                case 947 -> mc.options.keyHotbarSlots[2].setDown(false);
                case 992 -> {check(SanguineArt.state(mc.player).active(),"the form again, for its end");server.execute(()->SanguineArt.shed(self(server)));}
                case 1001 -> {var shed=java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(),false)
                        .filter(e->e instanceof com.murimblock.martial.BloodCrystalEntity c&&c.shed()).count();
                    check(shed>=10&&!SanguineArt.state(mc.player).active(),"at the form's end its crystals come away and fall: "+shed);}
                // Blood Iceberg: the mountain of blood crystal rises under the player, seen from afar.
                case 1045 -> server.execute(()->{var p=self(server);if(zombie!=null)zombie.discard();MartialService.resetCooldowns(p);QiService.setQi(p,100);});
                case 1047 -> view(mc,30,22,30);
                case 1052 -> press(mc,4);
                case 1054 -> mc.options.keyHotbarSlots[4].setDown(false);
                case 1100 -> server.execute(()->{var p=self(server);int n=0;var c=BlockPos.containing(start);
                    for(BlockPos at:BlockPos.betweenClosed(c.offset(-BloodIceberg.RADIUS,-BloodIceberg.DEPTH,-BloodIceberg.RADIUS),c.offset(BloodIceberg.RADIUS,BloodIceberg.TOP+2,BloodIceberg.RADIUS)))if(p.level().getBlockState(at).getBlock() instanceof BloodIcebergBlock)n++;
                    check(n>3000&&p.getY()>=start.y+BloodIceberg.FLOOR&&p.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST),"key 5 raises the Blood Iceberg arena and carries the player onto it: "+n+" blocks, y "+p.getY());});
                case 1125 -> finish(mc);
                default -> { }
            }
            if(ticks>=46&&ticks<=60&&playing(mc,MurimAnimations.SANGUINE_STRAIGHT))m1Seen=true;
            if(ticks>=470&&ticks<=495&&playing(mc,MurimAnimations.SANGUINE_CRYSTAL_THROW))throwSeen=true;
            if(ticks>=504&&ticks<=545&&playing(mc,MurimAnimations.SANGUINE_CRYSTAL_BARRAGE))barrageSeen=true;
            record(mc,"m1",44,60);record(mc,"scarlet",94,123);record(mc,"form",272,307);record(mc,"form-again",945,992);record(mc,"form-shed",992,1032);record(mc,"iceberg",1050,1120);
            record(mc,"crystal-charge",452,495);record(mc,"crystal-great",640,742);record(mc,"scarlet-sword",772,866);
            // Hold key 1 for the whole great charge: the key state is reset between ticks.
            if(ticks>640&&ticks<703)mc.options.keyHotbarSlots[0].setDown(true);record(mc,"barrage",502,548);
        }catch(Exception e){check(false,"tour exception: "+e);finish(mc);}
    }
    private static void target(MinecraftServer server,double distance) {
        var p=self(server);if(zombie!=null)zombie.discard();p.teleportTo(server.overworld(),start.x,start.y,start.z,0,0);
        p.setDeltaMovement(Vec3.ZERO);p.setHealth(10);MartialService.resetCooldowns(p);QiService.setQi(p,100);
        zombie=EntityType.ZOMBIE.create(server.overworld());zombie.setNoAi(true);zombie.setNoGravity(true);
        zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
        zombie.moveTo(p.getX(),p.getY(),p.getZ()+distance,180,0);zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);zombie.setHealth(500);server.overworld().addFreshEntity(zombie);
    }
    private static void view(Minecraft mc,double x,double y,double z) {
        Vec3 at=mc.player.position(),wanted=at.add(x,y,z),d=at.add(0,1.0,1).subtract(wanted),eye=wanted.add(d.normalize().scale(4));
        camera=new Marker(EntityType.MARKER,mc.level);camera.setPos(eye);camera.xo=camera.xOld=eye.x;camera.yo=camera.yOld=eye.y;camera.zo=camera.zOld=eye.z;
        camera.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));camera.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));
        camera.yRotO=camera.getYRot();camera.xRotO=camera.getXRot();mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);mc.setCameraEntity(camera);
    }
    /** A camera beside the player, looking along the throw at the middle of the way (z+4.5). */
    private static void viewAlong(Minecraft mc,double x,double y,double z) {
        Vec3 at=mc.player.position(),eye=at.add(x,y,z),d=at.add(0,1.5,4.5).subtract(eye);
        camera=new Marker(EntityType.MARKER,mc.level);camera.setPos(eye);camera.xo=camera.xOld=eye.x;camera.yo=camera.yOld=eye.y;camera.zo=camera.zOld=eye.z;
        camera.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));camera.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));
        camera.yRotO=camera.getYRot();camera.xRotO=camera.getXRot();mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);mc.setCameraEntity(camera);
    }
    private static ServerPlayer self(MinecraftServer server){return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());}
    private static boolean playing(Minecraft mc,AnimationAccessor<?> a){var p=EpicFightBridge.patch(mc.player);var clock=p.getAnimator().getPlayerFor(a);return clock!=null&&clock.getRealAnimation()==a;}
    private static void press(Minecraft mc,int slot){mc.options.keyHotbarSlots[slot].setDown(true);KeyMapping.click(mc.options.keyHotbarSlots[slot].getKey());}
    private static void record(Minecraft mc,String name,int from,int to)throws Exception{if(ticks>=from&&ticks<=to)capture(mc,name+"/frame-"+String.format("%03d",ticks-from)+".png");}
    private static void capture(Minecraft mc,String name)throws Exception{
        var file=mc.gameDirectory.toPath().resolve("screenshots").resolve(name);Files.createDirectories(file.getParent());var image=Screenshot.takeScreenshot(mc.getMainRenderTarget());
        net.minecraft.Util.ioPool().execute(()->{try(image){image.writeToFile(file);}catch(Exception e){check(false,"screenshot: "+e);}});
    }
    private static void check(boolean ok,String text){report.add((ok?"ok ":"FAILED ")+text);System.out.println("Sanguine tour: "+report.getLast());}
    private static void finish(Minecraft mc){finished=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("sanguine-tour-report.txt"),String.join("\n",report)+"\n"+(report.stream().anyMatch(s->s.startsWith("FAILED"))?"FAILED\n":"PASSED\n"));}catch(Exception e){System.err.println(e);}mc.stop();}
    @SubscribeEvent public static void hand(net.neoforged.neoforge.client.event.RenderHandEvent event){if(Boolean.getBoolean("murimblock.sanguineTour")&&camera!=null)event.setCanceled(true);}
}
