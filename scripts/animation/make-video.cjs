#!/usr/bin/env node
/*
 * The transformation's video (WarlordTour with murimblock.videoTour: gradlew -PgameTests runTransformVideo): the frames
 * it drew at 30 a second of the film's own time (build/transform-video-run/video/frames) and the sounds it played
 * (video/sounds.tsv: film time in ticks, file, volume, pitch, heard everywhere, distance to the camera) made into an MP4
 * with its soundtrack. Each sound is taken from the game's own assets (the asset index of the dev environment), placed
 * at its time, at its pitch, and as loud as the camera heard it (Minecraft's linear fall-off: 16 blocks times its volume
 * when above 1). The wind of the flight is cut where the film stops it. Needs ffmpeg on the PATH.
 *
 *   node scripts/animation/make-video.cjs [out.mp4]
 */
const fs = require('fs');
const path = require('path');
const os = require('os');
const { execFileSync } = require('child_process');

const ROOT = path.join(__dirname, '..', '..');
const RUN = path.join(ROOT, 'build', 'transform-video-run', 'video');
const OUT = path.resolve(process.argv[2] || path.join(RUN, 'son-of-the-demon.mp4'));
const ASSETS = path.join(os.homedir(), '.gradle', 'caches', 'neoformruntime', 'assets');
const FPS = 30;
/** When the wind stops (film ticks): the viewer's landing (WarlordTransformation.IMPACT). */
const WIND_STOP = 618;

const index = JSON.parse(fs.readFileSync(path.join(ASSETS, 'indexes', '17.json'), 'utf8')).objects;
function file(sound) {
  // minecraft:sounds/mob/warden/heartbeat_1.ogg -> minecraft/sounds/mob/warden/heartbeat_1.ogg
  const [ns, rest] = sound.split(':');
  const key = `${ns}/${rest}`;
  const o = index[key];
  if (!o) return null;
  return path.join(ASSETS, 'objects', o.hash.slice(0, 2), o.hash);
}

const cues = fs.readFileSync(path.join(RUN, 'sounds.tsv'), 'utf8').trim().split(/\r?\n/).filter(Boolean).map(line => {
  const [t, sound, volume, pitch, everywhere, distance, event] = line.split('\t');
  return { t: +t / 20, sound, volume: +volume, pitch: +pitch, everywhere: everywhere === 'true', distance: +distance, event };
});
const inputs = [], filters = [];
for (const c of cues) {
  const f = file(c.sound);
  if (!f) { console.warn('no asset for', c.sound); continue; }
  const range = 16 * Math.max(1, c.volume);
  const gain = Math.min(1, c.volume) * (c.everywhere ? 1 : Math.max(0, 1 - c.distance / range));
  if (gain < 0.02) continue;
  const n = inputs.length / 2;
  inputs.push('-i', f);
  const delay = Math.max(0, Math.round(c.t * 1000));
  const trim = /elytra/.test(c.sound) ? `,atrim=0:${Math.max(0.1, WIND_STOP / 20 - c.t).toFixed(2)},afade=t=out:st=${Math.max(0, WIND_STOP / 20 - c.t - 0.4).toFixed(2)}:d=0.4` : '';
  filters.push(`[${n}:a]aformat=sample_rates=44100:channel_layouts=stereo,asetrate=${Math.round(44100 * c.pitch)},aresample=44100${trim},volume=${gain.toFixed(3)},adelay=${delay}|${delay}[s${n}]`);
}
const frames = fs.readdirSync(path.join(RUN, 'frames')).filter(f => f.endsWith('.png')).length;
const seconds = frames / FPS;
console.log(`${frames} frames (${seconds.toFixed(1)} s), ${inputs.length / 2} sounds`);
// Mixed without normalising (amix would quiet each), then kept from clipping by a limiter.
const mix = `${filters.join(';')};${inputs.map((_, i) => i % 2 === 0 ? `[s${i / 2}]` : '').join('')}amix=inputs=${inputs.length / 2}:normalize=0:dropout_transition=0,alimiter=limit=0.9,atrim=0:${seconds.toFixed(2)}[mix]`;
const script = path.join(RUN, 'mix.txt');
fs.writeFileSync(script, mix);
const audio = path.join(RUN, 'soundtrack.wav');
execFileSync('ffmpeg', ['-y', '-loglevel', 'error', ...inputs, '-/filter_complex', script, '-map', '[mix]', audio], { stdio: 'inherit' });
execFileSync('ffmpeg', ['-y', '-loglevel', 'error', '-framerate', String(FPS), '-i', path.join(RUN, 'frames', 'f-%05d.png'), '-i', audio,
  '-c:v', 'libx264', '-preset', 'slow', '-crf', '18', '-pix_fmt', 'yuv420p', '-c:a', 'aac', '-b:a', '192k', '-shortest', '-movflags', '+faststart', OUT],
  { stdio: 'inherit' });
console.log('wrote', OUT);
