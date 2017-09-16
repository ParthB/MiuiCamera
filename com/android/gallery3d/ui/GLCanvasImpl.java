package com.android.gallery3d.ui;

import android.opengl.GLES20;
import com.android.camera.Device;
import com.android.camera.effect.EffectController;
import com.android.camera.effect.FrameBuffer;
import com.android.camera.effect.GLCanvasState;
import com.android.camera.effect.draw_mode.DrawAttribute;
import com.android.camera.effect.renders.BasicRender;
import com.android.camera.effect.renders.EffectRenderGroup;
import com.android.camera.effect.renders.RenderGroup;
import java.util.ArrayList;

public class GLCanvasImpl implements GLCanvas {
    private static final int PRELOAD_UPPER_BOUND;
    public static int sMaxTextureSize = 4096;
    private final IntArray mDeleteBuffers = new IntArray();
    private final IntArray mDeleteFrameBuffers = new IntArray();
    private final ArrayList<Integer> mDeletePrograms = new ArrayList();
    private final IntArray mDeleteTextures = new IntArray();
    private RenderGroup mEffectRenders;
    private int mHeight;
    private int mPreloadedRenders = 0;
    private RenderGroup mRenderGroup;
    private GLCanvasState mState = new GLCanvasState();
    private int mWidth;

    static {
        int i;
        int i2 = 0;
        if (Device.isSupportedTiltShift()) {
            i = 4;
        } else {
            i = 0;
        }
        i += 26;
        if (Device.isSupportedPeakingMF()) {
            i2 = 1;
        }
        PRELOAD_UPPER_BOUND = i + i2;
    }

    public GLCanvasImpl() {
        int[] size = new int[1];
        GLES20.glGetIntegerv(3379, size, 0);
        sMaxTextureSize = size[0];
        this.mRenderGroup = new RenderGroup(this);
        this.mEffectRenders = EffectController.getInstance().getEffectGroup(this, null, false, false, -1);
        this.mRenderGroup.addRender(new EffectRenderGroup(this));
        this.mRenderGroup.addRender(new BasicRender(this));
        initialize();
    }

    private void initialize() {
        GLES20.glEnable(3024);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClearStencil(0);
        GLES20.glLineWidth(1.0f);
        GLES20.glEnable(3042);
        GLES20.glBlendFunc(770, 771);
        GLES20.glPixelStorei(3317, 1);
        GLES20.glPixelStorei(3333, 1);
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public void setSize(int width, int height) {
        boolean z = false;
        if (width >= 0 && height >= 0) {
            z = true;
        }
        Utils.assertTrue(z);
        this.mWidth = width;
        this.mHeight = height;
        this.mRenderGroup.setViewportSize(width, height);
        this.mRenderGroup.setPreviewSize(width, height);
        this.mState.indentityAllM();
        this.mState.setAlpha(1.0f);
        this.mState.translate(0.0f, (float) height, 0.0f);
        this.mState.scale(1.0f, -1.0f, 1.0f);
    }

    public void setPreviewSize(int width, int height) {
        this.mRenderGroup.setPreviewSize(width, height);
    }

    public GLCanvasState getState() {
        return this.mState;
    }

    public void beginBindFrameBuffer(FrameBuffer frameBuffer) {
        this.mRenderGroup.beginBindFrameBuffer(frameBuffer);
    }

    public void endBindFrameBuffer() {
        this.mRenderGroup.endBindFrameBuffer();
    }

    public RenderGroup getEffectRenderGroup() {
        return this.mEffectRenders;
    }

    public void draw(DrawAttribute attri) {
        if (this.mPreloadedRenders < PRELOAD_UPPER_BOUND) {
            prepareEffectRenders(false, -1);
            this.mPreloadedRenders++;
        }
        this.mRenderGroup.draw(attri);
    }

    public void clearBuffer() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(16384);
    }

    public boolean deleteTexture(BasicTexture t) {
        synchronized (this.mDeleteTextures) {
            if (t.isLoaded()) {
                this.mDeleteTextures.add(t.mId);
                return true;
            }
            return false;
        }
    }

    public boolean deleteTexture(int textureId) {
        synchronized (this.mDeleteTextures) {
            if (textureId == 0) {
                return false;
            }
            this.mDeleteTextures.add(textureId);
            return true;
        }
    }

    public void deleteFrameBuffer(int frameBufferId) {
        synchronized (this.mDeleteFrameBuffers) {
            this.mDeleteFrameBuffers.add(frameBufferId);
        }
    }

    public void deleteProgram(int programId) {
        synchronized (this.mDeletePrograms) {
            this.mDeletePrograms.add(Integer.valueOf(programId));
        }
    }

    public void recycledResources() {
        synchronized (this.mDeleteTextures) {
            IntArray ids = this.mDeleteTextures;
            if (ids.size() > 0) {
                GLId.glDeleteTextures(ids.size(), ids.getInternalArray(), 0);
                ids.clear();
            }
            ids = this.mDeleteBuffers;
            if (ids.size() > 0) {
                GLId.glDeleteBuffers(ids.size(), ids.getInternalArray(), 0);
                ids.clear();
            }
            ids = this.mDeleteFrameBuffers;
            if (ids.size() > 0) {
                GLId.glDeleteFrameBuffers(ids.size(), ids.getInternalArray(), 0);
                ids.clear();
            }
            while (this.mDeletePrograms.size() > 0) {
                GLES20.glDeleteProgram(((Integer) this.mDeletePrograms.remove(0)).intValue());
            }
        }
    }

    public void prepareEffectRenders(boolean whole, int index) {
        if (Device.isSupportedShaderEffect() && this.mEffectRenders.isNeedInit(index)) {
            EffectController.getInstance().getEffectGroup(this, this.mEffectRenders, whole, false, index);
        }
    }

    public void prepareBlurRenders() {
        if (Device.isSupportedShaderEffect() && this.mRenderGroup.getRender(2) == null) {
            if (this.mEffectRenders.getRender(EffectController.sBackgroundBlurIndex) == null) {
                prepareEffectRenders(false, EffectController.sBackgroundBlurIndex);
            }
            this.mRenderGroup.addRender(this.mEffectRenders.getRender(EffectController.sBackgroundBlurIndex));
        }
    }
}
