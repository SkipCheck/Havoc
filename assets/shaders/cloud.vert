attribute vec3 a_position;
attribute vec2 a_texCoord0;

uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;
uniform float u_time; // Время для анимации

varying vec2 v_texCoords;

void main() {
    v_texCoords = a_texCoord0 + vec2(u_time * 0.01, u_time * 0.05);
    gl_Position = u_projViewTrans * u_worldTrans * vec4(a_position, 1.0);
}
