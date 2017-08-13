package com.android.camera.effect.draw_mode;

import com.android.gallery3d.ui.ExtTexture;

public class DrawExtTexAttribute extends DrawAttribute {
    public boolean mEffectPopup = false;
    public ExtTexture mExtTexture;
    public int mHeight;
    public float[] mTextureTransform;
    public int mWidth;
    public int mX;
    public int mY;

    public DrawExtTexAttribute(ExtTexture texture, float[] textureTransform, int x, int y, int w, int h) {
        this.mX = x;
        this.mY = y;
        this.mWidth = w;
        this.mHeight = h;
        this.mExtTexture = texture;
        this.mTextureTransform = textureTransform;
        this.mTarget = 8;
    }

    public DrawExtTexAttribute init(ExtTexture texture, float[] textureTransform, int x, int y, int w, int h) {
        this.mX = x;
        this.mY = y;
        this.mWidth = w;
        this.mHeight = h;
        this.mExtTexture = texture;
        this.mTextureTransform = textureTransform;
        this.mTarget = 8;
        return this;
    }

    public DrawExtTexAttribute(boolean isEffectPopup) {
        this.mEffectPopup = isEffectPopup;
    }
}
