#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

void main(){
    vec4 center = texture2D(DiffuseSampler, texCoord);
    if (center.a != 0) discard;

    vec4 left = texture2D(DiffuseSampler, texCoord - vec2(oneTexel.x, 0.0));
    vec4 right = texture2D(DiffuseSampler, texCoord + vec2(oneTexel.x, 0.0));
    vec4 up = texture2D(DiffuseSampler, texCoord - vec2(0.0, oneTexel.y));
    vec4 down = texture2D(DiffuseSampler, texCoord + vec2(0.0, oneTexel.y));

    float leftDiff  = abs(center.a - left.a);
    float rightDiff = abs(center.a - right.a);
    float upDiff    = abs(center.a - up.a);
    float downDiff  = abs(center.a - down.a);

    float a = clamp(leftDiff + rightDiff + upDiff + downDiff, 0.0, 1.0);
    vec3 color;
    if (left.a != 0) color = left.rgb;
    else if (right.a != 0) color = right.rgb;
    else if (up.a != 0) color = up.rgb;
    else color = down.rgb;

    gl_FragColor = vec4(color, a);
}
