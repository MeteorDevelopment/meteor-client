#version 120

uniform sampler2D DiffuseSampler;
uniform float width;
uniform float opacity;
uniform float fillOpacity;

varying vec2 texCoord;
varying vec2 oneTexel;

void main() {
    vec4 center = texture2D(DiffuseSampler, texCoord);
    int intWidth = int(width);

    if (center.a == 0) {
        for (int x = -intWidth; x <= intWidth; x++) {
            for (int y = -intWidth; y <= intWidth; y++) {
                vec4 offset = texture2D(DiffuseSampler, texCoord + vec2(float(x), float(y)) * oneTexel);
                if (offset.a > 0) center = vec4(offset.rgb, 1 * opacity);
            }
        }
    } else {
        center = vec4(center.rgb, fillOpacity * opacity);
    }

    gl_FragColor = center;
}
