// Evaluated in the isolated Blockbench (Chromium) by make-video.cjs: composes the original background music (Web Audio,
// synthesised here: no sample, no third-party track) and edits the filmed frames into a vertical video with titles.
window.MV = (() => {
  // ---------------------------------------------------------------- music
  const SR = 44100, BPM = 84, BEAT = 60 / BPM;
  // D major pentatonic (the Chinese gong mode): D E F# A B, over several octaves.
  const PENTA = [0, 2, 4, 7, 9];
  const freq = (degree, base = 62) => { // degree 0 = D4 (MIDI 62)
    const oct = Math.floor(degree / 5), step = ((degree % 5) + 5) % 5;
    const midi = base + oct * 12 + PENTA[step];
    return 440 * Math.pow(2, (midi - 69) / 12);
  };

  /** Plucked string (Karplus-Strong): a guzheng-like tone with a bright attack and a long decay. */
  function pluck(ctx, f, seconds = 2.6, brightness = 0.996) {
    const len = Math.floor(SR * seconds), buf = ctx.createBuffer(1, len, SR), y = buf.getChannelData(0);
    const n = Math.max(2, Math.round(SR / f));
    const ring = new Float32Array(n);
    for (let i = 0; i < n; i++) ring[i] = Math.random() * 2 - 1;
    let idx = 0, prev = 0;
    for (let i = 0; i < len; i++) {
      const cur = ring[idx];
      const next = ring[(idx + 1) % n];
      const v = 0.5 * (cur + next) * brightness;
      ring[idx] = v;
      idx = (idx + 1) % n;
      // A touch of body: soft low-pass and an attack transient.
      prev = prev + 0.55 * (cur - prev);
      y[i] = prev * (i < 60 ? i / 60 : 1);
    }
    return buf;
  }
  function drum(ctx, low = true) {
    const len = Math.floor(SR * (low ? 0.9 : 0.12)), buf = ctx.createBuffer(1, len, SR), y = buf.getChannelData(0);
    for (let i = 0; i < len; i++) {
      const t = i / SR;
      if (low) { const f = 55 + 70 * Math.exp(-t * 18); y[i] = Math.sin(2 * Math.PI * f * t) * Math.exp(-t * 4.5) * 0.9 + (Math.random() * 2 - 1) * Math.exp(-t * 60) * 0.15; }
      else y[i] = (Math.random() * 2 - 1) * Math.exp(-t * 70) * 0.5 + Math.sin(2 * Math.PI * 1150 * t) * Math.exp(-t * 45) * 0.5; // wood block
    }
    return buf;
  }
  function impulse(ctx, seconds = 3.2) {
    const len = Math.floor(SR * seconds), buf = ctx.createBuffer(2, len, SR);
    for (let c = 0; c < 2; c++) { const y = buf.getChannelData(c); for (let i = 0; i < len; i++) y[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / len, 3.2); }
    return buf;
  }

  // Phrases: [degree, start beat, length in beats] over 8 beats (two bars). Degree 5 = D5.
  const PH = {
    A1: [[5, 0, 1], [4, 1, .5], [3, 1.5, .5], [4, 2, 1.5], [3, 3.5, .5], [2, 4, 1], [3, 5, 1], [1, 6, 2]],
    A2: [[5, 0, 1], [6, 1, .5], [7, 1.5, .5], [8, 2, 1.5], [7, 3.5, .5], [6, 4, .75], [5, 4.75, .25], [4, 5, 1], [5, 6, 2]],
    B1: [[8, 0, .5], [7, .5, .5], [6, 1, .5], [5, 1.5, .5], [4, 2, 1], [5, 3, 1], [3, 4, .5], [2, 4.5, .5], [3, 5, 1], [4, 6, 2]],
    B2: [[3, 0, 1], [5, 1, 1], [4, 2, .5], [3, 2.5, .5], [2, 3, 1], [1, 4, 1], [2, 5, .5], [1, 5.5, .5], [0, 6, 2]],
    END: [[5, 0, 1.5], [4, 1.5, .5], [3, 2, 1], [5, 3, 1], [5, 4, 4]],
  };
  // Chord roots for the accompaniment, per phrase: two bars each.
  const ROOT = { A1: [0, 3], A2: [4, 0], B1: [3, 4], B2: [1, 0], END: [0, 0] };

  /** Renders the score into an AudioBuffer of `seconds` (the video length). */
  async function compose(seconds) {
    const ctx = new OfflineAudioContext(2, Math.ceil(SR * seconds), SR);
    const master = ctx.createDynamicsCompressor();
    master.threshold.value = -16; master.ratio.value = 3;
    const out = ctx.createGain(); out.gain.value = 1.45;
    master.connect(out); out.connect(ctx.destination);
    const verb = ctx.createConvolver(); verb.buffer = impulse(ctx);
    const verbGain = ctx.createGain(); verbGain.gain.value = 0.32;
    verb.connect(verbGain); verbGain.connect(master);
    const bus = (gain, pan = 0, send = 0.6) => {
      const g = ctx.createGain(); g.gain.value = gain;
      const p = ctx.createStereoPanner(); p.pan.value = pan;
      g.connect(p); p.connect(master);
      const s = ctx.createGain(); s.gain.value = send; p.connect(s); s.connect(verb);
      return g;
    };
    const lead = bus(0.55, 0.1, 0.7), harp = bus(0.28, -0.25, 0.5), padBus = bus(0.16, 0, 0.9), drums = bus(0.6, 0, 0.25), wood = bus(0.18, 0.35, 0.3);
    const cache = {};
    const note = (target, degree, t, vel = 1, base = 62) => {
      const k = degree + ':' + base;
      if (!cache[k]) cache[k] = pluck(ctx, freq(degree, base));
      const src = ctx.createBufferSource(); src.buffer = cache[k];
      const g = ctx.createGain(); g.gain.value = vel;
      src.connect(g); g.connect(target); src.start(t);
    };
    const pad = (degrees, t, dur) => {
      for (const d of degrees) for (const det of [-4, 4]) {
        const o = ctx.createOscillator(); o.type = 'triangle'; o.frequency.value = freq(d, 50); o.detune.value = det;
        const lp = ctx.createBiquadFilter(); lp.type = 'lowpass'; lp.frequency.value = 900;
        const g = ctx.createGain();
        g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(0.18, t + 1.2);
        g.gain.setValueAtTime(0.18, t + dur - 1.0); g.gain.linearRampToValueAtTime(0, t + dur + 0.6);
        o.connect(lp); lp.connect(g); g.connect(padBus); o.start(t); o.stop(t + dur + 0.7);
      }
    };
    const dl = drum(ctx, true), wb = drum(ctx, false);
    const hit = (buf, target, t, vel = 1) => { const s = ctx.createBufferSource(); s.buffer = buf; const g = ctx.createGain(); g.gain.value = vel; s.connect(g); g.connect(target); s.start(t); };

    // Form (two-bar phrases, 5.71 s each): intro, A A' B B', A A' with drums, B B' (night, soft), END.
    const form = [['INTRO'], ['A1'], ['A2'], ['B1'], ['B2'], ['A1', 'drums'], ['A2', 'drums'], ['B1', 'drums'], ['B2', 'soft'], ['A1', 'soft'], ['END']];
    let t0 = 0.4;
    const phraseLen = 8 * BEAT;
    for (const [name, mood] of form) {
      const roots = ROOT[name] || [0, 3];
      pad([roots[0], roots[0] + 3], t0, phraseLen * (name === 'END' ? 1.4 : 1));
      // Guzheng broken chords in the low register: root, fifth, octave, fifth on each half beat.
      if (name !== 'END') for (let b = 0; b < 8; b++) {
        const r = roots[b < 4 ? 0 : 1];
        const pattern = [r, r + 3, r + 5, r + 3];
        for (let h = 0; h < 2; h++) note(harp, pattern[(b * 2 + h) % 4] - 5, t0 + (b + h * 0.5) * BEAT, mood === 'soft' ? 0.5 : 0.75);
      }
      if (name === 'INTRO') {
        // A rising glissando across the pentatonic scale under the title.
        for (let i = 0; i < 12; i++) note(lead, i - 2, t0 + 0.6 + i * 0.09, 0.45 + i * 0.03);
        note(lead, 10, t0 + 2.2, 0.9);
      } else {
        for (const [deg, start, len] of PH[name]) {
          const t = t0 + start * BEAT;
          if (len >= 1.5) { note(lead, deg - 2, t - 0.11, 0.35); note(lead, deg - 1, t - 0.055, 0.45); } // grace slide
          note(lead, deg, t, mood === 'soft' ? 0.65 : 0.9);
          if (len >= 2 && mood !== 'soft') note(lead, deg, t + BEAT, 0.3); // tremolo echo
        }
      }
      if (mood === 'drums') for (let b = 0; b < 8; b++) {
        if (b % 4 === 0) hit(dl, drums, t0 + b * BEAT, 1);
        if (b % 4 === 2) hit(dl, drums, t0 + b * BEAT, 0.55);
        hit(wb, wood, t0 + (b + 0.5) * BEAT, b % 2 ? 0.7 : 0.4);
      }
      if (name === 'A1' && !mood) hit(dl, drums, t0, 0.6);
      if (name === 'END') { hit(dl, drums, t0, 0.8); hit(dl, drums, t0 + 4 * BEAT, 0.6); }
      t0 += phraseLen;
    }
    // Fade the very end.
    out.gain.setValueAtTime(1.45, Math.max(0, seconds - 2.5));
    out.gain.linearRampToValueAtTime(0, seconds);
    return ctx.startRendering();
  }

  function wav(buffer) {
    const n = buffer.length, ch = buffer.numberOfChannels, data = new DataView(new ArrayBuffer(44 + n * ch * 2));
    const w = (o, s) => [...s].forEach((c, i) => data.setUint8(o + i, c.charCodeAt(0)));
    w(0, 'RIFF'); data.setUint32(4, 36 + n * ch * 2, true); w(8, 'WAVE'); w(12, 'fmt ');
    data.setUint32(16, 16, true); data.setUint16(20, 1, true); data.setUint16(22, ch, true); data.setUint32(24, SR, true);
    data.setUint32(28, SR * ch * 2, true); data.setUint16(32, ch * 2, true); data.setUint16(34, 16, true); w(36, 'data'); data.setUint32(40, n * ch * 2, true);
    const chans = [...Array(ch)].map((_, c) => buffer.getChannelData(c));
    let o = 44;
    for (let i = 0; i < n; i++) for (let c = 0; c < ch; c++) { data.setInt16(o, Math.max(-1, Math.min(1, chans[c][i])) * 0x7fff, true); o += 2; }
    return new Blob([data], { type: 'audio/wav' });
  }

  // ---------------------------------------------------------------- video
  const channel = new MessageChannel();
  let wake = null;
  channel.port1.onmessage = () => { const w = wake; wake = null; if (w) w(); };
  /** Yields to the event loop without timer throttling. */
  const tick = () => new Promise(r => { wake = r; channel.port2.postMessage(0); });
  const load = src => new Promise((res, rej) => { const i = new Image(); i.onload = () => res(i); i.onerror = rej; i.src = src; });
  const ease = t => t * t * (3 - 2 * t);
  const clamp01 = t => Math.max(0, Math.min(1, t));

  function drawTitleCard(g, W, H, t, end) {
    // Big centred title with the two characters 武林 (wulin, the martial world) above it.
    const a = end ? clamp01(t / 0.8) : clamp01(t / 0.5);
    g.save();
    g.globalAlpha = a;
    const grad = g.createLinearGradient(0, 0, 0, H);
    grad.addColorStop(0, 'rgba(10,14,12,0.55)'); grad.addColorStop(0.5, 'rgba(10,14,12,0.15)'); grad.addColorStop(1, 'rgba(10,14,12,0.6)');
    g.fillStyle = grad; g.fillRect(0, 0, W, H);
    g.textAlign = 'center';
    g.fillStyle = '#B8322A';
    g.font = `${Math.round(W * 0.2)}px "KaiTi","STKaiti","SimSun","Microsoft YaHei",serif`;
    g.fillText('武林', W / 2, H * 0.36);
    const scale = 1 + 0.04 * (1 - ease(a));
    g.translate(W / 2, H * 0.47); g.scale(scale, scale);
    g.font = `bold ${Math.round(W * 0.115)}px "Georgia","Times New Roman",serif`;
    g.shadowColor = 'rgba(0,0,0,0.7)'; g.shadowBlur = 24;
    const gold = g.createLinearGradient(0, -W * 0.08, 0, W * 0.02);
    gold.addColorStop(0, '#F6E2A0'); gold.addColorStop(0.55, '#D4A93A'); gold.addColorStop(1, '#9C7420');
    g.fillStyle = gold;
    g.fillText('MURIMBLOCK', 0, 0);
    g.shadowBlur = 0;
    g.fillStyle = '#F3EBD4';
    g.font = `${Math.round(W * 0.038)}px "Georgia",serif`;
    g.fillText(end ? 'Bientôt : structures, sectes et techniques' : 'Un mod wuxia pour Minecraft', 0, W * 0.09);
    g.strokeStyle = '#C9A74B'; g.lineWidth = 3;
    g.beginPath(); g.moveTo(-W * 0.3, W * 0.035); g.lineTo(W * 0.3, W * 0.035); g.stroke();
    g.restore();
  }

  function drawCaption(g, W, H, shot, t, dur) {
    const a = clamp01(t / 0.45) * clamp01((dur - t) / 0.45);
    if (a <= 0) return;
    g.save();
    g.globalAlpha = a;
    const y = H * 0.78;
    const band = g.createLinearGradient(0, y - H * 0.08, 0, H);
    band.addColorStop(0, 'rgba(10,14,12,0)'); band.addColorStop(0.35, 'rgba(10,14,12,0.55)'); band.addColorStop(1, 'rgba(10,14,12,0.75)');
    g.fillStyle = band; g.fillRect(0, y - H * 0.08, W, H);
    const slide = (1 - ease(clamp01(t / 0.6))) * 40;
    g.textAlign = 'left';
    g.fillStyle = '#C9A74B'; g.fillRect(W * 0.07, y - W * 0.075, W * 0.12, 5);
    g.shadowColor = 'rgba(0,0,0,0.8)'; g.shadowBlur = 12;
    g.fillStyle = '#F3EBD4';
    g.font = `bold ${Math.round(W * 0.066)}px "Georgia",serif`;
    g.fillText(shot.title, W * 0.07 + slide, y);
    g.fillStyle = '#E2D3A8';
    g.font = `${Math.round(W * 0.04)}px "Georgia",serif`;
    g.fillText(shot.subtitle, W * 0.07 + slide, y + W * 0.065);
    g.restore();
  }

  /**
   * Plays the frames at 20 fps into a canvas and records it with the music. frameUrl(i) gives file URLs.
   * Returns the recorded video as a data URL.
   */
  async function record({ frameCount, frameUrl, shots, W, H, bitrate, music, mime }) {
    const canvas = document.createElement('canvas'); canvas.width = W; canvas.height = H;
    const g = canvas.getContext('2d');
    g.imageSmoothingEnabled = true; g.imageSmoothingQuality = 'high';
    const stream = canvas.captureStream(0);
    const track = stream.getVideoTracks()[0];
    const actx = new AudioContext({ sampleRate: SR });
    const dest = actx.createMediaStreamDestination();
    const src = actx.createBufferSource(); src.buffer = music; src.connect(dest);
    stream.addTrack(dest.stream.getAudioTracks()[0]);
    const rec = new MediaRecorder(stream, { mimeType: mime, videoBitsPerSecond: bitrate, audioBitsPerSecond: 160000 });
    const chunks = [];
    rec.ondataavailable = e => { if (e.data.size) chunks.push(e.data); };
    const shotAt = i => shots.find(s => i >= s.start && i <= s.end);
    // Preload ahead so drawing never waits on the disk.
    const cache = new Map();
    const want = i => { if (i < frameCount && !cache.has(i)) cache.set(i, load(frameUrl(i))); };
    for (let i = 0; i < 40; i++) want(i);
    rec.start(1000);
    await actx.resume();
    const t0 = performance.now() + 100;
    src.start(actx.currentTime + 0.1);
    let prevLast = null;
    for (let i = 0; i < frameCount; i++) {
      want(i + 40);
      const img = await cache.get(i); cache.delete(i);
      const shot = shotAt(i) || shots[shots.length - 1];
      const local = (i - shot.start) / 20, dur = (shot.end - shot.start + 1) / 20;
      g.globalAlpha = 1;
      // Menu shot: zoom on the GUI in the middle.
      const zoom = shot.name === 'menu' ? 1.9 : 1;
      const dw = W * zoom, dh = H * zoom;
      g.drawImage(img, (W - dw) / 2, (H - dh) / 2, dw, dh);
      // Short cross-fade from the previous shot's last frame.
      if (i - shot.start < 6 && prevLast && shot.start > 0) {
        g.globalAlpha = 1 - (i - shot.start + 1) / 7;
        g.drawImage(prevLast, 0, 0, W, H);
        g.globalAlpha = 1;
      }
      if (i === shot.end) {
        // Keep a copy of the composed frame for the next cross-fade.
        const c = document.createElement('canvas'); c.width = W; c.height = H; c.getContext('2d').drawImage(canvas, 0, 0); prevLast = c;
      }
      if (shot.name === 'title') drawTitleCard(g, W, H, local, false);
      else if (shot.name === 'end') drawTitleCard(g, W, H, local, true);
      else drawCaption(g, W, H, shot, local, dur);
      // Fade from and to black.
      const fadeIn = clamp01(i / 12), fadeOut = clamp01((frameCount - 1 - i) / 24);
      if (fadeIn < 1 || fadeOut < 1) { g.fillStyle = `rgba(0,0,0,${1 - Math.min(fadeIn, fadeOut)})`; g.fillRect(0, 0, W, H); }
      track.requestFrame();
      // Pace with message-channel turns, not setTimeout: Chromium throttles timers to 1 Hz in a background window,
      // which turned the first edit into a one-frame-per-second slideshow.
      const due = t0 + (i + 1) * 50;
      while (performance.now() < due) await tick();
    }
    const settle = performance.now() + 400;
    while (performance.now() < settle) await tick();
    const done = new Promise(r => rec.onstop = r);
    rec.stop();
    await done;
    src.stop(); actx.close();
    // Kept here; make-video.cjs reads it back in slices (one huge data URL would choke the debugging socket).
    window.__video = new Blob(chunks, { type: mime });
    return window.__video.size;
  }

  /** Slice i (8 MB) of the recorded video as a data URL. */
  const SLICE = 2 * 1024 * 1024;
  async function slice(i) {
    const b = window.__video.slice(i * SLICE, (i + 1) * SLICE);
    return await new Promise(res => { const r = new FileReader(); r.onload = () => res(r.result); r.readAsDataURL(b); });
  }

  /** Keeps a blob for slice() (the music as WAV, before the videos). */
  function stash(blob) { window.__video = blob; return blob.size; }

  return { compose, wav, record, slice, SLICE, stash, blobToDataUrl: b => new Promise(res => { const r = new FileReader(); r.onload = () => res(r.result); r.readAsDataURL(b); }) };
})();
true;
