#!/bin/sh
# Contact sheet of a reference clip, one picture per game tick (20 a second), to read its poses and timings:
# sheet.sh <video> <start s> <seconds> <out.png> [crop w:h:x:y] [columns, 6]. Also keeps the frames in <out>-frames/.
# Needs ffmpeg. Reference clips stay out of the repository (put them in run/references/).
set -e
video=$1; start=$2; length=$3; out=$4; crop=${5:-iw:ih:0:0}; cols=${6:-6}
frames="${out%.png}-frames"
mkdir -p "$frames"
ffmpeg -v error -y -ss "$start" -t "$length" -i "$video" -vf "fps=20,crop=$crop,scale=320:-2" "$frames/%03d.png"
count=$(ls "$frames" | wc -l)
rows=$(( (count + cols - 1) / cols ))
ffmpeg -v error -y -framerate 20 -i "$frames/%03d.png" -vf "tile=${cols}x${rows}:padding=4:color=white" -frames:v 1 "$out"
echo "$out: $count ticks from ${start}s (frame 001 = tick 0)"
