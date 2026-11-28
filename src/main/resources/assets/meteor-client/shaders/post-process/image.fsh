#version 330
#extension GL_ARB_separate_shader_objects : require

layout (location = 0) in vec2 v_TexCoord;
layout (location = 1) in vec2 v_OneTexel;

uniform sampler2D u_Texture;
uniform sampler2D u_TextureI;

layout (std140) uniform ImageData {
    vec4 u_Color;
};

layout (location = 0) out vec4 color;

void main() {
    if (texture(u_Texture, v_TexCoord).a == 0.0) discard;
    color = texture(u_TextureI, v_TexCoord) * u_Color;
}
