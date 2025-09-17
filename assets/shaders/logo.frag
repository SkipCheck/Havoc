#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_time;
uniform float u_alpha; // Добавляем управление альфа-каналом из кода

void main() {
    vec4 color = texture2D(u_texture, v_texCoords);
    if(color.a < 0.01) discard;

    // Эффект мерцания (ваш оригинальный код)
    float flashMod = sin(u_time - (v_texCoords.x + v_texCoords.y*0.3)) + 1.0;
    if(flashMod < 1.99) {
        flashMod = 1.0;
    } else {
        flashMod = 1.1 + (flashMod - 1.99) * 200.0;
        flashMod -= clamp(sin((v_texCoords.x + v_texCoords.y*0.3)*70.0 + u_time*2.0)*1.25, 0.0, 1.25);
    }
    flashMod = clamp(flashMod, 1.0, 10.0);

    // Применяем мерцание к RGB и управляем альфа-каналом через u_alpha
    gl_FragColor = vec4(color.rgb * flashMod, color.a * u_alpha);
}
