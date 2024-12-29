#ifndef INSTANCING
  $input v_texcoord0, v_posTime
#endif

#include <bgfx_shader.sh>

#ifndef INSTANCING
  #include <newb/main.sh>

  SAMPLER2D_AUTOREG(s_SkyTexture);
#endif

// Función calcPoint
float calcPoint(float2 pos) {
    pos = frac(pos) - 0.5;
    return 2.0 * dot(pos, pos);
}

// Función voronoi
float voronoi(float2 pos) {
    float2x2 matrix = float2x2(-0.8, -0.5, 0.314, 0.8);
    return min(calcPoint(pos), calcPoint(mul(pos, matrix)));
}

// Función amap
float amap(float2 uv, float t) {
    uv += 0.01 * sin(40.0 * uv.xy);
    float f = voronoi(uv + 0.03 * t) * (0.5 + 0.5 * voronoi(0.5 * uv + 0.08 * t));
    f = smoothstep(0.05, 0.8, f);
    return f;
}

// Función aurora
float3 aurora(float3 vdir, float t) {
    float2 uv = 0.2 * vdir.xz / vdir.y;
    float3 c = float3(0.0, 0.0, 0.0);
    const int s = 16;
    const float si = 1.0 / float(s);
    for (int i = 0; i < s; i++) {
        float h = float(i) * si;
        float f = amap(uv, t);
        uv *= 1.05 + 0.03 * sin(30.0 * uv.x + t) * sin(30.0 * uv.y - t);
        float3 col = lerp(float3(0.0, 1.0, 0.0), float3(0.0, 0.0, 1.0), h);
        col = lerp(col.xyz, col.zxy, 0.5 + 0.5 * sin(uv.x - t));
        c += col * f * si;
    }
    c *= smoothstep(0.0, 0.4, vdir.y);
    return 3.0 * c;
}

// Función GetEnderStars
float GetEnderStarNoise(float2 pos) {
    return frac(sin(dot(pos, float2(12.9898, 78.233))) * 43758.5453123);
}

float3 GetEnderStars(float3 viewPos, float VdotU) {
    float3 wpos = normalize(viewPos * 500.0);
    float3 starCoord = 0.85 * wpos / (abs(wpos.y) + length(wpos.xz));
    float2 starCoord2 = starCoord.xz * 3.5;
    if (VdotU < 0.0) starCoord2 += 100.0;
    float starFactor = 256.0;
    starCoord2 = floor(starCoord2 * starFactor) / starFactor;

    float star = 1.0;
    star *= GetEnderStarNoise(starCoord2.xy);
    star *= GetEnderStarNoise(starCoord2.xy + 0.1);
    star *= GetEnderStarNoise(starCoord2.xy + 0.23);
    star = max(star - 0.65, 0.0);
    star *= star;

    float3 endSkyColor = float3(0.9, 0.9, 1.0);
    float3 enderStars = star * endSkyColor * 1200.0;

    float VdotUM1 = abs(VdotU);
    float VdotUM2 = pow(1.0 - VdotUM1, 2.0);
    enderStars *= VdotUM1 * VdotUM1 * (VdotUM2 + 0.005) + 0.005;

    return enderStars;
}

// Función renderMoon
float3 renderMoon(float2 uv, float time) {
    time *= 1.5;

    float a0 = 0.1 * time;
    float a1 = 0.1 * time;
    float a2 = 0.6 * time;
    float3x3 r = float3x3(1, 0, 0, 0, cos(a0), -sin(a0), 0, sin(a0), cos(a0));
    r *= float3x3(cos(a1), 0, sin(a1), 0, 1, 0, -sin(a1), 0, cos(a1));
    r *= float3x3(cos(a2), -sin(a2), 0, sin(a2), cos(a2), 0, 0, 0, 1);

    float3 v = float3(0.0, 0.0, -1.0) * r;
    float3 p = float3(uv, 0.0) * r - 1.0 * v;
    float3 ldtmp = normalize(float3(9.0, 8.0, 1.0));
    float3 ld = ldtmp * r;

    float3 c = float3(0.0);

    float st = max(sin(20.0 * uv.x + 0.3 * time) * sin(20.0 * uv.y + 4.0 * sin(5.0 * uv.x)) * sin(9.0 * uv.x * uv.y), 0.0);
    st = 0.1 * pow(st, 32.0) + 2.0 * pow(st, 180.0);
    float3 stc = float3(sin(20.0 * uv.x), sin(30.0 * uv.x + 0.8), sin(40.0 * uv.y));
    c += st * (0.5 + 0.5 * stc * stc);

    float dp = 0.0;
    float g = 2.0;
    for (int i = 0; i < 32; i++) {
        float3 pe = p + dp * v;
        float3 q = abs(pe) - 0.5;
        float dt = length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0);
        dp += dt;

        g = min(g, dt);

        if (dp > 2.0) {
            break;
        }

        if (dt < 0.01) {
            float3 n = normalize(pe * pow(abs(2.0 * pe), float3(6.0)));

            float3 h = floor(7.81 * pe);
            float j = 0.4 + 0.6 * (frac(176.728 * sin(dot(h, float3(24.06, 12.75, 172.3)))));
            c = float3(0.7, 0.7, 0.8) * j;

            c *= 0.2 + 0.8 * max(dot(ld, n), 0.0);

            g = 0.0;
            break;
        }
    }

    g = 4.0 / (1.0 + 5.0 * g);
    c = 0.9 * c + 0.15 * g * smoothstep(2.0, 0.2, length(uv - ldtmp.xy));

    c *= lerp(float3(1.0, 0.0, 0.1), float3(1.0, 1.0, 0.2), c);

    return c;
}

void main() {
  #ifndef INSTANCING
    float4 diffuse = texture2D(s_SkyTexture, v_texcoord0);

    float3 color = renderEndSky(getEndHorizonCol(), getEndZenithCol(), normalize(v_posTime.xyz), v_posTime.w);
    color += 2.8 * diffuse.rgb;

    float3 vdir = normalize(v_posTime.xyz);

    // Añadimos las auroras
    float3 auroras = aurora(vdir, v_posTime.w);
    color += auroras;

    float2 mpos = vdir.xz / vdir.y;
    mpos *= 4.0;

    float3 moon = renderMoon(mpos, v_posTime.w);
    color += moon;

    /*
    // Desactivamos la galaxia
    #ifdef NL_GALAXY_STARS
      nl_environment env;
      env.end = true;
      env.nether = false;
      env.underwater = false;
      env.rainFactor = 0.0;

      color += NL_GALAXY_STARS * nlRenderGalaxy(normalize(v_posTime.xyz), vec3_splat(0.0), env, v_posTime.w);
    #endif
    */

    color += GetEnderStars(vdir, dot(vdir, float3(0.0, 1.0, 0.0)));

    color = colorCorrection(color);

    gl_FragColor = float4(color, 1.0);
  #else
    gl_FragColor = float4(0.0, 0.0, 0.0, 0.0);
  #endif
}