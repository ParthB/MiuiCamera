package com.android.camera;

import android.graphics.Color;
import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.camera.effect.draw_mode.FillRectAttribute;
import com.android.gallery3d.ui.GLCanvas;

public class ModuleAnimManager {
    private float mAnimDuration;
    private long mAnimStartTime;
    private int mAnimState;
    private Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

    public void animateStartHide() {
        this.mAnimState = 1;
        this.mAnimDuration = 300.0f;
        this.mAnimStartTime = SystemClock.uptimeMillis();
    }

    public void animateStartShow() {
        this.mAnimState = 3;
        this.mAnimDuration = 0.0f;
        this.mAnimStartTime = SystemClock.uptimeMillis();
    }

    public void clearAnimation() {
        this.mAnimState = 0;
        this.mAnimDuration = 0.0f;
    }

    public boolean drawAnimation(GLCanvas canvas, int x, int y, int width, int height) {
        boolean z;
        long timeDiff = SystemClock.uptimeMillis() - this.mAnimStartTime;
        if (((float) timeDiff) > this.mAnimDuration) {
            if (this.mAnimState == 3) {
                this.mAnimState = 0;
                this.mAnimDuration = 0.0f;
                return false;
            } else if (this.mAnimState == 1) {
                this.mAnimState = 2;
            }
        }
        int alpha = 0;
        float f = this.mAnimDuration != 0.0f ? ((float) timeDiff) / this.mAnimDuration : 0.0f;
        switch (this.mAnimState) {
            case 1:
                alpha = (int) (this.mInterpolator.getInterpolation(f) * 240.0f);
                break;
            case 2:
                alpha = 240;
                break;
            case 3:
                alpha = (int) (this.mInterpolator.getInterpolation(1.0f - f) * 240.0f);
                break;
        }
        canvas.draw(new FillRectAttribute((float) x, (float) y, (float) width, (float) height, Color.argb(alpha, 0, 0, 0)));
        if (this.mAnimState != 2) {
            z = true;
        } else {
            z = false;
        }
        return z;
    }
}
