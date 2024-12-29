$input v_color0, v_color1, v_fog, v_refl, v_texcoord0, v_lightmapUV, v_extra

#include <bgfx_shader.sh>
#include <newb/main.sh>

SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_SeasonsTexture);
SAMPLER2D_AUTOREG(s_LightMapTexture);

uniform sampler2D depthtex0;
uniform mat4 gbufferProjectionInverse;
uniform vec3 upVec;
uniform vec3 sunVec;
uniform float view;
uniform float far;

const vec2 darkOutlineOffsets[12] = vec2[12](
    vec2( 1.0, 0.0), vec2(-1.0, 1.0), vec2( 0.0, 1.0), vec2( 1.0, 1.0),
    vec2(-2.0, 2.0), vec2(-1.0, 2.0), vec2( 0.0, 2.0), vec2( 1.0, 2.0),
    vec2( 2.0, 2.0), vec2(-2.0, 1.0), vec2( 2.0, 1.0), vec2( 2.0, 0.0)
);

float GetLinearDepth(float depth) {
    return depth * far;
}

void DoDarkOutline(inout vec3 color, float depth, vec2 uv) {
    vec2 scale = vec2(1.0 / view);

    float outline = 1.0;
    float z = GetLinearDepth(depth) * far * 2.0;

    for (int i = 0; i < 12; i++) {
        vec2 offset = scale * darkOutlineOffsets[i];
        float sampleZA = texture2D(depthtex0, uv + offset).r;
        float sampleZB = texture2D(depthtex0, uv - offset).r;

        float sampleZsum = GetLinearDepth(sampleZA) + GetLinearDepth(sampleZB);
        outline *= clamp(1.0 - (z - sampleZsum * far), 0.0, 1.0);
    }

    color = mix(color, vec3(0.0), 1.0 - outline);
}

void main() {
  #if defined(DEPTH_ONLY_OPAQUE) || defined(DEPTH_ONLY) || defined(INSTANCING)
    gl_FragColor = vec4(1.0,1.0,1.0,1.0);
    return;
  #endif

  vec4 diffuse = texture2D(s_MatTexture, v_texcoord0);
  vec4 color = v_color0;

  #ifdef ALPHA_TEST
    if (diffuse.a < 0.6) {
      discard;
    }
  #endif

  #if defined(SEASONS) && (defined(OPAQUE) || defined(ALPHA_TEST))
    diffuse.rgb *= mix(vec3(1.0,1.0,1.0), texture2D(s_SeasonsTexture, v_color1.xy).rgb * 2.0, v_color1.z);
  #endif

  vec3 glow = nlGlow(s_MatTexture, v_texcoord0, v_extra.a);

  diffuse.rgb *= diffuse.rgb;

  vec3 lightTint = texture2D(s_LightMapTexture, v_lightmapUV).rgb;
  lightTint = mix(lightTint.bbb, lightTint*lightTint, 0.35 + 0.65*v_lightmapUV.y*v_lightmapUV.y*v_lightmapUV.y);

  color.rgb *= lightTint;

  #if defined(TRANSPARENT) && !(defined(SEASONS) || defined(RENDER_AS_BILLBOARDS))
    if (v_extra.b > 0.9) {
      diffuse.rgb = vec3_splat(1.0 - NL_WATER_TEX_OPACITY*(1.0 - diffuse.b*1.8));
      diffuse.a = color.a;
    }
  #else
    diffuse.a = 1.0;
  #endif

  diffuse.rgb *= color.rgb;
  diffuse.rgb += glow;

  if (v_extra.b > 0.9) {
    diffuse.rgb += v_refl.rgb*v_refl.a;
  } else if (v_refl.a > 0.0) {
    // reflective effect - only on xz plane
    float dy = abs(dFdy(v_extra.g));
    if (dy < 0.0002) {
      float mask = v_refl.a*(clamp(v_extra.r*10.0,8.2,8.8)-7.8);
      diffuse.rgb *= 1.0 - 0.6*mask;
      diffuse.rgb += v_refl.rgb*mask;
    }
  }

  diffuse.rgb = mix(diffuse.rgb, v_fog.rgb, v_fog.a);

  diffuse.rgb = colorCorrection(diffuse.rgb);

  // Apply dark outline effect
  float depth = texture2D(depthtex0, v_texcoord0).r;
  DoDarkOutline(diffuse.rgb, depth, v_texcoord0);

  gl_FragColor = diffuse;
}