#version 330 core

precision lowp float;

in vec2 uv;
out vec4 color;

uniform sampler2D u_Texture;

layout (std140) uniform BlurData {
    vec2 u_HalfTexelSize;
    float u_Offset;
};

void main() {
    color = (
        texture(u_Texture, uv) * 4 +
        texture(u_Texture, uv - u_HalfTexelSize * u_Offset) +
        texture(u_Texture, uv + u_HalfTexelSize * u_Offset) +
        texture(u_Texture, uv + vec2(u_HalfTexelSize.x, -u_HalfTexelSize.y) * u_Offset) +
        texture(u_Texture, uv - vec2(u_HalfTexelSize.x, -u_HalfTexelSize.y) * u_Offset)
    ) / 8;
    color.a = 1;
}
