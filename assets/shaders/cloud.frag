#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform float u_alpha; // Прозрачность (опционально)
uniform vec4 u_color;  // Цвет для тонирования (опционально)

varying vec2 v_texCoords;

void main() {
    vec4 texColor = texture2D(u_texture, v_texCoords);
    texColor.rgb = 1.0 - texColor.rgb; // Инвертируем цвета
    texColor.a = 1.0 - texColor.a + 0.1;     // Инвертируем альфа
    texColor.rgb = mix(texColor.rgb, u_color.rgb, 0.95);

    texColor.a *= u_alpha;
    gl_FragColor = texColor;
}
