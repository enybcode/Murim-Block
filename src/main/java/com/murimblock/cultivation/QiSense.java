package com.murimblock.cultivation;

import com.murimblock.cultivation.inner.HeartDemonEntity;
import com.murimblock.network.CultivationPayloads;
import com.murimblock.qi.QiService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Qi Sense (docs/cultivation/qi-cultivation.md): from Qi Condensation on, a cultivator holding its key reads the auras
 * around them, at a small cost in Qi each second. The server decides what each reader may see (so no client ever holds
 * a realm it should not know): a weaker one's realm and exact stage, the same realm with its stage uncertain, a seal
 * smudged one realm above, nothing but a crushing aura two realms above or more.
 */
public final class QiSense {
    private static final Set<UUID> ACTIVE = new HashSet<>();

    private QiSense() {
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.contains(player.getUUID());
    }

    public static boolean unlocked(ServerPlayer player) {
        return CultivationService.getRealm(player).ordinal() >= CultivationRules.SENSE_REALM.ordinal();
    }

    public static void setActive(ServerPlayer player, boolean on) {
        if (!on) {
            if (ACTIVE.remove(player.getUUID())) send(player, false, List.of());
            return;
        }
        if (!unlocked(player)) {
            player.displayClientMessage(Component.translatable("cultivation.murimblock.sense.locked",
                    Component.translatable("gui.murimblock.realm." + CultivationRules.SENSE_REALM.serializedName())), true);
            send(player, false, List.of());
            return;
        }
        if (QiService.getQi(player) < CultivationRules.SENSE_QI_PER_SECOND) {
            player.displayClientMessage(Component.translatable("cultivation.murimblock.sense.no_qi"), true);
            send(player, false, List.of());
            return;
        }
        if (ACTIVE.add(player.getUUID())) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.5F, 0.6F);
            send(player, true, readings(player));
        }
    }

    public static void tick(ServerPlayer player) {
        if (!isActive(player)) return;
        if (player.tickCount % 20 == 0) {
            if (QiService.getQi(player) < CultivationRules.SENSE_QI_PER_SECOND || !unlocked(player) || !player.isAlive()) {
                setActive(player, false);
                return;
            }
            QiService.removeQi(player, CultivationRules.SENSE_QI_PER_SECOND);
        }
        if (player.tickCount % 5 == 0) send(player, true, readings(player));
    }

    /**
     * What this reader reads of the auras around: within the sense's reach and in sight; the powerful ones (as strong as
     * the reader or stronger, risen high enough to be seen over the trees: Enzo, 18/10/2026) from three times as far,
     * unseen; from Core Formation, the near ones behind walls too (CultivationRules.readsThroughWalls). Both reaches grow with the reader's realm (CultivationRules.senseRange).
     */
    public static List<CultivationPayloads.Reading> readings(ServerPlayer viewer) {
        CultivationRealm own = CultivationService.getRealm(viewer);
        List<CultivationPayloads.Reading> out = new ArrayList<>();
        double range = CultivationRules.senseRange(own), far = CultivationRules.farSenseRange(own);
        for (LivingEntity e : viewer.level().getEntitiesOfClass(LivingEntity.class, viewer.getBoundingBox().inflate(far),
                e -> e != viewer && e.isAlive() && !e.isSpectator() && e.distanceToSqr(viewer) <= far * far)) {
            Optional<CultivationData> data = cultivationOf(e);
            if (data.isEmpty()) continue;
            double d2 = e.distanceToSqr(viewer);
            boolean near = d2 <= range * range && (viewer.hasLineOfSight(e) || CultivationRules.readsThroughWalls(own, d2));
            if (!near && data.get().realm().ordinal() < own.ordinal()) continue;
            CultivationRules.Reading kind = CultivationRules.reading(own, data.get().realm());
            int realm = kind == CultivationRules.Reading.CLEAR || kind == CultivationRules.Reading.FUZZY ? data.get().realm().ordinal() : -1;
            int stage = kind == CultivationRules.Reading.CLEAR ? data.get().stage().ordinal() : -1;
            out.add(new CultivationPayloads.Reading(e.getId(), realm, stage, kind.ordinal()));
            if (out.size() >= 64) break;
        }
        return out;
    }

    /** The cultivation an aura shows: a player's own, a Heart Demon's owner's, a creature's from its kind. */
    public static Optional<CultivationData> cultivationOf(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) return Optional.of(CultivationService.getCultivation(player));
        if (entity instanceof HeartDemonEntity demon) return demon.cultivation();
        return CultivationRules.creatureRealm(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }

    private static void send(ServerPlayer player, boolean open, List<CultivationPayloads.Reading> readings) {
        if (player.connection != null) PacketDistributor.sendToPlayer(player, new CultivationPayloads.SenseReadings(open, readings));
    }

    public static void clearForTests() {
        ACTIVE.clear();
    }
}
