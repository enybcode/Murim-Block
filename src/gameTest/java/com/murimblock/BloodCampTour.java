package com.murimblock;

import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.slf4j.Logger;

/** Development-only native framebuffer capture of real placed blocks, no synthetic meshes. */
@EventBusSubscriber(modid=Murimblock.MOD_ID,value=Dist.CLIENT)
public final class BloodCampTour {
    private static final Logger LOG=LogUtils.getLogger();
    private static final int GROUND=63;
    private static final int[] LENGTHS={240,200};
    private static final String[] TAKES={"camp_overview","camp_walkthrough"};
    private static boolean creating,building,filming,finished,checking;
    private static volatile boolean ready,verified;
    private static volatile String failure;
    private static int warmup,take,index,captured;
    private static Marker camera;
    private static Path root;
    private static String sourceHash;
    private static final List<Placed> expected=new ArrayList<>();
    private static int blockEntities;
    private record Placed(BlockPos pos,BlockState state){}
    private static boolean enabled(){return Boolean.getBoolean("murimblock.bloodCampTour");}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!enabled()||finished)return;
        Minecraft mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        try{
            if(failure!=null)throw new IllegalStateException(failure);
            if(mc.player==null||mc.getSingleplayerServer()==null){
                if(mc.screen!=null&&mc.screen.getClass().getSimpleName().contains("Onboarding"))mc.setScreen(new TitleScreen(false));
                if(!creating&&mc.screen instanceof TitleScreen){
                    creating=true;
                    String name="blood-camp-review-"+System.currentTimeMillis();
                    root=mc.gameDirectory.toPath().resolve("captures/"+name);Files.createDirectories(root);
                    mc.options.renderDistance().set(8);mc.options.simulationDistance().set(5);
                    mc.options.framerateLimit().set(30);mc.options.enableVsync().set(false);
                    mc.createWorldOpenFlows().createFreshLevel(name,
                        new LevelSettings("Blood Sect Camp - Review",GameType.SPECTATOR,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                        new WorldOptions(20261017L,false,false),r->{
                            var flat=new FlatLevelGeneratorSettings(Optional.of(HolderSet.direct()),
                                r.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.DESERT),List.of());
                            flat.getLayersInfo().add(new FlatLayerInfo(1,Blocks.BEDROCK));
                            flat.getLayersInfo().add(new FlatLayerInfo(126,Blocks.SANDSTONE));
                            flat.getLayersInfo().add(new FlatLayerInfo(1,Blocks.SAND));flat.updateLayers();
                            return r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value()
                                .createWorldDimensions().replaceOverworldGenerator(r,new FlatLevelSource(flat));
                        },mc.screen);
                }
                return;
            }
            mc.options.hideGui=true;mc.options.guiScale().set(2);mc.options.fov().set(55);
            mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.gamma().set(.65);
            mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getToasts().clear();
            if(!building){building=true;mc.getSingleplayerServer().execute(()->build(mc.getSingleplayerServer()));}
            if(!ready)return;
            if(camera==null){camera=new Marker(EntityType.MARKER,mc.level);mc.setCameraEntity(camera);}
            if(!filming){
                warmup++;
                if(warmup>=180&&!checking){
                    checking=true;mc.getSingleplayerServer().execute(()->verify(mc.getSingleplayerServer().overworld()));
                }
                if(warmup>=220&&verified&&mc.screen==null){filming=true;LOG.info("Native camp filming started: {}",TAKES[take]);}
            }
        }catch(Exception ex){fail(mc,ex);}
    }

    private static void build(MinecraftServer server){
        try{
            ServerLevel level=server.overworld();
            level.setDayTime(4500);level.setWeatherParameters(1000000,0,false,false);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
            level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0,server);
            level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(false,server);
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){level.setChunkForced(x,z,true);level.getChunk(x,z);}
            Path source=Path.of(System.getProperty("murimblock.bloodCampSchematic"));
            sourceHash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source)));
            CompoundTag s;
            try(var input=Files.newInputStream(source)){s=NbtIo.readCompressed(input,NbtAccounter.unlimitedHeap()).getCompound("Schematic");}
            if(s.getInt("Version")!=3||s.getInt("DataVersion")!=3955)throw new IllegalStateException("Unexpected schematic version");
            int w=s.getShort("Width")&65535,h=s.getShort("Height")&65535,l=s.getShort("Length")&65535;
            int[] offset=s.getIntArray("Offset");
            BlockPos paste=new BlockPos(0,GROUND+1,32),min=paste.offset(offset[0],offset[1],offset[2]);
            CompoundTag blocks=s.getCompound("Blocks"),pal=blocks.getCompound("Palette");
            BlockState[] palette=new BlockState[pal.size()];
            for(String name:pal.getAllKeys())palette[pal.getInt(name)]=BlockStateParser.parseForBlock(level.holderLookup(Registries.BLOCK),name,false).blockState();
            byte[] data=blocks.getByteArray("Data");int cursor=0,cell=0;
            while(cursor<data.length){
                int id=0,shift=0,b;
                do{b=data[cursor++];id|=(b&127)<<shift;shift+=7;if(shift>35)throw new IllegalStateException("Malformed varint");}while((b&128)!=0);
                int x=cell%w,z=(cell/w)%l,y=cell/(w*l);cell++;
                BlockPos pos=min.offset(x,y,z);BlockState state=palette[id];
                // Bottom-to-top import; known shapes avoid breaking attached blocks before their supports are placed.
                level.setBlock(pos,state,18);
                if(!state.isAir())expected.add(new Placed(pos,state));
            }
            if(cell!=w*h*l)throw new IllegalStateException("Incomplete schematic: "+cell);
            var entities=blocks.getList("BlockEntities",Tag.TAG_COMPOUND);
            for(int i=0;i<entities.size();i++){
                CompoundTag e=entities.getCompound(i);int[] p=e.getIntArray("Pos");BlockPos pos=min.offset(p[0],p[1],p[2]);
                var be=level.getBlockEntity(pos);if(be==null)throw new IllegalStateException("Missing block entity at "+pos);
                be.loadWithComponents(e.getCompound("Data"),level.registryAccess());be.setChanged();
                level.sendBlockUpdated(pos,level.getBlockState(pos),level.getBlockState(pos),3);blockEntities++;
            }
            // Real neighbor notifications and scheduled liquid ticks, followed by an in-world stability check.
            for(Placed p:expected){
                level.updateNeighborsAt(p.pos,p.state.getBlock());
                if(!p.state.getFluidState().isEmpty())level.scheduleTick(p.pos,p.state.getFluidState().getType(),1);
            }
            server.getPlayerList().getPlayers().getFirst().teleportTo(level,0,GROUND+25,0,180,30);
            Files.writeString(root.resolve("import.txt"),"Imported real blocks from Sponge v3\nSource SHA256: "+sourceHash+
                "\nCells: "+cell+"\nNon-air blocks: "+expected.size()+"\nBlock entities: "+blockEntities+
                "\nPaste origin: "+paste.toShortString()+"\n");
            ready=true;LOG.info("Blood camp imported: {} blocks, {} block entities",expected.size(),blockEntities);
        }catch(Exception ex){failure=ex.toString();LOG.error("Camp import failed",ex);}
    }

    private static void verify(ServerLevel level){
        try{
            int changed=0,blood=0;StringBuilder details=new StringBuilder();
            var fluidBlock=com.murimblock.bloodsect.BloodSect.BLOOD_BLOCK.get();
            for(Placed p:expected){
                BlockState actual=level.getBlockState(p.pos);
                if(!actual.is(p.state.getBlock())){changed++;if(changed<12)details.append(p.pos).append(": ").append(p.state).append(" -> ").append(actual).append('\n');}
            }
            for(int x=-32;x<=32;x++)for(int z=-32;z<=32;z++)for(int y=GROUND-3;y<=GROUND+19;y++){
                if(level.getBlockState(new BlockPos(x,y,z)).is(fluidBlock))blood++;
            }
            long wanted=expected.stream().filter(p->p.state.is(fluidBlock)).count();
            if(changed!=0||blood!=wanted)throw new IllegalStateException("Camp stability: changed="+changed+", blood="+blood+" expected="+wanted+"\n"+details);
            Files.writeString(root.resolve("stability.txt"),"PASSED\nAfter real neighbor updates and at least 180 client ticks\n"+
                "Non-air block types preserved: "+expected.size()+"\nContained blood cells: "+blood+"\nBlock entities imported: "+blockEntities+"\n");
            verified=true;LOG.info("Camp stability passed, {} blood cells",blood);
        }catch(Exception ex){failure=ex.toString();}
    }

    @SubscribeEvent public static void pre(RenderFrameEvent.Pre event){
        if(!enabled()||camera==null||finished)return;
        Vec3 eye,aim;
        double t=index/(double)LENGTHS[take];
        if(take==0){
            double a=Math.PI/4+t*Math.PI*2;
            eye=new Vec3(76*Math.sin(a),GROUND+58,76*Math.cos(a));aim=new Vec3(0,GROUND+4,0);
        }else if(index<140){
            double u=index/139.;u=u*u*(3-2*u);
            eye=new Vec3(0,GROUND+2.65,43-49*u);aim=new Vec3(0,GROUND+4,-18);
        }else{
            double u=(index-140)/59.;u=u*u*(3-2*u);
            eye=new Vec3(11+4*u,GROUND+6-2*u,-10-3*u);aim=new Vec3(21,GROUND+1.3,-20);
        }
        Vec3 d=aim.subtract(eye);
        float yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z)));
        camera.setPos(eye);camera.xo=camera.xOld=eye.x;camera.yo=camera.yOld=eye.y;camera.zo=camera.zOld=eye.z;
        camera.setYRot(yaw);camera.yRotO=yaw;camera.setXRot(pitch);camera.xRotO=pitch;
    }
    @SubscribeEvent public static void fov(ViewportEvent.ComputeFov e){if(enabled()&&camera!=null&&!finished)e.setFOV(take==0?55:68);}
    @SubscribeEvent public static void caption(RenderGuiEvent.Post e){
        if(!enabled()||!filming||finished)return;
        var g=e.getGuiGraphics();int w=g.guiWidth(),h=g.guiHeight();
        g.fill(0,0,w,18,0xdd181416);g.fill(0,h-17,w,h,0xdd181416);
        g.drawString(Minecraft.getInstance().font,"BLOOD SECT / DESERT CAMP",8,5,0xfff3dfca,false);
        g.drawString(Minecraft.getInstance().font,"Minecraft 1.21.1  |  65 x 65 blocks",8,h-12,0xfff3dfca,false);
    }
    @SubscribeEvent public static void capture(RenderFrameEvent.Post e){
        if(!enabled()||!filming||finished)return;
        Minecraft mc=Minecraft.getInstance();if(mc.screen!=null)return;
        try{
            Path folder=root.resolve(TAKES[take]);Files.createDirectories(folder);
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(folder.resolve(String.format("frame-%04d.png",index)));}
            captured++;index++;
            if(index==LENGTHS[take]){
                Files.writeString(root.resolve(TAKES[take]+"-complete.txt"),"PASSED\nNative Minecraft framebuffer\nFrames: "+index+"\nFPS: 20\n");
                LOG.info("Camp take complete: {}",TAKES[take]);take++;index=0;
                if(take==LENGTHS.length){
                    finished=true;filming=false;mc.setCameraEntity(mc.player);
                    Files.writeString(root.resolve("report.txt"),"PASSED\nSource: Native Minecraft 1.21.1 framebuffer; real placed blocks\n"+
                        "Frames: "+captured+"\nFPS: 20\nSchematic SHA256: "+sourceHash+"\nStability verified: "+verified+"\n");
                    Files.writeString(mc.gameDirectory.toPath().resolve("latest-capture.txt"),root.toAbsolutePath().toString());
                    mc.execute(mc::stop);
                }else{filming=false;warmup=140;}
            }
        }catch(Exception ex){fail(mc,ex);}
    }
    private static void fail(Minecraft mc,Exception ex){
        finished=true;LOG.error("Blood camp capture FAILED",ex);
        if(root!=null)try{Files.writeString(root.resolve("report.txt"),"FAILED\n"+ex);}catch(Exception ignored){}
        mc.execute(mc::stop);
    }
}
