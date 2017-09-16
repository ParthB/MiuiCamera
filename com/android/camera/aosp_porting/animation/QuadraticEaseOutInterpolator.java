package com.android.camera.aosp_porting.animation;

import android.view.animation.Interpolator;

public class QuadraticEaseOutInterpolator implements Interpolator {
    public float getInterpolation(float t) {
        return (-t) * (t - 2.0f);
    }
}
