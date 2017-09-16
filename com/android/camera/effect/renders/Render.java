package com.android.camera.effect.renders;

import android.opengl.GLES20;
import com.android.camera.effect.EffectController.EffectRectAttribute;
import com.android.camera.effect.draw_mode.DrawAttribute;
import com.android.gallery3d.ui.GLCanvas;

public abstract class Render {
    protected GLCanvas mGLCanvas;
    protected int mId = -1;
    protected int mJpegOrientation;
    protected boolean mMirror;
    protected int mOldViewportHeight;
    protected int mOldViewportWidth;
    protected int mOrientation;
    protected int mParentFrameBufferId;
    protected int mPreviewHeight;
    protected int mPreviewWidth;
    protected float mShootRotation;
    protected int mSnapshotOriginHeight;
    protected int mSnapshotOriginWidth;
    protected int mViewportHeight;
    protected int mViewportWidth;

    public abstract boolean draw(DrawAttribute drawAttribute);

    public Render(GLCanvas canvas) {
        this.mGLCanvas = canvas;
        this.mParentFrameBufferId = 0;
    }

    public Render(GLCanvas canvas, int id) {
        this.mGLCanvas = canvas;
        this.mParentFrameBufferId = 0;
        this.mId = id;
    }

    public void setViewportSize(int w, int h) {
        this.mViewportWidth = w;
        this.mViewportHeight = h;
        if (this.mOldViewportWidth == 0) {
            this.mOldViewportWidth = w;
            this.mOldViewportHeight = h;
        }
    }

    public void setPreviewSize(int w, int h) {
        this.mPreviewWidth = w;
        this.mPreviewHeight = h;
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
    }

    public void setJpegOrientation(int orientation) {
        this.mJpegOrientation = orientation;
    }

    public void setShootRotation(float shootRotation) {
        this.mShootRotation = shootRotation;
    }

    public void setMirror(boolean mirror) {
        this.mMirror = mirror;
    }

    public void setSnapshotSize(int snapshotWidth, int snapshotHeight) {
        this.mSnapshotOriginWidth = snapshotWidth;
        this.mSnapshotOriginHeight = snapshotHeight;
    }

    protected void updateViewport() {
        GLES20.glViewport(0, 0, this.mViewportWidth, this.mViewportHeight);
        this.mGLCanvas.getState().ortho(0.0f, (float) this.mViewportWidth, 0.0f, (float) this.mViewportHeight);
    }

    protected void setParentFrameBufferId(int id) {
        this.mParentFrameBufferId = id;
    }

    public int getId() {
        return this.mId;
    }

    public void setEffectRangeAttribute(EffectRectAttribute attribute) {
    }
}
