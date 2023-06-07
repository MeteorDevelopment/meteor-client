#version 330 core

precision lowp float;

in vec2 uv;
out vec4 color;

uniform sampler2D uTexture;
uniform vec2 uHalfTexelSize;
uniform float uOffset;

void main() {
    color = (
        texture(uTexture, uv) * 4 +
        texture(uTexture, uv - uHalfTexelSize.xy * uOffset) +
        texture(uTexture, uv + uHalfTexelSize.xy * uOffset) +
        texture(uTexture, uv + vec2(uHalfTexelSize.x, -uHalfTexelSize.y) * uOffset) +
        texture(uTexture, uv - vec2(uHalfTexelSize.x, -uHalfTexelSize.y) * uOffset)
    ) / 8;
    color.a = 1;
}
