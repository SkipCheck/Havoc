attribute vec3 a_position; // Позиция вершины
attribute vec3 a_normal;   // Нормаль вершины

uniform mat4 u_projTrans;  // Матрица проекции и вида
uniform mat4 u_worldTrans; // Мировая матрица

varying vec3 v_normal;     // Передаем нормаль во фрагментный шейдер
varying vec3 v_position;   // Передаем позицию во фрагментный шейдер

void main() {
    // Преобразуем позицию вершины в мировые координаты
    vec4 worldPos = u_worldTrans * vec4(a_position, 1.0);
    v_position = worldPos.xyz;

    // Преобразуем нормаль в мировые координаты
    v_normal = normalize((u_worldTrans * vec4(a_normal, 0.0)).xyz);

    // Преобразуем позицию вершины в экранные координаты
    gl_Position = u_projTrans * worldPos;
}
