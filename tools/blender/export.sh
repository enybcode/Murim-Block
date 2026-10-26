#!/bin/sh
# Rebuilds the biped in Blender and exports both arm widths into the mod's assets.
# Usage: sh tools/blender/export.sh  (BLENDER=path/to/blender.exe to override)
set -e
cd "$(dirname "$0")/../.."
BLENDER="${BLENDER:-/c/Program Files/Blender Foundation/Blender 5.2/blender.exe}"
OUT=src/main/resources/assets/murimblock/skeletal
"$BLENDER" -b --factory-startup --python tools/blender/build.py -- --gltf "$OUT/biped.gltf" --blend tools/blender/out/murim_biped.blend
"$BLENDER" -b --factory-startup --python tools/blender/build.py -- --slim --gltf "$OUT/biped_slim.gltf" --blend tools/blender/out/murim_biped_slim.blend
