#ifndef INSTANCING
  $input v_fogColor, v_worldPos, v_underwaterRainTime
#endif

#include <bgfx_shader.sh>

#ifndef INSTANCING
  #include <newb/main.sh>
  uniform vec4 FogAndDistanceControl;
#endif

float GetStarNoise(vec2 pos) {
    return fract(sin(dot(pos, vec2(12.9898, 78.233))) * 43758.5453123);
}

vec3 RenderStars(vec3 viewPos, float VdotU) {
    vec3 wpos = normalize(viewPos * 500.0);

    vec3 starCoord = 0.85 * wpos / (abs(wpos.y) + length(wpos.xz));
    vec2 starCoord2 = starCoord.xz * 3.5;
    if (VdotU < 0.0) starCoord2 += 100.0;
    float starFactor = 256.0;
    starCoord2 = floor(starCoord2 * starFactor) / starFactor;

    float star = 1.0;
    star *= GetStarNoise(starCoord2.xy);
    star *= GetStarNoise(starCoord2.xy + 0.1);
    star *= GetStarNoise(starCoord2.xy + 0.23);
    star = max(star - 0.65, 0.0);
    star *= star;

    vec3 starColor = vec3(0.9, 0.9, 1.0);
    vec3 stars = star * starColor * 1200.0;

    float VdotUM1 = abs(VdotU);
    float VdotUM2 = pow(1.0 - VdotUM1, 2.0);
    stars *= VdotUM1 * VdotUM1 * (VdotUM2 + 0.005) + 0.005;

    return stars;
}

void main() {
  #ifndef INSTANCING
    vec3 viewDir = normalize(v_worldPos);

    nl_environment env;
    env.end = false;
    env.nether = false;
    env.underwater = v_underwaterRainTime.x > 0.5;
    env.rainFactor = v_underwaterRainTime.y;

    nl_skycolor skycol;
    if (env.underwater) {
      skycol = nlUnderwaterSkyColors(env.rainFactor, v_fogColor.rgb);
    } else {
      skycol = nlOverworldSkyColors(env.rainFactor, v_fogColor.rgb);
    }

    vec3 skyColor = nlRenderSky(skycol, env, -viewDir, v_fogColor, v_underwaterRainTime.z);
    
    // Estrellas
    float mask = (1.0 - 1.0 * env.rainFactor) * max(1.0 - 3.0 * max(v_fogColor.b, v_fogColor.g), 0.0);
    skyColor += mask * RenderStars(viewDir, dot(viewDir, vec3(0.0, 1.0, 0.0)));

    #ifdef NL_SHOOTING_STAR
      skyColor += NL_SHOOTING_STAR * nlRenderShootingStar(viewDir, v_fogColor, v_underwaterRainTime.z);
    #endif

    skyColor = colorCorrection(skyColor);

    gl_FragColor = vec4(skyColor, 1.0);
  #else
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
  #endif
}