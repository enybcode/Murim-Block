package com.murimblock.martial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * A player's martial arts: the arts they know, the techniques they unlocked, their combat bar (keys 1 to 0 in combat
 * mode), the mastery of each technique, and the manuals they already received (one copy each). Saved; synced to the
 * player only. Immutable: every change makes a new value.
 */
public record MartialData(Set<ResourceLocation> arts, Set<ResourceLocation> techniques, List<Optional<ResourceLocation>> bar,
                          Map<ResourceLocation, Integer> mastery, Set<ResourceLocation> manualsReceived) {
    public static final int BAR_SLOTS = 10;
    public static final MartialData EMPTY = new MartialData(Set.of(), Set.of(), emptyBar(), Map.of(), Set.of());

    private static final Codec<Set<ResourceLocation>> SET = ResourceLocation.CODEC.listOf()
            .xmap(l -> Set.copyOf(new LinkedHashSet<>(l)), s -> List.copyOf(s));
    public static final Codec<MartialData> CODEC = RecordCodecBuilder.create(i -> i.group(
            SET.optionalFieldOf("arts", Set.of()).forGetter(MartialData::arts),
            SET.optionalFieldOf("techniques", Set.of()).forGetter(MartialData::techniques),
            Codec.STRING.listOf().optionalFieldOf("bar", List.of()).forGetter(d -> d.bar.stream().map(o -> o.map(ResourceLocation::toString).orElse("")).toList()),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("mastery", Map.of()).forGetter(MartialData::mastery),
            SET.optionalFieldOf("manuals_received", Set.of()).forGetter(MartialData::manualsReceived)
    ).apply(i, (arts, techniques, bar, mastery, manuals) -> new MartialData(arts, techniques, readBar(bar), mastery, manuals)));
    public static final StreamCodec<RegistryFriendlyByteBuf, MartialData> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public MartialData {
        arts = Set.copyOf(arts);
        techniques = Set.copyOf(techniques);
        List<Optional<ResourceLocation>> slots = new ArrayList<>(bar);
        while (slots.size() < BAR_SLOTS) slots.add(Optional.empty());
        bar = List.copyOf(slots.subList(0, BAR_SLOTS));
        mastery = Map.copyOf(mastery);
        manualsReceived = Set.copyOf(manualsReceived);
    }

    private static List<Optional<ResourceLocation>> emptyBar() {
        List<Optional<ResourceLocation>> bar = new ArrayList<>();
        for (int i = 0; i < BAR_SLOTS; i++) bar.add(Optional.empty());
        return bar;
    }

    private static List<Optional<ResourceLocation>> readBar(List<String> raw) {
        List<Optional<ResourceLocation>> bar = new ArrayList<>();
        for (String s : raw) bar.add(s.isEmpty() ? Optional.empty() : Optional.ofNullable(ResourceLocation.tryParse(s)));
        return bar;
    }

    public boolean knowsArt(ResourceLocation art) {
        return arts.contains(art);
    }

    public boolean hasTechnique(ResourceLocation technique) {
        return techniques.contains(technique);
    }

    public Optional<ResourceLocation> slot(int index) {
        return index >= 0 && index < BAR_SLOTS ? bar.get(index) : Optional.empty();
    }

    /** Learns an art with its first technique, placed in the first free slot of the bar. */
    public MartialData learn(MartialArts.Art art) {
        if (knowsArt(art.id())) return this;
        Set<ResourceLocation> a = new LinkedHashSet<>(arts);
        a.add(art.id());
        return new MartialData(a, techniques, bar, mastery, manualsReceived).unlock(art.first());
    }

    /** Unlocks a technique and puts it in the first free slot of the bar, if any. */
    public MartialData unlock(MartialArts.Technique technique) {
        if (hasTechnique(technique.id())) return this;
        Set<ResourceLocation> t = new LinkedHashSet<>(techniques);
        t.add(technique.id());
        List<Optional<ResourceLocation>> b = new ArrayList<>(bar);
        int free = b.indexOf(Optional.empty());
        if (free >= 0 && !b.contains(Optional.of(technique.id()))) b.set(free, Optional.of(technique.id()));
        return new MartialData(arts, t, b, mastery, manualsReceived);
    }

    /** Puts an unlocked technique in a slot (moving it if it was elsewhere), or empties the slot with null. */
    public MartialData assign(int slot, ResourceLocation technique) {
        if (slot < 0 || slot >= BAR_SLOTS || (technique != null && !hasTechnique(technique))) return this;
        List<Optional<ResourceLocation>> b = new ArrayList<>(bar);
        if (technique != null) for (int i = 0; i < BAR_SLOTS; i++) if (b.get(i).equals(Optional.of(technique))) b.set(i, Optional.empty());
        b.set(slot, Optional.ofNullable(technique));
        return new MartialData(arts, techniques, b, mastery, manualsReceived);
    }

    /** Unlocks a technique, knowing its art from then on (without the art's first technique, unlike {@link #learn}). */
    public MartialData unlockWithArt(MartialArts.Technique technique) {
        Set<ResourceLocation> a = new LinkedHashSet<>(arts);
        a.add(technique.art());
        return new MartialData(a, techniques, bar, mastery, manualsReceived).unlock(technique);
    }

    /** Forgets a technique: off the bar, its mastery lost. */
    public MartialData forget(ResourceLocation technique) {
        Set<ResourceLocation> t = new LinkedHashSet<>(techniques);
        t.remove(technique);
        List<Optional<ResourceLocation>> b = new ArrayList<>(bar);
        b.replaceAll(o -> o.equals(Optional.of(technique)) ? Optional.empty() : o);
        Map<ResourceLocation, Integer> m = new HashMap<>(mastery);
        m.remove(technique);
        return new MartialData(arts, t, b, m, manualsReceived);
    }

    /** Forgets an art, all its techniques, and that its manual was received (the library gives it again). */
    public MartialData forgetArt(MartialArts.Art art) {
        MartialData d = this;
        for (MartialArts.Technique t : art.techniques()) d = d.forget(t.id());
        Set<ResourceLocation> a = new LinkedHashSet<>(d.arts);
        a.remove(art.id());
        Set<ResourceLocation> m = new LinkedHashSet<>(d.manualsReceived);
        m.remove(art.id());
        return new MartialData(a, d.techniques, d.bar, d.mastery, m);
    }

    public MartialData receivedManual(ResourceLocation art) {
        Set<ResourceLocation> m = new LinkedHashSet<>(manualsReceived);
        m.add(art);
        return new MartialData(arts, techniques, bar, mastery, m);
    }

    public int mastery(ResourceLocation technique) {
        return mastery.getOrDefault(technique, 0);
    }

    public MartialData withMastery(ResourceLocation technique, int value) {
        Map<ResourceLocation, Integer> m = new HashMap<>(mastery);
        m.put(technique, Math.max(0, Math.min(100, value)));
        return new MartialData(arts, techniques, bar, m, manualsReceived);
    }
}
