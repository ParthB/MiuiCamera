package com.android.camera.aosp_porting.animation;

import android.view.animation.Interpolator;

public class SineEaseOutInterpolator implements Interpolator {
    public float getInterpolation(float t) {
        return (float) Math.sin(((double) t) * 1.5707963267948966d);
    }
}
