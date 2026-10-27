#!/bin/sh
# Makes GIFs of the frames the engine test recorded (gradlew runEngineTest -PengineGifs=true):
# run/engine-test/screenshots/gif/<scene>/*.png -> run/engine-test/screenshots/gif/<scene>.gif. Needs ffmpeg.
cd "$(dirname "$0")/../run/engine-test/screenshots/gif" || exit 1
for scene in */; do
    scene=${scene%/}
    [ -f "$scene/0000.png" ] || continue
    ffmpeg -v error -y -framerate 25 -i "$scene/%04d.png" -t 7 \
        -vf "fps=20,scale=400:-2:flags=lanczos,split[a][b];[a]palettegen=stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4" \
        -loop 0 "$scene.gif" && echo "$scene.gif"
done
