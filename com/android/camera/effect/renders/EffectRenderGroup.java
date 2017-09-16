package com.android.camera.effect.renders;

import android.graphics.Color;
import android.opengl.Matrix;
import android.util.Log;
import com.android.camera.Device;
import com.android.camera.effect.EffectController;
import com.android.camera.effect.EffectController.SurfacePosition;
import com.android.camera.effect.draw_mode.DrawAttribute;
import com.android.camera.effect.draw_mode.DrawBasicTexAttribute;
import com.android.camera.effect.draw_mode.DrawExtTexAttribute;
import com.android.camera.effect.draw_mode.FillRectAttribute;
import com.android.gallery3d.ui.GLCanvas;

public class EffectRenderGroup extends RenderGroup {
    private DrawBasicTexAttribute mBasicTextureAttri = new DrawBasicTexAttribute();
    private int mEffectIndex = 0;
    private RenderGroup mEffectRenders;
    private Render mFocusPeakingRender;
    private int mIgnoreTimes;
    private Render mNoneEffectRender;
    private PipeRenderPair mPreviewPeakRender;
    private PipeRenderPair mPreviewPipeRender;
    private float[] mTexMatrix;

    public EffectRenderGroup(GLCanvas canvas) {
        super(canvas);
        this.mPreviewPipeRender = new PipeRenderPair(canvas);
        addRender(this.mPreviewPipeRender);
        this.mPreviewPipeRender.setFirstRender(new SurfaceTextureRender(canvas));
        this.mPreviewPeakRender = new PipeRenderPair(canvas);
        this.mEffectRenders = canvas.getEffectRenderGroup();
    }

    public boolean draw(DrawAttribute attri) {
        int oldIndex = this.mEffectIndex;
        this.mEffectIndex = EffectController.getInstance().getEffect(true);
        if (this.mEffectIndex != oldIndex && EffectController.getInstance().isBackGroundBlur()) {
            this.mPreviewPipeRender.prepareCopyBlurTexture();
        }
        switch (attri.getTarget()) {
            case 8:
                return drawPreview(attri);
            default:
                return false;
        }
    }

    private boolean drawPreview(DrawAttribute attri) {
        if ((EffectController.getInstance().hasEffect() || EffectController.getInstance().isDisplayShow() || EffectController.getInstance().isBackGroundBlur()) && this.mNoneEffectRender == null && Device.isSupportedShaderEffect()) {
            this.mGLCanvas.prepareEffectRenders(false, this.mEffectIndex);
            this.mNoneEffectRender = new NoneEffectRender(this.mGLCanvas);
            addRender(this.mNoneEffectRender);
            addRender(this.mEffectRenders);
            setViewportSize(this.mViewportWidth, this.mViewportHeight);
            setPreviewSize(this.mPreviewWidth, this.mPreviewHeight);
        }
        this.mPreviewPipeRender.setSecondRender(getPreviewSecondRender(((DrawExtTexAttribute) attri).mEffectPopup));
        this.mPreviewPipeRender.setUsedMiddleBuffer(EffectController.getInstance().isBackGroundBlur());
        this.mPreviewPipeRender.draw(attri);
        drawAnimationMask(attri);
        drawDisplay(attri);
        return true;
    }

    private void drawAnimationMask(DrawAttribute attri) {
        int alpha = EffectController.getInstance().getBlurAnimationValue();
        if (alpha > 0) {
            this.mGLCanvas.draw(new FillRectAttribute(0.0f, 0.0f, (float) ((DrawExtTexAttribute) attri).mWidth, (float) ((DrawExtTexAttribute) attri).mHeight, Color.argb(alpha, 0, 0, 0)));
        }
    }

    private void drawDisplay(DrawAttribute attri) {
        DrawExtTexAttribute ext = (DrawExtTexAttribute) attri;
        if (!Device.isSupportedShaderEffect() || !EffectController.getInstance().isDisplayShow()) {
            this.mIgnoreTimes = 0;
        } else if (this.mIgnoreTimes > 0) {
            this.mIgnoreTimes--;
        } else if (this.mPreviewPipeRender.getTexture() != null) {
            this.mGLCanvas.prepareEffectRenders(true, -1);
            if (this.mTexMatrix == null) {
                float scale;
                if (ext.mHeight * 9 == ext.mWidth * 16) {
                    scale = 0.5625f;
                } else {
                    scale = 0.75f;
                }
                this.mTexMatrix = new float[]{1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f};
                Matrix.translateM(this.mTexMatrix, 0, 0.5f, 0.5f, 0.0f);
                Matrix.scaleM(this.mTexMatrix, 0, 1.0f, scale, 1.0f);
                Matrix.translateM(this.mTexMatrix, 0, -0.5f, -0.5f, 0.0f);
            }
            if (EffectController.getInstance().isDisplayShow()) {
                long last = System.currentTimeMillis();
                drawEffectTexture();
                Log.d("EffectRenderGroup", "Camera preview drawEffectTexture time =" + (System.currentTimeMillis() - last));
            }
        }
    }

    private void drawEffectTexture() {
        long last = System.currentTimeMillis();
        int start = EffectController.getInstance().getDisplayStartIndex();
        int end = EffectController.getInstance().getDisplayEndIndex();
        SurfacePosition position = EffectController.getInstance().getSurfacePosition();
        int surfaceWidth = position.mWidth;
        int honSpace = position.mHonSpace;
        int verSpace = position.mVerSpace;
        int factor = position.mIsRtl ? -1 : 1;
        this.mGLCanvas.getState().setTexMatrix(this.mTexMatrix);
        for (int i = start; i < end; i++) {
            this.mBasicTextureAttri.init(this.mPreviewPipeRender.getTexture(), position.mStartX + ((((i - start) % EffectController.COLUMN_COUNT) * (surfaceWidth + honSpace)) * factor), position.mStartY + (((i - start) / EffectController.COLUMN_COUNT) * (surfaceWidth + verSpace)), surfaceWidth, surfaceWidth);
            if (i == 0) {
                this.mNoneEffectRender.draw(this.mBasicTextureAttri);
            } else {
                this.mEffectRenders.getRender(i).draw(this.mBasicTextureAttri);
            }
        }
        if (System.currentTimeMillis() - last > 100) {
            this.mIgnoreTimes = 1;
        }
    }

    private Render getPreviewSecondRender(boolean isEffectPopup) {
        Render second;
        if (!Device.isSupportedShaderEffect() || this.mRenders.size() == 1 || this.mEffectRenders == null) {
            second = null;
        } else if (this.mEffectIndex == 0) {
            second = EffectController.getInstance().isDisplayShow() ? this.mNoneEffectRender : null;
        } else {
            second = this.mEffectRenders.getRender(this.mEffectIndex);
            if (second == null) {
                this.mGLCanvas.prepareEffectRenders(false, this.mEffectIndex);
                second = this.mEffectRenders.getRender(this.mEffectIndex);
            }
        }
        if (!EffectController.getInstance().isNeedDrawPeaking() || isEffectPopup) {
            return second;
        }
        if (this.mFocusPeakingRender == null) {
            this.mGLCanvas.prepareEffectRenders(false, EffectController.sPeakingMFIndex);
            this.mFocusPeakingRender = this.mEffectRenders.getRender(EffectController.sPeakingMFIndex);
        }
        if (second == null) {
            return this.mFocusPeakingRender;
        }
        this.mPreviewPeakRender.setRenderPairs(second, this.mFocusPeakingRender);
        return this.mPreviewPeakRender;
    }

    public void setPreviewSize(int w, int h) {
        super.setPreviewSize(w, h);
        this.mTexMatrix = null;
    }
}
