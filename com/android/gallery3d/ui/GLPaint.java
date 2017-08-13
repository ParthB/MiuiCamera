package com.android.gallery3d.ui;

public class GLPaint {
    private int mColor = 0;
    private float mLineWidth = 1.0f;

    public void setColor(int color) {
        this.mColor = color;
    }

    public int getColor() {
        return this.mColor;
    }

    public void setLineWidth(float width) {
        Utils.assertTrue(width >= 0.0f);
        this.mLineWidth = width;
    }

    public float getLineWidth() {
        return this.mLineWidth;
    }
}
