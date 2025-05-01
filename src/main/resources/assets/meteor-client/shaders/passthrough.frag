#version 330 core

precision lowp float;

in vec2 uv;
out vec4 color;

uniform sampler2D u_Texture;

void main() {
    color = texture(u_Texture, uv);
}
