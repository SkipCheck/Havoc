#version 120

#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_texCoord0;
varying vec3 v_worldPosition;
varying vec3 v_normal;
varying vec4 v_color;
varying float v_fogFactor;

uniform float u_time;
uniform sampler2D u_texture;
uniform vec4 u_fogColor;

// Water parameters
uniform vec3 u_waterColor;
uniform float u_waveFrequency;
uniform float u_waveAmplitude;

// Используем только 4 ближайших источника света
const int MAX_LIGHTS = 4;
uniform vec3 u_lightPositions[MAX_LIGHTS];
uniform vec4 u_lightColors[MAX_LIGHTS];
uniform float u_lightIntensities[MAX_LIGHTS];
uniform int u_lightCount;

// Camera position
uniform vec3 cameraPosition;

// Ambient light parameters
uniform vec3 u_ambientLightColor;
uniform float u_ambientIntensity;

// Coefficients for lighting
uniform float u_lightIntensityFactor;
uniform float u_glossiness;

// Function for simpler wave distortion
vec2 createWaveDistortion(vec2 texCoord, float time) {
    float waveX = sin(texCoord.x * 10.0 + time) * 0.02;
    float waveY = cos(texCoord.y * 10.0 + time) * 0.02;
    return texCoord + vec2(waveX, waveY);
}

vec3 adjustContrast(vec3 color, float contrast) {
    return ((color - 0.5) * contrast + 0.5);
}

vec3 adjustSaturation(vec3 color, float saturation) {
    float gray = dot(color, vec3(0.3, 0.11, 0.59));
    return mix(vec3(gray), color, saturation);
}

vec3 projectWarlockStyle(vec3 color) {
    color = adjustContrast(color, 1.22);
    color *= 1.2;
    return color;
}

void main() {
    // Create wave distortion
    vec2 animatedTexCoord = createWaveDistortion(v_texCoord0, u_time);

    // Get texture color
    vec4 texColor = texture2D(u_texture, animatedTexCoord);

    // Normalize normals
    vec3 normal = normalize(v_normal);

    // Calculate ambient lighting
    vec3 lighting = u_ambientLightColor * u_ambientIntensity;

    // Обрабатываем только ближайшие источники света (максимум 4)
    for (int i = 0; i < MAX_LIGHTS; i++) {
        if (i >= u_lightCount) break;
        if (u_lightIntensities[i] <= 0.0) continue;

        float distance = length(u_lightPositions[i] - v_worldPosition);
        float attenuation = 1.0 / (distance * distance);

        vec3 lightContribution = vec3(u_lightColors[i]) * u_lightIntensities[i] * attenuation;
        lighting += texColor.rgb * lightContribution;
    }

    // Combine effects
    vec3 waterEffect = texColor.rgb * u_waterColor;
    waterEffect += lighting;

    // Apply fog
    float smoothFogFactor = smoothstep(0.0, 1.0, v_fogFactor);
    vec4 foggedColor = mix(u_fogColor, vec4(waterEffect, 1.0), smoothFogFactor);

    // Final color with transparency control
    gl_FragColor = vec4(foggedColor.rgb, texColor.a);

    vec3 desaturated = mix(gl_FragColor.rgb, vec3(0.1,0.1,0.6), 0.1);
    float luminance = dot(desaturated, vec3(0.299, 0.587, 0.114));
    gl_FragColor.rgb = mix(vec3(luminance), desaturated, 0.85);

    float intensity = dot(gl_FragColor.rgb, vec3(0.299, 0.587, 0.114));
    gl_FragColor.rgb *= 2;
    float factor = mix(
        mix(
            mix(
                mix(0.52, 0.7, step(0.4, intensity)),
                0.8, step(0.5, intensity)
            ),
            0.9, step(0.7, intensity)
       ),
        1.1, step(0.9, intensity)
    );

    gl_FragColor.rgb *= factor;
    gl_FragColor.rgb = projectWarlockStyle(gl_FragColor.rgb);
    gl_FragColor.a = 1;
}
