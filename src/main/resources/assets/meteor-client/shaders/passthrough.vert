#version 330 core

precision lowp float;

layout (location = 0) in vec2 Position;

out vec2 uv;

void main() {
    gl_Position = vec4(Position, 0.0, 1.0);
    uv = Position * 0.5 + 0.5;
}
