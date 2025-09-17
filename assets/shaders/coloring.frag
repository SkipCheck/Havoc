#version 330 core

in vec2 v_TextureCoord;
in vec3 v_LightDirection;
in vec3 v_ViewDirection;

uniform sampler2D u_Texture; // Текстура декали
uniform vec3 u_DiffuseColor; // Цвет диффузного освещения
uniform float u_Shininess; // Шероховатость для блеска

out vec4 fragColor;

void main() {
    vec4 texColor = texture(u_Texture, v_TextureCoord);

    // Окружающее освещение
    vec3 ambient = 0.1 * u_DiffuseColor; // Умножаем на коэффициент окружающего света

    // Диффузное освещение
    float diff = max(dot(normalize(v_LightDirection), vec3(0.0, 0.0, 1.0)), 0.0); // Нормаль декали
    vec3 diffuse = diff * u_DiffuseColor * texColor.rgb;

    // Итоговый цвет
    fragColor = vec4(ambient + diffuse, texColor.a);
}
