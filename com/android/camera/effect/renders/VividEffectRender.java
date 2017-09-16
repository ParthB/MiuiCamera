package com.android.camera.effect.renders;

import com.android.gallery3d.ui.GLCanvas;

public class VividEffectRender extends PixelEffectRender {
    public VividEffectRender(GLCanvas canvas, int id) {
        super(canvas, id);
    }

    public String getFragShaderString() {
        return "precision mediump float; \nuniform sampler2D sTexture; \nvarying vec2 vTexCoord; \nuniform float uAlpha; \nfloat BlendOverlayf(float base, float blend) \n{ \n  return (base < 0.5 ? (2.0 * base * blend) : (1.0 - 2.0 * (1.0 - base) * (1.0 - blend))); \n} \nvec3 BlendOverlay(vec3 base, vec3 blend) \n{ \n  return vec3(BlendOverlayf(base.r, blend.r), BlendOverlayf(base.g, blend.g), BlendOverlayf(base.b, blend.b)); \n} \nvec3 HardLight(vec3 base, vec3 blend) \n{ \n  return BlendOverlay(blend, base); \n} \nvoid main() \n{ \n   vec4 color = texture2D(sTexture, vTexCoord); \n   vec3 tmpColor = HardLight(color.rgb, color.rgb); \n   gl_FragColor = vec4(mix(color.rgb, tmpColor, 0.7), color.a); \n   gl_FragColor = gl_FragColor*uAlpha; \n} \n";
    }
}
