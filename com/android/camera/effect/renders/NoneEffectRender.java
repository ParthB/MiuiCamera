package com.android.camera.effect.renders;

import com.android.gallery3d.ui.GLCanvas;

public class NoneEffectRender extends PixelEffectRender {
    public NoneEffectRender(GLCanvas canvas) {
        super(canvas);
    }

    public String getFragShaderString() {
        return "precision mediump float; \nuniform sampler2D sTexture; \nvarying vec2 vTexCoord; \nvoid main() { \n    gl_FragColor = vec4(texture2D(sTexture, vTexCoord).rgb, 1); \n}";
    }
}
