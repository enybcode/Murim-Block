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
 * mode), the mastery of each technique (and of each passive), the manuals they already received (one copy each), their
 * main style (whose passive gives their M1 chain and parry), the strikes they placed in its four M1 slots (empty: the
 * passive's defaults), and their quest counters. Saved; synced to the player only. Immutable: every change makes a new
 * value. Every field added after the first slice is optional in the save, so older saves load unchanged.
 */
public record MartialData(Set<ResourceLocation> arts, Set<ResourceLocation> techniques, List<Optional<ResourceLocation>> bar,
                          Map<ResourceLocation, Integer> mastery, Set<ResourceLocation> manualsReceived,
                          Optional<ResourceLocation> mainArt, List<ResourceLocation> strikes, Map<String, Integer> progress) {
    public static final int BAR_SLOTS = 10;
    public static final int MAX_MASTERY = 100;
    public static final MartialData EMPTY = new MartialData(Set.of(), Set.of(), emptyBar(), Map.of(), Set.of(), Optional.empty(), List.of(), Map.of());

    private static final Codec<Set<ResourceLocation>> SET = ResourceLocation.CODEC.listOf()
            .xmap(l -> Set.copyOf(new LinkedHashSet<>(l)), s -> List.copyOf(s));
    public static final Codec<MartialData> CODEC = RecordCodecBuilder.create(i -> i.group(
            SET.optionalFieldOf("arts", Set.of()).forGetter(MartialData::arts),
            SET.optionalFieldOf("techniques", Set.of()).forGetter(MartialData::techniques),
            Codec.STRING.listOf().optionalFieldOf("bar", List.of()).forGetter(d -> d.bar.stream().map(o -> o.map(ResourceLocation::toString).orElse("")).toList()),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("mastery", Map.of()).forGetter(MartialData::mastery),
            SET.optionalFieldOf("manuals_received", Set.of()).forGetter(MartialData::manualsReceived),
            ResourceLocation.CODEC.optionalFieldOf("main_art").forGetter(MartialData::mainArt),
            ResourceLocation.CODEC.listOf().optionalFieldOf("strikes", List.of()).forGetter(MartialData::strikes),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("progress", Map.of()).forGetter(MartialData::progress)
    ).apply(i, (arts, techniques, bar, mastery, manuals, main, strikes, progress) ->
            new MartialData(arts, techniques, readBar(bar), mastery, manuals, main, strikes, progress)));
    public static final StreamCodec<RegistryFriendlyByteBuf, MartialData> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public MartialData {
        arts = Set.copyOf(arts);
        techniques = Set.copyOf(techniques);
        List<Optional<ResourceLocation>> slots = new ArrayList<>(bar);
        while (slots.size() < BAR_SLOTS) slots.add(Optional.empty());
        bar = List.copyOf(slots.subList(0, BAR_SLOTS));
        mastery = Map.copyOf(mastery);
        manualsReceived = Set.copyOf(manualsReceived);
        strikes = List.copyOf(strikes);
        progress = Map.copyOf(progress);
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

    private MartialData with(Set<ResourceLocation> arts, Set<ResourceLocation> techniques, List<Optional<ResourceLocation>> bar,
                             Map<ResourceLocation, Integer> mastery, Set<ResourceLocation> manualsReceived) {
        return new MartialData(arts, techniques, bar, mastery, manualsReceived, mainArt, strikes, progress);
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

    /** Learns an art with its first technique, placed in the first free slot of the bar; the first art becomes the main style. */
    public MartialData learn(MartialArts.Art art) {
        if (knowsArt(art.id())) return this;
        Set<ResourceLocation> a = new LinkedHashSet<>(arts);
        a.add(art.id());
        MartialData d = with(a, techniques, bar, mastery, manualsReceived);
        if (mainArt.isEmpty()) d = d.withMainArt(art.id());
        return d.unlock(art.first());
    }

    /** Unlocks a technique and puts it in the first free slot of the bar, if any (a secret never goes on the bar). */
    public MartialData unlock(MartialArts.Technique technique) {
        if (hasTechnique(technique.id())) return this;
        Set<ResourceLocation> t = new LinkedHashSet<>(techniques);
        t.add(technique.id());
        List<Optional<ResourceLocation>> b = new ArrayList<>(bar);
        int free = b.indexOf(Optional.empty());
        if (technique.castable() && free >= 0 && !b.contains(Optional.of(technique.id()))) b.set(free, Optional.of(technique.id()));
        return with(arts, t, b, mastery, manualsReceived);
    }

    /** Puts an unlocked technique in a slot (moving it if it was elsewhere), or empties the slot with null. */
    public MartialData assign(int slot, ResourceLocation technique) {
        if (slot < 0 || slot >= BAR_SLOTS || (technique != null && !hasTechnique(technique))) return this;
        MartialArts.Technique def = technique == null ? null : MartialArts.technique(technique);
        if (def != null && !def.castable()) return this;
        List<Optional<ResourceLocation>> b = new ArrayList<>(bar);
        if (technique != null) for (int i = 0; i < BAR_SLOTS; i++) if (b.get(i).equals(Optional.of(technique))) b.set(i, Optional.empty());
        b.set(slot, Optional.ofNullable(technique));
        return with(arts, techniques, b, mastery, manualsReceived);
    }

    /** Unlocks a technique, knowing its art from then on (without the art's first technique, unlike {@link #learn}). */
    public MartialData unlockWithArt(MartialArts.Technique technique) {
        Set<ResourceLocation> a = new LinkedHashSet<>(arts);
        a.add(technique.art());
        MartialData d = with(a, techniques, bar, mastery, manualsReceived);
        if (mainArt.isEmpty()) d = d.withMainArt(technique.art());
        return d.unlock(technique);
    }

    /** Forgets a technique: off the bar, its mastery lost. */
    public MartialData forget(ResourceLocation technique) {
        Set<ResourceLocation> t = new LinkedHashSet<>(techniques);
        t.remove(technique);
        List<Optional<ResourceLocation>> b = new ArrayList<>(bar);
        b.replaceAll(o -> o.equals(Optional.of(technique)) ? Optional.empty() : o);
        Map<ResourceLocation, Integer> m = new HashMap<>(mastery);
        m.remove(technique);
        return with(arts, t, b, m, manualsReceived);
    }

    /**
     * Forgets an art: all its techniques, its passive's mastery, its quests, that its manual was received (the library
     * gives it again) and, if it was the main style, the main style and the M1 slots.
     */
    public MartialData forgetArt(MartialArts.Art art) {
        MartialData d = this;
        for (MartialArts.Technique t : art.techniques()) d = d.forget(t.id());
        Set<ResourceLocation> a = new LinkedHashSet<>(d.arts);
        a.remove(art.id());
        Set<ResourceLocation> m = new LinkedHashSet<>(d.manualsReceived);
        m.remove(art.id());
        Map<ResourceLocation, Integer> mastery = new HashMap<>(d.mastery);
        mastery.remove(art.passive().id());
        Map<String, Integer> p = new HashMap<>(d.progress);
        for (MartialArts.Quest q : art.quests()) for (MartialArts.QuestStep s : q.steps()) p.remove(s.key());
        p.remove(MartialArts.STANCE_HITS);
        boolean main = d.mainArt.equals(Optional.of(art.id()));
        return new MartialData(a, d.techniques, d.bar, mastery, m, main ? Optional.empty() : d.mainArt,
                main ? List.of() : d.strikes, p);
    }

    public MartialData receivedManual(ResourceLocation art) {
        Set<ResourceLocation> m = new LinkedHashSet<>(manualsReceived);
        m.add(art);
        return with(arts, techniques, bar, mastery, m);
    }

    /** Mastery of a technique or a passive, 0 to 100. */
    public int mastery(ResourceLocation id) {
        return mastery.getOrDefault(id, 0);
    }

    public MartialData withMastery(ResourceLocation id, int value) {
        Map<ResourceLocation, Integer> m = new HashMap<>(mastery);
        m.put(id, Math.max(0, Math.min(MAX_MASTERY, value)));
        return with(arts, techniques, bar, m, manualsReceived);
    }

    /**
     * The mastery of a whole style: the average of its passive and its techniques (secrets aside), a technique not yet
     * unlocked counting 0.
     */
    public int styleMastery(MartialArts.Art art) {
        int sum = mastery(art.passive().id()), n = 1;
        for (MartialArts.Technique t : art.techniques()) {
            if (t.secret()) continue;
            sum += hasTechnique(t.id()) ? mastery(t.id()) : 0;
            n++;
        }
        return sum / n;
    }

    /** Whether a technique has evolved: unlocked, and its style has reached the evolution's mastery. */
    public boolean evolved(MartialArts.Art art, ResourceLocation technique) {
        MartialArts.Evolution e = art.evolutionOf(technique);
        return e != null && hasTechnique(technique) && styleMastery(art) >= e.styleMastery();
    }

    // ---------------------------------------------------------------- main style and M1

    public MartialData withMainArt(ResourceLocation art) {
        if (!knowsArt(art) || mainArt.equals(Optional.of(art))) return this;
        // Another style's strikes do not carry over.
        return new MartialData(arts, techniques, bar, mastery, manualsReceived, Optional.of(art), List.of(), progress);
    }

    /** The main style's passive, if the player has a main style. */
    public Optional<MartialArts.Passive> passive() {
        return mainArt.map(MartialArts::art).map(MartialArts.Art::passive);
    }

    /** The four strikes of the M1 chain for this passive: the player's choice, or the passive's defaults. */
    public List<ResourceLocation> strikes(MartialArts.Passive passive) {
        return strikes.size() == MartialArts.Passive.M1_SLOTS ? strikes : passive.defaults();
    }

    /**
     * Places a strike in an M1 slot of the main style's passive. A strike already in another slot swaps with the one
     * there. Refused if the chain would no longer end on a launcher or a finisher.
     */
    public MartialData withStrike(int slot, ResourceLocation strike) {
        MartialArts.Passive passive = passive().orElse(null);
        if (passive == null || !passive.fits(slot, strike)) return this;
        List<ResourceLocation> s = new ArrayList<>(strikes(passive));
        int was = s.indexOf(strike);
        if (was == slot) return this;
        if (was >= 0) s.set(was, s.get(slot));
        s.set(slot, strike);
        for (int i = 0; i < s.size(); i++) if (!passive.fits(i, s.get(i))) return this;
        return new MartialData(arts, techniques, bar, mastery, manualsReceived, mainArt, s, progress);
    }

    // ---------------------------------------------------------------- quests

    public int progress(String key) {
        return progress.getOrDefault(key, 0);
    }

    public MartialData withProgress(String key, int value) {
        Map<String, Integer> p = new HashMap<>(progress);
        if (value <= 0) p.remove(key);
        else p.put(key, value);
        return new MartialData(arts, techniques, bar, mastery, manualsReceived, mainArt, strikes, p);
    }
}
