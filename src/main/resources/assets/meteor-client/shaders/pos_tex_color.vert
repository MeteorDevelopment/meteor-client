#version 330 core

layout (location = 0) in vec4 pos;
layout (location = 1) in vec2 texCoords;
layout (location = 2) in vec4 color;

uniform MeshData {
    mat4 proj;
    mat4 modelView;
} u_Mesh;

out vec2 v_TexCoord;
out vec4 v_Color;

void main() {
    gl_Position = u_Mesh.proj * u_Mesh.modelView * pos;

    v_TexCoord = texCoords;
    v_Color = color;
}
