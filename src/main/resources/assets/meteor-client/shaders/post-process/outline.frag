#version 330 core

in vec2 v_TexCoord;
in vec2 v_OneTexel;

uniform sampler2D u_Texture;

layout (std140) uniform OutlineData {
    int width;
    float fillOpacity;
    int shapeMode;
    float glowMultiplier;
} u_Outline;

out vec4 color;

void main() {
    vec4 center = texture(u_Texture, v_TexCoord);

    if (center.a != 0.0) {
        if (u_Outline.shapeMode == 0) discard;
        center = vec4(center.rgb, center.a * u_Outline.fillOpacity);
    }
    else {
        if (u_Outline.shapeMode == 1) discard;

        float dist = u_Outline.width * u_Outline.width * 4.0;

        for (int x = -u_Outline.width; x <= u_Outline.width; x++) {
            for (int y = -u_Outline.width; y <= u_Outline.width; y++) {
                vec4 offset = texture(u_Texture, v_TexCoord + v_OneTexel * vec2(x, y));

                if (offset.a != 0) {
                    float ndist = x * x + y * y - 1.0;
                    dist = min(ndist, dist);
                    center = offset;
                }
            }
        }

        float minDist = u_Outline.width * u_Outline.width;

        if (dist > minDist) center.a = 0.0;
        else center.a = min((1.0 - (dist / minDist)) * u_Outline.glowMultiplier, 1.0);
    }

    color = center;
}
