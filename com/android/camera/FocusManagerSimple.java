package com.android.camera;

import android.graphics.Rect;
import android.hardware.Camera.Area;
import java.util.ArrayList;
import java.util.List;

public class FocusManagerSimple extends FocusManagerAbstract {
    public FocusManagerSimple(int previewWidth, int previewHeight, boolean mirror, int displayOrientation) {
        this.mDisplayOrientation = displayOrientation;
        this.mMirror = mirror;
        setPreviewSize(previewWidth, previewHeight);
        this.mInitialized = true;
    }

    public void setPreviewSize(int previewWidth, int previewHeight) {
        if (this.mPreviewWidth != previewWidth || this.mPreviewHeight != previewHeight) {
            this.mPreviewWidth = previewWidth;
            this.mPreviewHeight = previewHeight;
            setMatrix();
        }
    }

    public void resetFocused() {
        this.mState = 0;
    }

    public void focusPoint() {
        this.mState = 1;
        this.mCancelAutoFocusIfMove = false;
    }

    public boolean isInValidFocus() {
        return this.mState == 0 || this.mState == 4;
    }

    public List<Area> getFocusArea(int x, int y, int focusWidth, int focusHeight) {
        if (!this.mInitialized) {
            return null;
        }
        List<Area> focusArea = new ArrayList();
        focusArea.add(new Area(new Rect(), 1));
        calculateTapArea(focusWidth, focusHeight, 1.0f, x, y, this.mPreviewWidth, this.mPreviewHeight, ((Area) focusArea.get(0)).rect);
        return focusArea;
    }

    public int getDefaultFocusAreaWidth() {
        return this.FOCUS_AREA_WIDTH;
    }

    public int getDefaultFocusAreaHeight() {
        return this.FOCUS_AREA_HEIGHT;
    }

    public List<Area> getMeteringsArea(int x, int y, int focusWidth, int focusHeight) {
        if (!this.mInitialized) {
            return null;
        }
        List<Area> meteringArea = new ArrayList();
        meteringArea.add(new Area(new Rect(), 1));
        calculateTapArea(focusWidth, focusHeight, 1.8f, x, y, this.mPreviewWidth, this.mPreviewHeight, ((Area) meteringArea.get(0)).rect);
        return meteringArea;
    }

    public void onDeviceKeepMoving() {
        if (this.mState == 3 || this.mState == 4) {
            this.mState = 0;
        }
    }

    public void onAutoFocus(boolean focused) {
        this.mState = focused ? 3 : 4;
        this.mCancelAutoFocusIfMove = true;
    }

    public boolean isNeedCancelAutoFocus() {
        return this.mCancelAutoFocusIfMove;
    }

    public boolean canRecord() {
        if (!isFocusing()) {
            return true;
        }
        this.mState = 2;
        return false;
    }

    public boolean isFocusing() {
        return this.mState == 1 || this.mState == 2;
    }

    public boolean isFocusingSnapOnFinish() {
        return this.mState == 2;
    }

    public boolean canAutoFocus() {
        return this.mInitialized && this.mState != 2;
    }

    public void cancelAutoFocus() {
        this.mState = 0;
        this.mCancelAutoFocusIfMove = false;
    }
}
