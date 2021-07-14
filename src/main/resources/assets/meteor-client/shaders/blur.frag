#version 330 core

out vec4 color;

in vec2 v_TexCoord;
in vec2 v_OneTexel;

uniform sampler2D u_Texture;
uniform float u_Radius;
uniform vec2 u_Direction;

void main() {
    vec3 final = vec3(0.0);

    for (float i = -u_Radius; i <= u_Radius; i += 2.0) {
        final += texture(u_Texture, v_TexCoord + v_OneTexel * (i + 0.5) * u_Direction).rgb;
    }

    color = vec4(final / (u_Radius + 1.0), 1.0);
}
