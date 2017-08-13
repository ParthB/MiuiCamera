package com.android.camera.effect.renders;

import android.opengl.GLES20;
import android.util.Log;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.GLId;
import java.nio.IntBuffer;

public abstract class RGBTransEffectRender extends PixelEffectRender {
    private static final int[] sTextureIds = new int[1];
    protected int mRGBLutId;
    protected boolean mRGBLutLoaded;
    protected int mUniformRGBLutH;

    public RGBTransEffectRender(GLCanvas canvas, int id) {
        super(canvas, id);
    }

    public void setRGBTransLutBuffer(IntBuffer lut) {
        GLId.glGenTextures(1, sTextureIds, 0);
        this.mRGBLutId = sTextureIds[0];
        GLES20.glBindTexture(3553, this.mRGBLutId);
        GLES20.glTexParameteri(3553, 10242, 33071);
        GLES20.glTexParameteri(3553, 10243, 33071);
        GLES20.glTexParameterf(3553, 10241, 9728.0f);
        GLES20.glTexParameterf(3553, 10240, 9728.0f);
        GLES20.glTexImage2D(3553, 0, 6408, lut.capacity(), 1, 0, 6408, 5121, lut);
        this.mRGBLutLoaded = true;
    }

    protected void initShader() {
        super.initShader();
        this.mUniformRGBLutH = GLES20.glGetUniformLocation(this.mProgram, "sRGBLut");
    }

    protected void initShaderValue(boolean isSnapShot) {
        super.initShaderValue(isSnapShot);
        GLES20.glUniform1i(this.mUniformRGBLutH, 1);
    }

    protected void bindExtraTexture() {
        super.bindExtraTexture();
        bindTexture(this.mRGBLutId, 33985);
    }

    protected void finalize() throws Throwable {
        if (this.mGLCanvas != null && this.mRGBLutLoaded) {
            Log.d("Camera", "delete RGBTransEffectRender texture = " + this.mRGBLutId);
            this.mGLCanvas.deleteTexture(this.mRGBLutId);
        }
        super.finalize();
    }
}
