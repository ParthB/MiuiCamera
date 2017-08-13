package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;

public class V6BottomAnimationViewGroup extends V6RelativeLayout implements AnimationListener {
    private Animation mAnimationIn;
    private Runnable mAnimationInCallback;
    private Animation mAnimationOut;
    private Runnable mAnimationOutCallback;

    public V6BottomAnimationViewGroup(Context context) {
        super(context);
    }

    public V6BottomAnimationViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mAnimationIn = new AlphaAnimation(0.0f, 1.0f);
        this.mAnimationIn.setDuration(200);
        this.mAnimationIn.setInterpolator(new DecelerateInterpolator());
        this.mAnimationIn.setAnimationListener(this);
        this.mAnimationOut = new AlphaAnimation(1.0f, 0.0f);
        this.mAnimationOut.setDuration(200);
        this.mAnimationOut.setInterpolator(new DecelerateInterpolator());
        this.mAnimationOut.setAnimationListener(this);
    }

    public void onResume() {
        super.onResume();
        clearAnimation();
    }

    public void animateIn(Runnable callback, int duration, boolean changeVisibility) {
        if (this.mAnimationInCallback != null) {
            this.mAnimationInCallback.run();
            this.mAnimationInCallback = null;
        }
        this.mAnimationInCallback = callback;
        if (changeVisibility) {
            setVisibility(0);
        }
        if (getVisibility() == 0) {
            clearAnimation();
            this.mAnimationIn.setDuration((long) duration);
            startAnimation(this.mAnimationIn);
        } else if (this.mAnimationInCallback != null) {
            this.mAnimationInCallback.run();
            this.mAnimationInCallback = null;
        }
    }

    public void animateOut(final Runnable callback, int duration, boolean changeVisibility) {
        if (this.mAnimationOutCallback != null) {
            this.mAnimationOutCallback.run();
            this.mAnimationOutCallback = null;
        }
        if (changeVisibility) {
            this.mAnimationOutCallback = new Runnable() {
                public void run() {
                    if (callback != null) {
                        callback.run();
                    }
                    V6BottomAnimationViewGroup.this.setVisibility(8);
                }
            };
        } else {
            this.mAnimationOutCallback = callback;
        }
        if (getVisibility() == 0) {
            clearAnimation();
            this.mAnimationOut.setDuration((long) duration);
            startAnimation(this.mAnimationOut);
        } else if (this.mAnimationOutCallback != null) {
            this.mAnimationOutCallback.run();
            this.mAnimationOutCallback = null;
        }
    }

    public void onAnimationStart(Animation animation) {
    }

    public void onAnimationEnd(Animation animation) {
        if (this.mAnimationIn == animation) {
            if (this.mAnimationInCallback != null) {
                this.mAnimationInCallback.run();
                this.mAnimationInCallback = null;
            }
        } else if (this.mAnimationOut == animation && this.mAnimationOutCallback != null) {
            this.mAnimationOutCallback.run();
            this.mAnimationOutCallback = null;
        }
    }

    public void onAnimationRepeat(Animation animation) {
    }
}
