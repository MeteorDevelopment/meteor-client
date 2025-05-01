#version 330 core

precision lowp float;

in vec2 uv;
out vec4 color;

uniform sampler2D u_Texture;

uniform BlurData {
    vec2 halfTexelSize;
    float offset;
} u_Blur;

void main() {
    color = (
        texture(u_Texture, uv) * 4 +
        texture(u_Texture, uv - u_Blur.halfTexelSize * u_Blur.offset) +
        texture(u_Texture, uv + u_Blur.halfTexelSize * u_Blur.offset) +
        texture(u_Texture, uv + vec2(u_Blur.halfTexelSize.x, -u_Blur.halfTexelSize.y) * u_Blur.offset) +
        texture(u_Texture, uv - vec2(u_Blur.halfTexelSize.x, -u_Blur.halfTexelSize.y) * u_Blur.offset)
    ) / 8;
    color.a = 1;
}
