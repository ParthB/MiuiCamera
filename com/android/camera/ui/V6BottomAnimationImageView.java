package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;

public class V6BottomAnimationImageView extends RotateImageView implements AnimationListener, V6FunctionUI {
    private Animation mAnimationIn;
    private Runnable mAnimationInCallback;
    private Animation mAnimationOut;
    private Runnable mAnimationOutCallback;
    private boolean mDoingAnimationIn;
    private boolean mDoingAnimationOut;
    protected MessageDispacher mMessageDispacher;

    public V6BottomAnimationImageView(Context context) {
        super(context);
    }

    public V6BottomAnimationImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mAnimationIn = new AlphaAnimation(0.0f, 1.0f);
        this.mAnimationIn.setDuration(150);
        this.mAnimationIn.setInterpolator(new DecelerateInterpolator());
        this.mAnimationIn.setAnimationListener(this);
        this.mAnimationOut = new AlphaAnimation(1.0f, 0.0f);
        this.mAnimationOut.setDuration(150);
        this.mAnimationOut.setInterpolator(new DecelerateInterpolator());
        this.mAnimationOut.setAnimationListener(this);
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
            this.mDoingAnimationIn = true;
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
                    V6BottomAnimationImageView.this.setVisibility(8);
                }
            };
        } else {
            this.mAnimationOutCallback = callback;
        }
        if (getVisibility() == 0) {
            clearAnimation();
            this.mAnimationOut.setDuration((long) duration);
            startAnimation(this.mAnimationOut);
            this.mDoingAnimationOut = true;
        } else if (this.mAnimationOutCallback != null) {
            this.mAnimationOutCallback.run();
            this.mAnimationOutCallback = null;
        }
    }

    public void onCreate() {
    }

    public void onCameraOpen() {
    }

    public void onResume() {
        clearAnimation();
    }

    public void onPause() {
    }

    public void setMessageDispacher(MessageDispacher p) {
        this.mMessageDispacher = p;
    }

    public void enableControls(boolean enable) {
        setEnabled(enable);
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
        this.mDoingAnimationIn = false;
        this.mDoingAnimationOut = false;
    }

    public boolean isVisibleWithAnimationDone() {
        return getVisibility() == 0 && !this.mDoingAnimationIn;
    }

    public boolean isInVisibleForUser() {
        return getVisibility() == 0 ? this.mDoingAnimationOut : true;
    }

    public void onAnimationRepeat(Animation animation) {
    }
}
