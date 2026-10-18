#version 150

// The aura overflowing (client/cultivation/PressureWaveClient, Enzo 18/10/2026, after Bleach): space ripples out from a
// cultivator holding too much Qi, a ring of bent light spreading over the screen and fading.

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
// Where the wave starts on the screen (0 to 1), how far it has spread (screen heights), how hard it bends.
uniform float CenterX;
uniform float CenterY;
uniform float Radius;
uniform float Strength;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    float aspect = OutSize.x / OutSize.y;
    vec2 d = texCoord - vec2(CenterX, CenterY);
    d.x *= aspect;
    float r = length(d);
    // A ring, thicker as it spreads; inside it the light is pulled outward, then let go.
    float width = 0.04 + Radius * 0.12;
    float ring = 1.0 - smoothstep(0.0, width, abs(r - Radius));
    vec2 dir = r > 0.0001 ? d / r : vec2(0.0);
    vec2 offset = dir * ring * Strength * 0.025 * sin((r - Radius) / width * 3.14159);
    offset.x /= aspect;
    vec3 c = texture(DiffuseSampler, texCoord - offset).rgb;
    // The ring's edge, faintly brightened, like heat in the air.
    c *= 1.0 + ring * Strength * 0.08;
    fragColor = vec4(c, 1.0);
}
