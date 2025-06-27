#version 330 core

layout (location = 0) in vec4 pos;
layout (location = 1) in vec2 texCoords;
layout (location = 2) in vec4 color;

layout (std140) uniform MeshData {
    mat4 u_Proj;
    mat4 u_ModelView;
};

out vec2 v_TexCoord;
out vec4 v_Color;

void main() {
    gl_Position = u_Proj * u_ModelView * pos;

    v_TexCoord = texCoords;
    v_Color = color;
}
