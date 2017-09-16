package com.android.camera.aosp_porting.animation;

import android.view.animation.Interpolator;

public class CubicEaseOutInterpolator implements Interpolator {
    public float getInterpolation(float t) {
        t -= 1.0f;
        return ((t * t) * t) + 1.0f;
    }
}
