package com.android.camera.effect.draw_mode;

import com.android.gallery3d.ui.BasicTexture;

public class DrawBlurTexAttribute extends DrawBasicTexAttribute {
    public DrawBlurTexAttribute(BasicTexture texture, int x, int y, int w, int h) {
        super(texture, x, y, w, h);
        this.mTarget = 10;
    }

    public DrawBlurTexAttribute init(BasicTexture texture, int x, int y, int w, int h) {
        super.init(texture, x, y, w, h);
        this.mTarget = 10;
        return this;
    }

    public DrawBlurTexAttribute init(BasicTexture texture, int x, int y, int w, int h, boolean isSnapshot) {
        super.init(texture, x, y, w, h, isSnapshot);
        this.mTarget = 10;
        return this;
    }
}
