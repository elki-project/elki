#version 130

uniform sampler2D texAlpha;
uniform sampler2D texColor;
uniform int grid; // grid size
uniform float alpha; // Alpha adjustment

varying vec3 type;

void main() {
    vec2 p = vec2(gl_TexCoord[0].x, 1. - gl_TexCoord[0].y) / grid;
    float a = alpha * texture2D(texAlpha, p + type.xy).a;
    if (a < 0.0039) { discard; } // discard fragment when alpha smaller than 1/255
    gl_FragColor = texture2D(texColor, vec2(type.z, 0.));
    gl_FragColor.a = a;
}