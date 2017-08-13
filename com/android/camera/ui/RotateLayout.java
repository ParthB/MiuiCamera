package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class RotateLayout extends ViewGroup implements Rotatable {
    protected View mChild;
    private int mOrientation;

    public RotateLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundResource(17170445);
    }

    protected void onFinishInflate() {
        this.mChild = getChildAt(0);
        this.mChild.setPivotX(0.0f);
        this.mChild.setPivotY(0.0f);
    }

    protected void onLayout(boolean change, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        switch (this.mOrientation) {
            case 0:
            case 180:
                this.mChild.layout(0, 0, width, height);
                return;
            case 90:
            case 270:
                this.mChild.layout(0, 0, height, width);
                return;
            default:
                return;
        }
    }

    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = 0;
        int h = 0;
        switch (this.mOrientation) {
            case 0:
            case 180:
                measureChild(this.mChild, widthSpec, heightSpec);
                w = this.mChild.getMeasuredWidth();
                h = this.mChild.getMeasuredHeight();
                break;
            case 90:
            case 270:
                measureChild(this.mChild, heightSpec, widthSpec);
                w = this.mChild.getMeasuredHeight();
                h = this.mChild.getMeasuredWidth();
                break;
        }
        setMeasuredDimension(w, h);
        switch (this.mOrientation) {
            case 0:
                this.mChild.setTranslationX(0.0f);
                this.mChild.setTranslationY(0.0f);
                break;
            case 90:
                this.mChild.setTranslationX(0.0f);
                this.mChild.setTranslationY((float) h);
                break;
            case 180:
                this.mChild.setTranslationX((float) w);
                this.mChild.setTranslationY((float) h);
                break;
            case 270:
                this.mChild.setTranslationX((float) w);
                this.mChild.setTranslationY(0.0f);
                break;
        }
        this.mChild.setRotation((float) (-this.mOrientation));
    }

    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public void setOrientation(int orientation, boolean animation) {
        orientation %= 360;
        if (this.mOrientation != orientation) {
            this.mOrientation = orientation;
            requestLayout();
        }
    }
}
