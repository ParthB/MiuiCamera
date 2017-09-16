package com.android.camera.effect.renders;

import com.android.gallery3d.ui.BasicTexture;
import com.android.gallery3d.ui.GLCanvas;

public class YBlurEffectRender extends RegionEffectRender {
    public YBlurEffectRender(GLCanvas canvas, int id) {
        super(canvas, id);
    }

    protected void drawTexture(BasicTexture texture, float x, float y, float w, float h, boolean isSnapShot) {
        setStep(texture.getTextureWidth(), texture.getTextureHeight());
        super.drawTexture(texture, x, y, w, h, isSnapShot);
    }

    public String getFragShaderString() {
        return "precision mediump float; \nuniform vec2 uStep; \nuniform sampler2D sTexture; \nvarying vec2 vTexCoord; \nuniform float uAlpha; \nvoid main() { \n    vec2 step = vec2(0.0, uStep.y) ; \n    vec2 delta = step; \n    int radius = 22; \n    float factor = 0.001890359; \n    float weight = factor * float(radius + 1); \n    vec3 sum = texture2D(sTexture, vTexCoord).rgb * weight; \n    for (int i = 1; i <= radius; ++i) { \n        weight -= factor; \n        sum += (texture2D(sTexture, vTexCoord + delta).rgb + texture2D(sTexture, vTexCoord - delta).rgb) * weight; \n        delta += step; \n    } \n    gl_FragColor = vec4(sum, 1.0)*uAlpha; \n}";
    }
}
