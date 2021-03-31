#version 130

out vec4 color;

uniform sampler2D u_Texture;
uniform vec2 u_Size;
uniform vec2 u_Offset;
uniform vec2 u_HalfPixel;

void main() {
	vec2 uv = gl_FragCoord.xy / u_Size;

	vec4 sum = texture(u_Texture, uv) * 4.0;
	sum += texture(u_Texture, uv - u_HalfPixel.xy * u_Offset);
    sum += texture(u_Texture, uv + u_HalfPixel.xy * u_Offset);
    sum += texture(u_Texture, uv + vec2(u_HalfPixel.x, -u_HalfPixel.y) * u_Offset);
    sum += texture(u_Texture, uv - vec2(u_HalfPixel.x, -u_HalfPixel.y) * u_Offset);

	color = sum / 8.0;
}