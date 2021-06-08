#version 150 core

out vec4 color;

uniform sampler2D u_Texture;
uniform float u_Width;
uniform float u_ShapeMode;
uniform float u_FillOpacity;

in vec2 v_TexCoord;
in vec2 v_OneTexel;

void main() {
    vec4 center = texture2D(u_Texture, v_TexCoord);

    int widthInt = int(u_Width);
    int shapeModeInt = int(u_ShapeMode);
    float minDist = u_Width * u_Width;

    if (center.a != 0.0) {
        if (shapeModeInt == 0) discard;

        center = vec4(center.rgb, center.a * u_FillOpacity);
    }
    else {
        if (shapeModeInt == 1) discard;

        float dist = u_Width * u_Width * 4.0;

        for (int x = -widthInt; x <= widthInt; x++) {
            for (int y = -widthInt; y <= widthInt; y++) {
                vec4 offset = texture2D(u_Texture, v_TexCoord + vec2(x, y) * v_OneTexel);

                if (offset.a != 0) {
                    float ndist = x * x + y * y - 1.0;
                    dist = min(ndist, dist);
                    center = offset;
                }
            }
        }

        if (dist > minDist) center.a = 0.0;
        else center.a = min((1.0 - (dist / minDist)) * 3.5, 1.0);
    }

    color = center;
}
