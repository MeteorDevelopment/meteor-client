#version 330 core

out vec4 color;

in vec2 v_TexCoord;
in vec2 v_OneTexel;

uniform sampler2D u_Texture;
uniform bool u_Horizontal;

const float offset[3] = float[] (0.0, 1.3846153846, 3.2307692308);
const float weight[3] = float[] (0.2270270270, 0.3162162162, 0.0702702703);

void main() {
    vec3 result = texture(u_Texture, v_TexCoord).rgb * weight[0];

    if (u_Horizontal) {
        for (int i = 1; i < 3; i++) {
            result += texture(u_Texture, v_TexCoord + vec2(v_OneTexel.x * offset[i], 0.0)).rgb * weight[i];
            result += texture(u_Texture, v_TexCoord - vec2(v_OneTexel.x * offset[i], 0.0)).rgb * weight[i];
        }
    }
    else {
        for (int i = 1; i < 3; i++) {
            result += texture(u_Texture, v_TexCoord + vec2(0.0, v_OneTexel.y * offset[i])).rgb * weight[i];
            result += texture(u_Texture, v_TexCoord - vec2(0.0, v_OneTexel.y * offset[i])).rgb * weight[i];
        }
    }

    color = vec4(result, 1.0);
}
