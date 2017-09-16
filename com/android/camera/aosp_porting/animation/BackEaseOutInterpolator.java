package com.android.camera.aosp_porting.animation;

import android.view.animation.Interpolator;

public class BackEaseOutInterpolator implements Interpolator {
    private final float mOvershot;

    public BackEaseOutInterpolator() {
        this(0.0f);
    }

    public BackEaseOutInterpolator(float overshot) {
        this.mOvershot = overshot;
    }

    public float getInterpolation(float t) {
        float s = this.mOvershot == 0.0f ? 1.70158f : this.mOvershot;
        t -= 1.0f;
        return ((t * t) * (((s + 1.0f) * t) + s)) + 1.0f;
    }
}
