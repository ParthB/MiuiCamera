package com.android.camera;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera.Area;
import android.support.v7.recyclerview.R;
import java.util.List;

public abstract class FocusManagerAbstract {
    protected final int FOCUS_AREA_HEIGHT = CameraAppImpl.getAndroidContext().getResources().getDimensionPixelSize(R.dimen.focus_area_height);
    protected final float FOCUS_AREA_SCALE = 1.0f;
    protected final int FOCUS_AREA_WIDTH = CameraAppImpl.getAndroidContext().getResources().getDimensionPixelSize(R.dimen.focus_area_width);
    protected final float METERING_AREA_SCALE = 1.8f;
    protected boolean mCancelAutoFocusIfMove;
    protected int mDisplayOrientation;
    protected List<Area> mFocusArea;
    protected boolean mInitialized = false;
    protected Matrix mMatrix = new Matrix();
    protected List<Area> mMeteringArea;
    protected boolean mMirror;
    protected Matrix mPreviewChangeMatrix = new Matrix();
    protected int mPreviewHeight;
    protected int mPreviewWidth;
    protected int mRenderHeight;
    protected int mRenderWidth;
    protected int mState = 0;

    protected void setMatrix() {
        if (this.mPreviewWidth != 0 && this.mPreviewHeight != 0) {
            Matrix matrix = new Matrix();
            Util.prepareMatrix(matrix, this.mMirror, this.mDisplayOrientation, this.mRenderWidth, this.mRenderHeight, this.mPreviewWidth / 2, this.mPreviewHeight / 2);
            matrix.invert(this.mMatrix);
            this.mPreviewChangeMatrix.reset();
            this.mPreviewChangeMatrix.postTranslate((float) ((-this.mPreviewWidth) / 2), (float) ((-this.mPreviewHeight) / 2));
            this.mPreviewChangeMatrix.postScale(0.6f, 0.6f);
            this.mPreviewChangeMatrix.postTranslate((float) (this.mPreviewWidth / 2), (float) (this.mPreviewHeight / 2));
            this.mInitialized = true;
        }
    }

    public void setRenderSize(int width, int height) {
        if (width != this.mRenderWidth || height != this.mRenderHeight) {
            this.mRenderWidth = width;
            this.mRenderHeight = height;
            setMatrix();
        }
    }

    protected void calculateTapArea(int focusWidth, int focusHeight, float areaMultiple, int x, int y, int previewWidth, int previewHeight, Rect rect) {
        int areaWidth = (int) (((float) focusWidth) * areaMultiple);
        int areaHeight = (int) (((float) focusHeight) * areaMultiple);
        int left = Util.clamp(x - (areaWidth / 2), 0, previewWidth - areaWidth);
        int top = Util.clamp(y - (areaHeight / 2), 0, previewHeight - areaHeight);
        RectF rectF = new RectF((float) left, (float) top, (float) (left + areaWidth), (float) (top + areaHeight));
        this.mMatrix.mapRect(rectF);
        Util.rectFToRect(rectF, rect);
    }

    public void setMirror(boolean mirror) {
        this.mMirror = mirror;
        setMatrix();
    }

    public void setDisplayOrientation(int displayOrientation) {
        this.mDisplayOrientation = displayOrientation;
        setMatrix();
    }
}
