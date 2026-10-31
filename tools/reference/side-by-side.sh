#!/bin/sh
# The reference clip and our in-game film side by side, both at 20 fps and the same height, to compare timings:
# side-by-side.sh <reference video> <start s> <seconds> <our frames dir (engine test gif scene) or video> <out.gif>
# Start both on the same pose (the guard before the first strike). Needs ffmpeg.
set -e
ref=$1; start=$2; length=$3; ours=$4; out=$5
if [ -d "$ours" ]; then
    input="-framerate 25 -i $ours/%04d.png"
else
    input="-i $ours"
fi
ffmpeg -v error -y -ss "$start" -t "$length" -i "$ref" $input -t "$length" -filter_complex \
    "[0]fps=20,scale=-2:300[a];[1]fps=20,scale=-2:300[b];[a][b]hstack,split[x][y];[x]palettegen=stats_mode=diff[p];[y][p]paletteuse" \
    -loop 0 "$out"
echo "$out"
