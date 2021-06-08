#version 120

uniform sampler2D DiffuseSampler;
uniform float width;
uniform float shapeMode;
uniform float fillOpacity;

varying vec2 texCoord;
varying vec2 oneTexel;

void main() {
    vec4 center = texture2D(DiffuseSampler, texCoord);

    int widthInt = int(width);
    int shapeModeInt = int(shapeMode);
    float mindist = width * width;

    if (center.a != 0.0) {
        if (shapeModeInt == 0) discard;
        center = vec4(center.rgb, center.a * fillOpacity);
    } else {
        if (shapeModeInt == 1) discard;
        float dist = width * width * 4.0;
        for (int x = -widthInt; x <= widthInt; x++) {
            for (int y = -widthInt; y <= widthInt; y++) {
                vec4 offset = texture2D(DiffuseSampler, texCoord + vec2(x, y) * oneTexel);
                if (offset.a != 0) {
                    float ndist = x*x + y*y - 1.0;
                    dist = min(ndist, dist);
                    center = offset;
                }
            }
        }
        if (dist > mindist) {
            center.a = 0.0;
        } else {
            center.a = min((1.0 - (dist / mindist)) * 3.5, 1.0);
        }
    }

    gl_FragColor = center;
}
