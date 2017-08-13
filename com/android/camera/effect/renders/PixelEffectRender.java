package com.android.camera.effect.renders;

import android.opengl.GLES20;
import com.android.camera.effect.ShaderUtil;
import com.android.camera.effect.draw_mode.DrawAttribute;
import com.android.camera.effect.draw_mode.DrawBasicTexAttribute;
import com.android.camera.effect.draw_mode.DrawIntTexAttribute;
import com.android.gallery3d.ui.BasicTexture;
import com.android.gallery3d.ui.GLCanvas;

public abstract class PixelEffectRender extends ShaderRender {
    private static final float[] TEXTURES = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] VERTICES = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};

    public PixelEffectRender(GLCanvas canvas) {
        super(canvas);
    }

    public PixelEffectRender(GLCanvas canvas, int id) {
        super(canvas, id);
    }

    protected void initShader() {
        this.mProgram = ShaderUtil.createProgram(getVertexShaderString(), getFragShaderString());
        if (this.mProgram != 0) {
            GLES20.glUseProgram(this.mProgram);
            this.mUniformMVPMatrixH = GLES20.glGetUniformLocation(this.mProgram, "uMVPMatrix");
            this.mUniformSTMatrixH = GLES20.glGetUniformLocation(this.mProgram, "uSTMatrix");
            this.mUniformTextureH = GLES20.glGetUniformLocation(this.mProgram, "sTexture");
            this.mAttributePositionH = GLES20.glGetAttribLocation(this.mProgram, "aPosition");
            this.mAttributeTexCoorH = GLES20.glGetAttribLocation(this.mProgram, "aTexCoord");
            this.mUniformAlphaH = GLES20.glGetUniformLocation(this.mProgram, "uAlpha");
            return;
        }
        throw new IllegalArgumentException(getClass() + ": mProgram = 0");
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
        this.mAttriSupportedList.add(Integer.valueOf(5));
        this.mAttriSupportedList.add(Integer.valueOf(6));
    }

    public boolean draw(DrawAttribute attri) {
        if (!isAttriSupported(attri.getTarget())) {
            return false;
        }
        switch (attri.getTarget()) {
            case 5:
                DrawBasicTexAttribute texture = (DrawBasicTexAttribute) attri;
                drawTexture(texture.mBasicTexture, (float) texture.mX, (float) texture.mY, (float) texture.mWidth, (float) texture.mHeight, texture.mIsSnapshot);
                break;
            case 6:
                DrawIntTexAttribute texture2 = (DrawIntTexAttribute) attri;
                drawTexture(texture2.mTexId, (float) texture2.mX, (float) texture2.mY, (float) texture2.mWidth, (float) texture2.mHeight, true);
                break;
        }
        return true;
    }

    protected void initShaderValue(boolean isSnapShot) {
        float f;
        GLES20.glVertexAttribPointer(this.mAttributePositionH, 2, 5126, false, 8, this.mVertexBuffer);
        GLES20.glVertexAttribPointer(this.mAttributeTexCoorH, 2, 5126, false, 8, this.mTexCoorBuffer);
        GLES20.glEnableVertexAttribArray(this.mAttributePositionH);
        GLES20.glEnableVertexAttribArray(this.mAttributeTexCoorH);
        GLES20.glUniformMatrix4fv(this.mUniformMVPMatrixH, 1, false, this.mGLCanvas.getState().getFinalMatrix(), 0);
        GLES20.glUniformMatrix4fv(this.mUniformSTMatrixH, 1, false, this.mGLCanvas.getState().getTexMaxtrix(), 0);
        GLES20.glUniform1i(this.mUniformTextureH, 0);
        int i = this.mUniformAlphaH;
        if (isSnapShot) {
            f = 1.0f;
        } else {
            f = this.mGLCanvas.getState().getAlpha();
        }
        GLES20.glUniform1f(i, f);
    }

    protected void drawTexture(BasicTexture texture, float x, float y, float w, float h, boolean isSnapShot) {
        GLES20.glUseProgram(this.mProgram);
        if (texture.onBind(this.mGLCanvas) && bindTexture(texture, 33984)) {
            bindExtraTexture();
            this.mGLCanvas.getState().pushState();
            updateViewport();
            setBlendEnabled(false);
            this.mGLCanvas.getState().translate(x, y, 0.0f);
            this.mGLCanvas.getState().scale(w, h, 1.0f);
            initShaderValue(isSnapShot);
            GLES20.glDrawArrays(5, 0, 4);
            this.mGLCanvas.getState().popState();
        }
    }

    private void drawTexture(int textureId, float x, float y, float w, float h, boolean isSnapShot) {
        GLES20.glUseProgram(this.mProgram);
        bindTexture(textureId, 33984);
        bindExtraTexture();
        updateViewport();
        setBlendEnabled(false);
        this.mGLCanvas.getState().pushState();
        this.mGLCanvas.getState().translate(x, y, 0.0f);
        this.mGLCanvas.getState().scale(w, h, 1.0f);
        initShaderValue(isSnapShot);
        GLES20.glDrawArrays(5, 0, 4);
        this.mGLCanvas.getState().popState();
    }

    protected void bindExtraTexture() {
    }
}
