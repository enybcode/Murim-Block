# Blender tools

Scripts that build, export and film the biped of Shenfa, the animation engine (`docs/combat/engine-roadmap.md`). They run
headless; nothing here goes into the jar.

Needs Blender 5.2 (`C:\Program Files\Blender Foundation\Blender 5.2\blender.exe`; set `BLENDER` to override).

- `murim_biped.py`: the skeleton (Epic Fight's 20-joint layout) and the body, built in code. Must match
  `com.murimblock.shenfa.Biped`.
- `sh tools/blender/export.sh`: rebuilds both arm widths, writes `assets/murimblock/skeletal/biped*.gltf` and
  `.bin` for the mod, and `tools/blender/out/murim_biped*.blend` to animate (not committed).
- `render.py`: films a test clip from the front, the side and three quarters with a UV test skin (or `--skin`):

  ```
  blender -b --factory-startup --python tools/blender/render.py -- --out <dir> --clip joints
  java tools/frames-to-gif.java <dir> out.gif 60 960
  ```

  Clips: `rest`, `bend`, `joints`, `elbow`, `knee`; or `--anim <set>/<clip>` films a clip of `murim_clips.py`
  over its length.
- `murim_clips.py`: clip sets written as keyframes at 20 frames per second (one per game tick). Each set is
  exported, skeleton only, by:

  ```
  blender -b --factory-startup --python tools/blender/export_clips.py -- --set test \
      --gltf src/main/resources/assets/murimblock/skeletal/animations/test.gltf [--blend tools/blender/out/test.blend]
  ```

  Sets: `test`, `biped` (the living motions, played by `skeletal/motions/biped.json`), `fists` (stance, guard and
  the fist moveset), `combat` (dodges and reactions). The settings of each clip (`loop`, `transition`, `mask`,
  `masks` by motion, `additive`, `move_speed`, `events`, `root_motion`, `attack`; see `ClipLoader`, `ClipEvent`,
  `RootMotion`, `AttackData`) go in the hand-written sidecar `<set>.json` next to the `.gltf`. In game: `/skeletal anim play murimblock:test/bounce [speed]`, `/skeletal anim speed 0.25`,
  `/skeletal anim stop`; F3+T reloads the clips.
- Mobs' clip sets are exported with `--declip <bodies>` (the mobs that play them), which keeps the arms out of the
  body (`murim_declip.py`: the shoulder moved out or forward and the elbow bent the least it takes, frame by frame;
  a two-handed grip moved whole, the left hand sliding on the haft); it prints the frames still meeting. As of
  29 Oct 2026:

  ```
  export_clips.py -- --set disciple --declip disciple,sables,noye,paysan,sanglier --gltf .../animations/disciple.gltf
  export_clips.py -- --set archer --declip archer,esprit --gltf .../animations/archer.gltf
  export_clips.py -- --set witch --mob witch --declip witch --gltf .../animations/witch.gltf
  export_clips.py -- --set meditation --gltf .../animations/meditation.gltf
  ```

  `limb_check.py -- --mob <mob> [--set <set>] [--declip ...]` lists, clip by clip, the frames where an arm or the held
  item enters the body; `mob.py --film ... --declip ...` films the corrected clips.

## Rig rules

- Bones point straight up with no roll, so each joint's rest rotation is the identity in the mod.
- Limbs are rigid. `Elbow_*` and `Knee_*` are helpers that follow the forearm (`Hand_*`) or shin (`Leg_*`) at half
  the angle (a Copy Rotation constraint at 0.5 in Blender, `Biped.settleHelpers` in the mod); do not key them.
- Flexing: the elbow bends with a negative X rotation of `Hand_*`, the knee with a positive X rotation of `Leg_*`.

## Mobs

`mobs/<name>.py` lists a mob's parts in relief on the biped's skeleton and paints its texture in code
(`murim_mob.py`: packing, painting helpers, skinning; rules in `docs/art/visual-rules.md`). One command paints the
texture, exports the model and films it from every side, close up and in a pose:

```
blender -b --factory-startup --python tools/blender/mob.py -- --mob disciple \
    --texture src/main/resources/assets/murimblock/textures/entity/disciple.png \
    --gltf src/main/resources/assets/murimblock/skeletal/mobs/disciple.gltf --renders <dir>
```
