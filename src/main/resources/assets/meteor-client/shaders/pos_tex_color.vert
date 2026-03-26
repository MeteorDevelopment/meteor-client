#version 330 core

in vec4 Position;
in vec2 Texture;
in vec4 Color;

layout (std140) uniform MeshData {
    mat4 u_Proj;
    mat4 u_ModelView;
};

out vec2 v_TexCoord;
out vec4 v_Color;

void main() {
    gl_Position = u_Proj * u_ModelView * Position;

    v_TexCoord = Texture;
    v_Color = Color;
}
