#ifdef GL_ES
precision mediump float;
#endif

uniform float u_fogDensity;  // Плотность тумана
uniform vec4 u_fogColor;     // Цвет тумана
uniform vec3 u_cameraPosition; // Позиция камеры для расчета расстояния

varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_texCoord;

uniform sampler2D u_texture;

void main() {
    // Расстояние от текущей позиции до камеры
    float distance = length(v_position - u_cameraPosition);

    // Интенсивность тумана на основе расстояния
    float fogFactor = exp(-u_fogDensity * distance);

    // Получаем исходный цвет из текстуры
    vec4 color = texture2D(u_texture, v_texCoord);

    // Применяем туман
    vec4 foggedColor = mix(color, u_fogColor, 1.0 - fogFactor);

    gl_FragColor = foggedColor;
}
