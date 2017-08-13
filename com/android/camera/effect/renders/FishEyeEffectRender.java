package com.android.camera.effect.renders;

import android.opengl.GLES20;
import com.android.gallery3d.ui.GLCanvas;

public class FishEyeEffectRender extends ConvolutionEffectRender {
    private float mF;
    private float mInvMaxDist;
    private int mUniformFH;
    private int mUniformInvMaxDistH;

    public FishEyeEffectRender(GLCanvas canvas, int id) {
        super(canvas, id);
    }

    public String getFragShaderString() {
        return "precision highp float; \nuniform sampler2D sTexture; \nvarying vec2 vTexCoord; \nuniform vec2 uStep; \nuniform float uInvMaxDist; \nuniform float uF; \nuniform float uAlpha; \nvec3 fisheye() { \n    const float slope = 20.0;               // vignette slope  \n    const float shade = 0.85;               // vignette shading  \n    const float range = 0.6;               // 0.6 - 1.3 \n    const float zoom = 0.3;               // smaller zoom means bigger image \n    vec2 coord = (vTexCoord - 0.5) / uStep; // convert to world coordinate  \n    float dist = length(coord); // distance to the center \n    float lumen = shade / (1.0 + exp((dist * uInvMaxDist - range) * slope)) + (1.0 - shade); \n    float t = zoom*dist/uF; \n    float theta = asin(t)*2.0; \n    float r = uF * tan(theta); \n    float angle = atan(coord.y, coord.x); \n    vec2 newCoord = vec2(cos(angle), sin(angle))*uStep*r+0.5; \n    return texture2D(sTexture, newCoord).rgb;  \n   // return texture2D(sTexture, newCoord).rgb * lumen; \n} \nvoid main() { \n    gl_FragColor.rgb = fisheye(); \n    gl_FragColor.a = 1.0; \n    gl_FragColor = gl_FragColor*uAlpha; \n}";
    }

    protected void initShader() {
        super.initShader();
        this.mUniformFH = GLES20.glGetUniformLocation(this.mProgram, "uF");
        this.mUniformInvMaxDistH = GLES20.glGetUniformLocation(this.mProgram, "uInvMaxDist");
    }

    public void setPreviewSize(int w, int h) {
        this.mPreviewWidth = w;
        this.mPreviewHeight = h;
        int d = w > h ? h : w;
        float L = (float) Math.sqrt((double) ((this.mPreviewWidth * this.mPreviewWidth) + (this.mPreviewHeight * this.mPreviewHeight)));
        if (d > 1080) {
            this.mStepX = 2.5f / ((float) this.mPreviewWidth);
            this.mStepY = 2.5f / ((float) this.mPreviewHeight);
            this.mF = (6.0f * L) / 35.0f;
        } else if (d > 720) {
            this.mStepX = 1.5f / ((float) this.mPreviewWidth);
            this.mStepY = 1.5f / ((float) this.mPreviewHeight);
            this.mF = (7.0f * L) / 35.0f;
        } else {
            this.mStepX = 1.0f / ((float) this.mPreviewWidth);
            this.mStepY = 1.0f / ((float) this.mPreviewHeight);
            this.mF = (10.0f * L) / 35.0f;
        }
        this.mInvMaxDist = 2.0f / L;
    }

    protected void initShaderValue(boolean isSnapShot) {
        super.initShaderValue(isSnapShot);
        GLES20.glUniform1f(this.mUniformFH, this.mF);
        GLES20.glUniform1f(this.mUniformInvMaxDistH, this.mInvMaxDist);
    }
}
