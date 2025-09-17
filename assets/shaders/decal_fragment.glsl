#version 120

#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
varying vec3 v_worldPos;

uniform sampler2D u_texture;

// Используем только 4 ближайших источника света
const int MAX_LIGHTS = 4;
uniform vec3 u_lightPositions[MAX_LIGHTS];
uniform vec3 u_lightColors[MAX_LIGHTS];
uniform float u_lightIntensities[MAX_LIGHTS];
uniform int u_lightCount;
uniform vec3 u_ambientColor;
uniform float u_ambientIntensity;
uniform int u_stencilValue;

#ifdef blendedFlag
varying float v_opacity;
#endif //blendedFlag

#ifdef fogFlag
uniform vec4 u_fogColor;
#endif

void main() {
    int stencilValue = int(gl_FragCoord.z);
    if (stencilValue != u_stencilValue) {
        discard;
    }

    vec4 texColor = texture2D(u_texture, v_texCoords);
    if(texColor.a < 1){
        discard;
    }

    // Start with ambient light
    vec3 finalColor = texColor.rgb * u_ambientColor * u_ambientIntensity;

    // Add point light contributions (максимум 4 ближайших источника)
    for (int i = 0; i < MAX_LIGHTS; i++) {
        if (i >= u_lightCount) break;

        vec3 lightDir = u_lightPositions[i] - v_worldPos;
        float distanceSquared = dot(lightDir, lightDir);
        float maxDistanceSquared = 100.0;
        if (distanceSquared > maxDistanceSquared) continue;

        float attenuation = 1.0 / (1.0 + distanceSquared);
        vec3 lightContribution = u_lightColors[i] * u_lightIntensities[i] * attenuation;
        finalColor += texColor.rgb * lightContribution;
    }

    // Ensure color doesn't exceed 1.0
    finalColor = min(finalColor, vec3(1.0));

    float intensity = dot(finalColor.rgb, vec3(0.299, 0.587, 0.114));
    finalColor.rgb *= 1.5;

    float factor = mix(
        mix(
            mix(
                mix(0.6, 0.7, step(0.4, intensity)),
                0.8, step(0.5, intensity)
            ),
            0.9, step(0.7, intensity)
        ),
        1.1, step(0.9, intensity)
    );

    finalColor.rgb *= factor;

    #ifdef fogFlag
    float eyeDistance = length(v_worldPos - u_cameraPosition);
    float fogFactor = smoothstep(40, 100, eyeDistance);
    fogFactor = pow(fogFactor, 1);
    vec3 fogColor = mix(u_fogColor.rgb, u_fogColor.rgb, fogFactor);
    finalColor = mix(finalColor, fogColor, fogFactor);
    #endif

    gl_FragColor = vec4(finalColor, texColor.a * v_color.a);
}
