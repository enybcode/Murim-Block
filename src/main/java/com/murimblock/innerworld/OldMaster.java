package com.murimblock.innerworld;

import com.murimblock.cultivation.QiParticleOptions;
import com.murimblock.registry.MurimEntities;
import com.murimblock.registry.MurimParticles;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The Old Master: the ghost of a master living in the player's inner world, hovering over the NPC zone north of
 * the arena. He turns his head toward the player and talks when right-clicked (the dialogue is client side). Summoned
 * each time the owner enters and never saved: he leaves with them.
 */
public final class OldMaster extends PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.old_master.idle");
    private static final int CHECK_INTERVAL = 20;
    /** Farther than this from his owner's island center, he does not belong. */
    private static final double ISLAND_RANGE = InnerIsland.RADIUS * 2;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID owner;

    public OldMaster(EntityType<? extends OldMaster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    /** Seats the master on the NPC zone of this island, facing the arena; any older one there goes first. */
    static void summon(ServerLevel level, BlockPos center, ServerPlayer owner) {
        double x = center.getX() + InnerIsland.NPC_X + InnerIsland.NPC_SIZE / 2.0;
        double z = center.getZ() + InnerIsland.NPC_Z + InnerIsland.NPC_SIZE / 2.0;
        double y = InnerIsland.SURFACE_Y + 1;
        level.getEntitiesOfClass(OldMaster.class, new AABB(x - 8, y - 4, z - 8, x + 8, y + 8, z + 8))
                .forEach(OldMaster::discard);
        OldMaster master = new OldMaster(MurimEntities.OLD_MASTER.get(), level);
        master.owner = owner.getUUID();
        master.moveTo(x, y, z, 0, 0);
        master.setYHeadRot(0);
        master.setYBodyRot(0);
        level.addFreshEntity(master);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10, 1));
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, 0, 0);
        if (level() instanceof ServerLevel level) {
            if (tickCount % CHECK_INTERVAL == 0 && !ownerHere(level)) {
                discard();
            }
        } else if (random.nextFloat() < 0.12f) {
            rise(random);
        }
    }

    /** Gone when his owner is no longer on this island (back to the body, logged out, died). */
    private boolean ownerHere(ServerLevel level) {
        ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (player == null || player.level() != level || !InnerWorld.state(player).inside()) {
            return false;
        }
        BlockPos own = InnerIslands.get(level.getServer()).existingCenter(owner);
        return own != null && Math.abs(getX() - own.getX()) < ISLAND_RANGE && Math.abs(getZ() - own.getZ()) < ISLAND_RANGE;
    }

    /** A faint speck of Qi rising around him. */
    private void rise(RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = 0.5 + random.nextDouble() * 0.4;
        level().addParticle(new QiParticleOptions(MurimParticles.QI_SPARK.get(), 0.6f),
                getX() + Math.cos(angle) * radius, getY() + 0.2 + random.nextDouble() * 1.2, getZ() + Math.sin(angle) * radius,
                0, 0.015 + random.nextDouble() * 0.015, 0);
    }

    /** Talking happens on the client; the server only keeps the click from doing anything else. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** Never written to disk: each visit summons him again. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
