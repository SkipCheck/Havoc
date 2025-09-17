#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_time;
uniform float u_alpha;

void main() {

    // Получаем оригинальный цвет текстуры
    vec4 texColor = texture2D(u_texture, v_texCoords);

    // Плавное изменение цветовых каналов
    float colorShift = sin(u_time * 1.5) * 0.1;
    texColor.r *= 0.9 + colorShift;
    texColor.g *= 0.9 - colorShift * 0.5;
    texColor.b *= 0.9 + colorShift * 0.3;

    // Добавляем легкое свечение
    float glow = sin(u_time * 3.0) * 0.1 + 0.1;
    texColor.rgb += glow * vec3(0.3, 0.5, 0.8);

    gl_FragColor = vec4(texColor.rgb, u_alpha);
}
