$input v_color0

#include <bgfx_shader.sh>

void main() {
    // Aquí asignamos el color final de la estrella
    gl_FragColor = v_color0;
    gl_FragColor.a = 1.0; // Opacidad total
}