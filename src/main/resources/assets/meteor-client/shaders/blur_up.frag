#version 330 core

precision lowp float;

in vec2 uv;
out vec4 color;

uniform sampler2D uTexture;
uniform vec2 uHalfTexelSize;
uniform float uOffset;

void main() {
    color = (
        texture(uTexture, uv + vec2(- uHalfTexelSize.x * 2, 0) * uOffset) +
        texture(uTexture, uv + vec2(- uHalfTexelSize.x, uHalfTexelSize.y) * uOffset) * 2 +
        texture(uTexture, uv + vec2(0, uHalfTexelSize.y * 2) * uOffset) +
        texture(uTexture, uv + uHalfTexelSize * uOffset) * 2 +
        texture(uTexture, uv + vec2(uHalfTexelSize.x * 2, 0) * uOffset) +
        texture(uTexture, uv + vec2(uHalfTexelSize.x, -uHalfTexelSize.y) * uOffset) * 2 +
        texture(uTexture, uv + vec2(0, -uHalfTexelSize.y * 2) * uOffset) +
        texture(uTexture, uv - uHalfTexelSize * uOffset) * 2
    ) / 12;
    color.a = 1;
}
