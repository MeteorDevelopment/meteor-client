#version 330 core

layout (location = 0) in vec2 pos;

uniform vec2 u_Size;

out vec2 v_TexCoord;
out vec2 v_OneTexel;

void main() {
    v_TexCoord = (pos.xy + 1.0) / 2.0;
    v_OneTexel = 1.0 / u_Size;

    gl_Position = vec4(pos + v_OneTexel * 0.5, 0.0, 1.0);
}
