#version 330 core

out vec4 color;

in vec2 v_TexCoord;
in vec2 v_OneTexel;

uniform sampler2D u_Texture;
uniform float u_Radius;
uniform vec2 u_Direction;

void main() {
    vec3 final = vec3(0.0);

    for (float i = -u_Radius; i <= u_Radius; i += 1.0) {
        vec3 pixel = texture(u_Texture, v_TexCoord + v_OneTexel * i * u_Direction).rgb;

        final = final + pixel;
    }

    color = vec4(final / (u_Radius * 2.0 + 1.0), 1.0);
}
