#version 330
#extension GL_ARB_separate_shader_objects : require

precision lowp float;

layout (location = 0) in vec2 uv;
layout (location = 0) out vec4 color;

uniform sampler2D u_Texture;

layout (std140) uniform BlurData {
    vec2 u_HalfTexelSize;
    float u_Offset;
};

void main() {
    color = (
        texture(u_Texture, uv + vec2(- u_HalfTexelSize.x * 2, 0) * u_Offset) +
        texture(u_Texture, uv + vec2(- u_HalfTexelSize.x, u_HalfTexelSize.y) * u_Offset) * 2 +
        texture(u_Texture, uv + vec2(0, u_HalfTexelSize.y * 2) * u_Offset) +
        texture(u_Texture, uv + u_HalfTexelSize * u_Offset) * 2 +
        texture(u_Texture, uv + vec2(u_HalfTexelSize.x * 2, 0) * u_Offset) +
        texture(u_Texture, uv + vec2(u_HalfTexelSize.x, -u_HalfTexelSize.y) * u_Offset) * 2 +
        texture(u_Texture, uv + vec2(0, -u_HalfTexelSize.y * 2) * u_Offset) +
        texture(u_Texture, uv - u_HalfTexelSize * u_Offset) * 2
    ) / 12;
    color.a = 1;
}
