#line 1

// Extensions required for WebGL and some Android versions

#ifdef GLSL3

#define textureCubeLodEXT textureLod

#else

#ifdef USE_TEXTURE_LOD_EXT

#extension GL_EXT_shader_texture_lod: enable

#else

// Note : "textureCubeLod" is used for compatibility but should be "textureLod" for GLSL #version 130 (OpenGL 3.0+)

#define textureCubeLodEXT textureCubeLod

#endif

#endif

#ifdef USE_DERIVATIVES_EXT

#extension GL_OES_standard_derivatives: enable

#endif

// required to have same precision in both shader for light structure

#ifdef GL_ES

#define LOWP lowp

#define MED mediump

#define HIGH highp

precision highp float;

#else

#define MED

#define LOWP

#define HIGH

#endif

#ifdef GLSL3

#define varying in

out vec4 out_FragColor;

#define textureCube texture

#define texture2D texture

#else

#define out_FragColor gl_FragColor

#endif

// Utilities

#define saturate(_v) clamp((_v), 0.0, 1.0)

#if defined(specularTextureFlag) || defined(specularColorFlag)

#define specularFlag

#endif

#ifdef normalFlag

#ifdef tangentFlag

varying mat3 v_TBN;

#else

varying vec3 v_normal;

#endif

#endif //normalFlag

#if defined(colorFlag)

varying vec4 v_color;

#endif

#ifdef blendedFlag

varying float v_opacity;

#ifdef alphaTestFlag

varying float v_alphaTest;

#endif //alphaTestFlag

#endif //blendedFlag

#ifdef textureFlag

varying MED vec2 v_texCoord0;

#endif // textureFlag

#ifdef textureCoord1Flag

varying MED vec2 v_texCoord1;

#endif // textureCoord1Flag

// texCoord unit mapping

#ifndef v_diffuseUV

#define v_diffuseUV v_texCoord0

#endif

#ifndef v_specularUV

#define v_specularUV v_texCoord0

#endif

#ifndef v_emissiveUV

#define v_emissiveUV v_texCoord0

#endif

#ifndef v_normalUV

#define v_normalUV v_texCoord0

#endif

#ifndef v_occlusionUV

#define v_occlusionUV v_texCoord0

#endif

uniform mat4 u_viewProjectionMatrix;

#ifndef v_metallicRoughnessUV

#define v_metallicRoughnessUV v_texCoord0

#endif

#ifdef diffuseColorFlag

uniform vec4 u_diffuseColor;

#endif

uniform vec4 u_BaseColorFactor;

#ifdef diffuseTextureFlag

uniform sampler2D u_diffuseTexture;

#endif

#ifdef specularColorFlag

uniform vec4 u_specularColor;

#endif

#ifdef specularTextureFlag

uniform sampler2D u_specularTexture;

#endif

#ifdef normalTextureFlag

uniform sampler2D u_normalTexture;

uniform float u_NormalScale;

#endif

#ifdef emissiveColorFlag

uniform vec4 u_emissiveColor;

#endif

#ifdef emissiveTextureFlag

uniform sampler2D u_emissiveTexture;

#endif

#ifdef shadowMapFlag

uniform float u_shadowBias;

uniform sampler2D u_shadowTexture;

uniform float u_shadowPCFOffset;

varying vec3 v_shadowMapUv;

float getShadowness(vec2 offset)

{

const vec4 bitShifts = vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 16581375.0);

return step(v_shadowMapUv.z, dot(texture2D(u_shadowTexture, v_shadowMapUv.xy + offset), bitShifts) + u_shadowBias); // (1.0/255.0)

}

float getShadow() {
    // Упрощенный PCF с меньшим количеством выборок
    float shadow = getShadowness(vec2(0.0));
    return mix(shadow, 1.0, 0.7); // Частичная компенсация
}

#endif //shadowMapFlag

#ifdef fogFlag

uniform vec4 u_fogColor;

#ifdef fogEquationFlag

uniform vec3 u_fogEquation;

#endif

#endif // fogFlag

#ifdef ambientLightFlag

uniform vec3 u_ambientLight;

#endif // ambientLightFlag

#ifdef USE_IBL

uniform samplerCube u_DiffuseEnvSampler;

#ifdef diffuseSpecularEnvSeparateFlag

uniform samplerCube u_SpecularEnvSampler;

#else

#define u_SpecularEnvSampler u_DiffuseEnvSampler

#endif

#ifdef brdfLUTTexture

uniform sampler2D u_brdfLUT;

#endif

#ifdef USE_TEX_LOD

uniform float u_mipmapScale; // = 9.0 for resolution of 512x512

#endif

#endif

#ifdef occlusionTextureFlag

uniform sampler2D u_OcclusionSampler;

uniform float u_OcclusionStrength;

#endif

#ifdef metallicRoughnessTextureFlag

uniform sampler2D u_MetallicRoughnessSampler;

#endif

#if numDirectionalLights > 0

struct DirectionalLight

{

vec3 color;

vec3 direction;

};

uniform DirectionalLight u_dirLights[numDirectionalLights];

#endif // numDirectionalLights

#if numPointLights > 0

struct PointLight

{

vec3 color;

vec3 position;

};

uniform PointLight u_pointLights[numPointLights];

#endif // numPointLights

#if numSpotLights > 0

struct SpotLight

{

vec3 color;

vec3 position;

vec3 direction;

float cutoffAngle;

float exponent;

};

uniform SpotLight u_spotLights[numSpotLights];

#endif // numSpotLights

uniform vec4 u_cameraPosition;

uniform vec2 u_MetallicRoughnessValues;

varying vec3 v_position;

// Encapsulate the various inputs used by the various functions in the shading equation

// We store values in structs to simplify the integration of alternative implementations

// PBRSurfaceInfo contains light independant information (surface/material only)

// PBRLightInfo contains light information (incident rays)

struct PBRSurfaceInfo

{

vec3 n;						  // Normal vector at surface point

vec3 v;						  // Vector from surface point to camera

float NdotV;                  // cos angle between normal and view direction

float perceptualRoughness;    // roughness value, as authored by the model creator (input to shader)

vec3 reflectance0;            // full reflectance color (normal incidence angle)

vec3 reflectance90;           // reflectance color at grazing angle

float alphaRoughness;         // roughness mapped to a more linear change in the roughness (proposed by [2])

vec3 diffuseColor;            // color contribution from diffuse lighting

vec3 specularColor;           // color contribution from specular lighting

};

struct PBRLightInfo

{

float NdotL;                  // cos angle between normal and light direction

float NdotH;                  // cos angle between normal and half vector

float LdotH;                  // cos angle between light direction and half vector

float VdotH;                  // cos angle between view direction and half vector

};

const float M_PI = 3.141592653589793;

const float c_MinRoughness = 0.04;

vec4 SRGBtoLINEAR(vec4 srgbIn)

{

#ifdef MANUAL_SRGB

#ifdef SRGB_FAST_APPROXIMATION

vec3 linOut = pow(srgbIn.xyz,vec3(2.3));

#else //SRGB_FAST_APPROXIMATION

vec3 bLess = step(vec3(0.04045),srgbIn.xyz);

vec3 linOut = mix( srgbIn.xyz/vec3(12.92), pow((srgbIn.xyz+vec3(0.055))/vec3(1.055),vec3(2.4)), bLess );

#endif //SRGB_FAST_APPROXIMATION

return vec4(linOut,srgbIn.w);;

#else //MANUAL_SRGB

return srgbIn;

#endif //MANUAL_SRGB

}

// Find the normal for this fragment, pulling either from a predefined normal map

// or from the interpolated mesh normal and tangent attributes.

vec3 getNormal() {
    #ifdef normalTextureFlag
        return normalize(texture2D(u_normalTexture, v_normalUV).xyz * 2.0 - 1.0);
    #else
        return vec3(0.0, 1.0, 0.0);
    #endif
}

// Calculation of the lighting contribution from an optional Image Based Light source.

// Precomputed Environment Maps are required uniform inputs and are computed as outlined in [1].

// See our README.md on Environment Maps [3] for additional discussion.

#ifdef USE_IBL

vec3 getIBLContribution(PBRSurfaceInfo pbrSurface, vec3 n, vec3 reflection)

{

// retrieve a scale and bias to F0. See [1], Figure 3

#ifdef brdfLUTTexture

vec2 brdf = SRGBtoLINEAR(texture2D(u_brdfLUT, vec2(pbrSurface.NdotV, 1.0 - pbrSurface.perceptualRoughness))).xy;

#else // TODO not sure about how to compute it ...

vec2 brdf = vec2(pbrSurface.NdotV, pbrSurface.perceptualRoughness);

#endif

vec3 diffuseLight = SRGBtoLINEAR(textureCube(u_DiffuseEnvSampler, n)).rgb;
#ifdef USE_TEX_LOD

float lod = (pbrSurface.perceptualRoughness * u_mipmapScale);

vec3 specularLight = SRGBtoLINEAR(textureCubeLodEXT(u_SpecularEnvSampler, reflection, lod)).rgb;

#else

vec3 specularLight = SRGBtoLINEAR(textureCube(u_SpecularEnvSampler, reflection)).rgb;

#endif

vec3 diffuse = diffuseLight * pbrSurface.diffuseColor;
vec3 specular = specularLight * (pbrSurface.specularColor * brdf.x + brdf.y);

return diffuse + specular;
}

#endif

// Basic Lambertian diffuse

// Implementation from Lambert's Photometria https://archive.org/details/lambertsphotome00lambgoog

// See also [1], Equation 1

vec3 diffuse(PBRSurfaceInfo pbrSurface)

{

return pbrSurface.diffuseColor / M_PI;

}

// The following equation models the Fresnel reflectance term of the spec equation (aka F())

// Implementation of fresnel from [4], Equation 15

vec3 specularReflection(PBRSurfaceInfo pbrSurface, PBRLightInfo pbrLight)

{

return mix(pbrSurface.specularColor, pbrSurface.reflectance90,
               pow(1.0 - pbrLight.LdotH, 5.0));
}

// This calculates the specular geometric attenuation (aka G()),

// where rougher material will reflect less light back to the viewer.

// This implementation is based on [1] Equation 4, and we adopt their modifications to

// alphaRoughness as input as originally proposed in [2].

float geometricOcclusion(PBRSurfaceInfo pbrSurface, PBRLightInfo pbrLight) {
    float r = pbrSurface.alphaRoughness;
    float k = (r + 1.0) * (r + 1.0) / 8.0;
    float GL = pbrLight.NdotL / (pbrLight.NdotL * (1.0 - k) + k);
    float GV = pbrSurface.NdotV / (pbrSurface.NdotV * (1.0 - k) + k);
    return GL * GV;
}

// The following equation(s) model the distribution of microfacet normals across the area being drawn (aka D())

// Implementation from "Average Irregularity Representation of a Roughened Surface for Ray Reflection" by T. S. Trowbridge, and K. P. Reitz

// Follows the distribution function recommended in the SIGGRAPH 2013 course notes from EPIC Games [1], Equation 3.

float microfacetDistribution(PBRSurfaceInfo pbrSurface, PBRLightInfo pbrLight)

{

float roughnessSq = pbrSurface.alphaRoughness * pbrSurface.alphaRoughness;

float f = (pbrLight.NdotH * roughnessSq - pbrLight.NdotH) * pbrLight.NdotH + 1.0;

return roughnessSq / (M_PI * f * f);

}

vec3 adjustContrast(vec3 color, float contrast) {

return ((color - 0.5) * contrast + 0.5);

}

vec3 adjustSaturation(vec3 color, float saturation) {

float gray = dot(color, vec3(0.3, 0.11, 0.59));

return mix(vec3(gray), color, saturation);

}

float comicStep(float value, float threshold, float sharpness) {

return smoothstep(threshold - sharpness, threshold + sharpness, value);

}

// Модифицированная функция для получения диффузного освещения с комиксным стилем

vec3 comicDiffuse(PBRSurfaceInfo pbrSurface)

{

float threshold = 0.5;

float sharpness = 0.1;

float intensity = smoothstep(threshold - sharpness, threshold + sharpness, pbrSurface.NdotV);

return pbrSurface.diffuseColor.rgb * intensity;

}

vec3 softComicShading(vec3 color, float intensity) {

float threshold = 0.7;

float sharpness = 0.3;

float step = smoothstep(threshold - sharpness, threshold + sharpness, intensity);

return mix(color, color * 1.1, step * 0.2);

}

vec2 pixelate(vec2 uv, float pixelScale) {

return vec2(

floor(uv.x * pixelScale) / pixelScale,

floor(uv.y * pixelScale) / pixelScale

);

}

vec3 softLighting(vec3 color, float intensity) {

// Мягкое свечение и подсветка

float softness = smoothstep(0.3, 0.7, intensity);

vec3 softColor = color * (1.0 + softness * 0.3);

return mix(color, softColor, 0.5);

}

vec3 colorGrading(vec3 color) {

// Улучшенная цветокоррекция

vec3 graded = color * vec3(1.1, 1.05, 1.0); // Теплые тона

graded = mix(vec3(dot(graded, vec3(0.299, 0.587, 0.114))), graded, 1.2); // Повышение контраста

return saturate(graded);

}

vec3 enhanceVisuals(vec3 color) {

// Динамический диапазон и тональная компрессия

float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));

vec3 toneMapped = color / (1.0 + luminance);

// Добавление легкого свечения
float bloom = pow(max(0.0, luminance - 0.8), 2.0);

return color + toneMapped * 0.3 + vec3(bloom) * 0.2;
}

vec3 projectWarlockStyle(vec3 color) {
    color = adjustContrast(color, 1.22);
    color = adjustSaturation(color, 1.4);
    return color;
}

vec3 invertColor(vec3 color) {
    return vec3(1.0) - color;
}

vec3 darkGradientFall(vec3 color) {
    // Основные параметры стиля
    float contrast = 1.4;
    float saturation = 0.8;
    float darkenAmount = 0.7;

    // Увеличиваем контраст
    color = (color - 0.5) * contrast + 0.5;

    // Уменьшаем насыщенность
    float luminance = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(vec3(luminance), color, saturation);

    // Применяем темный градиент
    vec3 darkTint = vec3(0.1, 0.15, 0.2);
    color = mix(color * darkTint, color, luminance);

    // Добавляем холодный оттенок
    vec3 coolColor = vec3(0.7, 0.75, 0.8) * luminance;
    color = mix(color, coolColor, luminance * 0.5);

    return clamp(color, 0.0, 1.0);
}

vec3 applyColdPixelFilter(vec3 color) {
    // Яркость пикселя (0-1)
    float luminance = dot(color, vec3(0.299, 0.587, 0.114));

    // Если пиксель почти белый (luminance > 0.9), делаем его тёмно-синим
    if (luminance > 0.9) {
        return vec3(0.05, 0.1, 0.3); // Тёмно-синий
    }
    // Если пиксель светлый (0.7-0.9), добавляем синий оттенок
    else if (luminance > 0.7) {
        return mix(color, vec3(0.1, 0.2, 0.5), 0.7);
    }
    // Если пиксель средний (0.4-0.7), слегка охлаждаем
    else if (luminance > 0.4) {
        return mix(color, vec3(0.3, 0.4, 0.6), 0.4);
    }
    // Тёмные пиксели (0-0.4) оставляем почти чёрными с лёгким синим отливом
    else {
        return mix(color, vec3(0.0, 0.05, 0.1), 0.8);
    }
}


vec3 applyWarlockColorGrading(vec3 color) {
    // Уменьшаем общую яркость
    color *= 0.7;

    // Извлекаем яркость (luminance)
    float luminance = dot(color, vec3(0.299, 0.587, 0.114));

    // Синевато-серый оттенок для светлых участков
    vec3 coolHighlight = vec3(0.7, 0.75, 0.8); // Светлый сине-серый

    // Смешиваем оригинальный цвет с холодным оттенком на основе яркости
    float highlightMix = smoothstep(0.4, 0.8, luminance);
    color = mix(color, coolHighlight * luminance, highlightMix);

    // Увеличиваем контраст
    color = (color - 0.5) * 1.4 + 0.5;

    // Добавляем легкий синий оттенок в тени
    vec3 shadowTint = vec3(0.1, 0.15, 0.2);
    float shadowMix = smoothstep(0.1, 0.3, luminance);
    color = mix(color * shadowTint, color, shadowMix);

    return clamp(color, 0.0, 1.0);
}

float calculatePixelCoverage(vec2 uv) {

// Создание градиентной маски с краевым затуханием

float distFromCenter = length(uv - vec2(0.5));

float coverage = 1.0 - smoothstep(0.3, 0.7, distFromCenter);

// Добавление шума для неровных краев
float noise = fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453);
coverage *= mix(0.7, 1.0, noise);

return coverage;
}

float stochasticCoverage(vec2 uv) {

// Генерация псевдослучайного значения

float random = fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453);

// Создание стохастической маски
float threshold = 0.5 + 0.5 * sin(uv.x * 100.0) * cos(uv.y * 100.0);

return step(threshold, random);
}

float computeDepthOcclusion(float depth) {

// Нелинейное затухание по глубине

float occlusionFactor = smoothstep(0.0, 1.0, depth);

// Применение логарифмической шкалы
return 1.0 - log(1.0 + depth) / log(2.0);
}

float computeViewAngleOcclusion(vec3 normal, vec3 viewDir) {

float viewAngle = abs(dot(normal, viewDir));

// Затухание по краям в зависимости от угла обзора
float occlusion = smoothstep(0.0, 1.0, viewAngle);

// Применение нелинейной функции для более резкого перехода
return pow(occlusion, 2.2);
}

float scanLine(vec2 uv) {

float lines = 100.0; // Количество линий

float intensity = 0.1; // Интенсивность эффекта

float scanLine = sin(uv.y * lines);
return 1.0 - scanLine * intensity;
}

float warlockAmbientOcclusion(vec3 normal, float intensity) {

// Базовый AO с характерным стилем Warlock

float ao = 1;

// Зависимость от угла нормали
float normalFactor = abs(dot(normal, vec3(0.2, 0.2, 0.2)));

// Жесткое затенение с характерным стилем
ao *= smoothstep(0.1, 1, normalFactor);

// Интенсивность затенения
ao = mix(ao, 1.0, intensity * 0.5);

// Резкость и контраст
//ao = pow(ao, 1.5);

return ao;
}

vec3 retroPalette(vec3 color) {

// Палитра в стиле 8-bit

vec3 palette[8] = vec3[](

vec3(0.0, 0.0, 0.0),      // Черный

vec3(1.0, 1.0, 1.0),      // Белый

vec3(1.0, 0.0, 0.0),      // Красный

vec3(0.0, 1.0, 0.0),      // Зеленый

vec3(0.0, 0.0, 1.0),      // Синий

vec3(1.0, 1.0, 0.0),      // Желтый

vec3(1.0, 0.0, 1.0),      // Пурпурный

vec3(0.0, 1.0, 1.0)       // Циан

);

// Квантование цвета
float quantLevels = 8.0;
vec3 quantColor = floor(color * quantLevels) / quantLevels;

return quantColor;
}

#ifdef unlitFlag

void main() {

vec2 pixelatedUV = pixelate(v_diffuseUV, 100.0);

#ifdef diffuseTextureFlag
vec4 baseColor = SRGBtoLINEAR(texture2D(u_diffuseTexture, v_diffuseUV, 0.0)) * u_BaseColorFactor;
#else
vec4 baseColor = u_BaseColorFactor;
#endif

#ifdef colorFlag

baseColor *= v_color;

#endif

vec3 color = baseColor.rgb;

// final frag color
#ifdef MANUAL_SRGB

out_FragColor = vec4(pow(color,vec3(1.0/2.2)), baseColor.a);
#else

out_FragColor = vec4(color, baseColor.a);

#endif

// Blending and Alpha Test
#ifdef blendedFlag

out_FragColor.a = baseColor.a * v_opacity;

#ifdef alphaTestFlag

if (out_FragColor.a <= v_alphaTest)

discard;

#endif

#else

out_FragColor.a = 1.0;

#endif

}

#else

// Light contribution calculation independent of light type

// l is a unit vector from surface point to light

vec3 getLightContribution(PBRSurfaceInfo pbrSurface, vec3 l)
{
    float light = max(dot(pbrSurface.n, l), 0.0);

    #if defined(ambientLightFlag)
        float ambient = u_ambientLight.r * 3;
    #else
        float ambient = 1.0;
    #endif

    return pbrSurface.diffuseColor.rgb * (ambient  + light);
}
#if numDirectionalLights > 0

vec3 getDirectionalLightContribution(PBRSurfaceInfo pbrSurface, DirectionalLight light)

{

vec3 l = normalize(-light.direction);  // Vector from surface point to light

return getLightContribution(pbrSurface, l) * light.color;

}

#endif

#if numPointLights > 0

vec3 getPointLightContribution(PBRSurfaceInfo pbrSurface, PointLight light)

{

// light direction and distance

vec3 d = light.position - v_position.xyz;

float dist2 = dot(d, d);

d *= inversesqrt(dist2);

return getLightContribution(pbrSurface, d) * light.color / (1.0 + dist2) * 1;
}

#endif

#if numSpotLights > 0

vec3 getSpotLightContribution(PBRSurfaceInfo pbrSurface, SpotLight light)

{

// light distance

vec3 d = light.position - v_position.xyz;

float dist2 = dot(d, d);

d *= inversesqrt(dist2);

// light direction
vec3 l = normalize(-light.direction);  // Vector from surface point to light

// from https://github.com/KhronosGroup/glTF/blob/master/extensions/2.0/Khronos/KHR_lights_punctual/README.md#inner-and-outer-cone-angles
float lightAngleOffset = light.cutoffAngle;
float lightAngleScale = light.exponent;

float cd = dot(l, d);
float angularAttenuation = saturate(cd * lightAngleScale + lightAngleOffset);
angularAttenuation *= angularAttenuation;

return getLightContribution(pbrSurface, d) * light.color * (angularAttenuation / (1.0 + dist2));
}

#endif

void main() {
// Metallic and Roughness material properties are packed together

// In glTF, these factors can be specified by fixed scalar values

// or from a metallic-roughness map

float perceptualRoughness = u_MetallicRoughnessValues.y;

#ifdef metallicRoughnessTextureFlag

// Roughness is stored in the 'g' channel, metallic is stored in the 'b' channel.

// This layout intentionally reserves the 'r' channel for (optional) occlusion map data

vec4 mrSample = texture2D(u_MetallicRoughnessSampler, v_metallicRoughnessUV);
perceptualRoughness = mrSample.g * perceptualRoughness;
#endif

perceptualRoughness = clamp(perceptualRoughness, c_MinRoughness, 1.0);

// Convert to material roughness by squaring the perceptual roughness.
float alphaRoughness = perceptualRoughness * perceptualRoughness;

#ifdef diffuseTextureFlag
vec4 baseColor = SRGBtoLINEAR(texture2D(u_diffuseTexture, v_diffuseUV)) * u_BaseColorFactor;
#else
vec4 baseColor = u_BaseColorFactor;
#endif

#ifdef colorFlag
baseColor *= v_color;
#endif

vec3 f0 = vec3(0.04);
vec3 diffuseColor = baseColor.rgb * (vec3(1.0) - f0);
vec3 specularColor = f0;

// Compute reflectance.
float reflectance = max(max(specularColor.r, specularColor.g), specularColor.b);

// For typical incident reflectance range (between 4% to 100%) set the grazing reflectance to 100% for typical Fresnel effect.
float reflectance90 = clamp(reflectance * 25.0, 0.0, 1.0);
vec3 specularEnvironmentR0 = specularColor.rgb;
vec3 specularEnvironmentR90 = vec3(1.0, 1.0, 1.0) * reflectance90;

vec3 surfaceToCamera = u_cameraPosition.xyz - v_position;
float eyeDistance = length(surfaceToCamera);

vec3 n = getNormal(); // Normal at surface point
vec3 v = surfaceToCamera / eyeDistance; // Vector from surface point to camera
vec3 reflection = -normalize(reflect(v, n));

float NdotV = clamp(abs(dot(n, v)), 0.001, 1.0);

PBRSurfaceInfo pbrSurface = PBRSurfaceInfo(
    n,
    v,
    NdotV,
    perceptualRoughness,
    specularEnvironmentR0,
    specularEnvironmentR90,
    alphaRoughness,
    diffuseColor,
    specularColor
);

vec3 color = vec3(0.0);

#ifdef ambientLightFlag
        color += u_ambientLight * pbrSurface.diffuseColor;
    #endif

#if (numDirectionalLights > 0)
// Directional lights calculation
for(int i = 0; i < numDirectionalLights; i++) {
    color += getDirectionalLightContribution(pbrSurface, u_dirLights[i]);
}
#endif

#ifdef shadowMapFlag
vec3 l0 = normalize(-u_dirLights[0].direction);
float NdotL0 = clamp(dot(n, l0), 0.001, 1.0);
color = mix(color * getShadow() * NdotL0, color, ambientColor.r * 1.5);
#else
// color += ambientColor;
#endif

#if (numPointLights > 0)
// Point lights calculation
for(int i = 0; i < numPointLights; i++) {
    color += getPointLightContribution(pbrSurface, u_pointLights[i]);
}
#endif // numPointLights

#if (numSpotLights > 0)
// Spot lights calculation
for(int i = 0; i < numSpotLights; i++) {
    color += getSpotLightContribution(pbrSurface, u_spotLights[i]);
}
#endif // numSpotLights



// Apply optional PBR terms for additional (optional) shading
#ifdef occlusionTextureFlag
float ao = texture2D(u_occlusionTexture, v_occlusionUV).r;
color = mix(color, color * ao, 0.5);
#endif

// Add emissive
#if defined(emissiveTextureFlag) && defined(emissiveColorFlag)
vec3 emissive = SRGBtoLINEAR(texture2D(u_emissiveTexture, v_emissiveUV)).rgb * u_emissiveColor.rgb;
#elif defined(emissiveTextureFlag)
vec3 emissive = SRGBtoLINEAR(texture2D(u_emissiveTexture, v_emissiveUV)).rgb;
#elif defined(emissiveColorFlag)
vec3 emissive = u_emissiveColor.rgb;
#endif

#if defined(emissiveTextureFlag) || defined(emissiveColorFlag)
color += emissive;
#endif

// Final fragment color
#ifdef MANUAL_SRGB
out_FragColor = vec4(pow(color, vec3(1.0 / 2.3)), baseColor.a);
#else
out_FragColor = vec4(color, baseColor.a);
#endif

vec3 desaturated = mix(out_FragColor.rgb, vec3(0.1,0.1,0.4), 0.2); // Добавляем 20% белого
float luminance = dot(desaturated, vec3(0.299, 0.587, 0.114));
out_FragColor.rgb = mix(vec3(luminance), desaturated, 0.7);

#ifdef fogFlag
float fogFactor = smoothstep(0.1, 170, eyeDistance);
fogFactor = pow(fogFactor, 1);
vec3 fogColor = mix(u_fogColor.rgb, u_fogColor.rgb, fogFactor);
fogColor.rg *= 0.6;
fogColor.b *= 0.7;
fogColor.rgb *= 1.1;
out_FragColor.rgb = mix(out_FragColor.rgb, fogColor, fogFactor*1);
#endif

// Blending and Alpha Test
#ifdef blendedFlag
out_FragColor.a = baseColor.a * v_opacity;
#ifdef alphaTestFlag
if (out_FragColor.a <= v_alphaTest)
    discard;
#endif
#else
out_FragColor.a = 1.0;
#endif

float intensity = dot(out_FragColor.rgb, vec3(0.299, 0.587, 0.114));
out_FragColor.rgb *= 1.1;
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

out_FragColor.rgb *= factor;
out_FragColor.rgb = projectWarlockStyle(out_FragColor.rgb);
out_FragColor.rgb = softLighting(out_FragColor.rgb, 2);
}
#endif
