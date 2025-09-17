#version 120

attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;
uniform mat4 u_normalMatrix;

uniform float u_time;
uniform float u_waveFrequency;
uniform float u_waveAmplitude;

// Используем только 4 ближайших источника света
const int MAX_LIGHTS = 4;
uniform vec3 u_lightPositions[MAX_LIGHTS];
uniform vec4 u_lightColors[MAX_LIGHTS]; // Интенсивность в альфа-канале
uniform int u_lightCount;

uniform vec3 u_cameraPosition;
uniform float u_fogStart;
uniform float u_fogEnd;
uniform vec4 u_fogColor;
uniform vec3 u_ambientLightColor;
uniform float u_ambientIntensity;

varying vec2 v_texCoord0;
varying vec3 v_worldPosition;
varying vec3 v_normal;
varying vec4 v_color;
varying float v_fogFactor;

float calculateFogFactor(float distanceToCamera) {
    float fogFactor = (u_fogEnd - distanceToCamera) / (u_fogEnd - u_fogStart);
    return clamp(fogFactor, 0.0, 1.0);
}

void main() {
    v_texCoord0 = a_texCoord0;

    // Анимация вершин
    vec3 animatedPosition = a_position;
    float wave1 = sin(u_time + a_position.x * u_waveFrequency) * u_waveAmplitude;
    float wave2 = sin(u_time + a_position.z * u_waveFrequency * 0.5) * u_waveAmplitude * 0.5;
    animatedPosition.y += wave1 + wave2;

    // Трансформация позиции
    vec4 worldPosition = u_worldTrans * vec4(animatedPosition, 1.0);
    v_worldPosition = worldPosition.xyz;
    gl_Position = u_projViewTrans * worldPosition;

    // Трансформация нормали
    v_normal = normalize((u_normalMatrix * vec4(a_normal, 0.0)).xyz);

    // Инициализация накопленного света с ambient
    vec4 accumulatedLight = vec4(u_ambientLightColor * u_ambientIntensity, 1.0);

    // Обработка источников света (максимум 4)
    for (int i = 0; i < MAX_LIGHTS; i++) {
        if (i >= u_lightCount) break;

        vec3 lightDir = normalize(u_lightPositions[i] - v_worldPosition);
        float diffuse = max(dot(v_normal, lightDir), 0.0);
        float distance = length(u_lightPositions[i] - v_worldPosition);
        float attenuation = 1.0 / (1.0 + 0.05 * distance + 0.005 * distance * distance);
        accumulatedLight.rgb += u_lightColors[i].rgb * u_lightColors[i].a * diffuse * attenuation;
    }

    // Тон-маппинг и гамма-коррекция
    vec3 finalColor = accumulatedLight.rgb;
    finalColor = finalColor / (1.0 + finalColor);
    finalColor = pow(finalColor, vec3(1.0 / 2.2));
    finalColor *= vec3(0.7, 0.8, 1.0); // Синий оттенок воды

    // Применение тумана
    float distanceToCamera = length(u_cameraPosition - v_worldPosition);
    v_fogFactor = calculateFogFactor(distanceToCamera);
    finalColor = mix(u_fogColor.rgb, finalColor, v_fogFactor);

    v_color = vec4(finalColor, 1.0);
}
