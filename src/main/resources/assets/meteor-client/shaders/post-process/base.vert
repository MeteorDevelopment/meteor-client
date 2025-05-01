#version 330 core

layout (location = 0) in vec4 pos;

uniform PostData {
    vec2 size;
    float time;
} u_Post;

out vec2 v_TexCoord;
out vec2 v_OneTexel;

void main() {
    gl_Position = pos;

    v_TexCoord = (pos.xy + 1.0) / 2.0;
    v_OneTexel = 1.0 / u_Post.size;
}
