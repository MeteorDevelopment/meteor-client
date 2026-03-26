#version 330 core

in vec4 Position;

layout (std140) uniform PostData {
    vec2 u_Size;
    float u_Time;
};

out vec2 v_TexCoord;
out vec2 v_OneTexel;

void main() {
    gl_Position = Position;

    v_TexCoord = (Position.xy + 1.0) / 2.0;
    v_OneTexel = 1.0 / u_Size;
}
