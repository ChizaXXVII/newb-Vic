$input a_color0, a_position

$output v_color0

#include <bgfx_shader.sh>

uniform vec4 StarsColor;
uniform float starDensity; // Controla la densidad de estrellas (puedes multiplicarlo por 1.5 para un 50% más)

void main() {
    vec3 pos = a_position;

    // Generamos una dispersión aleatoria de las estrellas para que haya más
    // Usamos el seno y coseno de la posición para variar más las posiciones de las estrellas
    pos.x += sin(pos.y * 0.5) * 2.0; 
    pos.y += cos(pos.x * 0.5) * 2.0;
    pos.z += sin(pos.x * pos.y * 0.3) * 2.0;

    // Ajustamos la densidad de estrellas
    pos *= starDensity;

    // Transformamos la posición a espacio mundial
    vec3 worldPos = mul(u_model[0], vec4(pos, 1.0)).xyz;

    // Ajuste de color y brillo de las estrellas
    vec4 color = a_color0;
    color.rgb *= 0.6 + 0.4 * sin(3.0 * pos.x * pos.y); // Variabilidad en brillo

    // Se usa el color proporcionado para las estrellas
    color.rgb *= StarsColor.rgb;

    // Calculamos la posición final del vértice
    gl_Position = mul(u_viewProj, vec4(worldPos, 1.0));

    // Pasamos el color al fragment shader
    v_color0 = color;
}