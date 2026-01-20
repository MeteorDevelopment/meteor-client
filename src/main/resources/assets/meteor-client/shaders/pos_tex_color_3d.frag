#version 330 core

out vec4 color;

layout (std140) uniform AnimationData {
    float u_Anim;
};

uniform sampler2DArray u_Texture;

in vec2 v_TexCoord;
in vec4 v_Color;

void main() {
    int layer = int(u_Anim);
    //vec4 tex = texture(u_Texture, vec3(v_TexCoord, layer));
    color = texture(u_Texture, vec3(v_TexCoord, layer)) * v_Color;
}
