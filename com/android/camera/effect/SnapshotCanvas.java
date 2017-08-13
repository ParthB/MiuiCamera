package com.android.camera.effect;

import android.opengl.GLES20;
import com.android.camera.Device;
import com.android.camera.effect.draw_mode.DrawAttribute;
import com.android.camera.effect.renders.BasicRender;
import com.android.camera.effect.renders.RenderGroup;
import com.android.gallery3d.ui.BasicTexture;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.GLId;
import com.android.gallery3d.ui.IntArray;
import java.util.ArrayList;

public class SnapshotCanvas implements GLCanvas {
    private final int BASIC_RENDER_INDEX = 1;
    private final int EFFECT_GROUP_INDEX = 0;
    private final IntArray mDeleteBuffers = new IntArray();
    private final IntArray mDeleteFrameBuffers = new IntArray();
    private final ArrayList<Integer> mDeletePrograms = new ArrayList();
    private final IntArray mDeleteTextures = new IntArray();
    private RenderGroup mEffectRenders = EffectController.getInstance().getEffectGroup(this, null, false, true, -1);
    private int mHeight;
    private RenderGroup mRenderGroup = new RenderGroup(this);
    private GLCanvasState mState = new GLCanvasState();
    private int mWidth;

    public SnapshotCanvas() {
        this.mRenderGroup.addRender(this.mEffectRenders);
        this.mRenderGroup.addRender(new BasicRender(this));
        initialize();
    }

    private void initialize() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClearStencil(0);
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
        return (RenderGroup) this.mRenderGroup.getRender(0);
    }

    public BasicRender getBasicRender() {
        return (BasicRender) this.mRenderGroup.getRender(1);
    }

    public void draw(DrawAttribute attri) {
        this.mRenderGroup.draw(attri);
    }

    public void clearBuffer() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(16384);
    }

    public boolean deleteTexture(BasicTexture t) {
        synchronized (this.mDeleteTextures) {
            if (t.isLoaded()) {
                this.mDeleteTextures.add(t.getId());
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
            EffectController.getInstance().getEffectGroup(this, this.mEffectRenders, whole, true, index);
        }
    }

    public void prepareBlurRenders() {
    }
}
