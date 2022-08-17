#version 330 core

in vec2 v_TexCoord;
in vec2 v_OneTexel;

uniform sampler2D u_Texture;
uniform sampler2D u_TextureI;
uniform vec4 u_Color;

out vec4 color;

void main() {
    if (texture(u_Texture, v_TexCoord).a == 0.0) discard;
    color = texture(u_TextureI, v_TexCoord) * u_Color;
}
