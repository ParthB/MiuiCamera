package com.android.gallery3d.ui;

import android.opengl.GLES20;

public class ExtTexture extends BasicTexture {
    private static int[] sTextureId = new int[1];
    private int mTarget;

    public ExtTexture() {
        GLId.glGenTextures(1, sTextureId, 0);
        this.mId = sTextureId[0];
        this.mTarget = 36197;
    }

    private void uploadToCanvas(GLCanvas canvas) {
        GLES20.glBindTexture(this.mTarget, this.mId);
        GLES20.glTexParameteri(this.mTarget, 10242, 33071);
        GLES20.glTexParameteri(this.mTarget, 10243, 33071);
        GLES20.glTexParameterf(this.mTarget, 10241, 9729.0f);
        GLES20.glTexParameterf(this.mTarget, 10240, 9729.0f);
        setAssociatedCanvas(canvas);
        this.mState = 1;
    }

    public void draw(GLCanvas canvas, int x, int y, int w, int h) {
    }

    public boolean onBind(GLCanvas canvas) {
        if (!isLoaded()) {
            uploadToCanvas(canvas);
        }
        return true;
    }

    public int getTarget() {
        return this.mTarget;
    }

    public boolean isOpaque() {
        return true;
    }
}
