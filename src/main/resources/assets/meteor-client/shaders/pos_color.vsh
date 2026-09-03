#version 330
#extension GL_ARB_separate_shader_objects : require

layout (location = 0) in vec4 Position;
layout (location = 1) in vec4 Color;

layout (std140) uniform MeshData {
    mat4 u_Proj;
    mat4 u_ModelView;
};

layout (location = 0) out vec4 v_Color;

void main() {
    gl_Position = u_Proj * u_ModelView * Position;

    v_Color = Color;
}
