package com.android.camera.effect.renders;

import android.graphics.Color;
import android.graphics.RectF;
import android.opengl.GLES20;
import com.android.camera.effect.ShaderUtil;
import com.android.camera.effect.draw_mode.DrawAttribute;
import com.android.camera.effect.draw_mode.DrawBasicTexAttribute;
import com.android.camera.effect.draw_mode.DrawLineAttribute;
import com.android.camera.effect.draw_mode.DrawMeshAttribute;
import com.android.camera.effect.draw_mode.DrawMixedAttribute;
import com.android.camera.effect.draw_mode.DrawRectAttribute;
import com.android.camera.effect.draw_mode.DrawRectFTexAttribute;
import com.android.camera.effect.draw_mode.FillRectAttribute;
import com.android.gallery3d.ui.BasicTexture;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.GLPaint;

public class BasicRender extends ShaderRender {
    private static final float[] TEXTURES = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f};
    private static final float[] VERTICES = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f};
    private int mUniformBlendFactorH;
    private int mUniformPaintColorH;

    public BasicRender(GLCanvas canvas) {
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
            this.mUniformBlendFactorH = GLES20.glGetUniformLocation(this.mProgram, "uBlendFactor");
            this.mUniformPaintColorH = GLES20.glGetUniformLocation(this.mProgram, "uPaintColor");
            this.mAttributePositionH = GLES20.glGetAttribLocation(this.mProgram, "aPosition");
            this.mAttributeTexCoorH = GLES20.glGetAttribLocation(this.mProgram, "aTexCoord");
            return;
        }
        throw new IllegalArgumentException(getClass() + ": mProgram = 0");
    }

    public String getFragShaderString() {
        return ShaderUtil.loadFromAssetsFile("frag_normal.txt");
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
        this.mAttriSupportedList.add(Integer.valueOf(0));
        this.mAttriSupportedList.add(Integer.valueOf(1));
        this.mAttriSupportedList.add(Integer.valueOf(2));
        this.mAttriSupportedList.add(Integer.valueOf(3));
        this.mAttriSupportedList.add(Integer.valueOf(4));
        this.mAttriSupportedList.add(Integer.valueOf(5));
        this.mAttriSupportedList.add(Integer.valueOf(7));
    }

    public boolean draw(DrawAttribute attri) {
        if (!isAttriSupported(attri.getTarget())) {
            return false;
        }
        switch (attri.getTarget()) {
            case 0:
                DrawLineAttribute line = (DrawLineAttribute) attri;
                drawLine(line.mX1, line.mY1, line.mX2, line.mY2, line.mGLPaint);
                break;
            case 1:
                DrawRectAttribute rect = (DrawRectAttribute) attri;
                drawRect(rect.mX, rect.mY, rect.mWidth, rect.mHeight, rect.mGLPaint);
                break;
            case 2:
                DrawMeshAttribute mesh = (DrawMeshAttribute) attri;
                drawMesh(mesh.mBasicTexture, mesh.mX, mesh.mY, mesh.mXYBuffer, mesh.mUVBuffer, mesh.mIndexBuffer, mesh.mIndexCount);
                break;
            case 3:
                DrawMixedAttribute mix = (DrawMixedAttribute) attri;
                drawMixed(mix.mBasicTexture, mix.mToColor, mix.mRatio, mix.mX, mix.mY, mix.mWidth, mix.mHeight);
                break;
            case 4:
                FillRectAttribute rect2 = (FillRectAttribute) attri;
                fillRect(rect2.mX, rect2.mY, rect2.mWidth, rect2.mHeight, rect2.mColor);
                break;
            case 5:
                DrawBasicTexAttribute texture = (DrawBasicTexAttribute) attri;
                drawTexture(texture.mBasicTexture, (float) texture.mX, (float) texture.mY, (float) texture.mWidth, (float) texture.mHeight);
                break;
            case 7:
                DrawRectFTexAttribute texture2 = (DrawRectFTexAttribute) attri;
                drawTexture(texture2.mBasicTexture, texture2.mSourceRectF, texture2.mTargetRectF);
                break;
        }
        return true;
    }

    private void drawRect(float x, float y, float w, float h, GLPaint paint) {
        GLES20.glUseProgram(this.mProgram);
        initAttribPointer();
        updateViewport();
        initGLPaint(paint);
        this.mGLCanvas.getState().pushState();
        this.mGLCanvas.getState().translate(x, y, 0.0f);
        this.mGLCanvas.getState().scale(w, h, 1.0f);
        GLES20.glUniformMatrix4fv(this.mUniformMVPMatrixH, 1, false, this.mGLCanvas.getState().getFinalMatrix(), 0);
        GLES20.glUniformMatrix4fv(this.mUniformSTMatrixH, 1, false, this.mGLCanvas.getState().getTexMaxtrix(), 0);
        GLES20.glUniform1f(this.mUniformAlphaH, this.mGLCanvas.getState().getAlpha());
        GLES20.glUniform1f(this.mUniformBlendAlphaH, this.mGLCanvas.getState().getBlendAlpha());
        GLES20.glUniform4f(this.mUniformBlendFactorH, 0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glDrawArrays(2, 6, 4);
        this.mGLCanvas.getState().popState();
    }

    private void fillRect(float x, float y, float w, float h, int color) {
        GLES20.glUseProgram(this.mProgram);
        initAttribPointer();
        updateViewport();
        initGLPaint(color);
        this.mGLCanvas.getState().pushState();
        this.mGLCanvas.getState().translate(x, y, 0.0f);
        this.mGLCanvas.getState().scale(w, h, 1.0f);
        GLES20.glUniformMatrix4fv(this.mUniformMVPMatrixH, 1, false, this.mGLCanvas.getState().getFinalMatrix(), 0);
        GLES20.glUniformMatrix4fv(this.mUniformSTMatrixH, 1, false, this.mGLCanvas.getState().getTexMaxtrix(), 0);
        GLES20.glUniform1f(this.mUniformAlphaH, this.mGLCanvas.getState().getAlpha());
        GLES20.glUniform1f(this.mUniformBlendAlphaH, this.mGLCanvas.getState().getBlendAlpha());
        GLES20.glUniform4f(this.mUniformBlendFactorH, 0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glDrawArrays(5, 0, 4);
        this.mGLCanvas.getState().popState();
    }

    private void drawLine(float x1, float y1, float x2, float y2, GLPaint paint) {
        GLES20.glUseProgram(this.mProgram);
        initAttribPointer();
        updateViewport();
        initGLPaint(paint);
        this.mGLCanvas.getState().pushState();
        this.mGLCanvas.getState().translate(x1, y1, 0.0f);
        this.mGLCanvas.getState().scale(x2 - x1, y2 - y1, 1.0f);
        GLES20.glUniformMatrix4fv(this.mUniformMVPMatrixH, 1, false, this.mGLCanvas.getState().getFinalMatrix(), 0);
        GLES20.glUniformMatrix4fv(this.mUniformSTMatrixH, 1, false, this.mGLCanvas.getState().getTexMaxtrix(), 0);
        GLES20.glUniform1f(this.mUniformAlphaH, this.mGLCanvas.getState().getAlpha());
        GLES20.glUniform1f(this.mUniformBlendAlphaH, this.mGLCanvas.getState().getBlendAlpha());
        GLES20.glUniform4f(this.mUniformBlendFactorH, 0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glDrawArrays(3, 4, 2);
        this.mGLCanvas.getState().popState();
    }

    private void drawTexture(BasicTexture texture, float x, float y, float w, float h) {
        this.mGLCanvas.getState().pushState();
        this.mGLCanvas.getState().indentityTexM();
        drawTextureInternal(texture, x, y, w, h);
        this.mGLCanvas.getState().popState();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void drawTexture(com.android.gallery3d.ui.BasicTexture r7, android.graphics.RectF r8, android.graphics.RectF r9) {
        /*
        r6 = this;
        r1 = 0;
        r0 = r9.width();
        r0 = (r0 > r1 ? 1 : (r0 == r1 ? 0 : -1));
        if (r0 <= 0) goto L_0x0011;
    L_0x0009:
        r0 = r9.height();
        r0 = (r0 > r1 ? 1 : (r0 == r1 ? 0 : -1));
        if (r0 > 0) goto L_0x0012;
    L_0x0011:
        return;
    L_0x0012:
        r0 = r6.mGLCanvas;
        r0 = r7.onBind(r0);
        if (r0 != 0) goto L_0x001b;
    L_0x001a:
        return;
    L_0x001b:
        r6.convertCoordinate(r8, r9, r7);
        r0 = r6.mGLCanvas;
        r0 = r0.getState();
        r0.pushState();
        r0 = r6.mGLCanvas;
        r0 = r0.getState();
        r1 = r8.left;
        r2 = r8.top;
        r3 = r8.right;
        r4 = r8.bottom;
        r0.setTexMatrix(r1, r2, r3, r4);
        r2 = r9.left;
        r3 = r9.top;
        r4 = r9.width();
        r5 = r9.height();
        r0 = r6;
        r1 = r7;
        r0.drawTextureInternal(r1, r2, r3, r4, r5);
        r0 = r6.mGLCanvas;
        r0 = r0.getState();
        r0.popState();
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.effect.renders.BasicRender.drawTexture(com.android.gallery3d.ui.BasicTexture, android.graphics.RectF, android.graphics.RectF):void");
    }

    private void drawTextureInternal(BasicTexture texture, float x, float y, float width, float height) {
        if (width > 0.0f && height > 0.0f) {
            GLES20.glUseProgram(this.mProgram);
            if (bindTexture(texture, 33984)) {
                GLES20.glUniform4f(this.mUniformBlendFactorH, 1.0f, 0.0f, 0.0f, 0.0f);
                GLES20.glUniform1i(this.mUniformTextureH, 0);
                initAttribPointer();
                updateViewport();
                float alpha = this.mGLCanvas.getState().getAlpha();
                float blendalpha = this.mGLCanvas.getState().getBlendAlpha();
                boolean z = this.mBlendEnabled && (!texture.isOpaque() || alpha < 0.95f || blendalpha >= 0.0f);
                setBlendEnabled(z);
                this.mGLCanvas.getState().translate(x, y, 0.0f);
                this.mGLCanvas.getState().scale(width, height, 1.0f);
                GLES20.glUniformMatrix4fv(this.mUniformMVPMatrixH, 1, false, this.mGLCanvas.getState().getFinalMatrix(), 0);
                GLES20.glUniformMatrix4fv(this.mUniformSTMatrixH, 1, false, this.mGLCanvas.getState().getTexMaxtrix(), 0);
                GLES20.glUniform1f(this.mUniformAlphaH, this.mGLCanvas.getState().getAlpha());
                GLES20.glUniform1f(this.mUniformBlendAlphaH, blendalpha);
                GLES20.glDrawArrays(5, 0, 4);
            }
        }
    }

    private void drawMixed(BasicTexture from, int toColor, float ratio, float x, float y, float w, float h) {
        GLES20.glUseProgram(this.mProgram);
        if (bindTexture(from, 33984)) {
            boolean z;
            initAttribPointer();
            initGLPaint(toColor);
            updateViewport();
            if (!this.mBlendEnabled || (from.isOpaque() && this.mGLCanvas.getState().getAlpha() >= 0.95f)) {
                z = false;
            } else {
                z = true;
            }
            setBlendEnabled(z);
            this.mGLCanvas.getState().pushState();
            this.mGLCanvas.getState().translate(x, y, 0.0f);
            this.mGLCanvas.getState().scale(w, h, 1.0f);
            GLES20.glUniformMatrix4fv(this.mUniformMVPMatrixH, 1, false, this.mGLCanvas.getState().getFinalMatrix(), 0);
            GLES20.glUniformMatrix4fv(this.mUniformSTMatrixH, 1, false, this.mGLCanvas.getState().getTexMaxtrix(), 0);
            GLES20.glUniform1f(this.mUniformAlphaH, this.mGLCanvas.getState().getAlpha());
            GLES20.glUniform4f(this.mUniformBlendFactorH, 1.0f - ratio, 0.0f, 0.0f, ratio);
            GLES20.glUniform1i(this.mUniformTextureH, 0);
            GLES20.glUniform1f(this.mUniformBlendAlphaH, this.mGLCanvas.getState().getBlendAlpha());
            GLES20.glDrawArrays(5, 0, 4);
            this.mGLCanvas.getState().popState();
        }
    }

    private void drawMesh(BasicTexture tex, float x, float y, int xyBuffer, int uvBuffer, int indexBuffer, int indexCount) {
        GLES20.glUseProgram(this.mProgram);
        if (bindTexture(tex, 33984)) {
            boolean z = this.mBlendEnabled && this.mGLCanvas.getState().getAlpha() < 0.95f;
            setBlendEnabled(z);
            GLES20.glBindBuffer(34962, xyBuffer);
            GLES20.glVertexAttribPointer(this.mAttributePositionH, 2, 5126, false, 0, 0);
            GLES20.glEnableVertexAttribArray(this.mAttributePositionH);
            GLES20.glBindBuffer(34962, uvBuffer);
            GLES20.glVertexAttribPointer(this.mAttributeTexCoorH, 2, 5126, false, 0, 0);
            GLES20.glEnableVertexAttribArray(this.mAttributeTexCoorH);
            this.mGLCanvas.getState().pushState();
            this.mGLCanvas.getState().translate(x, y, 0.0f);
            GLES20.glUniformMatrix4fv(this.mUniformMVPMatrixH, 1, false, this.mGLCanvas.getState().getFinalMatrix(), 0);
            GLES20.glUniformMatrix4fv(this.mUniformSTMatrixH, 1, false, this.mGLCanvas.getState().getTexMaxtrix(), 0);
            GLES20.glUniform1f(this.mUniformAlphaH, this.mGLCanvas.getState().getAlpha());
            GLES20.glUniform1f(this.mUniformBlendAlphaH, this.mGLCanvas.getState().getBlendAlpha());
            GLES20.glUniform4f(this.mUniformBlendFactorH, 1.0f, 0.0f, 0.0f, 0.0f);
            GLES20.glUniform1i(this.mUniformTextureH, 0);
            GLES20.glBindBuffer(34963, indexBuffer);
            GLES20.glDrawElements(5, indexCount, 5121, 0);
            GLES20.glBindBuffer(34963, 0);
            GLES20.glBindBuffer(34962, 0);
            this.mGLCanvas.getState().popState();
        }
    }

    private void initGLPaint(GLPaint paint) {
        initGLPaint(paint.getColor());
        GLES20.glLineWidth(paint.getLineWidth());
    }

    private void initGLPaint(int color) {
        boolean z = true;
        float colorAlpha = ((float) Color.alpha(color)) * 0.003921569f;
        if (!this.mBlendEnabled) {
            z = false;
        } else if (colorAlpha >= 0.95f && this.mGLCanvas.getState().getAlpha() >= 0.95f) {
            z = false;
        }
        setBlendEnabled(z);
        GLES20.glUniform4f(this.mUniformPaintColorH, ((float) Color.red(color)) * 0.003921569f, ((float) Color.green(color)) * 0.003921569f, ((float) Color.blue(color)) * 0.003921569f, colorAlpha);
    }

    private void initAttribPointer() {
        GLES20.glVertexAttribPointer(this.mAttributePositionH, 2, 5126, false, 8, this.mVertexBuffer);
        GLES20.glVertexAttribPointer(this.mAttributeTexCoorH, 2, 5126, false, 8, this.mTexCoorBuffer);
        GLES20.glEnableVertexAttribArray(this.mAttributePositionH);
        GLES20.glEnableVertexAttribArray(this.mAttributeTexCoorH);
    }

    private void convertCoordinate(RectF source, RectF target, BasicTexture texture) {
        int width = texture.getWidth();
        int height = texture.getHeight();
        int texWidth = texture.getTextureWidth();
        int texHeight = texture.getTextureHeight();
        source.left /= (float) texWidth;
        source.right /= (float) texWidth;
        source.top /= (float) texHeight;
        source.bottom /= (float) texHeight;
        float xBound = ((float) width) / ((float) texWidth);
        if (source.right > xBound) {
            target.right = target.left + ((target.width() * (xBound - source.left)) / source.width());
            source.right = xBound;
        }
        float yBound = ((float) height) / ((float) texHeight);
        if (source.bottom > yBound) {
            target.bottom = target.top + ((target.height() * (yBound - source.top)) / source.height());
            source.bottom = yBound;
        }
    }
}
