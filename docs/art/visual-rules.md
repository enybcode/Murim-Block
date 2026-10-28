# Visual rules

Set by Enzo on 28 Oct 2026. Murimblock should be known for how beautiful it looks, more than for how complex it
is. Every visual piece gets real care: models, textures, animations, effects, sounds that go with them, and screens.

## Models (3D)

- Relief everywhere. No plain boxes wearing a flat texture: give a model raised and sunken parts that catch the light.
  For example:
  - clothes in layers: a collar that stands off the neck, sleeves and a hem a pixel wider than the limb, a sash
    or belt that stands out;
  - straps, buckles, wraps, knots and seams as small cubes;
  - torn edges cut into the geometry, not only painted on;
  - hair as locks and buns, not a painted cap.
- Original design: no copy of vanilla or of another mod's models or textures. Each mob keeps a clear silhouette that
  can be read from far away and in a fight.
- Keep the Minecraft scale and block look. Pixels stay on a 1/16 grid, with no smooth or high-poly shapes. Hitboxes
  and saved IDs do not change.
- Detail serves the read: every added piece says who the mob is (sect, rank, how it died). Do not add noise.

## Textures

- Shade by hand: a light from the top, darker folds and undersides, highlights on edges and raised parts.
- Small palettes. About 3 to 5 tones per material, with colour in the shadows (not grey). Every mob gets a palette
  sheet first.
- Wear and story show in details: dirt at the hem, worn fabric, blood, bandages.
- Glowing parts (eyes, Qi, talismans) are emissive, and used sparingly.

## Animation and effects

- Motion carries weight and character: anticipation, follow-through, and the body moving from the feet and hips
  (see the boxing rule in the combat docs).
- Cloth (capes, sashes, robe tails) is simulated where it adds life.
- Effects (Photon) are clean and readable: short, coloured to match the Qi or element, never covering the fight.

## Process

No model and no animation is made before its storyboard is drawn and validated by Enzo (rule set 28 Oct 2026).

1. Storyboard first, validated by Enzo before any work in Blender:
   - a model: a design sheet (look, palette, parts in relief, lore, stats, attacks), as `docs/mobs/disciple-dechu.png`;
   - an animation: its key poses drawn with the weapon's path, the body's motion, the timing in ticks, the feel
     aimed for and what the player must read, as `docs/mobs/boards/` (drawn by `board.js`).
2. Then the model or the clip, built in Blender, following the validated storyboard.
3. Before calling a model done, show Enzo renders from the front, the side, the back and three quarters, close up.
4. Then check it in game: daylight, night and in a fight.
5. Beauty is part of "done": a model that works but looks plain is not finished.
