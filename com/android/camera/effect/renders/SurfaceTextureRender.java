package com.android.camera.effect.renders;

import android.opengl.GLES20;
import com.android.camera.effect.ShaderUtil;
import com.android.camera.effect.draw_mode.DrawAttribute;
import com.android.camera.effect.draw_mode.DrawExtTexAttribute;
import com.android.gallery3d.ui.BasicTexture;
import com.android.gallery3d.ui.ExtTexture;
import com.android.gallery3d.ui.GLCanvas;

public class SurfaceTextureRender extends ShaderRender {
    private static final float[] TEXTURES = new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f};
    private static final float[] VERTICES = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};

    public SurfaceTextureRender(GLCanvas canvas) {
        super(canvas);
    }

    protected void initShader() {
        this.mProgram = ShaderUtil.createProgram(getVertexShaderString(), getFragShaderString());
        if (this.mProgram != 0) {
            GLES20.glUseProgram(this.mProgram);
            this.mUniformMVPMatrixH = GLES20.glGetUniformLocation(this.mProgram, "uMVPMatrix");
            this.mUniformSTMatrixH = GLES20.glGetUniformLocation(this.mProgram, "uSTMatrix");
            this.mUniformTextureH = GLES20.glGetUniformLocation(this.mProgram, "sTexture");
            this.mUniformAlphaH = GLES20.glGetUniformLocation(this.mProgram, "uAlpha");
            this.mUniformBlendAlphaH = GLES20.glGetUniformLocation(this.mProgram, "uMixAlpha");
            this.mAttributePositionH = GLES20.glGetAttribLocation(this.mProgram, "aPosition");
            this.mAttributeTexCoorH = GLES20.glGetAttribLocation(this.mProgram, "aTexCoord");
            return;
        }
        throw new IllegalArgumentException(getClass() + ": mProgram = 0");
    }

    public String getFragShaderString() {
        return "#extension GL_OES_EGL_image_external : require  \nprecision mediump float; \nuniform float uAlpha; \nuniform float uMixAlpha; \nuniform samplerExternalOES sTexture; \nvarying vec2 vTexCoord; \nvoid main() \n{ \n    gl_FragColor = texture2D(sTexture, vTexCoord)*uAlpha; \n    if (uMixAlpha >= 0.0) { \n       gl_FragColor.a = uMixAlpha; \n    } \n}";
    }

    protected void initVertexData() {
        this.mVertexBuffer = ShaderRender.allocateByteBuffer((VERTICES.length * 32) / 8).asFloatBuffer();
        this.mVertexBuffer.put(VERTICES);
        this.mVertexBuffer.position(0);
        this.mTexCoorBuffer = ShaderRender.allocateByteBuffer((TEXTURES.length * 32) / 8).asFloatBuffer();
        this.mTexCoorBuffer.put(TEXTURES);
        this.mTexCoorBuffer.position(0);
    }

    protected void initSupportAttriList() {
        this.mAttriSupportedList.add(Integer.valueOf(8));
    }

    public boolean draw(DrawAttribute attri) {
        if (!isAttriSupported(attri.getTarget())) {
            return false;
        }
        DrawExtTexAttribute texture = (DrawExtTexAttribute) attri;
        drawTexture(texture.mExtTexture, texture.mTextureTransform, (float) texture.mX, (float) texture.mY, (float) texture.mWidth, (float) texture.mHeight);
        return true;
    }

    private void drawTexture(ExtTexture texture, float[] textureTransform, float x, float y, float w, float h) {
        GLES20.glUseProgram(0);
        GLES20.glUseProgram(this.mProgram);
        if (bindTexture((BasicTexture) texture, 33984)) {
            initAttribPointer();
            updateViewport();
            float alpha = this.mGLCanvas.getState().getAlpha();
            float blendalpha = this.mGLCanvas.getState().getBlendAlpha();
            boolean z = this.mBlendEnabled && (!texture.isOpaque() || alpha < 0.95f || blendalpha >= 0.0f);
            setBlendEnabled(z);
            this.mGLCanvas.getState().pushState();
            this.mGLCanvas.getState().translate(x, y, 0.0f);
            this.mGLCanvas.getState().scale(w, h, 1.0f);
            GLES20.glUniformMatrix4fv(this.mUniformMVPMatrixH, 1, false, this.mGLCanvas.getState().getFinalMatrix(), 0);
            GLES20.glUniformMatrix4fv(this.mUniformSTMatrixH, 1, false, textureTransform, 0);
            GLES20.glUniform1i(this.mUniformTextureH, 0);
            GLES20.glUniform1f(this.mUniformAlphaH, this.mGLCanvas.getState().getAlpha());
            GLES20.glUniform1f(this.mUniformBlendAlphaH, blendalpha);
            GLES20.glDrawArrays(5, 0, 4);
            this.mGLCanvas.getState().popState();
        }
    }

    private void initAttribPointer() {
        GLES20.glVertexAttribPointer(this.mAttributePositionH, 2, 5126, false, 8, this.mVertexBuffer);
        GLES20.glVertexAttribPointer(this.mAttributeTexCoorH, 2, 5126, false, 8, this.mTexCoorBuffer);
        GLES20.glEnableVertexAttribArray(this.mAttributePositionH);
        GLES20.glEnableVertexAttribArray(this.mAttributeTexCoorH);
    }
}
