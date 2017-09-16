package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class AnimationImageView extends RotateImageView implements V6FunctionUI, AnimateView {
    private boolean mIsEnable;
    protected MessageDispacher mMessageDispacher;

    private class AnimationImageViewListener extends SimpleAnimationListener {
        public AnimationImageViewListener(AnimationImageView view, boolean show) {
            super(view, show);
        }

        public void onAnimationStart(Animation animation) {
            super.onAnimationStart(animation);
            AnimationImageView.this.setEnabled(false);
        }

        public void onAnimationEnd(Animation animation) {
            super.onAnimationEnd(animation);
            AnimationImageView.this.setEnabled(AnimationImageView.this.mIsEnable);
        }
    }

    public AnimationImageView(Context context) {
        super(context);
    }

    public AnimationImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onCreate() {
    }

    public void onResume() {
    }

    public void onPause() {
    }

    public void setMessageDispacher(MessageDispacher p) {
        this.mMessageDispacher = p;
    }

    public void onCameraOpen() {
    }

    public void enableControls(boolean enable) {
        if (getAnimation() == null || getAnimation().hasEnded()) {
            setEnabled(enable);
        }
        this.mIsEnable = enable;
    }

    public void hide(boolean animation) {
        if (animation) {
            clearAnimation();
            startAnimation(initAnimation(false));
            setEnabled(false);
            return;
        }
        setVisibility(8);
    }

    public void show(boolean animation) {
        setVisibility(0);
        if (animation) {
            clearAnimation();
            startAnimation(initAnimation(true));
            setEnabled(false);
        }
    }

    private Animation initAnimation(boolean show) {
        if (show) {
            Animation animation = AnimationUtils.loadAnimation(this.mContext, R.anim.show);
            animation.setAnimationListener(new AnimationImageViewListener(this, true));
            return animation;
        }
        animation = AnimationUtils.loadAnimation(this.mContext, R.anim.dismiss);
        animation.setAnimationListener(new AnimationImageViewListener(this, false));
        return animation;
    }
}
