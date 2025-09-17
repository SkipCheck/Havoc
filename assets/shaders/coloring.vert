#version 330 core

layout(location = 0) in vec2 a_Position;
layout(location = 1) in vec2 a_TextureCoord;

uniform mat4 u_MVPMatrix; // Матрица проекции, вид и модель
uniform vec3 u_LightPosition; // Позиция точечного света
uniform vec3 u_AmbientColor; // Цвет окружающего света

out vec2 v_TextureCoord;
out vec3 v_LightDirection;
out vec3 v_ViewDirection;

void main() {
    gl_Position = u_MVPMatrix * vec4(a_Position, 0.0, 1.0);

    // Вычисляем направление света и направление взгляда
    v_LightDirection = normalize(u_LightPosition - vec3(gl_Position));
    v_ViewDirection = normalize(-gl_Position.xyz); // Предполагаем, что камера в (0,0,0)

    v_TextureCoord = a_TextureCoord;
}
