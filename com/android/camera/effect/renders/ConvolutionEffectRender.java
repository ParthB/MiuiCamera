package com.android.camera.effect.renders;

import android.opengl.GLES20;
import com.android.gallery3d.ui.GLCanvas;

public abstract class ConvolutionEffectRender extends PixelEffectRender {
    protected float mStepX;
    protected float mStepY;
    protected int mUniformStepH;

    public ConvolutionEffectRender(GLCanvas canvas) {
        super(canvas);
    }

    public ConvolutionEffectRender(GLCanvas canvas, int id) {
        super(canvas, id);
    }

    protected void initShader() {
        super.initShader();
        this.mUniformStepH = GLES20.glGetUniformLocation(this.mProgram, "uStep");
    }

    public void setPreviewSize(int w, int h) {
        super.setPreviewSize(w, h);
        setStep(this.mPreviewWidth, this.mPreviewHeight);
    }

    public void setStep(int width, int height) {
        this.mStepX = 1.0f / ((float) width);
        this.mStepY = 1.0f / ((float) height);
    }

    protected void initShaderValue(boolean isSnapShot) {
        super.initShaderValue(isSnapShot);
        GLES20.glUniform2f(this.mUniformStepH, this.mStepX, this.mStepY);
    }
}
