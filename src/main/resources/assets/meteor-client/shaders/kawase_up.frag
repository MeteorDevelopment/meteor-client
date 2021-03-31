#version 130

out vec4 color;

uniform sampler2D u_Texture;
uniform vec2 u_Size;
uniform vec2 u_Offset;
uniform vec2 u_HalfPixel;
uniform float u_Alpha;

void main() {
	vec2 uv = gl_FragCoord.xy / u_Size;

	vec4 sum = texture(u_Texture, uv + vec2(-u_HalfPixel.x * 2.0, 0.0) * u_Offset);
    sum += texture(u_Texture, uv + vec2(-u_HalfPixel.x, u_HalfPixel.y) * u_Offset) * 2.0;
    sum += texture(u_Texture, uv + vec2(0.0, u_HalfPixel.y * 2.0) * u_Offset);
    sum += texture(u_Texture, uv + vec2(u_HalfPixel.x, u_HalfPixel.y) * u_Offset) * 2.0;
    sum += texture(u_Texture, uv + vec2(u_HalfPixel.x * 2.0, 0.0) * u_Offset);
    sum += texture(u_Texture, uv + vec2(u_HalfPixel.x, -u_HalfPixel.y) * u_Offset) * 2.0;
    sum += texture(u_Texture, uv + vec2(0.0, -u_HalfPixel.y * 2.0) * u_Offset);
    sum += texture(u_Texture, uv + vec2(-u_HalfPixel.x, -u_HalfPixel.y) * u_Offset) * 2.0;

    sum /= 12.0;
    color = vec4(sum.rgb, sum.a * u_Alpha);
}