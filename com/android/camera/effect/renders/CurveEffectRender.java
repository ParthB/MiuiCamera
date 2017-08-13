package com.android.camera.effect.renders;

import com.android.gallery3d.ui.GLCanvas;

public class CurveEffectRender extends RGBTransEffectRender {
    public CurveEffectRender(GLCanvas canvas, int id) {
        super(canvas, id);
    }

    public String getFragShaderString() {
        return "precision mediump int; \nprecision mediump float; \nuniform sampler2D sTexture; \nuniform sampler2D sRGBLut; \nvarying vec2 vTexCoord; \nuniform float uAlpha; \nvoid main() { \n    vec3 color = texture2D(sTexture, vTexCoord).rgb; \n    vec3 index; \n    index.r = float(int(color.r * 255.0)) / 256.0; \n    index.g = float(int(color.g * 255.0)) / 256.0; \n    index.b = float(int(color.b * 255.0)) / 256.0; \n    color.r = texture2D(sRGBLut, vec2(index.r, 0.0)).r; \n    color.g = texture2D(sRGBLut, vec2(index.g, 0.0)).g; \n    color.b = texture2D(sRGBLut, vec2(index.b, 0.0)).b; \n    gl_FragColor = vec4(color, 1.0) * uAlpha; \n}";
    }
}
