#version 330
#extension GL_ARB_separate_shader_objects : require

layout (location = 0) out vec4 color;

layout (location = 0) in vec4 v_Color;

void main() {
    color = v_Color;
}
