#version 330 core

in vec3 v_Position;
in vec3 v_Normal;

out vec4 fragColor;

uniform vec3 u_LightPos;
uniform vec3 u_ViewPos; // Убедитесь, что эта строка есть
uniform float u_AmbientStrength;

void main() {
    vec3 ambient = u_AmbientStrength * vec3(1.0, 1.0, 1.0);
    // Простой расчет AO (в реальной реализации нужно использовать более сложные методы)
    float ao = max(dot(v_Normal, normalize(u_LightPos - v_Position)), 0.0);
    fragColor = vec4(ambient * ao, 1.0);
}
