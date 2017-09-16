package com.android.camera.aosp_porting.animation;

import android.view.animation.Interpolator;

public class CubicEaseInOutInterpolator implements Interpolator {
    public float getInterpolation(float t) {
        t *= 2.0f;
        if (t < 1.0f) {
            return ((0.5f * t) * t) * t;
        }
        t -= 2.0f;
        return (((t * t) * t) + 2.0f) * 0.5f;
    }
}
