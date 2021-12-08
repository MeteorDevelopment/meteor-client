#version 330 core

in vec2 v_TexCoord;
in vec2 v_OneTexel;

out vec4 color;

uniform sampler2D u_Texture;
uniform float u_Time;

vec2 fluid(vec2 uv) {
    for (float i = 1.0; i <= 9.0; i++) {
        uv.x -= u_Time / 10.0 + sin(u_Time + uv.y * -i) / i * 0.8;
        uv.y -= u_Time / 10.0 + uv.x * i * 0.5 / i * 0.9;
    }

    return uv;
}

void main() {
    if (texture(u_Texture, v_TexCoord).a == 0.0) discard;
    vec2 uv = fluid(v_TexCoord * 20.0);

    float r = abs(sin(uv.x / 2.3));
    float g = abs(sin(uv.x / 3.5));
    float b = abs(sin(uv.x / 4.0));

    color = vec4(r, g, b, 1.0);
}
