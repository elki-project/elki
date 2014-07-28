#version 130

uniform vec3 eye; // camera position
uniform float size; // size scale
uniform int grid; // grid size
uniform int numcolors; // Number of colors

varying vec3 type;

void main() {
    vec4 position = vec4(gl_MultiTexCoord0.x, gl_MultiTexCoord1.x, gl_MultiTexCoord2.x, 1.0);
 
    // transform position
    gl_Position = gl_ModelViewProjectionMatrix * position;
 
    // scale by distance
    gl_PointSize = size / distance(position.xyz, eye);
    gl_PointSize = clamp(gl_PointSize, 1., size);
    
    // texture number, via vertex.x
    float typex = mod(int(gl_Vertex.x), grid);
    float typey = mod(int((gl_Vertex.x - typex) / grid), grid);
    // color number, via normal.x
    float typez = mod(int(gl_Normal.x), numcolors);
    // Hackish, but the color is not destroyed by the GL_POINTS emulation
    type = vec4(typex / grid, typey / grid, typez / (numcolors + 1));
}