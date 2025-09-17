#version 330 core

in vec2 v_TexCoord;
in vec3 v_Normal;

out vec4 fragColor;

uniform sampler2D u_Texture;
uniform vec3 u_LightDirection;

void main() {
    vec4 texColor = texture(u_Texture, v_TexCoord);
 // Применение простого эффекта контуров
    float brightness = dot(normalize(v_Normal), -u_LightDirection);
    brightness = clamp(brightness, 0.0, 1.0);
 // Эффект комикса
    fragColor = vec4(texColor.rgb * brightness, texColor.a);

    // Применение черного контура
    if (brightness < 0.5) {
        fragColor.rgb = vec3(0.0);
    }
}
