#ifdef GL_ES
    precision mediump float; // Устанавливаем точность для мобильных устройств
#endif

varying vec3 v_normal;     // Нормаль, переданная из вершинного шейдера
varying vec3 v_position;   // Позиция, переданная из вершинного шейдера

void main() {
    // Нормализуем нормаль (на случай интерполяции)
    vec3 normal = normalize(v_normal);

    // Устанавливаем итоговый цвет фрагмента
    gl_FragColor = vec4(normal, 1.0);
}
