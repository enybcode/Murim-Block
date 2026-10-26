# Blender tools

Scripts that build, export and film the biped of the combat engine (`docs/combat/engine-roadmap.md`). They run
headless; nothing here goes into the jar.

Needs Blender 5.2 (`C:\Program Files\Blender Foundation\Blender 5.2\blender.exe`; set `BLENDER` to override).

- `murim_biped.py`: the skeleton (Epic Fight's 20-joint layout) and the body, built in code. Must match
  `com.murimblock.anim.Biped`.
- `sh tools/blender/export.sh`: rebuilds both arm widths, writes `assets/murimblock/skeletal/biped*.gltf` and
  `.bin` for the mod, and `tools/blender/out/murim_biped*.blend` to animate (not committed).
- `render.py`: films a test clip from the front, the side and three quarters with a UV test skin (or `--skin`):

  ```
  blender -b --factory-startup --python tools/blender/render.py -- --out <dir> --clip joints
  java scripts/frames-to-gif.java <dir> out.gif 60 960
  ```

  Clips: `rest`, `bend`, `joints`, `elbow`, `knee`.

## Rig rules

- Bones point straight up with no roll, so each joint's rest rotation is the identity in the mod.
- Limbs are rigid. `Elbow_*` and `Knee_*` are helpers that follow the forearm (`Hand_*`) or shin (`Leg_*`) at half
  the angle (a Copy Rotation constraint at 0.5 in Blender, `Biped.settleHelpers` in the mod); do not key them.
- Flexing: the elbow bends with a negative X rotation of `Hand_*`, the knee with a positive X rotation of `Leg_*`.
