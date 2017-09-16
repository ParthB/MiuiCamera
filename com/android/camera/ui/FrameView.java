package com.android.camera.ui;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public abstract class FrameView extends View implements FocusIndicator, Rotatable {
    private final boolean LOGV = true;
    protected boolean mIsBigEnoughRect;
    protected Matrix mMatrix = new Matrix();
    protected int mOrientation;
    protected boolean mPause;

    public abstract RectF getFocusRect();

    public FrameView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean isNeedExposure() {
        return this.mIsBigEnoughRect;
    }

    public void setOrientation(int orientation, boolean animation) {
        this.mOrientation = orientation;
        invalidate();
    }

    public boolean faceExists() {
        return false;
    }

    public void showStart() {
    }

    public void showSuccess() {
    }

    public void showFail() {
    }

    public void clear() {
    }

    public void pause() {
        this.mPause = true;
    }

    public void resume() {
        this.mPause = false;
    }
}
