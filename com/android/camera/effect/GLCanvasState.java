package com.android.camera.effect;

import android.opengl.Matrix;
import com.android.gallery3d.ui.Utils;
import java.util.Stack;

public class GLCanvasState {
    private float mAlpha = 1.0f;
    private float mBlendAlpha = -1.0f;
    private Stack<CanvasStateConfig> mCanvaStateStack = new Stack();
    private final float[] mIdentityMatrix = new float[]{1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f};
    private float[] mMVPMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mTexMatrix = new float[16];
    private float[] mViewMatrix = new float[16];

    private class CanvasStateConfig {
        float mAlpha = 1.0f;
        float mBlendAlpha = -1.0f;
        float[] mModelMatrix = new float[16];
        float[] mTexMatrix = new float[16];

        public CanvasStateConfig(float[] modelMatrix, float[] texMatrix, float alpha, float blendAlpha) {
            int i;
            for (i = 0; i < 16; i++) {
                this.mModelMatrix[i] = modelMatrix[i];
            }
            for (i = 0; i < 16; i++) {
                this.mTexMatrix[i] = texMatrix[i];
            }
            this.mAlpha = alpha;
            this.mBlendAlpha = blendAlpha;
        }

        public float[] getModelMatrix() {
            return this.mModelMatrix;
        }

        public float[] getTexMatrix() {
            return this.mTexMatrix;
        }

        public float getAlpha() {
            return this.mAlpha;
        }

        public float getBlendAlpha() {
            return this.mBlendAlpha;
        }
    }

    public GLCanvasState() {
        Matrix.setIdentityM(this.mModelMatrix, 0);
        Matrix.setIdentityM(this.mViewMatrix, 0);
        Matrix.setIdentityM(this.mProjectionMatrix, 0);
        Matrix.setIdentityM(this.mTexMatrix, 0);
    }

    public void indentityAllM() {
        Matrix.setIdentityM(this.mModelMatrix, 0);
        Matrix.setIdentityM(this.mTexMatrix, 0);
        Matrix.setIdentityM(this.mViewMatrix, 0);
        Matrix.setIdentityM(this.mProjectionMatrix, 0);
    }

    public void indentityTexM() {
        Matrix.setIdentityM(this.mTexMatrix, 0);
    }

    public void pushState() {
        this.mCanvaStateStack.push(new CanvasStateConfig(this.mModelMatrix, this.mTexMatrix, this.mAlpha, this.mBlendAlpha));
    }

    public void popState() {
        if (!this.mCanvaStateStack.isEmpty()) {
            CanvasStateConfig matrixObject = (CanvasStateConfig) this.mCanvaStateStack.pop();
            if (matrixObject == null) {
                throw new IllegalStateException();
            }
            this.mModelMatrix = matrixObject.getModelMatrix();
            this.mTexMatrix = matrixObject.getTexMatrix();
            this.mAlpha = matrixObject.getAlpha();
            this.mBlendAlpha = matrixObject.getBlendAlpha();
        }
    }

    public void translate(float x, float y, float z) {
        Matrix.translateM(this.mModelMatrix, 0, x, y, z);
    }

    public void translate(float x, float y) {
        Matrix.translateM(this.mModelMatrix, 0, x, y, 0.0f);
    }

    public void rotate(float angle, float x, float y, float z) {
        if (angle != 0.0f) {
            Matrix.rotateM(this.mModelMatrix, 0, angle, x, y, z);
        }
    }

    public void scale(float x, float y, float z) {
        Matrix.scaleM(this.mModelMatrix, 0, x, y, z);
    }

    public void ortho(float left, float right, float bottom, float top) {
        Matrix.orthoM(this.mProjectionMatrix, 0, left, right, bottom, top, -1.0f, 1.0f);
    }

    public float getAlpha() {
        return this.mAlpha;
    }

    public void setAlpha(float alpha) {
        boolean z = false;
        if (alpha >= 0.0f && alpha <= 1.0f) {
            z = true;
        }
        Utils.assertTrue(z);
        this.mAlpha = alpha;
    }

    public float getBlendAlpha() {
        return this.mBlendAlpha;
    }

    public void setBlendAlpha(float alpha) {
        boolean z = false;
        if (alpha >= 0.0f && alpha <= 1.0f) {
            z = true;
        }
        Utils.assertTrue(z);
        this.mBlendAlpha = alpha;
    }

    public void setTexMatrix(float left, float top, float right, float bottom) {
        Matrix.setIdentityM(this.mTexMatrix, 0);
        this.mTexMatrix[0] = right - left;
        this.mTexMatrix[5] = bottom - top;
        this.mTexMatrix[10] = 1.0f;
        this.mTexMatrix[12] = left;
        this.mTexMatrix[13] = top;
        this.mTexMatrix[15] = 1.0f;
    }

    public void setTexMatrix(float[] textureTransform) {
        for (int i = 0; i < 16; i++) {
            this.mTexMatrix[i] = textureTransform[i];
        }
    }

    public float[] getFinalMatrix() {
        Matrix.multiplyMM(this.mMVPMatrix, 0, this.mViewMatrix, 0, this.mModelMatrix, 0);
        Matrix.multiplyMM(this.mMVPMatrix, 0, this.mProjectionMatrix, 0, this.mMVPMatrix, 0);
        return this.mMVPMatrix;
    }

    public float[] getTexMaxtrix() {
        return this.mTexMatrix;
    }
}
