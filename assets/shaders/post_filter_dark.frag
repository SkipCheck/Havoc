#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP 
#endif

varying LOWP vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform sampler2D u_texture1;
uniform sampler2D u_texture2;
uniform sampler2D u_texture3;

uniform float u_brightness;
uniform float u_eyeAdaptation;

vec4 blur9(sampler2D image, vec2 uv, vec2 resolution, vec2 direction);

void main()
{    
    vec4 color = texture2D(u_texture, v_texCoords);

    // combine blurs to do a high pass glow
    vec4 blur_color_wide = texture2D(u_texture1, v_texCoords);
    blur_color_wide *= blur_color_wide;

    vec4 blur_color_mid = texture2D(u_texture2, v_texCoords);
    blur_color_mid *= blur_color_mid;

    vec4 blur_color_narrow = texture2D(u_texture3, v_texCoords);
    blur_color_narrow *= blur_color_narrow;

    // bias the blurs to get a nice shape
    blur_color_wide.rgb *= 0.5;
    blur_color_mid.rgb *= 0.1;
    blur_color_narrow.rgb *= 0.1;

    vec4 bloom_total = blur_color_wide + blur_color_mid + blur_color_narrow;

    // adjust bloom and base colors based on the iris size
    bloom_total *= (1.0 * (1.0 - u_eyeAdaptation));

    // final colors
    float color_mod = 0.1;
    gl_FragColor = (color + color * color_mod) - (color_mod * 1) + bloom_total;
}