#version 330
#extension GL_ARB_separate_shader_objects : require

precision lowp float;

layout (location = 0) in vec2 uv;
layout (location = 0) out vec4 color;

uniform sampler2D u_Texture;

void main() {
    color = texture(u_Texture, uv);
}
