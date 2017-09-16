package com.android.camera.effect.renders;

import android.graphics.Color;
import android.opengl.GLES20;
import com.android.gallery3d.ui.GLCanvas;

/* compiled from: FocusPeakingRender */
class FocusPeakingSecondPassRender extends ConvolutionEffectRender {
    private int mPeakColor = -65536;
    private int mUniformPeakColorH;

    public FocusPeakingSecondPassRender(GLCanvas canvas) {
        super(canvas);
    }

    protected void initShader() {
        super.initShader();
        this.mUniformPeakColorH = GLES20.glGetUniformLocation(this.mProgram, "uPeakColor");
    }

    protected void initShaderValue(boolean isSnapShot) {
        super.initShaderValue(isSnapShot);
        GLES20.glUniform3f(this.mUniformPeakColorH, (float) Color.red(this.mPeakColor), (float) Color.green(this.mPeakColor), (float) Color.blue(this.mPeakColor));
    }

    protected void setBlendEnabled(boolean enabled) {
        GLES20.glEnable(3042);
    }

    public String getFragShaderString() {
        return "precision mediump float; \nuniform vec2 uStep; \nuniform vec3 uPeakColor; \nuniform sampler2D sTexture; \nvarying vec2 vTexCoord; \nvec3 neighbor_color() { \n    vec3 sum = vec3(0.0, 0.0, 0.0); \n    vec2 step = uStep; \n    //sum += texture2D(sTexture, vTexCoord -                  step).rgb; \n    sum += texture2D(sTexture, vTexCoord + vec2(0.0,    -step.y)).rgb; \n    //sum += texture2D(sTexture, vTexCoord + vec2(step.x, -step.y)).rgb; \n    sum += texture2D(sTexture, vTexCoord + vec2(step.x,     0.0)).rgb; \n    sum += texture2D(sTexture, vTexCoord                        ).rgb; \n    sum += texture2D(sTexture, vTexCoord + vec2(-step.x,    0.0)).rgb; \n    //sum += texture2D(sTexture, vTexCoord + vec2(-step.x, step.y)).rgb; \n    sum += texture2D(sTexture, vTexCoord + vec2(0.0,     step.y)).rgb; \n    //sum += texture2D(sTexture, vTexCoord +                  step).rgb; \n    return sum; \n} \nvoid main() { \n    vec3 sum = neighbor_color(); \n    if (any(greaterThan(sum, vec3(0.0, 0.0, 0.0)))) { \n        gl_FragColor.rgb = uPeakColor; \n        gl_FragColor.a = 1.0; \n    } else { \n        gl_FragColor.rgb = vec3(0.0, 0.0, 0.0); \n        gl_FragColor.a = 0.0; \n    } \n}";
    }
}
