#version 330 core

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec3 a_Normal;

out vec3 v_Position;
out vec3 v_Normal;

uniform mat4 u_MVP;

void main() {
    v_Position = a_Position;
    v_Normal = normalize(a_Normal);
    gl_Position = u_MVP * vec4(a_Position, 1.0);
}
