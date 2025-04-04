#version 330 core

layout (location = 0) in vec4 pos;
layout (location = 1) in vec4 color;

uniform mat4 u_Proj;
uniform mat4 u_ModelView;

out vec4 v_Color;

void main() {
    gl_Position = u_Proj * u_ModelView * pos;

    v_Color = color;
}
