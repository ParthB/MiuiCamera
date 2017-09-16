package com.android.camera.effect.renders;

import android.opengl.GLES20;
import com.android.camera.effect.EffectController;
import com.android.camera.effect.EffectController.EffectRectAttribute;
import com.android.camera.effect.FrameBuffer;
import com.android.camera.effect.draw_mode.DrawAttribute;
import com.android.camera.ui.V6ModulePicker;
import com.android.gallery3d.ui.GLCanvas;
import java.util.ArrayList;

public class RenderGroup extends Render {
    protected int mParentFrameBufferIdOld;
    private ArrayList<Render> mPartRenders = new ArrayList();
    protected ArrayList<Render> mRenders = new ArrayList();

    public RenderGroup(GLCanvas canvas) {
        super(canvas);
    }

    public RenderGroup(GLCanvas canvas, int size) {
        super(canvas);
        for (int i = 0; i < size; i++) {
            addRender(null);
        }
    }

    public void addRender(Render render) {
        this.mRenders.add(render);
        setSize(render);
    }

    public void setRender(Render render, int index) {
        this.mRenders.set(index, render);
        setSize(render);
    }

    private void setSize(Render render) {
        if (render != null) {
            if (!(this.mPreviewWidth == 0 && this.mPreviewHeight == 0)) {
                render.setPreviewSize(this.mPreviewWidth, this.mPreviewHeight);
            }
            if (this.mViewportWidth != 0 || this.mViewportHeight != 0) {
                render.setViewportSize(this.mViewportWidth, this.mViewportHeight);
            }
        }
    }

    public Render getRender(int index) {
        if (index < 0 || index >= this.mRenders.size()) {
            return null;
        }
        return (Render) this.mRenders.get(index);
    }

    public boolean draw(DrawAttribute attri) {
        if (this.mRenders.isEmpty()) {
            return false;
        }
        for (Render render : this.mRenders) {
            if (render.draw(attri)) {
                return true;
            }
        }
        return false;
    }

    public void beginBindFrameBuffer(FrameBuffer frameBuffer) {
        GLES20.glBindFramebuffer(36160, frameBuffer.getId());
        GLES20.glFramebufferTexture2D(36160, 36064, 3553, frameBuffer.getTexture().getId(), 0);
        this.mGLCanvas.getState().pushState();
        this.mGLCanvas.getState().indentityAllM();
        this.mOldViewportWidth = this.mViewportWidth;
        this.mOldViewportHeight = this.mViewportHeight;
        this.mParentFrameBufferIdOld = this.mParentFrameBufferId;
        setParentFrameBufferId(frameBuffer.getId());
        setViewportSize(frameBuffer.getWidth(), frameBuffer.getHeight());
    }

    public void endBindFrameBuffer() {
        this.mGLCanvas.getState().popState();
        GLES20.glBindFramebuffer(36160, this.mParentFrameBufferIdOld);
        setViewportSize(this.mOldViewportWidth, this.mOldViewportHeight);
        setParentFrameBufferId(this.mParentFrameBufferIdOld);
    }

    public void setViewportSize(int w, int h) {
        super.setViewportSize(w, h);
        if (!this.mRenders.isEmpty()) {
            for (Render render : this.mRenders) {
                if (render != null) {
                    render.setViewportSize(w, h);
                }
            }
        }
    }

    public void setEffectRangeAttribute(EffectRectAttribute attribute) {
        super.setEffectRangeAttribute(attribute);
        if (!this.mRenders.isEmpty()) {
            for (Render render : this.mRenders) {
                if (render != null) {
                    render.setEffectRangeAttribute(attribute);
                }
            }
        }
    }

    public void setPreviewSize(int w, int h) {
        super.setPreviewSize(w, h);
        if (!this.mRenders.isEmpty()) {
            for (Render render : this.mRenders) {
                if (render != null) {
                    render.setPreviewSize(w, h);
                }
            }
        }
    }

    public void setOrientation(int orientation) {
        if (this.mOrientation != orientation) {
            super.setOrientation(orientation);
            if (!this.mRenders.isEmpty()) {
                for (Render render : this.mRenders) {
                    if (render != null) {
                        render.setOrientation(orientation);
                    }
                }
            }
        }
    }

    public void setJpegOrientation(int orientation) {
        if (this.mJpegOrientation != orientation) {
            super.setJpegOrientation(orientation);
            if (!this.mRenders.isEmpty()) {
                for (Render render : this.mRenders) {
                    if (render != null) {
                        render.setJpegOrientation(orientation);
                    }
                }
            }
        }
    }

    public void setMirror(boolean mirror) {
        super.setMirror(mirror);
        if (!this.mRenders.isEmpty()) {
            for (Render render : this.mRenders) {
                if (render != null) {
                    render.setMirror(mirror);
                }
            }
        }
    }

    protected void setParentFrameBufferId(int id) {
        super.setParentFrameBufferId(id);
        if (!this.mRenders.isEmpty()) {
            for (Render child : this.mRenders) {
                if (child != null) {
                    child.setParentFrameBufferId(id);
                }
            }
        }
    }

    public boolean isNeedInit(int index) {
        boolean z = true;
        if (index > -1) {
            if (index == 0) {
                z = false;
            } else if (this.mRenders.size() > index && this.mRenders.get(index) != null) {
                z = false;
            }
            return z;
        }
        int i = 0;
        while (i < this.mRenders.size()) {
            if (this.mRenders.get(i) == null && i != 0 && (V6ModulePicker.isCameraModule() || i == EffectController.sBackgroundBlurIndex)) {
                return true;
            }
            i++;
        }
        return false;
    }

    public void addPartRender(Render render) {
        this.mPartRenders.add(render);
    }

    public void clearPartRenders() {
        this.mPartRenders.clear();
    }

    public Render getPartRender(int index) {
        if (index < 0 || index >= this.mPartRenders.size()) {
            return null;
        }
        return (Render) this.mPartRenders.get(index);
    }

    public boolean isPartComplete(int wholeSize) {
        return this.mPartRenders.size() == wholeSize;
    }
}
