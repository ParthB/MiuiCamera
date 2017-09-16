package com.android.camera.effect.draw_mode;

public class FillRectAttribute extends DrawAttribute {
    public int mColor;
    public float mHeight;
    public float mWidth;
    public float mX;
    public float mY;

    public FillRectAttribute(float x, float y, float w, float h, int color) {
        this.mX = x;
        this.mY = y;
        this.mWidth = w;
        this.mHeight = h;
        this.mColor = color;
        this.mTarget = 4;
    }
}
