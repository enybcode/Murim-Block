#version 150

// Qi vision (client/cultivation/QiVisionClient), as if blind (Enzo, 18/10/2026: "comme si on était aveugle", then "plutôt
// des nuances de gris"): the world in shades of grey without its textures (the view blurred, SoftSampler: lighter where
// lit, darker into the distance, the sky black), the outlines of things drawn in white (where the depth breaks, and where
// the light of a face turns), fading with distance; only the Qi keeps its colour: bright, strongly coloured light, the
// auras of the living and the currents of Qi.

uniform sampler2D DiffuseSampler;
uniform sampler2D SoftSampler;
uniform sampler2D DepthSampler;
// How far one sees, by realm (blocks): beyond, all is black.
uniform float SeeFar;
// The columns of the powerful (QiVisionClient.columns): on the screen, each one's middle, foot, top, half-width; its colour.
uniform float Time;
uniform float Columns;
uniform float Col0X, Col0Foot, Col0Top, Col0Width, Col0R, Col0G, Col0B;
uniform float Col1X, Col1Foot, Col1Top, Col1Width, Col1R, Col1G, Col1B;
uniform float Col2X, Col2Foot, Col2Top, Col2Width, Col2R, Col2G, Col2B;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

/** The depth buffer back to blocks from the eye (near plane 0.05, far plane well beyond). */
float dist(vec2 at) {
    float d = texture(DepthSampler, at).r;
    return 0.05 * 4096.0 / (4096.0 - d * (4096.0 - 0.05));
}

float lum(vec2 at) {
    return dot(texture(DiffuseSampler, at).rgb, vec3(0.3, 0.59, 0.11));
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

/** One column of spiritual pressure: a wide shaft of light, edges flickering like flames, a bright core, fading up. */
vec3 shaft(vec2 uv, float x, float foot, float top, float width, vec3 tint) {
    if (top <= foot || width <= 0.0) return vec3(0.0);
    float t = (uv.y - foot) / (top - foot);
    if (t < -0.05 || t > 1.0) return vec3(0.0);
    // The edges waver: the width breathes with the height and the time, as tongues of flame climbing.
    float flicker = noise(vec2(uv.x / width * 1.5, t * 7.0 - Time * 2.2)) * 0.55 + noise(vec2(uv.x / width * 4.0, t * 18.0 - Time * 4.0)) * 0.25;
    float w = width * (0.75 + flicker * 0.7) * (1.0 - 0.35 * t);
    float d = abs(uv.x - x) / w;
    float body = exp(-d * d * 2.2) * (0.55 + 0.45 * noise(vec2(uv.x / width * 3.0, t * 10.0 - Time * 3.0)));
    float core = exp(-d * d * 14.0);
    float fade = smoothstep(-0.05, 0.06, t) * (1.0 - smoothstep(0.6, 1.0, t));
    return (tint * body * 0.9 + mix(tint, vec3(1.0), 0.6) * core * 0.6) * fade;
}

void main() {
    vec3 c = texture(DiffuseSampler, texCoord).rgb;
    float here = dist(texCoord);
    // Where the depth breaks: the silhouettes of things against what lies behind them.
    float breaks = 0.0;
    for (int i = 0; i < 4; i++) {
        vec2 o = oneTexel * (i == 0 ? vec2(1.0, 0.0) : i == 1 ? vec2(-1.0, 0.0) : i == 2 ? vec2(0.0, 1.0) : vec2(0.0, -1.0));
        breaks = max(breaks, abs(dist(texCoord + o) - here) / here);
    }
    float silhouette = smoothstep(0.03, 0.08, breaks);
    // Where the light of a face turns (the creases between faces of one block): a strong step of light, both sides near.
    float l = lum(texCoord);
    float step = max(max(abs(lum(texCoord + vec2(oneTexel.x, 0.0)) - l), abs(lum(texCoord - vec2(oneTexel.x, 0.0)) - l)),
                     max(abs(lum(texCoord + vec2(0.0, oneTexel.y)) - l), abs(lum(texCoord - vec2(0.0, oneTexel.y)) - l)));
    float crease = smoothstep(0.22, 0.4, step) * 0.2;
    // Lines fade with distance; the sky has none.
    float line = max(silhouette, crease) * (1.0 - smoothstep(SeeFar * 0.45, SeeFar, min(here, dist(texCoord + oneTexel))));
    // Shades of grey: the blurred light, darkening into the distance; the sky black.
    float soft = dot(texture(SoftSampler, texCoord).rgb, vec3(0.3, 0.59, 0.11));
    float grey = texture(DepthSampler, texCoord).r >= 1.0 ? 0.0 : (0.04 + 0.34 * soft) * (1.0 - smoothstep(SeeFar * 0.2, SeeFar, here));
    vec3 world = mix(vec3(grey), vec3(0.5, 0.52, 0.56), line * 0.8);
    // The Qi keeps its colour: bright, strongly coloured light.
    float mx = max(c.r, max(c.g, c.b)), mn = min(c.r, min(c.g, c.b));
    float sat = (mx - mn) / max(mx, 0.001);
    float qi = smoothstep(0.55, 0.8, sat) * smoothstep(0.6, 0.85, mx);
    vec3 shafts = vec3(0.0);
    if (Columns > 0.5) shafts += shaft(texCoord, Col0X, Col0Foot, Col0Top, Col0Width, vec3(Col0R, Col0G, Col0B));
    if (Columns > 1.5) shafts += shaft(texCoord, Col1X, Col1Foot, Col1Top, Col1Width, vec3(Col1R, Col1G, Col1B));
    if (Columns > 2.5) shafts += shaft(texCoord, Col2X, Col2Foot, Col2Top, Col2Width, vec3(Col2R, Col2G, Col2B));
    fragColor = vec4(max(world, c * 1.3 * qi) + shafts, 1.0);
}
