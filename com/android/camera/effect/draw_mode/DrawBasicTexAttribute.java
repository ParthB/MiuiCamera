package com.android.camera.effect.draw_mode;

import com.android.gallery3d.ui.BasicTexture;

public class DrawBasicTexAttribute extends DrawAttribute {
    public BasicTexture mBasicTexture;
    public int mHeight;
    public boolean mIsSnapshot;
    public int mWidth;
    public int mX;
    public int mY;

    public DrawBasicTexAttribute(BasicTexture texture, int x, int y, int w, int h) {
        this.mX = x;
        this.mY = y;
        this.mWidth = w;
        this.mHeight = h;
        this.mBasicTexture = texture;
        this.mTarget = 5;
        this.mIsSnapshot = false;
    }

    public DrawBasicTexAttribute init(BasicTexture texture, int x, int y, int w, int h) {
        this.mX = x;
        this.mY = y;
        this.mWidth = w;
        this.mHeight = h;
        this.mBasicTexture = texture;
        this.mTarget = 5;
        this.mIsSnapshot = false;
        return this;
    }

    public DrawBasicTexAttribute init(BasicTexture texture, int x, int y, int w, int h, boolean isSnapshot) {
        this.mX = x;
        this.mY = y;
        this.mWidth = w;
        this.mHeight = h;
        this.mBasicTexture = texture;
        this.mTarget = 5;
        this.mIsSnapshot = isSnapshot;
        return this;
    }
}
