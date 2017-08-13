package com.android.camera.effect.renders;

import android.util.Log;
import com.android.camera.Device;
import com.android.camera.effect.EffectController;
import com.android.camera.effect.FrameBuffer;
import com.android.camera.effect.draw_mode.DrawAttribute;
import com.android.camera.effect.draw_mode.DrawBasicTexAttribute;
import com.android.camera.effect.draw_mode.DrawExtTexAttribute;
import com.android.camera.effect.draw_mode.DrawIntTexAttribute;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.RawTexture;
import com.android.gallery3d.ui.Utils;
import java.util.ArrayList;
import java.util.Locale;

public final class PipeRenderPair extends RenderGroup {
    private DrawBasicTexAttribute mBasicTexureAttri = new DrawBasicTexAttribute();
    private FrameBuffer mBlurFrameBuffer;
    private int mBufferHeight = -1;
    private int mBufferWidth = -1;
    private DrawExtTexAttribute mExtTexture = new DrawExtTexAttribute();
    private Render mFirstRender;
    private FrameBuffer mFrameBuffer;
    private ArrayList<FrameBuffer> mFrameBuffers = new ArrayList();
    private FrameBuffer mMiddleFrameBuffer;
    private Render mSecondRender;
    private boolean mTextureFilled = false;
    private boolean mUseMiddleBuffer = false;

    public PipeRenderPair(GLCanvas canvas) {
        super(canvas);
    }

    public PipeRenderPair(GLCanvas canvas, Render first, Render second, boolean useMiddleBuffer) {
        super(canvas);
        setRenderPairs(first, second);
        this.mUseMiddleBuffer = useMiddleBuffer;
    }

    public void setRenderPairs(Render first, Render second) {
        if (first != this.mFirstRender || second != this.mSecondRender) {
            this.mRenders.clear();
            if (first != null) {
                this.mRenders.add(first);
            }
            if (second != null) {
                this.mRenders.add(second);
            }
            this.mFirstRender = first;
            this.mSecondRender = second;
        }
    }

    public void setPreviewSize(int w, int h) {
        super.setPreviewSize(w, h);
        this.mBufferWidth = this.mUseMiddleBuffer ? this.mPreviewWidth / 12 : this.mPreviewWidth;
        this.mBufferHeight = this.mUseMiddleBuffer ? this.mPreviewHeight / 12 : this.mPreviewHeight;
    }

    public void setFirstRender(Render first) {
        this.mRenders.clear();
        if (first != null) {
            this.mRenders.add(first);
        }
        this.mFirstRender = first;
        if (this.mSecondRender != null) {
            this.mRenders.add(this.mSecondRender);
        }
    }

    public void setSecondRender(Render second) {
        this.mRenders.clear();
        if (this.mFirstRender != null) {
            this.mRenders.add(this.mFirstRender);
        }
        if (second != null) {
            this.mRenders.add(second);
        }
        this.mSecondRender = second;
    }

    public void addRender(Render render) {
        throw new RuntimeException("Not supportted addRender in PipeRenderPair !");
    }

    private int getEffectBufferRatio() {
        return EffectController.getInstance().isDisplayShow() ? 2 : 1;
    }

    public void copyBlurTexture(DrawExtTexAttribute ext) {
        if (EffectController.getInstance().isBackGroundBlur() && !this.mTextureFilled) {
            if (this.mBlurFrameBuffer != null && this.mBlurFrameBuffer.getWidth() == ext.mWidth) {
                if (this.mBlurFrameBuffer.getHeight() != ext.mHeight) {
                }
                beginBindFrameBuffer(this.mBlurFrameBuffer);
                this.mSecondRender.draw(this.mBasicTexureAttri.init(this.mUseMiddleBuffer ? this.mMiddleFrameBuffer.getTexture() : this.mFrameBuffer.getTexture(), ext.mX, ext.mY, ext.mWidth, ext.mHeight));
                endBindFrameBuffer();
                this.mTextureFilled = true;
            }
            this.mBlurFrameBuffer = new FrameBuffer(this.mGLCanvas, ext.mWidth, ext.mHeight, this.mParentFrameBufferId);
            beginBindFrameBuffer(this.mBlurFrameBuffer);
            if (this.mUseMiddleBuffer) {
            }
            this.mSecondRender.draw(this.mBasicTexureAttri.init(this.mUseMiddleBuffer ? this.mMiddleFrameBuffer.getTexture() : this.mFrameBuffer.getTexture(), ext.mX, ext.mY, ext.mWidth, ext.mHeight));
            endBindFrameBuffer();
            this.mTextureFilled = true;
        }
    }

    public void drawBlurTexture(DrawExtTexAttribute ext) {
        if (EffectController.getInstance().isBackGroundBlur() && this.mTextureFilled) {
            this.mGLCanvas.draw(new DrawBasicTexAttribute(this.mBlurFrameBuffer.getTexture(), ext.mX, ext.mY, ext.mWidth, ext.mHeight));
        }
    }

    public boolean draw(DrawAttribute attri) {
        if (this.mRenders.size() == 0) {
            return false;
        }
        if (this.mRenders.size() == 1 || this.mFirstRender == this.mSecondRender) {
            return ((Render) this.mRenders.get(0)).draw(attri);
        }
        if (attri.getTarget() == 8) {
            DrawExtTexAttribute ext = (DrawExtTexAttribute) attri;
            this.mFrameBuffer = getFrameBuffer(this.mPreviewWidth / getEffectBufferRatio(), this.mPreviewHeight / getEffectBufferRatio());
            beginBindFrameBuffer(this.mFrameBuffer);
            this.mFirstRender.draw(this.mExtTexture.init(ext.mExtTexture, ext.mTextureTransform, 0, 0, this.mFrameBuffer.getTexture().getTextureWidth(), this.mFrameBuffer.getTexture().getTextureHeight()));
            endBindFrameBuffer();
            if (this.mUseMiddleBuffer) {
                updateMiddleBuffer(this.mPreviewWidth, this.mPreviewHeight);
                this.mMiddleFrameBuffer = getFrameBuffer(this.mBufferWidth, this.mBufferHeight);
                beginBindFrameBuffer(this.mMiddleFrameBuffer);
                this.mFirstRender.draw(this.mExtTexture.init(ext.mExtTexture, ext.mTextureTransform, 0, 0, this.mBufferWidth, this.mBufferHeight));
                endBindFrameBuffer();
            }
            if (EffectController.getInstance().isMainFrameDisplay()) {
                if (Device.isHoldBlurBackground() && EffectController.getInstance().isBackGroundBlur()) {
                    copyBlurTexture(ext);
                    drawBlurTexture(ext);
                } else {
                    this.mSecondRender.draw(this.mBasicTexureAttri.init(this.mUseMiddleBuffer ? this.mMiddleFrameBuffer.getTexture() : this.mFrameBuffer.getTexture(), ext.mX, ext.mY, ext.mWidth, ext.mHeight));
                }
            }
            return true;
        } else if (attri.getTarget() == 5 || attri.getTarget() == 10) {
            DrawBasicTexAttribute basic = (DrawBasicTexAttribute) attri;
            updateMiddleBuffer(basic.mWidth, basic.mHeight);
            this.mFrameBuffer = getFrameBuffer(this.mBufferWidth, this.mBufferHeight);
            beginBindFrameBuffer(this.mFrameBuffer);
            this.mFirstRender.draw(this.mBasicTexureAttri.init(basic.mBasicTexture, 0, 0, this.mFrameBuffer.getTexture().getTextureWidth(), this.mFrameBuffer.getTexture().getTextureHeight()));
            endBindFrameBuffer();
            this.mSecondRender.draw(this.mBasicTexureAttri.init(this.mFrameBuffer.getTexture(), basic.mX, basic.mY, basic.mWidth, basic.mHeight));
            return true;
        } else if (attri.getTarget() != 6) {
            return false;
        } else {
            DrawIntTexAttribute intTex = (DrawIntTexAttribute) attri;
            this.mFrameBuffer = getFrameBuffer(intTex.mWidth, intTex.mHeight);
            beginBindFrameBuffer(this.mFrameBuffer);
            this.mFirstRender.draw(new DrawIntTexAttribute(intTex.mTexId, 0, 0, intTex.mWidth, intTex.mHeight));
            endBindFrameBuffer();
            this.mSecondRender.draw(this.mBasicTexureAttri.init(this.mFrameBuffer.getTexture(), intTex.mX, intTex.mY, intTex.mWidth, intTex.mHeight, true));
            return true;
        }
    }

    public RawTexture getTexture() {
        if (this.mFrameBuffer == null) {
            return null;
        }
        return this.mFrameBuffer.getTexture();
    }

    public FrameBuffer getFrameBuffer(int w, int h) {
        FrameBuffer frameBuffer = null;
        if (!this.mFrameBuffers.isEmpty()) {
            for (int i = this.mFrameBuffers.size() - 1; i >= 0; i--) {
                double ratio;
                int bufferW = ((FrameBuffer) this.mFrameBuffers.get(i)).getWidth();
                int bufferH = ((FrameBuffer) this.mFrameBuffers.get(i)).getHeight();
                if (w < h) {
                    ratio = Math.abs((((double) bufferH) / ((double) bufferW)) - (((double) h) / ((double) w)));
                } else {
                    ratio = Math.abs((((double) bufferW) / ((double) bufferH)) - (((double) w) / ((double) h)));
                }
                if (ratio <= 0.1d && Utils.nextPowerOf2(bufferW) == Utils.nextPowerOf2(w) && Utils.nextPowerOf2(bufferH) == Utils.nextPowerOf2(h)) {
                    frameBuffer = (FrameBuffer) this.mFrameBuffers.get(i);
                    break;
                }
            }
        }
        if (frameBuffer == null) {
            frameBuffer = new FrameBuffer(this.mGLCanvas, w, h, this.mParentFrameBufferId);
            Log.d("PipeRenderPair", String.format(Locale.ENGLISH, "Camera new framebuffer thread = %d  w = %d, h= %d", new Object[]{Long.valueOf(Thread.currentThread().getId()), Integer.valueOf(w), Integer.valueOf(h)}));
            if (this.mFrameBuffers.size() > 5) {
                this.mFrameBuffers.remove(this.mFrameBuffers.size() - 1);
            }
            this.mFrameBuffers.add(frameBuffer);
        }
        return frameBuffer;
    }

    public void setUsedMiddleBuffer(boolean useMiddleBuffer) {
        this.mUseMiddleBuffer = useMiddleBuffer;
    }

    public void updateMiddleBuffer(int width, int height) {
        if (this.mUseMiddleBuffer) {
            this.mBufferWidth = width / 12;
            this.mBufferHeight = height / 12;
            return;
        }
        this.mBufferWidth = width;
        this.mBufferHeight = height;
    }

    public void prepareCopyBlurTexture() {
        this.mTextureFilled = false;
    }
}
