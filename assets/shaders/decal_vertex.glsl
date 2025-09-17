#version 120

attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;

uniform mat4 u_projectionViewMatrix;

// Используем только 4 ближайших источника света
const int MAX_LIGHTS = 4;
uniform vec3 u_lightPositions[MAX_LIGHTS];
uniform vec3 u_lightColors[MAX_LIGHTS];
uniform float u_lightIntensities[MAX_LIGHTS];
uniform int u_lightCount;

varying vec4 v_color;
varying vec2 v_texCoords;
varying vec3 v_worldPos;

void main() {
   v_color = a_color;
   v_color.a = v_color.a * 1.0039;
   v_texCoords = a_texCoord0;
   v_worldPos = a_position.xyz;
   gl_Position = u_projectionViewMatrix * a_position;
}
