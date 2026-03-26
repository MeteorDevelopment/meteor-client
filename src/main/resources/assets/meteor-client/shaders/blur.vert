#version 330 core

precision lowp float;

in vec2 Position;
out vec2 uv;

void main() {
    gl_Position = vec4(Position, 0, 1);
    uv = Position * .5 + .5;
}
