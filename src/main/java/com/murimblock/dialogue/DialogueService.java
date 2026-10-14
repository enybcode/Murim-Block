package com.murimblock.dialogue;

import com.murimblock.network.DialoguePayloads;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Conversations with characters, run by the server: a speaker shows the player a page (a line and the answers they may
 * give, on DialogueScreen); the answer chosen comes back to the speaker, which shows the next page or acts. Only an
 * answer offered on the last page, to a speaker still near, is taken.
 */
public final class DialogueService {
    /** A character one can talk to. */
    public interface Speaker {
        /** The player chose this answer on the last page this speaker showed them. */
        void choose(ServerPlayer player, String option);
    }

    /** How near the speaker must still be when the answer comes. */
    public static final double REACH = 8;

    private record Offer(int speakerId, Set<String> options) { }

    private static final Map<UUID, Offer> OFFERS = new HashMap<>();
    /** The last page shown to each player (the tests read it). */
    private static final Map<UUID, DialoguePayloads.Page> LAST = new HashMap<>();

    private DialogueService() {
    }

    public static DialoguePayloads.Option option(String id, Component label) {
        return new DialoguePayloads.Option(id, label);
    }

    /** Shows the player a page of this speaker (no answers: the last page). */
    public static void show(ServerPlayer player, Entity speaker, Component line, List<DialoguePayloads.Option> options) {
        DialoguePayloads.Page page = new DialoguePayloads.Page(speaker.getId(), speaker.getDisplayName(), line, List.copyOf(options));
        OFFERS.put(player.getUUID(), new Offer(speaker.getId(), options.stream().map(DialoguePayloads.Option::id).collect(Collectors.toSet())));
        LAST.put(player.getUUID(), page);
        if (player.connection != null) PacketDistributor.sendToPlayer(player, page);
    }

    public static void choose(ServerPlayer player, int speakerId, String option) {
        Offer offer = OFFERS.get(player.getUUID());
        if (offer == null || offer.speakerId != speakerId || !offer.options.contains(option)) return;
        OFFERS.remove(player.getUUID());
        Entity speaker = player.level().getEntity(speakerId);
        if (speaker instanceof Speaker s && speaker.isAlive() && speaker.distanceTo(player) <= REACH) s.choose(player, option);
    }

    /** The last page shown to this player, if any. */
    public static DialoguePayloads.Page last(ServerPlayer player) {
        return LAST.get(player.getUUID());
    }

    public static void forget(ServerPlayer player) {
        OFFERS.remove(player.getUUID());
        LAST.remove(player.getUUID());
    }

    public static void clearForTests() {
        OFFERS.clear();
        LAST.clear();
    }
}
