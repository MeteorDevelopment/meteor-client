#version 330
#extension GL_ARB_separate_shader_objects : require

layout (location = 0) in vec2 Position;

layout (std140) uniform PostData {
    vec2 u_Size;
    float u_Time;
};

layout (location = 0) out vec2 v_TexCoord;
layout (location = 1) out vec2 v_OneTexel;

void main() {
    gl_Position = vec4(Position, 0.0, 1.0);

    v_TexCoord = Position * 0.5 + 0.5;
    v_OneTexel = 1.0 / u_Size;
}
