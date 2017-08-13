package com.android.camera;

import android.graphics.Color;
import android.os.SystemClock;
import com.android.camera.effect.draw_mode.DrawBasicTexAttribute;
import com.android.camera.effect.draw_mode.FillRectAttribute;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.RawTexture;

public class CaptureAnimManager {
    private long mAnimStartTime;
    private int mAnimType;
    private int mDrawHeight;
    private int mDrawWidth;
    private float mX;
    private float mY;

    public void startAnimation(int x, int y, int w, int h) {
        this.mAnimStartTime = SystemClock.uptimeMillis();
        this.mDrawWidth = w;
        this.mDrawHeight = h;
        this.mX = (float) x;
        this.mY = (float) y;
    }

    public void animateSlide() {
        if (this.mAnimType == 2) {
            this.mAnimType = 3;
            this.mAnimStartTime = SystemClock.uptimeMillis();
        }
    }

    public void animateHold() {
        this.mAnimType = 2;
    }

    public void clearAnimation() {
        this.mAnimType = 0;
    }

    public void animateHoldAndSlide() {
        this.mAnimType = 1;
    }

    public boolean drawAnimation(GLCanvas canvas, CameraScreenNail preview, RawTexture review) {
        long timeDiff = SystemClock.uptimeMillis() - this.mAnimStartTime;
        if (this.mAnimType == 3 && timeDiff > 120) {
            return false;
        }
        if (this.mAnimType == 1 && timeDiff > 140) {
            return false;
        }
        int animStep = this.mAnimType;
        if (this.mAnimType == 1) {
            animStep = timeDiff < 20 ? 2 : 3;
            if (animStep == 3) {
                timeDiff -= 20;
            }
        }
        if (animStep == 2) {
            canvas.draw(new DrawBasicTexAttribute(review, (int) this.mX, (int) this.mY, this.mDrawWidth, this.mDrawHeight));
        } else if (animStep != 3) {
            return false;
        } else {
            canvas.draw(new DrawBasicTexAttribute(review, (int) this.mX, (int) this.mY, this.mDrawWidth, this.mDrawHeight));
            canvas.draw(new FillRectAttribute(this.mX, this.mY, (float) this.mDrawWidth, (float) this.mDrawHeight, Color.argb(178, 0, 0, 0)));
        }
        return true;
    }
}
