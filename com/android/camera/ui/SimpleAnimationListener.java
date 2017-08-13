package com.android.camera.ui;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;

public class SimpleAnimationListener implements AnimationListener {
    private boolean mShowAnimation;
    private View mView;

    public SimpleAnimationListener(View view, boolean show) {
        this.mView = view;
        this.mShowAnimation = show;
    }

    public void onAnimationStart(Animation animation) {
        if (this.mShowAnimation) {
            this.mView.setVisibility(0);
        }
    }

    public void onAnimationEnd(Animation animation) {
        if (!this.mShowAnimation && this.mView != null) {
            this.mView.setVisibility(8);
        }
    }

    public void onAnimationRepeat(Animation animation) {
    }
}
