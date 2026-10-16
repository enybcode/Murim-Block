package com.murimblock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.gameasset.Armatures;

/**
 * A diagnostic (required = false): the phases of every Murim attack animation on the biped, written to
 * build/game-test-run/murim-attack-phases.json, for scripts/animation/swing-trails.cjs (each swing's trail window).
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AnimationPhaseDump {
    private AnimationPhaseDump() { }

    @GameTest(template = "foundation_arena", batch = "animation_phase_dump", required = false)
    public static void dumpTheMurimAttackPhases(GameTestHelper helper) throws Exception {
        Map<String, JsonArray> out = new TreeMap<>();
        var animations = AnimationManager.getInstance().getAnimations(a -> a.registryName().getNamespace().equals(Murimblock.MOD_ID));
        animations.forEach((id, accessor) -> {
            if (!(accessor.get() instanceof AttackAnimation attack) || attack.getArmature().get() != Armatures.BIPED.get()) return;
            JsonArray phases = new JsonArray();
            for (AttackAnimation.Phase p : attack.phases) {
                JsonObject o = new JsonObject();
                o.addProperty("start", p.start);
                o.addProperty("antic", p.antic);
                o.addProperty("preDelay", p.preDelay);
                o.addProperty("contact", p.contact);
                o.addProperty("recovery", p.recovery);
                o.addProperty("end", p.end);
                o.addProperty("hand", p.hand.name());
                phases.add(o);
            }
            out.put(id.getPath(), phases);
        });
        Path file = helper.getLevel().getServer().getServerDirectory().resolve("murim-attack-phases.json");
        Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(out));
        helper.succeed();
    }
}
