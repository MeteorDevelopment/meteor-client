#version 330
#extension GL_ARB_separate_shader_objects : require

#ifdef UI
    #define pos_vec vec2
    #define pos_vec_to_vec4(vec) vec4(vec, 0.0, 1.0)
#else
    #define pos_vec vec3
    #define pos_vec_to_vec4(vec) vec4(vec, 1.0)
#endif

layout (location = 0) in pos_vec Position;
layout (location = 1) in vec4 Color;

layout (std140) uniform MeshData {
    mat4 u_Proj;
    mat4 u_ModelView;
};

layout (location = 0) out vec4 v_Color;

void main() {
    gl_Position = u_Proj * u_ModelView * pos_vec_to_vec4(Position);

    v_Color = Color;
}
